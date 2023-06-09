package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.jetbrains.annotations.NotNull;

public class Unifier {
  public record FailureData(@NotNull Term l, @NotNull Term r) {}
  public FailureData data;

  public boolean untyped(@NotNull Term l, @NotNull Term r) {
    if (l == r) return true;
    var happy = switch (l) {
      case Term.Lam lam when r instanceof Term.Lam ram -> untyped(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll when r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.Ref lref when r instanceof Term.Ref rref -> lref.var() == rref.var();
      case Term.Two lapp when r instanceof Term.Two rapp ->
        lapp.getClass().equals(rapp.getClass()) && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt when r instanceof Term.DT rdt -> ldt.getClass().equals(rdt.getClass())
        && untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param().x()));
      case Term.Proj lproj when r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.Lit lu when r instanceof Term.Lit ru -> lu.keyword() == ru.keyword();
      case Term.FnCall lcall when r instanceof Term.FnCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      case Term.DataCall lcall when r instanceof Term.DataCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      // We probably won't need to compare dataArgs cus the two sides of conversion should be of the same type
      case Term.ConCall lcall when r instanceof Term.ConCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      case Term.INeg lineg when r instanceof Term.INeg rineg -> lineg.body() == rineg.body();
      case Term.Cofib lphi when r instanceof Term.Cofib rphi -> cofibImply(lphi, rphi) && cofibImply(rphi, lphi);
      default -> false;
    };
    if (!happy && data == null)
      data = new FailureData(l, r);
    return happy;
  }

  boolean cofibImply(@NotNull Term.Cofib p, @NotNull Term.Cofib q) {
    return p.conjs().allMatch(conj -> new Normalizer(conj.whnfToSubst()).term(q).isTrue());
  }

  private boolean unifySeq(@NotNull ImmutableSeq<Term> l, @NotNull ImmutableSeq<Term> r) {
    return l.allMatchWith(r, this::untyped);
  }

  private boolean eta(@NotNull Term r, Term.Lam lam) {
    return untyped(lam.body(), r.app(new Term.Ref(lam.x())));
  }

  private static @NotNull Term rhs(Term rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }
}
