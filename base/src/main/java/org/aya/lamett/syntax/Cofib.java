package org.aya.lamett.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.lamett.tyck.Unifier;
import org.aya.lamett.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/** @implNote Cofibration, in disjunction normal form.
 * `Forall`s eleminated in elaboration. */
public record Cofib(@NotNull ImmutableSeq<Conj> conjs) implements Term {
  public @NotNull Cofib forall(@NotNull LocalVar i, Unifier unifier) {
    var cofib = MutableList.<Cofib.Conj>create();
      for (Conj(var atoms) : conjs) {
        for (var atom : atoms) {
          if (atom instanceof Eq eq) {
            if (unifier.untyped(eq.lhs(), eq.lhs())) continue;
            if (eq.freeVars().contains(i)) break;
          }
          cofib.append(Conj.of(atom));
        }
      }

    return new Cofib(cofib.toImmutableSeq());
  }

  public @NotNull Cofib disj(@NotNull Cofib cofib) {
    return new Cofib(conjs.appendedAll(cofib.conjs));
  }

  public @NotNull Cofib conj(@NotNull Cofib cofib) {
    return new Cofib(conjs.flatMap(conj -> cofib.conjs.map(conj::conj)));
  }

  static public @NotNull Cofib eq(@NotNull Term lhs, @NotNull Term rhs) {
    return new Cofib(ImmutableSeq.of(new Conj(ImmutableSeq.of(new Eq(lhs, rhs)))));
  }

  static public @NotNull Cofib of(@NotNull Conj... conjs) {
    return new Cofib(ImmutableSeq.of(conjs));
  }

  static public @NotNull Cofib from(@NotNull Term atom) {
    return Cofib.of(Conj.of(atom));
  }

  static public @NotNull Cofib known(boolean isTrue) {
    return isTrue ? Cofib.of(Conj.of()) : Cofib.of();
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

    static public @NotNull Conj of(@NotNull Term... atoms) {
      return new Conj(ImmutableSeq.of(atoms));
    }
  }

  /** @implNote `Eq` should always be normalized */
  public record Eq(@NotNull Term lhs, @NotNull Term rhs) implements Term {
    @Override public @NotNull Eq neg() {
      return map(Term::neg);
    }

    public @NotNull Eq map(@NotNull UnaryOperator<Term> f) {
      return new Eq(f.apply(lhs), f.apply(rhs));
    }

    public @NotNull Eq norm() {
      var eq = this;
      if (eq.lhs() instanceof Term.INeg) {
        eq = eq.neg();
      } else if (eq.lhs() instanceof Term.Lit) {
        if (eq.rhs() instanceof Term.Lit) {
          return eq;
        } else {
          eq = new Cofib.Eq(eq.rhs(), eq.lhs());
          if (eq.lhs() instanceof Term.INeg) eq = eq.neg();
        }
      }

      assert eq.lhs() instanceof Term.Ref;
      return eq;
    }

    public @NotNull ImmutableSeq<LocalVar> freeVars() {
      return lhs instanceof Ref(var lvar) ? switch (rhs) {
        case Ref(var rvar) -> ImmutableSeq.of(lvar, rvar);
        case INeg(var body) when body instanceof Ref(var rvar) -> ImmutableSeq.of(lvar, rvar);
        default -> ImmutableSeq.of(lvar);
      } : ImmutableSeq.empty();
    }
  }
}
