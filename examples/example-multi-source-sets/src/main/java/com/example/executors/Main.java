package com.example.executors;

import java.util.concurrent.ExecutorService;

/**
 * Compiled once, into the jar's base entry: prints which thread {@link ExecutorFactory} actually
 * handed back.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    ExecutorService executor = ExecutorFactory.create();
    try {
      executor
          .submit(
              () ->
                  System.out.println(
                      "Java " + Runtime.version().feature() + " -> " + Thread.currentThread()))
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      executor.shutdown();
    }
  }
}
