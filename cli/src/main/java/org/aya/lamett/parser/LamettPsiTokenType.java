package org.aya.lamett.parser;

import com.intellij.psi.tree.IElementType;

public class LamettPsiTokenType extends IElementType {
  public LamettPsiTokenType(String debugName) {
    super(debugName, LamettLanguage.INSTANCE);
  }
}
