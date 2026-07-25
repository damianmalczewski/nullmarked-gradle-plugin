package com.example.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates the {@link ExecutorService} tasks should run on. This is the baseline implementation,
 * used on any JVM below 21: a cached platform-thread pool. Java 21+ runs the {@code
 * META-INF/versions/21} override instead, backed by virtual threads.
 */
public final class ExecutorFactory {

  private ExecutorFactory() {}

  public static ExecutorService create() {
    return Executors.newCachedThreadPool();
  }
}
