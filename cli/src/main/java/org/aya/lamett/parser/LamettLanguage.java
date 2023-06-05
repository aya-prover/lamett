package org.aya.lamett.parser;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class LamettLanguage extends Language {
  public static final @NotNull LamettLanguage INSTANCE = new LamettLanguage();

  private LamettLanguage() {
    super("lamett");
  }
}
