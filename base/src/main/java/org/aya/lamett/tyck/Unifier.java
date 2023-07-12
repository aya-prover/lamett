package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.syntax.Type;
import org.aya.lamett.util.LocalVar;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Unifier {
  public record FailureData(@NotNull Term l, @NotNull Term r) {}
  public FailureData data;

  /** @return {@literal false} if `conj` is `⊥`, thus any subsequent unification succeeds immediately */
  public boolean addNFConj(Term.Cofib.Conj conj) {
    for (var atom : conj.atoms()) {
      switch (atom) {
        case Term.Cofib.Eq eq -> {
          assert eq.lhs() instanceof Term.Ref;
          var lvar = Unification.LocalVarWithNeg.from(eq.lhs());
          switch (eq.rhs()) {
            case Term.Ref(var var) -> {
              if (!unification.setEquiv(lvar, Unification.LocalVarWithNeg.from(var))) return false;
            }
            case Term.INeg(var body) when body instanceof Term.Ref -> {
              if (!unification.setEquiv(lvar, Unification.LocalVarWithNeg.from(body))) return false;
            }
            case Term.Lit lit when lit.keyword() == Keyword.One -> {
              if (!unification.setValue(lvar, true)) return false;
            }
            case Term.Lit lit when lit.keyword() == Keyword.Zero -> {
              if (!unification.setValue(lvar, false)) return false;
            }
            default -> throw new InternalException("not a nf conj: found " + eq.rhs());
          }
        }
        case Term.Ref(var ref) -> unification.cofibVars.add(ref);
        default -> throw new InternalException("not a whnf conj: found " + atom);
      }
    }
    conjunction = conjunction.conj(conj);
    return true;
  }

  public void loadNFConj(Term.Cofib.Conj conj) {
    unification = new Unification();
    if (!addNFConj(conj)) throw new InternalException("loading a false conj: " + conj);
    conjunction = conj;
  }

  private Term.Cofib.Conj conjunction = new Term.Cofib.Conj(ImmutableSeq.empty());

  // This is but a cache of the conjunction
  private Unification unification = new Unification();

  public Unification unification() {
    return unification;
  }

  public <U> U withCofibConj(Term.Cofib.Conj conj, Supplier<U> f, U succeed) {
    var oldConj = conjunction;
    if (addNFConj(conj)) {
      var res = f.get();
      loadNFConj(oldConj);
      return res;
    } else {
      return succeed;
    }
  }

  public boolean withCofib(@NotNull Term.Cofib cofib, Supplier<Boolean> f, boolean succeed) {
    return cofib.conjs().allMatch(conj -> withCofibConj(conj, f, succeed));
  }

  public @NotNull Unifier derive() {
    var unifier = new Unifier();
    unifier.loadNFConj(conjunction);
    return unifier;
  }

  public boolean type(@NotNull Type l, @NotNull Type r) {
    if (l == r) return true; // This includes the case of `Type.Lit`
    return switch (l) {
      case Type.El ll when r instanceof Type.El rr -> untyped(ll.term(), rr.term());
      case Type.Pi lpi when r instanceof Type.Pi rpi -> type(lpi.param().type(), rpi.param().type())
        && type(lpi.cod(), rhs(rpi.cod(), rpi.param().x(), lpi.param().x()));
      case Type.Sigma lsig when r instanceof Type.Sigma rsig -> type(lsig.param().type(), rsig.param().type())
        && type(lsig.cod(), rhs(rsig.cod(), rsig.param().x(), lsig.param().x()));
      case Type.Sub lsub when r instanceof Type.Sub rsub -> type(lsub.underlying(), rsub.underlying())
        && lsub.restrs().allMatch(ltup ->
        rsub.restrs().allMatch(rtup -> withCofibConj(
          ltup.component1().conj(rtup.component1()), () -> untyped(ltup.component2(), rtup.component2()), true)));
      default -> false;
    };
  }

  public boolean untyped(@NotNull Term oldL, @NotNull Term oldR) {
    if (oldL == oldR) return true;
    var normalizer = new Normalizer(this);
    final var l = normalizer.term(oldL);
    final var r = normalizer.term(oldR);
    return untypedInner(l, r);
  }

  public boolean untypedInner(@NotNull Term l, @NotNull Term r) {
    if (l == r) return true;
    var happy = switch (l) {
      case Term.Lam lam when r instanceof Term.Lam ram -> untypedInner(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll when r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.App(var lf, var la) when r instanceof Term.App(var rf, var ra) ->
        untypedInner(lf, rf) && untypedInner(la, ra);
      case Term.Pair(var la, var lb) when r instanceof Term.Pair(var ra, var rb) ->
        untypedInner(la, ra) && untypedInner(lb, rb);
      case Term.DT ldt when r instanceof Term.DT rdt -> ldt.getClass().equals(rdt.getClass())
        && untypedInner(ldt.param().type(), rdt.param().type())
        && untypedInner(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param().x()));
      case Term.Proj lproj when r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untypedInner(lproj.t(), rproj.t());
      case Term.Lit lu when r instanceof Term.Lit ru -> lu.keyword() == ru.keyword();
      case Term.FnCall lcall when r instanceof Term.FnCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      case Term.DataCall lcall when r instanceof Term.DataCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      // We probably won't need to compare dataArgs cus the two sides of conversion should be of the same type
      case Term.ConCall lcall when r instanceof Term.ConCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      case Term.Ref lphi when r instanceof Term.Cofib rphi -> untypedInner(Term.Cofib.atom(lphi), rphi);
      case Term.Cofib lphi when r instanceof Term.Ref rphi -> untypedInner(lphi, Term.Cofib.atom(rphi));
      case Term.Cofib lphi when r instanceof Term.Cofib rphi -> cofibImply(lphi, rphi) && cofibImply(rphi, lphi);
      case Term.PartTy lp when r instanceof Term.PartTy rp -> untypedInner(lp.cofib(), rp.cofib())
        && untypedInner(lp.type(), rp.type());
      case Term.PartEl le when r instanceof Term.PartEl re -> le.elems().allMatch(ltup ->
        re.elems().allMatch(rtup -> withCofibConj(
          ltup.component1().conj(rtup.component1()), () -> untyped(ltup.component2(), rtup.component2()), true)));
      // `Ref`s, and `INeg`s
      case Term.Ref lref when r instanceof Term.Ref rref -> unification.unify(
        Unification.LocalVarWithNeg.from(lref.var()), Unification.LocalVarWithNeg.from(rref.var()));
      case Term.Ref lref when r instanceof Term.INeg rneg && rneg.body() instanceof Term.Ref -> unification.unify(
        Unification.LocalVarWithNeg.from(lref.var()), Unification.LocalVarWithNeg.from(rneg));
      case Term.INeg lneg when r instanceof Term.Ref rref && lneg.body() instanceof Term.Ref -> unification.unify(
        Unification.LocalVarWithNeg.from(lneg), Unification.LocalVarWithNeg.from(rref.var()));
      case Term.INeg(var ll) when r instanceof Term.INeg(var rr) -> untypedInner(ll, rr);
      case Term.Coe(var r1, var s1, var A1) when r instanceof Term.Coe(var r2, var s2, var A2) ->
        untypedInner(r1, r2) && untypedInner(s1, s2) && untypedInner(A1, A2);
      default -> false;
    };
    if (!happy && data == null) data = new FailureData(l, r);
    if (happy) data = null;
    return happy;
  }

  public boolean untypedUnderCofib(Term.Cofib cofib, @NotNull Term l, @NotNull Term r) {
    return withCofib(cofib, () -> untyped(l, r), true);
  }

  boolean cofibIsTrue(@NotNull Term.Cofib p) {
    return p.conjs().anyMatch(conj -> conj.atoms().allMatch(atom -> switch (atom) {
      case Term.Cofib.Eq eq -> untyped(eq.lhs(), eq.rhs());
      case Term.Ref(var ref) -> unification.cofibVars.contains(ref);
      default -> throw new InternalException("Unexpected cofib atom: " + atom);
    }));
  }

  boolean cofibImply(@NotNull Term.Cofib p, @NotNull Term.Cofib q) {
    return withCofib(p, () -> cofibIsTrue(q), true);
  }

  private boolean unifySeq(@NotNull ImmutableSeq<Term> l, @NotNull ImmutableSeq<Term> r) {
    return l.allMatchWith(r, this::untypedInner);
  }

  private boolean eta(@NotNull Term r, Term.Lam lam) {
    return untypedInner(lam.body(), r.app(new Term.Ref(lam.x())));
  }

  private static @NotNull Term rhs(Term rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }
  private static @NotNull Type rhs(Type rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }
}
