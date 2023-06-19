package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.*;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.lamett.util.SPE;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public record Elaborator(
  @NotNull MutableMap<DefVar<?>, Def> sigma,
  @NotNull MutableMap<LocalVar, Term> gamma,
  @NotNull Unifier unifier
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return term.subst(unifier.unification().toSubst());
  }

  @NotNull public Term.Cofib normalize(@NotNull Term.Cofib cofib) {
    return new Normalizer(unifier.unification().toSubst()).term(cofib);
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {}

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (normalize(type) instanceof Term.Pi dt)
          yield new Term.Lam(lam.x(), hof(lam.x(), dt.param().type(), () ->
            inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        else throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      }
      case Expr.Pair two -> {
        if (!(normalize(type) instanceof Term.Sigma dt)) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.a(), dt.param().type());
        yield new Term.Tuple(lhs, inherit(two.b(), dt.codomain(lhs)));
      }
      case Expr.PartEl elem -> {
        if (!(normalize(type) instanceof Term.Partial partial)) throw new SPE(elem.pos(),
          Doc.english("Expects a partial type for"), expr, Doc.plain("got"), type);
        var elems = elem.elems().flatMap(tup -> {
          var cofib = checkCofib(tup.component1());
          cofib = normalize(cofib);
          assert cofib.params().isEmpty();
          return cofib.conjs().mapNotNull(conj -> unifier.withCofibConj(
            conj, () -> new Tuple2<>(conj, inherit(tup.component2(), partial.type())), null));
        });
        for (var i = 0; i < elems.size(); i++) {
          for (var j = i + 1; j < elems.size(); j++) {
            var conj = elems.get(i).component1().conj(elems.get(j).component1());
            var term1 = elems.get(i).component2();
            var term2 = elems.get(j).component2();
            unifier.withCofibConj(conj, () -> {
              unify(normalize(term1), normalize(partial.type()), normalize(term2), elem.pos());
              return null;
            }, null);
          }
        }
        unify(new Term.Cofib(ImmutableSeq.empty(), elems.map(Tuple2::component1)), Term.F, partial.cofib(), elem.pos());
        yield new Term.PartEl(elems);
      }
      case Expr.Hole hole -> {
        var docs = MutableList.<Doc>create();
        gamma.forEach((k, v) -> {
          var list = MutableList.of(Doc.plain(k.name()));
          if (!hole.accessible().contains(k)) list.append(Doc.english("(out of scope)"));
          list.appendAll(new Doc[]{Doc.symbol(":"), normalize(v).toDoc()});
          docs.append(Doc.sep(list));
        });
        docs.append(Doc.plain("----------------------------------"));
        var tyDoc = type.toDoc();
        docs.append(tyDoc);
        var normDoc = normalize(type).toDoc();
        if (!tyDoc.equals(normDoc)) {
          docs.append(Doc.symbol("|->"));
          docs.append(normDoc);
        }
        throw new SPE(hole.pos(), Doc.vcat(docs));
      }
      default -> {
        var synth = synth(expr);
        unify(normalize(type), synth.wellTyped, synth.type, expr.pos());
        yield synth.wellTyped;
      }
    };
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    unify(ty, actual, pos, u -> unifyDoc(ty, on, actual, u));
  }

  private void unify(Term ty, Term actual, SourcePos pos, Function<Unifier, Doc> message) {
    if (!unifier.untyped(actual, ty))
      throw new SPE(pos, message.apply(unifier));
  }

  private static @NotNull Doc unifyDoc(Docile ty, Docile on, Docile actual, Unifier unifier) {
    var line1 = Doc.sep(Doc.plain("Umm,"), ty.toDoc(), Doc.plain("!="),
      actual.toDoc(), Doc.english("on"), on.toDoc());
    if (unifier.data != null) {
      var line2 = Doc.sep(Doc.english("In particular,"),
        unifier.data.l().toDoc(), Doc.symbol("!="), unifier.data.r().toDoc());
      line1 = Doc.vcat(line1, line2);
    }
    return line1;
  }

  public Synth synth(Expr expr) {
    var synth = switch (expr) {
      case Expr.Unresolved unresolved -> throw new InternalError("Unresolved expr: " + unresolved);
      case Expr.Kw(var $, var kw) when
          kw == Keyword.I -> new Synth(new Term.Lit(kw), Term.ISet);
      case Expr.Kw(var $, var kw) when
        kw == Keyword.F ||
          kw == Keyword.ISet ||
          kw == Keyword.Set -> new Synth(new Term.Lit(kw), Term.Set);
      case Expr.Kw(var $, var kw) when
          kw == Keyword.U -> new Synth(new Term.Lit(kw), Term.U);
      case Expr.Kw(var $, var kw) when
        kw == Keyword.Zero ||
          kw == Keyword.One -> new Synth(new Term.Lit(kw), Term.I);
      case Expr.Resolved resolved -> switch (resolved.ref()) {
        case DefVar<?> defv -> {
          var def = defv.core;
          if (def == null) {
            var sig = defv.signature;
            var pi = Term.mkPi(sig.telescope(), sig.result());
            var call = mkCall(defv, sig);
            yield new Synth(Normalizer.rename(Term.mkLam(
              sig.teleVars(), call)), pi);
          }
          var pi = Term.mkPi(def.telescope(), def.result());
          yield switch (def) {
            case Def.Fn fn -> new Synth(Normalizer.rename(Term.mkLam(
              fn.teleVars(), new Term.FnCall(fn.name(), fn.teleRefs().toImmutableSeq()))), pi);
            case Def.Print print -> throw new AssertionError("unreachable: " + print);
            case Def.Cons cons -> new Synth(Normalizer.rename(Term.mkLam(
              cons.teleVars(), new Term.ConCall(cons.name(),
                cons.tele().map(x -> new Term.Ref(x.x())),
                cons.owner().core.teleRefs().toImmutableSeq()))), pi);
            case Def.Data data -> new Synth(Normalizer.rename(Term.mkLam(
              data.teleVars(), new Term.DataCall(data.name(), data.teleRefs().toImmutableSeq()))), pi);
          };
        }
        case LocalVar loc -> new Synth(new Term.Ref(loc), gamma.get(loc));
      };
      case Expr.Proj(var pos, var t, var isOne) -> {
        var t_ = synth(t);
        if (!(t_.type instanceof Term.Sigma dt))
          throw new SPE(pos, Doc.english("Expects a left adjoint, got"), t_.type);
        var fst = t_.wellTyped.proj(true);
        if (isOne) yield new Synth(fst, dt.param().type());
        yield new Synth(t_.wellTyped.proj(false), dt.codomain(fst));
      }
      case Expr.App two -> {
        var f = synth(two.f());
        if (!(f.type instanceof Term.Pi dt))
          throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
        var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
        yield new Synth(f.wellTyped.app(a), dt.codomain(a));
      }
      case Expr.Pair two -> {
        var a = synth(two.a());
        var b = synth(two.b());
        yield new Synth(new Term.Tuple(b.wellTyped, a.wellTyped),
          new Term.Sigma(new Param<>(new LocalVar("_"), b.type), a.type));
      }
      case Expr.DT dt -> {
        var param = synth(dt.param().type());
        var x = dt.param().x();
        var cod = hof(x, param.wellTyped, () -> synth(dt.cod()));
        assert param.type() instanceof Term.Lit;
        var domKw = ((Term.Lit) param.type()).keyword();
        var codKw = ((Term.Lit) cod.type()).keyword();
        Term.Lit resType;
        if (domKw == Keyword.ISet) {
          if (codKw == Keyword.U) {
            resType = Term.U;
          } else {
            resType = Term.Set;
          }
        } else {
          if (codKw == Keyword.ISet)
            throw new SPE(dt.pos(), Doc.english("Expects a U or Set in codomain, got"), cod.type);
          if (domKw == Keyword.U && codKw == Keyword.U) {
            resType = Term.U;
          } else {
            resType = Term.Set;
          }
        }
        yield new Synth(
          dt instanceof Expr.Pi
            ? new Term.Pi(new Param<>(x, param.wellTyped), cod.wellTyped)
            : new Term.Sigma(new Param<>(x, param.wellTyped), cod.wellTyped),
          resType);
      }
      case Expr.INeg neg -> {
        var t = inherit(neg.body(), Term.I);
        yield new Synth(new Term.INeg(t), Term.I);
      }
      case Expr.Cofib cofib -> new Synth(checkCofib(cofib), Term.F);
      case Expr.Partial partial -> {
        var cofib = checkCofib(partial.cofib());
        var type = inherit(partial.type(), Term.U);
        yield new Synth(new Term.Partial(cofib, type), Term.Set);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    return new Synth(synth.wellTyped, type);
  }

  public Term.Cofib checkCofib(Expr expr) {
    return switch (expr) {
      case Expr.CofibEq eq -> {
        var lhs = inherit(eq.lhs(), Term.I);
        var rhs = inherit(eq.rhs(), Term.I);
        yield Term.Cofib.eq(lhs, rhs);
      }
      case Expr.CofibConj conj -> {
        var lhs = checkCofib(conj.lhs());
        var rhs = checkCofib(conj.rhs());
        yield lhs.conj(rhs);
      }
      case Expr.CofibDisj disj -> {
        var lhs = checkCofib(disj.lhs());
        var rhs = checkCofib(disj.rhs());
        yield lhs.disj(rhs);
      }
      case Expr.CofibForall forall -> {
        var phi = hof(forall.i(), Term.I, () -> checkCofib(forall.body()));
        yield phi.forall(forall.i());
      }
      default -> Term.Cofib.atom(inherit(expr, Term.F));
    };
  }

  @SuppressWarnings("unchecked") private static Term mkCall(DefVar<?> defv, Def.Signature sig) {
    return sig.isData() ? new Term.DataCall((DefVar<Def.Data>) defv,
      sig.teleRefs().toImmutableSeq()) : new Term.FnCall((DefVar<Def.Fn>) defv,
      sig.teleRefs().toImmutableSeq());
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Term type, @NotNull Supplier<T> t) {
    gamma.put(x, type);
    var ok = t.get();
    gamma.remove(x);
    return ok;
  }

  public @NotNull Def def(@NotNull Decl def) {
    var telescope = telescope(def.tele());
    return switch (def) {
      case Decl.Fn fn -> {
        var result = synth(fn.result());
        if (!((Term.Lit) result.type()).isUniv())
          throw new SPE(fn.result().pos(), Doc.english("Expects a type, got"), result.wellTyped());
        fn.name().signature = new Def.Signature(false, telescope, result.wellTyped());
        var body = fn.body().map(
          expr -> inherit(expr, result.wellTyped()),
          clauses -> tyckFunBody(telescope, result.wellTyped(), clauses.getLeftValue())
        );
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Fn(fn.name(), telescope, result.wellTyped(), body);
      }
      case Decl.Print print -> {
        var result = synth(print.result());
        if (!((Term.Lit) result.type()).isUniv())
          throw new SPE(print.result().pos(), Doc.english("Expects a type, got"), result.wellTyped());
        var body = inherit(print.body(), result.wellTyped());
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Print(telescope, result.wellTyped(), body);
      }
      case Decl.Cons ignored -> throw new IllegalArgumentException("unreachable");
      case Decl.Data data -> {
        var ref = data.name();
        ref.signature = new Def.Signature(true, telescope, Term.U);
        yield new Def.Data(ref, telescope, data.cons().map(c -> cons(ref, c)));
      }
    };
  }

  private Pat.ClauseSet<Term> tyckFunBody(
    ImmutableSeq<Param<Term>> telescope, Term result,
    Pat.ClauseSet<Expr> clauseSet
  ) {
    var clauses = new Pat.ClauseSet<>(clauseSet.clauses().map(c ->
      new Matchy(this).clause(telescope, result, c)));
    Classifier.classify(clauses, telescope);
    return clauses;
  }

  private Def.Cons cons(DefVar<Def.Data> ref, Decl.Cons c) {
    return new Def.Cons(c.name(), ref, telescope(c.tele()));
  }

  private @NotNull ImmutableSeq<Param<Term>> telescope(Decl.Tele tele) {
    var telescope = MutableArrayList.<Param<Term>>create(tele.scope().size());
    for (var param : tele.scope()) {
      var ty = synth(param.type());
      if (!((Term.Lit) ty.type()).isUniv())
        throw new SPE(param.type().pos(), Doc.english("Expects a type, got"), ty.wellTyped());
      telescope.append(new Param<>(param.x(), ty.wellTyped()));
      gamma.put(param.x(), ty.wellTyped());
    }
    return telescope.toImmutableArray();
  }
}
