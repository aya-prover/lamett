package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Unifier {
  public record FailureData(@NotNull Term l, @NotNull Term r) {}
  public FailureData data;

  public Term.Cofib.Conj getConjunction() {
    return conjunction;
  }

  /** @return {@literal false} if `conj` is `⊥`, thus any subsequent unification succeeds immediately */
  public boolean loadWhnfConj(Term.Cofib.Conj conj) {
    var unification = new Unification();
    for (var eq : conj.eqs()) {
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
        default -> throw new InternalException("not a whnf conj: found " + eq.rhs());
      }
    }
    this.conjunction = conj;
    this.unification = unification;
    return true;
  }

  private Term.Cofib.Conj conjunction = new Term.Cofib.Conj(ImmutableSeq.empty());
  // This is but a cache of the conjunction
  private Unification unification = new Unification();

  public boolean untyped(@NotNull Term oldL, @NotNull Term oldR) {
    if (oldL == oldR) return true;
    var normalizer = new Normalizer(unification.toSubst());
    final var l = normalizer.term(oldL);
    final var r = normalizer.term(oldR);
    var happy = switch (l) {
      case Term.Lam lam when r instanceof Term.Lam ram -> untyped(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll when r instanceof Term.Lam ram -> eta(ll, ram);
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
      case Term.Cofib lphi when r instanceof Term.Cofib rphi -> cofibImply(lphi, rphi) && cofibImply(rphi, lphi);
      // `Ref`s, and `INeg`s
      case Term.Ref lref when r instanceof Term.Ref rref -> unification.unify(
        Unification.LocalVarWithNeg.from(lref.var()), Unification.LocalVarWithNeg.from(rref.var()));
      case Term.Ref lref when r instanceof Term.INeg rneg && rneg.body() instanceof Term.Ref -> unification.unify(
        Unification.LocalVarWithNeg.from(lref.var()), Unification.LocalVarWithNeg.from(rneg));
      case Term.INeg lneg when r instanceof Term.Ref rref && lneg.body() instanceof Term.Ref -> unification.unify(
        Unification.LocalVarWithNeg.from(lneg), Unification.LocalVarWithNeg.from(rref.var()));
      case Term.INeg lineg when r instanceof Term.INeg rineg -> lineg.body() == rineg.body();
      default -> false;
    };
    if (!happy && data == null)
      data = new FailureData(l, r);
    return happy;
  }

  boolean cofibIsTrue(@NotNull Term.Cofib p) {
    return p.conjs().anyMatch(conj -> conj.eqs().allMatch(eq -> untyped(eq.lhs(), eq.rhs())));
  }

  boolean cofibImply(@NotNull Term.Cofib p, @NotNull Term.Cofib q) {
    var oldConj = conjunction;
    var res = p.conjs().allMatch(conj -> loadWhnfConj(conj.conj(oldConj)) && cofibIsTrue(q));
    loadWhnfConj(oldConj);
    return res;
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

  public class Unification {
    public Unification() {}
    private class Node {
      int rank = 0;
      @NotNull Either<@NotNull Node, @Nullable Boolean> parentOrTerm;
      Node( @Nullable Boolean term) {
        this.parentOrTerm = Either.right(term);
      }
      @NotNull Node root() {
        var node = this;
        while (node.parentOrTerm.isLeft())
          node = node.parentOrTerm.getLeftValue();
        return node;
      }
      boolean union(@NotNull Node rhs) {
        var lhs = this.root();
        rhs = rhs.root();
        if (lhs == rhs) return true;

        var lterm = lhs.parentOrTerm.getRightValue();
        var rterm = rhs.parentOrTerm.getRightValue();
        if (lterm != null) {
          if (lterm.equals(rterm)) return false;
        } else {
          lterm = rterm;
        }

        if (lhs.rank < rhs.rank) {
          lhs.parentOrTerm = Either.left(rhs);
          rhs.parentOrTerm = Either.right(lterm);
        } else if (lhs.rank > rhs.rank) {
          rhs.parentOrTerm = Either.left(lhs);
          lhs.parentOrTerm = Either.right(lterm);
        } else {
          lhs.parentOrTerm = Either.left(rhs);
          rhs.parentOrTerm = Either.right(lterm);
          rhs.rank++;
        }
        return true;
      }
      @Nullable Boolean get() {
        var node = this.root();
        return node.parentOrTerm.getRightValue();
      }
      boolean set(@NotNull Boolean term) {
        var node = this.root();
        var oldTerm = node.parentOrTerm.getRightValue();
        if (oldTerm != null) {
          return term.equals(oldTerm);
        } else {
          node.parentOrTerm = Either.right(term);
          return true;
        }
      }
    }
    public record LocalVarWithNeg(@NotNull LocalVar var, boolean sign) {
      public LocalVarWithNeg negate() {
        return new LocalVarWithNeg(var, !sign);
      }
      /** @param term is whnf */
      static @NotNull LocalVarWithNeg from(Term term) {
        return switch (term) {
          case Term.Ref(var var) -> new LocalVarWithNeg(var, true);
          case Term.INeg(var body) when body instanceof Term.Ref(var var) -> new LocalVarWithNeg(var, false);
          default -> throw new InternalException("not a local var");
        };
      }
      static @NotNull LocalVarWithNeg from(LocalVar var) {
        return new LocalVarWithNeg(var, true);
      }
    }

    /** @implNote Invariance: `i` and `¬ i` must both exist or not exist in the map,
      * and their value, if existed, must be each other's negation.
      */
    private final @NotNull MutableMap<@NotNull LocalVarWithNeg, @NotNull Node> vars = MutableMap.create();

    private Tuple2<Node, Node> findOrCreateNode(@NotNull LocalVarWithNeg var) {
      var node = vars.getOrNull(var);
      var nodeNeg = vars.getOrNull(var.negate());
      if (node == null) {
        node = new Node(null);
        nodeNeg = new Node(null);
        vars.put(var, node);
        vars.put(var.negate(), nodeNeg);
      }
      assert nodeNeg != null;
      return new Tuple2<>(node, nodeNeg);
    }
    public boolean setEquiv(@NotNull LocalVarWithNeg lhs, @NotNull LocalVarWithNeg rhs) {
      if (lhs.var == rhs.var && lhs.sign == rhs.sign) return true;
      var l = findOrCreateNode(lhs);
      var r = findOrCreateNode(rhs);
      return l.component1().union(r.component1()) && l.component2().union(r.component2());
    }
    public boolean setValue(LocalVarWithNeg var, Boolean term) {
      var n = findOrCreateNode(var);
      return n.component1().set(term) && n.component2().set(!term);
    }
    public boolean unify(LocalVarWithNeg lhs, LocalVarWithNeg rhs) {
      if (lhs.var == rhs.var) return lhs.sign == rhs.sign;
      var lnode = vars.getOrNull(lhs);
      var rnode = vars.getOrNull(rhs);
      if (lnode != null && rnode != null) {
        lnode = lnode.root();
        rnode = rnode.root();
        if (lnode == rnode) return true;
        var lterm = lnode.parentOrTerm.getRightValue();
        var rterm = rnode.parentOrTerm.getRightValue();
        if (lterm != null) {
          return lterm.equals(rterm);
        }
      }
      return false;
    }
    public @Nullable Boolean getValue(LocalVarWithNeg var) {
      var node = vars.getOrNull(var);
      if (node != null) {
        return node.get();
      } else {
        return null;
      }
    }
    public @NotNull MutableMap<LocalVar, Term> toSubst() {
      var map = MutableMap.<LocalVar, Term>create();
      for(var var : vars.keysView()) {
        var node = vars.getOrNull(var);
        if (node != null) {
          var term = node.root().parentOrTerm.getRightValue();
          if (term != null) {
            map.put(var.var, Term.Lit.fromBool(var.sign ^ term));
          }
        }
      }
      return map;
    }
  }
}
