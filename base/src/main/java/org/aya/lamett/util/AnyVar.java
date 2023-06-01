package org.aya.lamett.util;

import org.aya.lamett.syntax.DefVar;
import org.jetbrains.annotations.NotNull;

public sealed interface AnyVar permits DefVar, LocalVar {
  @NotNull String name();
}
