package com.example.executors;

import com.example.executors.virtual.ExecutorDiagnostics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Java 21 override of {@code com.example.executors.ExecutorFactory}: same public API as the base
 * implementation, but backed by virtual threads instead of a platform-thread pool. The jar's {@code
 * META-INF/versions/21} entry makes the JVM load this class instead of the base one whenever it
 * runs on 21+.
 */
public final class ExecutorFactory {

  private ExecutorFactory() {}

  public static ExecutorService create() {
    ThreadFactory factory = Thread.ofVirtual().factory();
    return Executors.newThreadPerTaskExecutor(
        r -> {
          Thread thread = factory.newThread(r);
          System.out.println("NOTICE: " + ExecutorDiagnostics.describe(thread));
          return thread;
        });
  }
}
