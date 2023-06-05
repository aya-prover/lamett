package org.aya.lamett.parser;

import com.intellij.psi.tree.IElementType;

public class AyaPsiElementType extends IElementType {
  public AyaPsiElementType(String debugName) {
    super(debugName, LamettLanguage.INSTANCE);
  }
}
