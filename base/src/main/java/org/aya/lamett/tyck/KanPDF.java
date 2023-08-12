package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.lamett.syntax.Cofib;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.aya.lamett.syntax.Term.Ref.ref;
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

  static @NotNull Term hcomHcomU(
    // hcom
    @NotNull Term hcomR, @NotNull Term hcomS,
    @NotNull Term hcomPhi, @NotNull LocalVar hcomI, @NotNull Term.PartEl hcomU,
    // hcomU
    @NotNull Term hcomUr, @NotNull Term hcomUs,
    @NotNull Term hcomUphi, @NotNull LocalVar hcomUi, @NotNull Term.PartEl hcomUA,
    @NotNull Unifier unifier
  ) {
    var conjHcomPhi = Cofib.Conj.of(hcomPhi);
    var conjHcomUphi = Cofib.Conj.of(hcomUphi);
    var conjReqS = Cofib.Conj.of(new Cofib.Eq(hcomUr, hcomUs));

    // hcom { r ~> s } (hcom { r' ~> s' } U ψ A) φ u =
    // box { r' ~> s' } ?0 ?1 : hcom { r' ~> s' } U ψ A
    //   where ψ ⊢ ?0 : A s'
    //           ⊢ ?1 : A r' | ψ ↦ coe { s' ~> r' } A ?0

    // r' = s' ∨ ψ , i : I ⊢ P := hcom { r ~> i } (A s') φ u : A s' | ψ ↦ u i
    Function<Term, Term> P = i ->
      new Term.Hcom(hcomR, i, hcomUA.subst(hcomUi, hcomUs), hcomPhi, new Term.Lam(hcomI, hcomU));

    // ⊢ ?1 := hcom { r ~> s } (A r') (φ ∨ ψ ∨ r' = s')
    //    (λ i. {| i = r ∨ φ ↦ ?2 ; ψ ↦ ?3 ; r' = s' ↦ ?4 |}
    //    : A r'
    //    | r = s ∨ φ ↦ ...
    //    | ψ ↦ coe { s' ~> r' } A (P s')       (required by box)
    //    | r' = s' ↦ ...

    // i : I, i = r ∨ φ ⊢ ?2 := cap { r' <~ s' } ψ (u i)
    BiFunction<Term, Cofib.Conj, Term> Q = (i, proof) -> {
      var result = unifier.withCofibConj(proof, () -> {
        var realU = hcomU.simplify(unifier);
        assert realU != null : "φ ∧ ¬ φ";
        return new Term.Cap(hcomUr, hcomUs, hcomUphi, hcomUA, realU.component2().subst(hcomI, i));
      }, null);   // TODO: I am not sure !!
      assert result != null : "i != r";
      return result;
    };

    Function<Term, Term> Q0 = i -> Q.apply(i, Cofib.Conj.of(new Cofib.Eq(i, hcomR)));
    Function<Term, Term> Q1 = i -> Q.apply(i, conjHcomPhi);

    // i : I, ψ ⊢ ?3 := coe { s' ~> r' } A (P i)
    Function<Term, Term> R = i -> {
      var realA = hcomUA.simplify(unifier);
      assert realA != null : "φ ∧ ¬ φ";
      return new Term.Coe(hcomUs, hcomUr, realA.component2()).app(P.apply(i));
    };

    // i : I, r' = s' ⊢ ?4 := P i : A r'
    Function<Term, Term> S = P;

    Function<Term, Term.PartEl> T = i -> new Term.PartEl(ImmutableSeq.of(
      Tuple.of(Cofib.Conj.of(new Cofib.Eq(i, hcomR)), Q0.apply(i)),
      Tuple.of(conjHcomPhi, Q1.apply(i)),
      Tuple.of(conjHcomUphi, R.apply(i)),
      Tuple.of(conjReqS, S.apply(i))
    ));

    // (i : I) -> Partial (i = r ∨ φ ∨ ψ ∨ r' = s') (A r')
    var termT = Term.mkLam("i", i -> T.apply(ref(i)));

    var halfRealAr = ((Term.PartEl) hcomUA.subst(hcomUi, hcomUr))
      .simplify(unifier);     // since `i : I ⊢ A : Partial (i = r' ∨ ψ) U`, we have a clause with cof `r' = r'`
    assert halfRealAr != null : "φ ∧ ¬ φ";
    var realAr = halfRealAr.component2();


    return new Term.Box(hcomUr, hcomUs, hcomUphi, new Term.Lam(hcomUi, hcomUA),
      new Term.PartEl(ImmutableSeq.of(Tuple.of(conjHcomUphi, P.apply(hcomUs)))),
      new Term.Hcom(hcomR, hcomS, realAr,
        Cofib.of(conjHcomPhi, conjHcomUphi, conjReqS),
        termT));
  }
}
