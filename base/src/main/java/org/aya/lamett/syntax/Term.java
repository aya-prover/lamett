package org.aya.lamett.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.lamett.tyck.Normalizer;
import org.aya.lamett.util.Distiller;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public sealed interface Term extends Docile {
  @Override default @NotNull Doc toDoc() {
    return Distiller.term(this, Distiller.Prec.Free);
  }
  default @NotNull Term subst(@NotNull LocalVar x, @NotNull Term t) {
    return subst(MutableMap.of(x, t));
  }
  default @NotNull Term subst(@NotNull MutableMap<LocalVar, Term> map) {
    return new Normalizer(map).term(this);
  }

  record Error(@NotNull String msg) implements Term {}
  record Ref(@NotNull LocalVar var) implements Term {}
  record FnCall(@NotNull DefVar<Def.Fn> fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record DataCall(@NotNull DefVar<Def.Data> fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record ConCall(@NotNull DefVar<Def.Cons> fn, @NotNull ImmutableSeq<Term> args,
                 @NotNull ImmutableSeq<Term> dataArgs) implements Term {}
  sealed interface Two extends Term {
    @NotNull Term f();
    @NotNull Term a();
    @NotNull default Two make(@NotNull Term f, @NotNull Term a) {
      return switch (this) {
        case Tuple $ -> new Tuple(f, a);
        case App $ -> new App(f, a);
      };
    }
  }
  record Tuple(@NotNull Term f, @NotNull Term a) implements Two {
    @Override @NotNull public Term proj(boolean isOne) {return isOne ? f() : a();}
  }
  record App(@NotNull Term f, @NotNull Term a) implements Two {}
  record Proj(@NotNull Term t, boolean isOne) implements Term {}
  record Lam(@NotNull LocalVar x, @NotNull Term body) implements Term {}

  static @NotNull Term mkLam(@NotNull SeqView<LocalVar> telescope, @NotNull Term body) {
    return telescope.foldRight(body, Lam::new);
  }
  default @NotNull Term app(@NotNull Term... args) {
    var f = this;
    for (var a : args) f = f instanceof Lam lam ? lam.body.subst(lam.x, a) : new App(f, a);
    return f;
  }
  default @NotNull Term proj(boolean isOne) {return new Proj(this, isOne);}

  sealed interface DT extends Term {
    @NotNull Param<Term> param();
    @NotNull Term cod();
    @NotNull default Term codomain(@NotNull Term term) {
      return cod().subst(param().x(), term);
    }
    @NotNull default DT make(@NotNull Param<Term> param, @NotNull Term cod) {
      return switch (this) {
        case Pi $ -> new Pi(param, cod);
        case Sigma $ -> new Sigma(param, cod);
      };
    }
  }

  record Pi(@NotNull Param<Term> param, @NotNull Term cod) implements DT {}
  record Sigma(@NotNull Param<Term> param, @NotNull Term cod) implements DT {}

  static @NotNull Term mkPi(@NotNull ImmutableSeq<Param<Term>> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, Pi::new);
  }
  static @NotNull Term mkPi(@NotNull Term dom, @NotNull Term cod) {
    return new Pi(new Param<>(new LocalVar("_"), dom), cod);
  }
  @NotNull Term U = new Lit(Keyword.U);
  @NotNull Term I = new Lit(Keyword.I);
  record Lit(@NotNull Keyword keyword) implements Term {}
}
