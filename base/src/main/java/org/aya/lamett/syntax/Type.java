package org.aya.lamett.syntax;

import org.aya.lamett.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

/**
 * Weakly Tarski universe.
 */
public sealed interface Type extends Docile {
  @Override default @NotNull Doc toDoc() {
    return Doc.plain("TODO");
  }

  enum Lit implements Type {
    U, I, F, ISet, Set
  }

  record El(@NotNull Term term) implements Type {
  }

  record Pi(@NotNull Param<Type> param, @NotNull Type cod) implements Type {
  }

  record Sigma(@NotNull Param<Type> param, @NotNull Type cod) implements Type {
  }
}
