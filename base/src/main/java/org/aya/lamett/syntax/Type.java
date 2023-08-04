package org.aya.lamett.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple2;
import org.aya.lamett.tyck.Normalizer;
import org.aya.lamett.tyck.Unification;
import org.aya.lamett.util.LocalVar;
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
    U, I, F, ISet, Set;

    public boolean isUniv() {
      return this == U || this == ISet || this == Set;
    }

    @Override @NotNull public Doc toDoc() {
      return Doc.plain(name());
    }
  }

  record El(@NotNull Term term) implements Type {
    @Override @NotNull public Doc toDoc() {
      return Doc.wrap("El(", ")", term.toDoc());
    }
  }

  record Pi(@NotNull Param<Type> param, @NotNull Type cod) implements Type {
    public Type codomain(Term tm) {
      return cod.subst(param.x(), tm);
    }

    @Override @NotNull public Doc toDoc() {
      return Doc.parened(Doc.sep(param.toDoc(), Doc.symbol("->"), cod.toDoc()));
    }
  }

  record Sigma(@NotNull Param<Type> param, @NotNull Type cod) implements Type {
    public Type codomain(Term tm) {
      return cod.subst(param.x(), tm);
    }
    @Override @NotNull public Doc toDoc() {
      return Doc.parened(Doc.sep(param.toDoc(), Doc.symbol("**"), cod.toDoc()));
    }
  }

  static @NotNull Type mkPi(@NotNull ImmutableSeq<Param<Type>> telescope, @NotNull Type body) {
    return telescope.view().foldRight(body, Type.Pi::new);
  }
  static @NotNull Type mkPi(@NotNull Type dom, @NotNull Type cod) {
    return new Type.Pi(new Param<>(new LocalVar("_"), dom), cod);
  }

  record PartTy(
    @NotNull Type underlying,
    @NotNull Cofib restrs
  ) implements Type {
    @Override @NotNull public Doc toDoc() {
      return Doc.plain("PartTy");
    }
  }

  record Sub(
    @NotNull Type underlying,
    @NotNull ImmutableSeq<Tuple2<Cofib.Conj, Term>> restrs
  ) implements Type {
    @Override @NotNull public Doc toDoc() {
      return Doc.plain("Sub");
    }
  }

  record HcomU(
    @NotNull Term r, @NotNull Term s,
    @NotNull LocalVar i,
    @NotNull ImmutableSeq<Tuple2<Term.Conj, Type>> restrs // under i
  ) /* implements Type */ {
  }

  default @NotNull Type subst(@NotNull LocalVar x, @NotNull Term t) {
    return subst(MutableMap.of(x, t));
  }
  default @NotNull Type subst(@NotNull MutableMap<LocalVar, Term> map) {
    return new Normalizer(map).type(this);
  }
  default @NotNull Type subst(@NotNull Unification unification) {
    return new Normalizer(unification).type(this);
  }

  static Type ref(LocalVar var) {
    return new Type.El(new Term.Ref(var));
  }
}
