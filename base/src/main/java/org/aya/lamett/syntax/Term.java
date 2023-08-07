package org.aya.lamett.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.lamett.tyck.Normalizer;
import org.aya.lamett.tyck.Unification;
import org.aya.lamett.util.Distiller;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public sealed interface Term extends Docile permits Cofib, Cofib.Eq, Term.App, Term.Coe, Term.ConCall, Term.DT, Term.DataCall,
  Term.Error, Term.Ext, Term.FnCall, Term.Hcom, Term.INeg, Term.InS, Term.Lam, Term.Lit, Term.OutS, Term.Pair, Term.PartEl,
  Term.PartTy, Term.Path, Term.Proj, Term.Ref, Term.Sub {
  @Override default @NotNull Doc toDoc() {
    return Distiller.term(this, Distiller.Prec.Free);
  }
  default @NotNull Term subst(@NotNull LocalVar x, @NotNull Term t) {
    return subst(MutableMap.of(x, t));
  }
  default @NotNull Term subst(@NotNull MutableMap<LocalVar, Term> map) {
    return new Normalizer(map).term(this);
  }
  default @NotNull Term subst(@NotNull Unification unification) {
    return new Normalizer(unification).term(this);
  }

  record Error(@NotNull String msg) implements Term {}
  record Ref(@NotNull LocalVar var) implements Term {}
  record FnCall(@NotNull DefVar<Def.Fn> fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record DataCall(@NotNull DefVar<Def.Data> fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record ConCall(@NotNull DefVar<Def.Cons> fn, @NotNull ImmutableSeq<Term> args,
                 @NotNull ImmutableSeq<Term> dataArgs) implements Term {}
  record Pair(@NotNull Term f, @NotNull Term a) implements Term {
    @Override @NotNull public Term proj(boolean isOne) {return isOne ? f() : a();}
  }
  record App(@NotNull Term f, @NotNull Term a) implements Term {}
  record Proj(@NotNull Term t, boolean isOne) implements Term {}
  record Lam(@NotNull LocalVar x, @NotNull Term body) implements Term {
    public @NotNull Term apply(@NotNull Term arg) {
      return body.subst(x, arg);
    }
  }

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
  }

  record Pi(@NotNull Param<Term> param, @NotNull Term cod) implements DT {}
  record Sigma(@NotNull Param<Term> param, @NotNull Term cod) implements DT {}

  static @NotNull Term mkPi(@NotNull ImmutableSeq<Param<Term>> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, Pi::new);
  }
  static @NotNull Term mkPi(@NotNull Term dom, @NotNull Term cod) {
    return new Pi(new Param<>(new LocalVar("_"), dom), cod);
  }
  @NotNull Lit U = new Lit(Keyword.U);
  @NotNull Lit Set = new Lit(Keyword.Set);
  @NotNull Lit ISet = new Lit(Keyword.ISet);
  @NotNull Lit I = new Lit(Keyword.I);
  @NotNull Lit F = new Lit(Keyword.F);
  @NotNull Lit One = new Lit(Keyword.One);
  @NotNull Lit Zero = new Lit(Keyword.Zero);
  record Lit(@NotNull Keyword keyword) implements Term {
    @Override
    @NotNull public Term neg() {
      return switch (keyword) {
        case One -> Zero;
        case Zero -> One;
        default -> throw new InternalError(keyword.name() + " can't be negated");
      };
    }

    @NotNull static public Lit fromBool(boolean b) {
      return b ? One : Zero;
    }

    public boolean isUniv() {
      return keyword == Keyword.U || keyword == Keyword.Set || keyword == Keyword.ISet;
    }

    @Override public boolean equals(Object o) {
      return o instanceof Lit lit && lit.keyword == keyword;
    }
  }

  record INeg(@NotNull Term body) implements Term {
    @NotNull public Term neg() {
      return body;
    }
  }

  @NotNull default Term neg() {
    return new INeg(this);
  }

  record PartTy(@NotNull Term cofib, @NotNull Term type) implements Term {}
  record PartEl(@NotNull ImmutableSeq<Tuple2<Cofib.Conj, Term>> elems) implements Term {
    public @NotNull PartEl map(UnaryOperator<Tuple2<Cofib.Conj, Term>> f) {
      return new PartEl(elems.map(f));
    }

    public @NotNull PartEl map1(UnaryOperator<Cofib.Conj> f) {
      return new PartEl(elems.map(t -> Tuple.of(f.apply(t.component1()), t.component2())));
    }

    public @NotNull PartEl map2(UnaryOperator<Term> f) {
      return new PartEl(elems.map(t -> Tuple.of(t.component1(), f.apply(t.component2()))));
    }
  }


  /**
   * <pre>
   * Γ ⊢ {@param r} {@param s} : 𝕀
   * Γ ⊢ {@param A} : 𝕀 → U
   * ------------------------------
   * Γ ⊢ coe r s A : (u : A r) → (A s | r = s ↦ u)
   * </pre>
   */
  record Coe(@NotNull Term r, @NotNull Term s, @NotNull Term A) implements Term {
    public @NotNull Coe update(@NotNull Term r, @NotNull Term s, @NotNull Term A) {
      return A == A() && r == r() && s == s() ? this : new Coe(r, s, A);
    }

    public @NotNull Coe descent(@NotNull UnaryOperator<Term> f, @NotNull UnaryOperator<Pat> g) {
      return update(f.apply(r), f.apply(s), f.apply(this.A));
    }

    public @NotNull Coe inverse(Term newTy) {
      return new Coe(s, r, newTy);
    }

    /**
     * For parameter and variable names, see Carlo Angiuli's PhD thesis, page 160.
     * <ul>
     *   <li>x ∈ FV(A.type()), x ∈ FV(B)</li>
     *   <li>A.ref() ∈ FV(B)</li>
     * </ul>
     *
     * @return {@code \x : A.type() => B[coe(r, x, A, arg) / A.ref()]}
     */
    public static @NotNull Lam cover(LocalVar x, Param<Term> A, Term B, Term arg, Term r) {
      var innerCover = Normalizer.rename(new Lam(x, A.type()));
      var coeRX = new App(new Coe(r, new Ref(x), innerCover), arg);
      return new Lam(x, B.subst(A.x(), coeRX));
    }

    public @NotNull Coe recoe(Term cover) {
      return new Coe(r, s, cover);
    }

    public @NotNull Term family() {
      return familyI2J(A, r, s);
    }
  }

  /**
   * <pre>
   * Γ ⊢ {@param r} {@param s} : 𝕀
   * Γ ⊢ {@param A} : U
   * Γ ⊢ φ : F (this is {@param phi})
   * Γ ⊢ {@param u} : (i : 𝕀) → {@link PartTy} (i = r ∨ φ) A
   * --------------------------------------------------
   * Γ ⊢ hcom r s A φ u : (A | i = r ∨ φ ↦ outPar (u 0))
   * </pre>
   */
  record Hcom(@NotNull Term r, @NotNull Term s, @NotNull Term A, @NotNull Term phi, @NotNull Term u) implements Term {}

  // com (r s : 𝕀) (A : 𝕀 → U) (φ : F) (u : (i : 𝕀) → Partial (i = r ∨ φ) A) : A
  static @NotNull Term com(@NotNull Term r, @NotNull Term s, @NotNull Term A, @NotNull Term phi, LocalVar i, Term.PartEl partEl) {
    var coe = new Coe(new Ref(i), s, A);
    var newEl = partEl.map(tup -> Tuple.of(tup.component1(), coe.app(tup.component2())));
    return new Hcom(r, s, A.app(s), phi, mkPi(ImmutableSeq.of(new Param<>(i, Lit.I)), newEl));
  }

  /**
   * Generalized extension type.
   *
   * @see Restr
   */
  record Ext<F extends Restr>(@NotNull Term type, @NotNull F restr) implements Term {
  }

  /**
   * Cubical extension type, also known as Path type.
   */
  record Path(@NotNull ImmutableSeq<LocalVar> binders, @NotNull Ext<Restr.Cubical> ext) implements Term {
  }

  record Sub(@NotNull Term type, @NotNull Term partEl) implements Term {}
  record InS(@NotNull Term phi, @NotNull Term of) implements Term {}
  record OutS(@NotNull Term phi, @NotNull Term partEl, @NotNull Term of) implements Term {}

  /** Let A be argument, then <code>A i -> A j</code> */
  static @NotNull Pi familyI2J(Term term, Term i, Term j) {
    return (Pi) mkPi(new App(term, i), new App(term, j));
  }
}
