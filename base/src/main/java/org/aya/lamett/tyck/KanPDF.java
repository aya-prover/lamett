package org.aya.lamett.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.lamett.syntax.Restr;
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

  /**
   * <pre>
   *   θ := r = s ∨ φ
   *   u : I -> Partial θ (A -> B)
   *   hcom r s _ u : A -> B
   *   -------------------------------
   *   λ a. hcom r s (λ i. (u i) a)
   *   normalize(u(new Ref(i)))
   * </pre>
   */
  static @NotNull Term hcomPi(
    @NotNull Term.Pi pi, Term hcomR, Term hcomS,
    Term phi, LocalVar i, Term.PartEl partEl
  ) {
    var a = new LocalVar("a");
    var refA = new Term.Ref(a);
    var newEl = partEl.map2(t -> t.app(refA));
    return new Term.Lam(a, new Term.Hcom(hcomR, hcomS, pi.codomain(refA), phi, new Term.Lam(i, newEl)));
  }

  static @NotNull Term hcomSigma(
    @NotNull Term.Sigma sigma, Term hcomR, Term hcomS,
    Term phi, LocalVar i, Term.PartEl partEl
  ) {
    UnaryOperator<Term> m0 = z ->
      new Term.Hcom(hcomR, z, sigma.param().type(), sigma.param().type(),
        new Term.Lam(i, partEl.map2(t -> t.proj(true))));
    var z = new LocalVar("z");
    var comType = new Term.Lam(z, sigma.codomain(m0.apply(new Term.Ref(z))));
    var m1 = Term.com(hcomR, hcomS, comType, phi, i, partEl.map2(t -> t.proj(false)));
    return new Term.Pair(m0.apply(hcomS), m1);
  }

  static @NotNull Term hcomU(
    @NotNull Term hcomR,
    @NotNull Term hcomS,
    @NotNull LocalVar i,
    @NotNull Term.PartEl el) {
    return new Term.HcomU(hcomR, hcomS, i, el);
  }
}
