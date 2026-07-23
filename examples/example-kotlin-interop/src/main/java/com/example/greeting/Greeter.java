package com.example.greeting;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class Greeter {

  private final Map<String, String> nicknames = Map.of("Ada", "Ace");

  public String greet(@Nullable String name) {
    return name == null ? "Hello, world!" : "Hello, " + name.trim() + "!";
  }

  public @Nullable String lookupNickname(String name) {
    return nicknames.get(name);
  }
}
