package org.aya.lamett.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple2;
import org.aya.lamett.tyck.Normalizer;
import org.aya.lamett.util.Distiller;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

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
  }

  record Cofib(@NotNull ImmutableSeq<LocalVar> params, @NotNull ImmutableSeq<Conj> conjs) implements Term {
    public @NotNull Cofib forall(@NotNull LocalVar i) {
      return new Cofib(params.appended(i), conjs);
    }

    public @NotNull Cofib disj(@NotNull Cofib cofib) {
      return new Cofib(params.appendedAll(cofib.params), conjs.appendedAll(cofib.conjs));
    }

    public @NotNull Cofib conj(@NotNull Cofib cofib) {
      return new Cofib(params.appendedAll(cofib.params), conjs.flatMap(conj -> cofib.conjs.map(conj::conj)));
    }

    static public @NotNull Cofib eq(@NotNull Term lhs, @NotNull Term rhs) {
      return new Cofib(ImmutableSeq.empty(), ImmutableSeq.of(new Cofib.Conj(ImmutableSeq.of(new Eq(lhs, rhs)))));
    }

    static public @NotNull Cofib atom(@NotNull Term atom) {
      return new Cofib(ImmutableSeq.empty(), ImmutableSeq.of(new Cofib.Conj(ImmutableSeq.of(atom))));
    }

    static public @NotNull Cofib known(boolean isTrue) {
      if (isTrue) {
        return new Cofib(ImmutableSeq.empty(), ImmutableSeq.of(new Cofib.Conj(ImmutableSeq.empty())));
      } else {
        return new Cofib(ImmutableSeq.empty(), ImmutableSeq.empty());
      }
    }

    public boolean isTrue() {
      return !isFalse() && conjs.allMatch(conj -> conj.atoms.isEmpty());
    }

    public boolean isFalse() {
      return conjs.isEmpty();
    }

    public record Conj(@NotNull ImmutableSeq<Term> atoms) {
      public @NotNull Conj conj(@NotNull Conj conj2) {
        return new Conj(atoms.appendedAll(conj2.atoms));
      }
    }
    public record Eq(@NotNull Term lhs, @NotNull Term rhs) implements Term {
      @Override
      public @NotNull Eq neg() {
        return map(Term::neg);
      }

      public @NotNull Eq map(@NotNull UnaryOperator<Term> f) {
        return new Eq(f.apply(lhs), f.apply(rhs));
      }

      public @NotNull ImmutableSeq<LocalVar> freeVars() {
        if (lhs instanceof Ref(var lvar)) {
          return switch (rhs) {
            case Ref(var rvar) -> ImmutableSeq.of(lvar, rvar);
            case INeg(var body) when body instanceof Ref(var rvar) -> ImmutableSeq.of(lvar, rvar);
            default -> ImmutableSeq.of(lvar);
          };
        } else {
          return ImmutableSeq.empty();
        }
      }
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

  record Partial(@NotNull Term cofib, @NotNull Term type) implements Term {}
  record PartEl(@NotNull ImmutableSeq<Tuple2<Cofib.Conj, Term>> elems) implements Term {}
  record Coe(@NotNull Term r, @NotNull Term s, @NotNull Term A) implements Term {
    public @NotNull Coe update(@NotNull Term type, @NotNull Term r, @NotNull Term s) {
      return A == A() && r == r() && s == s() ? this : new Coe(type, r, s);
    }

    public @NotNull Coe descent(@NotNull UnaryOperator<Term> f, @NotNull UnaryOperator<Pat> g) {
      return update(f.apply(this.A), f.apply(r), f.apply(s));
    }

    public @NotNull Coe inverse(Term newTy) {
      return new Coe(newTy, s, r);
    }

    /**
     * For parameter and variable names, see Carlo Angiuli's PhD thesis, page 160.
     * <ul>
     *   <li>x ∈ FV(A.type()), x ∈ FV(B)</li>
     *   <li>A.ref() ∈ FV(B)</li>
     * </ul>
     * @return {@code \x : A.type() => B[coe(r, x, A, arg) / A.ref()]}
     */
    public static @NotNull Lam cover(LocalVar x, Param<Term> A, Term B, Term arg, Term r) {
      var innerCover = Normalizer.rename(new Lam(x, A.type()));
      var coeRX = new App(new Coe(innerCover, r, new Ref(x)), arg);
      return new Lam(x, B.subst(A.x(), coeRX));
    }

    public @NotNull Coe recoe(Term cover) {
      return new Coe(cover, r, s);
    }

    public @NotNull Term family() {
      return familyI2J(A, r, s);
    }
  }

  /** Let A be argument, then <code>A i -> A j</code> */
  static @NotNull Pi familyI2J(Term term, Term i, Term j) {
    return new Pi(
      new Param<>(new LocalVar("_"), new App(term, i)),
      new App(term, j));
  }
}
