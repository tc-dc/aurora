package com.twitter.mesos.scheduler.thrift;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.thrift.TException;

import com.twitter.mesos.gen.CommitRecoveryResponse;
import com.twitter.mesos.gen.CreateJobResponse;
import com.twitter.mesos.gen.DeleteRecoveryTasksResponse;
import com.twitter.mesos.gen.DrainHostsResponse;
import com.twitter.mesos.gen.EndMaintenanceResponse;
import com.twitter.mesos.gen.FinishUpdateResponse;
import com.twitter.mesos.gen.ForceTaskStateResponse;
import com.twitter.mesos.gen.GetQuotaResponse;
import com.twitter.mesos.gen.Hosts;
import com.twitter.mesos.gen.JobConfiguration;
import com.twitter.mesos.gen.JobKey;
import com.twitter.mesos.gen.KillResponse;
import com.twitter.mesos.gen.ListBackupsResponse;
import com.twitter.mesos.gen.MaintenanceStatusResponse;
import com.twitter.mesos.gen.MesosAdmin;
import com.twitter.mesos.gen.MesosAdmin.Iface;
import com.twitter.mesos.gen.PerformBackupResponse;
import com.twitter.mesos.gen.PopulateJobResponse;
import com.twitter.mesos.gen.QueryRecoveryResponse;
import com.twitter.mesos.gen.Quota;
import com.twitter.mesos.gen.RestartShardsResponse;
import com.twitter.mesos.gen.RollbackShardsResponse;
import com.twitter.mesos.gen.ScheduleStatus;
import com.twitter.mesos.gen.ScheduleStatusResponse;
import com.twitter.mesos.gen.SessionKey;
import com.twitter.mesos.gen.SetQuotaResponse;
import com.twitter.mesos.gen.StageRecoveryResponse;
import com.twitter.mesos.gen.StartCronResponse;
import com.twitter.mesos.gen.StartMaintenanceResponse;
import com.twitter.mesos.gen.StartUpdateResponse;
import com.twitter.mesos.gen.TaskQuery;
import com.twitter.mesos.gen.UnloadRecoveryResponse;
import com.twitter.mesos.gen.UpdateResult;
import com.twitter.mesos.gen.UpdateShardsResponse;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A decorating scheduler thrift interface that logs requests.
 */
class LoggingThriftInterface implements MesosAdmin.Iface {

  private static final Logger LOG = Logger.getLogger(LoggingThriftInterface.class.getName());

  private static final MethodInterceptor UNHANDLED_EXCEPTION_INTERCEPTOR = new MethodInterceptor() {
    @Override public Object invoke(MethodInvocation invocation) throws Throwable {
      try {
        return invocation.proceed();
      } catch (RuntimeException e) {
        LOG.log(Level.WARNING, "Uncaught exception while handling "
            + invocation.getMethod().getName()
            + "(" + ImmutableList.copyOf(invocation.getArguments()) + ")", e);
        throw e;
      }
    }
  };

  /**
   * A {@literal @BindingAnnotation} that the delegate thrift interface should be bound against.
   */
  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @interface ThriftDelegate { }

  private final Iface delegate;

  @Inject
  LoggingThriftInterface(@ThriftDelegate Iface delegate) {
    this.delegate = checkNotNull(delegate);
  }

  private void logUserAction(SessionKey session, String messageTemplate, Object... formatArgs) {
    LOG.info(
        "Request by user " + session.getUser() + " to "
        + String.format(messageTemplate, formatArgs));
  }

  private void logUnauthenticatedAction(String messageTemplate, Object... formatArgs) {
    LOG.info(String.format(messageTemplate, formatArgs));
  }

  public static void bind(Binder binder, Class<? extends Iface> delegate) {
    checkNotNull(binder);
    checkNotNull(delegate);
    binder.bind(Iface.class).to(LoggingThriftInterface.class);
    binder.bind(LoggingThriftInterface.class).in(Singleton.class);
    binder.bind(Iface.class).annotatedWith(ThriftDelegate.class).to(delegate);
    binder.bindInterceptor(
        Matchers.only(LoggingThriftInterface.class),
        Matchers.any(),
        UNHANDLED_EXCEPTION_INTERCEPTOR);
  }

  @Override
  public SetQuotaResponse setQuota(String ownerRole, Quota quota, SessionKey session)
      throws TException {

    logUserAction(session, "setQuota|ownerRole: %s |quota: %s", ownerRole, quota);
    return delegate.setQuota(ownerRole, quota, session);
  }

  @Override
  public ForceTaskStateResponse forceTaskState(
      String taskId,
      ScheduleStatus status,
      SessionKey session) throws TException {

    logUserAction(session, "forceTaskState|taskId: %s |status: %s", taskId, status);
    return delegate.forceTaskState(taskId, status, session);
  }

  @Override
  public CreateJobResponse createJob(JobConfiguration description, SessionKey session)
      throws TException {

    logUserAction(session, "createJob|description: %s", description);
    return delegate.createJob(description, session);
  }

  @Override
  public PopulateJobResponse populateJobConfig(JobConfiguration description) throws TException {
    logUnauthenticatedAction("populateJobConfig|description: %s", description);
    return delegate.populateJobConfig(description);
  }

  @Override
  public StartCronResponse startCronJob(
      @Nullable String ownerRole,
      @Nullable String jobName,
      @Nullable JobKey job,
      SessionKey session) throws TException {

    logUserAction(session,
        "startCronJob|ownerRole: %s |jobName: %s |job: %s", ownerRole, jobName, job);

    return delegate.startCronJob(ownerRole, jobName, job, session);
  }

