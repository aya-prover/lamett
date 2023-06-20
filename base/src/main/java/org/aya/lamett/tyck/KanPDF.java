package org.aya.lamett.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

import static org.aya.lamett.tyck.Normalizer.rename;

public interface KanPDF {
  static Term coeSigma(Term.Sigma sigma, LocalVar i, Term coeR, Term coeS) {
    var t = new Term.Ref(new LocalVar("t"));
    var subst = new Normalizer(MutableMap.create());

    // Item: t.ix
    var t1 = new Term.Proj(t, true);
    var t2 = new Term.Proj(t, false);

    // Because i : I |- params, so is i : I |- param, now bind A_n := \i. param
    var A = rename(new Term.Lam(i, sigma.param().type()));
    // coe r s' (\i => A_n) t.ix
    UnaryOperator<Term> fill1 = s -> new Term.Coe(A, coeR, s).app(t1);
    subst.rho().put(sigma.param().x(), fill1.apply(new Term.Ref(i)));

    var B = rename(new Term.Lam(i, subst.term(sigma.cod())));
    UnaryOperator<Term> fill2 = s -> new Term.Coe(B, coeR, s).app(t2);

    return new Term.Lam(t.var(), new Term.Pair(
      fill1.apply(coeS),
      fill2.apply(coeS)
    ));
  }

  static @NotNull Term coePi(@NotNull Term.Pi pi, @NotNull Term.Coe coe, LocalVar i) {
    var M = new LocalVar("f");
    var a = new LocalVar("a");
    var arg = new Term.App(coe.inverse(Normalizer.rename(new Term.Lam(i, pi.param().type()))), new Term.Ref(a));
    var cover = Normalizer.rename(Term.Coe.cover(i, pi.param(), pi.cod(), new Term.Ref(a), coe.s()));
    return new Term.Lam(M, new Term.Lam(a,
      new Term.App(coe.recoe(cover),
        new Term.App(new Term.Ref(M), arg))));
  }
}
