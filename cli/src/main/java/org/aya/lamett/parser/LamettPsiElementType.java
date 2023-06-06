package org.aya.lamett.parser;

import com.intellij.psi.tree.IElementType;

public class LamettPsiElementType extends IElementType {
  public LamettPsiElementType(String debugName) {
    super(debugName, LamettLanguage.INSTANCE);
  }
}
