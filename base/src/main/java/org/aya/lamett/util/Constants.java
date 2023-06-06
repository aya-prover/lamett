package org.aya.lamett.util;

import org.aya.util.error.Global;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface Constants {
  @NotNull @NonNls String ANONYMOUS_PREFIX = "_";

  static @NotNull LocalVar randomlyNamed(@NotNull SourcePos pos) {
    return new LocalVar(randomName(pos));
  }

  @Contract(pure = true)
  static @NotNull String randomName(@NotNull Object pos) {
    if (Global.NO_RANDOM_NAME) return ANONYMOUS_PREFIX;
    return ANONYMOUS_PREFIX + Math.abs(pos.hashCode()) % 10;
  }
}
