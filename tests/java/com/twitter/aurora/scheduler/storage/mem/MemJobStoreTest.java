package com.twitter.aurora.scheduler.storage.mem;

import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;

import com.twitter.aurora.gen.JobConfiguration;
import com.twitter.aurora.gen.JobKey;
import com.twitter.aurora.scheduler.base.JobKeys;
import com.twitter.aurora.scheduler.storage.JobStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MemJobStoreTest {

  private static final String MANAGER_1 = "manager1";
  private static final String MANAGER_2 = "manager2";

  private static final JobConfiguration JOB_A = makeJob("a");
  private static final JobConfiguration JOB_B = makeJob("b");

  private static final JobKey KEY_A = JOB_A.getKey();
  private static final JobKey KEY_B = JOB_B.getKey();

  private JobStore.Mutable store;

  @Before
  public void setUp() {
    store = new MemJobStore();
  }

  @Test
  public void testJobStore() {
    assertNull(store.fetchJob(MANAGER_1, JobKeys.from("nobody", "nowhere", "noname")).orNull());
    assertEquals(ImmutableSet.<JobConfiguration>of(), store.fetchJobs(MANAGER_1));
    assertEquals(ImmutableSet.<String>of(), store.fetchManagerIds());

    store.saveAcceptedJob(MANAGER_1, JOB_A);
    assertEquals(JOB_A, store.fetchJob(MANAGER_1, KEY_A).orNull());
    assertEquals(ImmutableSet.of(JOB_A), store.fetchJobs(MANAGER_1));

    store.saveAcceptedJob(MANAGER_1, JOB_B);
    assertEquals(JOB_B, store.fetchJob(MANAGER_1, KEY_B).orNull());
    assertEquals(ImmutableSet.of(JOB_A, JOB_B), store.fetchJobs(MANAGER_1));
    assertEquals(ImmutableSet.of(MANAGER_1), store.fetchManagerIds());

    store.saveAcceptedJob(MANAGER_2, JOB_B);
    assertEquals(JOB_B, store.fetchJob(MANAGER_1, KEY_B).orNull());
    assertEquals(ImmutableSet.of(JOB_B), store.fetchJobs(MANAGER_2));
    assertEquals(ImmutableSet.of(MANAGER_1, MANAGER_2), store.fetchManagerIds());

    store.removeJob(KEY_B);
    assertEquals(ImmutableSet.of(JOB_A), store.fetchJobs(MANAGER_1));
    assertEquals(ImmutableSet.<JobConfiguration>of(), store.fetchJobs(MANAGER_2));

    store.deleteJobs();
    assertEquals(ImmutableSet.<JobConfiguration>of(), store.fetchJobs(MANAGER_1));
    assertEquals(ImmutableSet.<JobConfiguration>of(), store.fetchJobs(MANAGER_2));
  }

  @Test
  public void testJobStoreSameEnvironment() {
    JobConfiguration templateConfig = makeJob("labrat");
    JobConfiguration prod = templateConfig.deepCopy();
    prod.getKey().setEnvironment("prod");
    JobConfiguration staging = templateConfig.deepCopy();
    staging.getKey().setEnvironment("staging");

    store.saveAcceptedJob(MANAGER_1, prod);
    store.saveAcceptedJob(MANAGER_1, staging);

    assertNull(store.fetchJob(MANAGER_1, templateConfig.deepCopy().getKey().setEnvironment("test"))
        .orNull());
    assertEquals(prod, store.fetchJob(MANAGER_1, prod.getKey()).orNull());
    assertEquals(staging, store.fetchJob(MANAGER_1, staging.getKey()).orNull());

    store.removeJob(prod.getKey());
    assertNull(store.fetchJob(MANAGER_1, prod.getKey()).orNull());
    assertEquals(staging, store.fetchJob(MANAGER_1, staging.getKey()).orNull());
  }

  private static JobConfiguration makeJob(String name) {
    return new JobConfiguration().setKey(JobKeys.from("role-" + name, "env-" + name, name));
  }
}