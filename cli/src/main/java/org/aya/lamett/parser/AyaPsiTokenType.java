package org.aya.lamett.parser;

import com.intellij.psi.tree.IElementType;

public class AyaPsiTokenType extends IElementType {
  public AyaPsiTokenType(String debugName) {
    super(debugName, LamettLanguage.INSTANCE);
  }
}
