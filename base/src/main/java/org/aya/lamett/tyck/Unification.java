package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.collection.mutable.MutableSet;
import kala.control.Either;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unification of interval terms.
 */
public class Unification {
  public Unification() {}

  public Unification(Term.Cofib.Conj conj) {
    if (!addNFConj(conj)) throw new InternalException("loading a false conj: " + conj);
    conjunction = conj;
  }
  private Term.Cofib.Conj conjunction = new Term.Cofib.Conj(ImmutableSeq.empty());

  /** @return {@literal false} if `conj` is `⊥`, thus any subsequent unification succeeds immediately */
  public boolean addNFConj(Term.Cofib.Conj conj) {
    for (var atom : conj.atoms()) {
      switch (atom) {
        case Term.Cofib.Eq eq -> {
          assert eq.lhs() instanceof Term.Ref;
          var lvar = Unification.LocalVarWithNeg.from(eq.lhs());
          switch (eq.rhs()) {
            case Term.Ref(var var) -> {
              if (!setEquiv(lvar, Unification.LocalVarWithNeg.from(var))) return false;
            }
            case Term.INeg(var body) when body instanceof Term.Ref -> {
              if (!setEquiv(lvar, Unification.LocalVarWithNeg.from(body))) return false;
            }
            case Term.Lit lit when lit.keyword() == Keyword.One -> {
              if (!setValue(lvar, true)) return false;
            }
            case Term.Lit lit when lit.keyword() == Keyword.Zero -> {
              if (!setValue(lvar, false)) return false;
            }
            default -> throw new InternalException("not a nf conj: found " + eq.rhs());
          }
        }
        case Term.Ref(var ref) -> cofibVars.add(ref);
        default -> throw new InternalException("not a nf conj: found " + atom);
      }
    }
    conjunction = conjunction.conj(conj);
    return true;
  }

  public Unification derive() {
    return new Unification(conjunction);
  }

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
    return Tuple.of(node, nodeNeg);
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
