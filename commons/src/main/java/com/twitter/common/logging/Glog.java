/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.common.logging;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A utility that can format log records to match the format generated by glog:
 * <pre>
 * I0218 17:36:47.461 (source) (message)
 * </pre>
 */
public final class Glog {

  /**
   * Classifies the importance of a log message.
   */
  public enum Level {

    /**
     * Indicates the message's classification is unknown.  This most likely indicates a
     * configuration or programming error that can be corrected by mapping the underlying log
     * system's level appropriately.
     */
    UNKNOWN('U'),

    /**
     * Indicates the message is for debugging purposes only.
     */
    DEBUG('D'),

    /**
     * Indicates a message of general interest.
     */
    INFO('I'),

    /**
     * Indicates a warning message likely worth of attention.
     */
    WARNING('W'),

    /**
     * Indicates an unexpected error.
     */
    ERROR('E'),

    /**
     * Indicates a fatal exception generally paired with actions to shut down the errored process.
     */
    FATAL('F');

    final char label;

    private Level(char label) {
      this.label = label;
    }
  }

  /**
   * An object that can provide details of a log record.
   *
   * @param <T> The type of log record the formatter handles.
   */
  public interface Formatter<T> {

    /**
     * Gets the message contained in the log record.
     *
     * @param record The record to extract a message from.
     * @return The formatted message.
     */
    String getMessage(T record);

    /**
     * Gets the class name of the class that sent the log record for logging.
     *
     * @param record The record to extract a producing class name from.
     * @return The producing class if known; otherwise {@code null}.
     */
    @Nullable
    String getClassName(T record);

    /**
     * Gets the name of the method of within the class that sent the log record for logging.
     *
     * @param record The record to extract a producing method name from.
     * @return The producing method name if known; otherwise {@code null}.
     */
    @Nullable
    String getMethodName(T record);

    /**
     * Gets the level of the log record.
     *
     * @param record The record to extract a log level from.
     * @return The record's log level. Can be {@code null} or {@link Level#UNKNOWN} if unknown.
     */
    @Nullable
    Level getLevel(T record);

    /**
     * Gets the timestamp in milliseconds since the epoch when the log record was generated.
     *
     * @param record The record to extract a time stamp from.
     * @return The log record's birth date.
     */
    long getTimeStamp(T record);

    /**
     * Gets the id of the thread that generated the log record.
     *
     * @param record The record to extract a thread id from.
     * @return The id of the thread that generated the log record.
     */
    long getThreadId(T record);

    /**
     * Gets the exception associated with the log record if any.
     *
     * @param record The record to extract an exception from.
     * @return The exception associated with the log record; may be {@code null}.
     */
    @Nullable
    Throwable getThrowable(T record);
  }

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormat.forPattern("MMdd HH:mm:ss.SSS").withZone(DateTimeZone.UTC);

  private static final int BASE_MESSAGE_LENGTH =
      1     // Level char.
      + 4   // Month + day
      + 1   // space
      + 12  // Timestamp
      + 1   // space
      + 6   // THREAD
      + 4   // Room for thread ID.
      + 1;  // space

  /**
   * Converts the given log record into a glog format log line using the given formatter.
   *
   * @param formatter A formatter that understands how to unpack the given log record.
   * @param record A structure containing log data.
   * @param <T> The type of log record.
   * @return A glog formatted log line.
   */
  public static <T> String formatRecord(Formatter<T> formatter, T record) {
    String message = formatter.getMessage(record);
    int messageLength = BASE_MESSAGE_LENGTH
        + 2  // Colon and space
        + message.length();

    String className = formatter.getClassName(record);
    String methodName = null;
    if (className != null) {
      messageLength += className.length();
      methodName = formatter.getMethodName(record);
      if (methodName != null) {
        messageLength += 1;  // Period between class and method.
        messageLength += methodName.length();
      }
    }

    StringBuilder sb = new StringBuilder(messageLength)
        .append(Objects.firstNonNull(formatter.getLevel(record), Level.UNKNOWN).label)
        .append(DATE_TIME_FORMATTER.print(formatter.getTimeStamp(record)))
        .append(" THREAD")
        .append(formatter.getThreadId(record));

    if (className != null) {
      sb.append(' ').append(className);
      if (methodName != null) {
        sb.append('.').append(methodName);
      }
    }

    sb.append(": ").append(message);
    Throwable throwable = formatter.getThrowable(record);
    if (throwable != null) {
      sb.append('\n').append(Throwables.getStackTraceAsString(throwable));
    }

    return sb.append('\n').toString();
  }

  private Glog() {
    // utility
  }
}
