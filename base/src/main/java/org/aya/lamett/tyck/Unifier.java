package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.collection.mutable.MutableSet;
import kala.control.Either;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public Term.Cofib.Conj conjunction() {
    return conjunction;
  }

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

  public static class Unification {
    public Unification() {}

    private static class Node {
      int rank = 0;
      @NotNull Either<@NotNull Node, @Nullable Boolean> parentOrTerm;

      Node(@Nullable Boolean term) {
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
          if (rterm != null && lterm != rterm) return false;
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

    /**
     * @implNote Invariance: `i` and `¬ i` must both exist or not exist in the map,
     * and their value, if existed, must be each other's negation.
     */
    private final @NotNull MutableMap<@NotNull LocalVarWithNeg, @NotNull Node> vars = MutableMap.create();
    final @NotNull MutableSet<@NotNull LocalVar> cofibVars = MutableSet.create();

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
      for (var var : vars.keysView()) {
        var node = vars.getOrNull(var);
        if (node != null) {
          var term = node.root().parentOrTerm.getRightValue();
          if (term != null) {
            map.put(var.var, Term.Lit.fromBool(!var.sign ^ term));
          }
        }
      }
      for (var cofibVar : cofibVars) {
        map.put(cofibVar, Term.Cofib.known(true));
      }
      return map;
    }
  }
}
