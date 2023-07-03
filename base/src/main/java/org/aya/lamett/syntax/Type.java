package org.aya.lamett.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple2;
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

  static @NotNull Type mkPi(@NotNull ImmutableSeq<Param<Type>> telescope, @NotNull Type body) {
    return telescope.view().foldRight(body, Type.Pi::new);
  }

  record Sub(
    @NotNull Type underlying,
    @NotNull ImmutableSeq<Tuple2<Term.Cofib.Conj, Term>> restrs
  ) implements Type {
  }
}
