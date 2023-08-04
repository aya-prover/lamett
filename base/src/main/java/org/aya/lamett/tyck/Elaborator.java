package org.aya.lamett.tyck;

import kala.collection.Map;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
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

import static org.aya.lamett.tyck.Normalizer.rename;

public record Elaborator(
  @NotNull MutableMap<DefVar<?>, Def> sigma,
  @NotNull MutableMap<LocalVar, Type> gamma,
  @NotNull Unifier unifier
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return term.subst(unifier.unification().toSubst());
  }

  @NotNull public Type normalize(@NotNull Type type) {
    return type.subst(unifier.unification().toSubst());
  }

  @NotNull public Term.Cofib normalize(@NotNull Term.Cofib cofib) {
    return new Normalizer(unifier.unification().toSubst()).term(cofib);
  }

  public Type el(Term tm) {
    return tarski().el(tm);
  }

  private @NotNull WeaklyTarski tarski() {
    return new WeaklyTarski(new Normalizer(unifier));
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Type type) {}

  public Term inherit(Expr expr, Type type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (normalize(type) instanceof Type.Pi dt)
          yield new Term.Lam(lam.x(), hof(lam.x(), dt.param().type(), () ->
            inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        else throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      }
      case Expr.Pair two -> {
        if (!(normalize(type) instanceof Type.Sigma dt)) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.a(), dt.param().type());
        yield new Term.Pair(lhs, inherit(two.b(), dt.codomain(lhs)));
      }
      case Expr.PartEl elem -> elaboratePartial(elem, type, true);
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
        if (synth.type == Type.Lit.F && synth.wellTyped instanceof Term.Ref) {
          yield Term.Cofib.atom(synth.wellTyped);
        } else {
          yield synth.wellTyped;
        }
      }
    };
  }

  private void unify(Type ty, Docile on, @NotNull Type actual, SourcePos pos) {
    unify(ty, actual, pos, u -> unifyDoc(ty, on, actual, u));
  }

  private void unify(Type ty, Type actual, SourcePos pos, Function<Unifier, Doc> message) {
    if (!unifier.type(actual, ty))
      throw new SPE(pos, message.apply(unifier));
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    if (!unifier.untyped(actual, ty))
      throw new SPE(pos, unifyDoc(ty, on, actual, unifier));
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
        kw == Keyword.I -> new Synth(new Term.Lit(kw), Type.Lit.ISet);
      case Expr.Kw(var $, var kw) when
        kw == Keyword.F ||
          kw == Keyword.ISet ||
          kw == Keyword.Set -> new Synth(new Term.Lit(kw), Type.Lit.Set);
      case Expr.Kw(var $, var kw) when
        kw == Keyword.U -> new Synth(new Term.Lit(kw), Type.Lit.U);
      case Expr.Kw(var $, var kw) when
        kw == Keyword.Zero ||
          kw == Keyword.One -> new Synth(new Term.Lit(kw), Type.Lit.I);
      case Expr.Resolved resolved -> switch (resolved.ref()) {
        case DefVar<?> defv -> {
          var def = defv.core;
          if (def == null) {
            var sig = defv.signature;
            yield new Synth(rename(Term.mkLam(
              sig.teleVars(), mkCall(defv, sig))), sig.type(tarski()));
          }
          var pi = def.type(tarski());
          yield switch (def) {
            case Def.Fn fn -> new Synth(rename(Term.mkLam(
              fn.teleVars(), new Term.FnCall(fn.name(), fn.teleRefs().toImmutableSeq()))), pi);
            case Def.Print print -> throw new AssertionError("unreachable: " + print);
            case Def.Cons cons -> new Synth(rename(Term.mkLam(
              cons.teleVars(), new Term.ConCall(cons.name(),
                cons.tele().map(x -> new Term.Ref(x.x())),
                cons.owner().core.teleRefs().toImmutableSeq()))), pi);
            case Def.Data data -> new Synth(rename(Term.mkLam(
              data.teleVars(), new Term.DataCall(data.name(), data.teleRefs().toImmutableSeq()))), pi);
          };
        }
        case LocalVar loc -> new Synth(new Term.Ref(loc), gamma.get(loc));
      };
      case Expr.Proj(var pos, var t, var isOne) -> {
        var t_ = synth(t);
        if (!(t_.type instanceof Type.Sigma dt))
          throw new SPE(pos, Doc.english("Expects a left adjoint, got"), t_.type);
        var fst = t_.wellTyped.proj(true);
        if (isOne) yield new Synth(fst, dt.param().type());
        yield new Synth(t_.wellTyped.proj(false), dt.codomain(fst));
      }
      case Expr.App two -> {
        var f = synth(two.f());
        if (!(f.type instanceof Type.Pi dt))
          throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
        var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
        yield new Synth(f.wellTyped.app(a), dt.codomain(a));
      }
      case Expr.Pair two -> {
        var a = synth(two.a());
        var b = synth(two.b());
        yield new Synth(new Term.Pair(b.wellTyped, a.wellTyped),
          new Type.Sigma(new Param<>(new LocalVar("_"), b.type), a.type));
      }
      case Expr.DT dt -> {
        var param = synth(dt.param().type());
        var x = dt.param().x();
        var cod = hof(x, el(param.wellTyped), () -> synth(dt.cod()));
        assert param.type() instanceof Type.Lit;
        var domKw = (Type.Lit) param.type();
        var codKw = (Type.Lit) cod.type();
        var resType = computeMax(domKw, codKw, cod, dt.pos());
        yield new Synth(
          dt instanceof Expr.Pi
            ? new Term.Pi(new Param<>(x, param.wellTyped), cod.wellTyped)
            : new Term.Sigma(new Param<>(x, param.wellTyped), cod.wellTyped),
          resType);
      }
      case Expr.INeg neg -> {
        var t = inherit(neg.body(), Type.Lit.I);
        yield new Synth(new Term.INeg(t), Type.Lit.I);
      }
      case Expr.Cofib cofib -> new Synth(checkCofib(cofib), Type.Lit.F);
      case Expr.PrimCall prim -> switch (prim.type()) {
        case Partial -> {
          var phi = new LocalVar("φ");
          var A = new LocalVar("A");
          var term = Term.mkLam(
            ImmutableSeq.of(phi, A).view(),
            new Term.PartTy(new Term.Ref(phi), new Term.Ref(A)));
          var type = Type.mkPi(Type.Lit.F, Type.mkPi(Type.Lit.U, Type.Lit.U));
          yield new Synth(term, type);
        }
        case Coe, Hcom -> throw new UnsupportedOperationException("TODO");
        case Sub -> {
          // Sub (A : U) (φ : F) (partEl : Partial φ A) : Set
          var A = new LocalVar("A");
          var phi = new LocalVar("φ");
          var partEl = new LocalVar("partEl");
          var term = Term.mkLam(
            ImmutableSeq.of(A, phi, partEl).view(),
            new Term.Sub(new Term.Ref(A), new Term.Ref(partEl)));
          var type = Type.mkPi(ImmutableSeq.of(
              new Param<>(A, Type.Lit.U),
              new Param<>(phi, Type.Lit.F),
              new Param<>(partEl, new Type.El(new Term.PartTy(new Term.Ref(phi), new Term.Ref(A))))
            ),
            Type.Lit.Set);
          yield new Synth(term, type);
        }
        case InS -> {
          // inS (A : U) (φ : F) (of : A) : Sub A φ {|φ ↦ of|}
          var A = new LocalVar("A");
          var phi = new LocalVar("φ");
          var of = new LocalVar("of");
          var term = Term.mkLam(
            ImmutableSeq.of(A, phi, of).view(),
            new Term.InS(new Term.Ref(phi), new Term.Ref(of)));
          var type = Type.mkPi(ImmutableSeq.of(
              new Param<>(A, Type.Lit.U),
              new Param<>(phi, Type.Lit.F),
              new Param<>(of, Type.ref(A))
            ),
            new Type.Sub(new Type.El(new Term.Ref(A)), ImmutableSeq.of(
              Tuple.of(new Term.Conj(ImmutableSeq.of(new Term.Ref(phi))), new Term.Ref(of))
            )));
          yield new Synth(term, type);
        }
        case OutS -> {
          // outS (A : U) (φ : F) (partEl : Partial φ A) (of : Sub A φ partEl) : A
          var A = new LocalVar("A");
          var phi = new LocalVar("φ");
          var partEl = new LocalVar("partEl");
          var of = new LocalVar("of");
          var term = Term.mkLam(
            ImmutableSeq.of(A, phi, partEl, of).view(),
            new Term.OutS(new Term.Ref(phi), new Term.Ref(partEl), new Term.Ref(of)));
          var type = Type.mkPi(ImmutableSeq.of(
              new Param<>(A, Type.Lit.U),
              new Param<>(phi, Type.Lit.F),
              new Param<>(partEl, new Type.El(new Term.PartTy(new Term.Ref(phi), new Term.Ref(A)))),
              new Param<>(of, new Type.El(new Term.Sub(new Term.Ref(A), new Term.Ref(partEl))))
            ),
            Type.ref(A));
          yield new Synth(term, type);
        }
      };
      case Expr.Ext ext -> hhof(ext.i().map(x -> Tuple.of(x, Type.Lit.I)), () -> {
        var codeType = synth(ext.type()).wellTyped;
        var ty = el(codeType);
        var partial = elaboratePartial(ext.partial(), new Type.PartTy(ty, ImmutableSeq.of() /* TODO: face check */), false);
        var wellTyped = new Term.Path(ext.i(), new Term.Ext<>(codeType, Restr.Cubical.fromPartial(partial)));
        return new Synth(wellTyped, Type.Lit.U);
      });
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    return new Synth(synth.wellTyped, type);
  }

  private static @NotNull Type.Lit computeMax(Type.Lit domKw, Type.Lit codKw, Synth cod, @NotNull SourcePos pos) {
    if (domKw == Type.Lit.ISet) return codKw == Type.Lit.U ? Type.Lit.U : Type.Lit.Set;
    if (codKw == Type.Lit.ISet)
      throw new SPE(pos, Doc.english("Expects a U or Set in codomain, got"), cod.type);
    return domKw == Type.Lit.U && codKw == Type.Lit.U ? Type.Lit.U : Type.Lit.Set;
  }

  public Term.Cofib checkCofib(Expr expr) {
    return switch (expr) {
      case Expr.CofibEq eq -> {
        var lhs = inherit(eq.lhs(), Type.Lit.I);
        var rhs = inherit(eq.rhs(), Type.Lit.I);
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
        var phi = hof(forall.i(), Type.Lit.I, () -> checkCofib(forall.body()));
        yield phi.forall(forall.i());
      }
      default -> Term.Cofib.atom(inherit(expr, Type.Lit.F));
    };
  }

  @SuppressWarnings("unchecked") private static Term mkCall(DefVar<?> defv, Def.Signature sig) {
    return sig.isData() ? new Term.DataCall((DefVar<Def.Data>) defv,
      sig.teleRefs().toImmutableSeq()) : new Term.FnCall((DefVar<Def.Fn>) defv,
      sig.teleRefs().toImmutableSeq());
  }

  private <T> T hhof(@NotNull ImmutableSeq<Tuple2<LocalVar, Type>> xs, @NotNull Supplier<T> t) {
    var map = Map.from(xs);
    gamma.putAll(map);
    var ook = t.get();
    gamma.removeAll(map.keysView());
    return ook;
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Type type, @NotNull Supplier<T> t) {
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
        if (!((Type.Lit) result.type()).isUniv())
          throw new SPE(fn.result().pos(), Doc.english("Expects a type, got"), result.wellTyped());
        fn.name().signature = new Def.Signature(false, telescope, result.wellTyped());
        var body = fn.body().map(
          expr -> inherit(expr, el(result.wellTyped())),
          clauses -> tyckFunBody(telescope, result.wellTyped(), clauses.getLeftValue())
        );
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Fn(fn.name(), telescope, result.wellTyped(), body);
      }
      case Decl.Print print -> {
        var result = synth(print.result());
        if (!((Type.Lit) result.type()).isUniv())
          throw new SPE(print.result().pos(), Doc.english("Expects a type, got"), result.wellTyped());
        var body = inherit(print.body(), el(result.wellTyped()));
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

  /**
   * @param faceCheck i am sorry
   * @author Alias Qli
   */
  private @NotNull Term.PartEl elaboratePartial(@NotNull Expr.PartEl elem, @NotNull Type type, boolean faceCheck) {
    if (!(normalize(type) instanceof Type.PartTy partTy)) throw new SPE(elem.pos(),
      Doc.english("Expects a partial type for"), elem, Doc.plain("got"), type);
    var elems = elem.elems().flatMap(tup -> {
      var cofib = normalize(checkCofib(tup.component1()));
      assert cofib.params().isEmpty();
      // type check
      return cofib.conjs().mapNotNull(conj -> unifier.withCofibConj(
        conj, () -> Tuple.of(conj, inherit(tup.component2(), partTy.underlying())), null));
    });

    // confluence check
    for (var i = 0; i < elems.size(); i++) {
      for (var j = i + 1; j < elems.size(); j++) {
        var cls1 = elems.get(i);
        var cls2 = elems.get(j);
        var conj = cls1.component1().conj(cls2.component1());
        var term1 = cls1.component2();
        var term2 = cls2.component2();
        unifier.withCofibConj(conj, () -> {
          unify(term1, partTy.underlying(), term2, elem.pos());
          return null;
        }, null);
      }
    }

    if (faceCheck) {
      var ty = new Term.Cofib(ImmutableSeq.empty(), elems.map(Tuple2::component1));
      var restrCofib = new Term.Cofib(ImmutableSeq.empty(), partTy.restrs());
      unify(ty, Term.F, restrCofib, elem.pos());
    }

    return new Term.PartEl(elems);
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
      if (!((Type.Lit) ty.type()).isUniv())
        throw new SPE(param.type().pos(), Doc.english("Expects a type, got"), ty.wellTyped());
      telescope.append(new Param<>(param.x(), ty.wellTyped()));
      gamma.put(param.x(), el(ty.wellTyped()));
    }
    return telescope.toImmutableArray();
  }
}
