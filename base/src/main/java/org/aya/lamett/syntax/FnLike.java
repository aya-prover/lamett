package org.aya.lamett.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.tyck.WeaklyTarski;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;

public interface FnLike {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull Term result();
  default @NotNull SeqView<LocalVar> teleVars() {
    return telescope().view().map(Param::x);
  }
  default @NotNull SeqView<Term> teleRefs() {
    return teleVars().map(Term.Ref::new);
  }

  default @NotNull Type type(@NotNull WeaklyTarski tarski) {
    var tele = telescope().map(param -> new Param<>(param.x(), tarski.el(param.type())));
    return Type.mkPi(tele, tarski.el(result()));
  }
}
