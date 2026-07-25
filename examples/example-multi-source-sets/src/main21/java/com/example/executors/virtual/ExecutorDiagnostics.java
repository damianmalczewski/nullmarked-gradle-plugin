package com.example.executors.virtual;

import java.lang.Thread.State;

/**
 * Java 21-only package: has no counterpart under {@code src/main}, so it only exists in the jar's
 * {@code META-INF/versions/21} entry, never at the jar root. A JVM below 21 never sees this class
 * at all.
 */
public final class ExecutorDiagnostics {

  private ExecutorDiagnostics() {}

  public static String describe(Thread thread) {
    State state = thread.getState();
    return (thread.isVirtual() ? "virtual" : "platform") + " thread, state " + state;
  }
}
