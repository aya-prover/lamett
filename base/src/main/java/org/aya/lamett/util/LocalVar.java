package org.aya.lamett.util;

import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;

public record LocalVar(@Override @NotNull String name) implements AnyVar {
  public static @NotNull LocalVar from(@NotNull WithPos<String> id) {
    return new LocalVar(id.data());
  }

  public @NotNull String toString() {
    return name;
  }

  @Override public boolean equals(Object obj) {
    return this == obj;
  }

  @Override public int hashCode() {
    return System.identityHashCode(this);
  }
}