  @Override
  public StartUpdateResponse startUpdate(JobConfiguration updatedConfig, SessionKey session)
      throws TException {

    logUserAction(session, "startUpdate|updatedConfig: %s", updatedConfig);
    return delegate.startUpdate(updatedConfig, session);
  }

  @Override
  public UpdateShardsResponse updateShards(
      @Nullable String ownerRole,
      @Nullable String jobName,
      @Nullable JobKey job,
      Set<Integer> shards,
      String updateToken,
      SessionKey session) throws TException {

    logUserAction(session,
        "updateJob|ownerRole: %s |jobName: %s |job: %s |shards: %s",
        ownerRole, jobName, job, shards);
    return delegate.updateShards(ownerRole, jobName, job, shards, updateToken, session);
  }

  @Override
  public RollbackShardsResponse rollbackShards(
      @Nullable String ownerRole,
      @Nullable String jobName,
      @Nullable JobKey job,
      Set<Integer> shards,
      String updateToken,
      SessionKey session) throws TException {

    logUserAction(session,
        "rollbackShards|ownerRole: %s |jobName: %s |job: %s |shards: %s",
        ownerRole, jobName, job, shards);
    return delegate.rollbackShards(ownerRole, jobName, job, shards, updateToken, session);
  }

  @Override
  public FinishUpdateResponse finishUpdate(
      @Nullable String ownerRole,
      @Nullable String jobName,
      @Nullable JobKey job,
      UpdateResult updateResult,
      String updateToken,
      SessionKey session) throws TException {

    logUserAction(session,
        "finishUpdate|ownerRole: %s |jobName: %s |job: %s |updateResult: %s",
        ownerRole, jobName, job, updateResult);
    return delegate.finishUpdate(ownerRole, jobName, job, updateResult, updateToken, session);
  }

  @Override
  public RestartShardsResponse restartShards(
      @Nullable String role,
      @Nullable String jobName,
      @Nullable JobKey job,
      Set<Integer> shardIds,
      SessionKey session) throws TException {

    logUserAction(
        session,
        "restartShards|role: %s |jobName: %s |job: %s |shardIds: %s",
        role, jobName, job, shardIds);
    return delegate.restartShards(role, jobName, job, shardIds, session);
  }

  @Override
  public ScheduleStatusResponse getTasksStatus(TaskQuery query) throws TException {
    logUnauthenticatedAction("getTasksStatus|query: %s", query);
    return delegate.getTasksStatus(query);
  }

  @Override
  public KillResponse killTasks(TaskQuery query, SessionKey session) throws TException {
    logUserAction(session, "killTasks|query: %s", query);
    return delegate.killTasks(query, session);
  }

  @Override
  public GetQuotaResponse getQuota(String ownerRole) throws TException {
    logUnauthenticatedAction("getQuota|ownerRole: %s", ownerRole);
    return delegate.getQuota(ownerRole);
  }

  @Override
  public PerformBackupResponse performBackup(SessionKey session) throws TException {
    logUserAction(session, "performBackup");
    return delegate.performBackup(session);
  }

  @Override
  public ListBackupsResponse listBackups(SessionKey session) throws TException {
    logUserAction(session, "listBackups");
    return delegate.listBackups(session);
  }

  @Override
  public StageRecoveryResponse stageRecovery(String backupId, SessionKey session)
      throws TException {

    logUserAction(session, String.format("stageBackup|backupId: %s", backupId));
    return delegate.stageRecovery(backupId, session);
  }

  @Override
  public QueryRecoveryResponse queryRecovery(TaskQuery query, SessionKey session)
      throws TException {

    logUserAction(session, String.format("queryRecovery|query: %s", query));
    return delegate.queryRecovery(query, session);
  }

  @Override
  public DeleteRecoveryTasksResponse deleteRecoveryTasks(TaskQuery query, SessionKey session)
      throws TException {

    logUserAction(session, String.format("deleteRecoveryTasks|query: %s", query));
    return delegate.deleteRecoveryTasks(query, session);
  }

  @Override
  public CommitRecoveryResponse commitRecovery(SessionKey session) throws TException {
    logUserAction(session, "commitRecovery");
    return delegate.commitRecovery(session);
  }

  @Override
  public UnloadRecoveryResponse unloadRecovery(SessionKey session) throws TException {
    logUserAction(session, "unloadRecovery");
    return delegate.unloadRecovery(session);
  }

  @Override
  public StartMaintenanceResponse startMaintenance(
      Hosts hosts, SessionKey session) throws TException {

    logUserAction(session, "startMaintenance|hosts: %s", hosts);
    return delegate.startMaintenance(hosts, session);
  }

  @Override
  public DrainHostsResponse drainHosts(Hosts hostNames, SessionKey session) throws TException {
    logUserAction(session, "drainHosts|hostNames: %s", hostNames);
    return delegate.drainHosts(hostNames, session);
  }

  @Override
  public MaintenanceStatusResponse maintenanceStatus(
      Hosts hosts,
      SessionKey session) throws TException {

    logUserAction(session, "maintenanceStatus|hosts: %s", hosts);
    return delegate.maintenanceStatus(hosts, session);
  }

  @Override
  public EndMaintenanceResponse endMaintenance(
      Hosts hosts,
      SessionKey session) throws TException {

    logUserAction(session, "endMaintenance|hosts: %s", hosts);
    return delegate.endMaintenance(hosts, session);  }

}