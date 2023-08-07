package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.*;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;


public class Normalizer {
  private final @NotNull MutableMap<LocalVar, Term> rho;

  public MutableMap<LocalVar, Term> rho() {return rho;}

  private final @NotNull Unification unification;

  /** @apiNote Arguments should agree */
  public Normalizer(@NotNull Unification unification) {
    this.unification = unification;
    this.rho = unification.toSubst();
  }

  public Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
    this.rho = rho;
    this.unification = new Unification();
  }

  public Normalizer(@NotNull MutableMap<LocalVar, Term> rho, @NotNull Unification unification) {
    this.rho = rho;
    this.unification = unification;
  }

  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public Type type(Type type) {
    return switch (type) {
      case Type.Lit lit -> lit;
      case Type.El(var tm) -> new WeaklyTarski(this).el(tm);
      case Type.Pi pi -> new Type.Pi(new Param<>(pi.param().x(), type(pi.param().type())), type(pi.cod()));
      case Type.Sigma sig -> new Type.Sigma(new Param<>(sig.param().x(), type(sig.param().type())), type(sig.cod()));
      case Type.PartTy partTy -> new Type.PartTy(type(partTy.underlying()), term(new Cofib(partTy.restrs())).conjs());
      case Type.Sub sub -> new Type.Sub(type(sub.underlying()), partEl(sub.restrs()));
    };
  }

  public Term term(Term term) {
    var unifier = new Unifier(unification);
    return switch (term) {
      case Term.Ref ref -> rho.getOption(ref.var()).map(Normalizer::rename).map(this::term).getOrDefault(ref);
      case Term.Lit u -> u;
      case Term.Lam(var x, var body) -> new Term.Lam(x, term(body));
      case Term.Pi dt -> new Term.Pi(param(dt.param()), term(dt.cod()));
      case Term.Sigma dt -> new Term.Sigma(param(dt.param()), term(dt.cod()));
      case Term.App app -> {
        var f = term(app.f());
        var a = term(app.a());
        if (!(f instanceof Term.Lam lam)) yield new Term.App(f, a);
        yield lam.apply(a);
      }
      case Term.Pair(var a, var b) -> new Term.Pair(term(a), term(b));
      case Term.Proj proj -> {
        var t = term(proj.t());
        if (!(t instanceof Term.Pair tup)) yield new Term.Proj(t, proj.isOne());
        yield proj.isOne() ? tup.f() : tup.a();
      }
      case Term.FnCall call -> {
        var fn = call.fn().core;
        var args = call.args().map(this::term);
        if (fn == null) yield new Term.FnCall(call.fn(), args);
        fn.teleVars().zip(args).forEach(rho::put);
        var bud = fn.body().fold(this::term, cls -> Matchy.unfold(cls, args).map(this::term).getOrElse(() -> new Term.FnCall(call.fn(), args)));
        fn.teleVars().forEach(rho::remove);
        yield bud;
      }
      case Term.ConCall conCall ->
        new Term.ConCall(conCall.fn(), conCall.args().map(this::term), conCall.dataArgs().map(this::term));
      case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
      case Term.INeg(var t) -> term(t).neg();
      case Cofib cofib -> term(cofib);
      case Cofib.Eq eq -> eq.map(this::term).norm();
      case Term.PartTy partTy -> new Term.PartTy(term(partTy.cofib()), term(partTy.type()));
      case Term.PartEl elem -> new Term.PartEl(partEl(elem.elems()));
      case Term.Error error -> error;
      case Term.Coe(var r, var s, var A) -> {
        r = term(r);
        s = term(s);
        A = term(A);

        if (unifier.untyped(r, s)) yield identity("c");

        var varI = new LocalVar("i");
        var codom = term(A.app(new Term.Ref(varI)));

        yield switch (codom) {
          case Term.Sigma sigma -> KanPDF.coeSigma(sigma, varI, r, s);
          case Term.Pi pi -> KanPDF.coePi(pi, new Term.Coe(r, s, A), varI);
          case Term.Lit(var lit) when lit == Keyword.U -> identity("u");
          default -> new Term.Coe(r, s, A);
        };
      }
      // TODO
      case Term.Hcom(var r, var s, var A, var i, var el) -> {
        r = term(r);
        s = term(s);
        A = term(A);
        el = term(el);

        if (unifier.untyped(r, s)) yield identity("u");

        yield switch (term(A)) {
          case Term.Sigma sigma -> KanPDF.hcomSigma(sigma, r, s, i, el);
          case Term.Pi pi -> KanPDF.hcomPi(pi, r, s, i, el);
          case Term.Lit(var lit) when lit == Keyword.U -> identity("u");
          default -> new Term.Hcom(r, s, A, i, el);
        };
      }
      case Term.Ext<?>(var type, var face) -> new Term.Ext<>(term(type),
        face.map(this::term, UnaryOperator.identity()));
      case Term.Path(var binders, Term.Ext<Restr.Cubical>(var type, Restr.Cubical(var bdry))) -> {
        var faces = partEl(bdry);
        yield new Term.Path(
          binders,
          new Term.Ext<>(term(type), new Restr.Cubical(faces))
        );
      }
      case Term.Sub(var A, var partEl) -> new Term.Sub(term(A), term(partEl));
      case Term.InS(var phi, var of) -> {
        var inPhi = term(phi);
        var inOf = term(of);
        if (inOf instanceof Term.OutS(var outPhi, var outPartEl, var outOf)
          && outPhi instanceof Cofib outCofib
          && inPhi instanceof Cofib inCofib) {
          assert unifier.untyped(outCofib, inCofib);
          yield term(outOf);
        }
        yield new Term.InS(inPhi, inOf);
      }
      case Term.OutS(var phi, var partEl, var of) -> {
        var outPhi = term(phi);
        var outPartEl = term(partEl);
        var outOf = term(of);
        if (outOf instanceof Term.InS(var inPhi, var inOf))
          yield term(inOf);
        // TODO[is-this-correct?]: check if the partial element is constant
        if (outPartEl instanceof Term.PartEl(var elems) && elems.sizeEquals(1)) {
          var only = elems.first();
          yield unifier.withCofibConj(only.component1(), only::component2,
            new Term.OutS(outPhi, outPartEl, outOf));
        }
        yield new Term.OutS(outPhi, outPartEl, outOf);
      }
    };
  }

  private @NotNull Term identity(String u) {
    var x = new LocalVar(u);
    return Term.mkLam(ImmutableSeq.of(x).view(), new Term.Ref(x));
  }

  public @NotNull Cofib term(Cofib cofib) {
    var res = Cofib.known(false);
    for (var conj : cofib.conjs())
      res = res.disj(term(conj));
    return res;
  }

  public @NotNull Cofib term(Cofib.Conj conj) {
    var cofib = Cofib.known(true);
    for (var atom : conj.atoms()) {
      atom = term(atom);
      switch (atom) {
        case Cofib.Eq eq -> {
          var unifier = new Unifier(unification);
          // I guess in fact `eq.lhs().equal(eq.rhs())` is enough
          if (unifier.untyped(eq.lhs(), eq.rhs())) continue;
          if (unifier.untyped(eq.lhs().neg(), eq.rhs())) return Cofib.known(false);
        }
        case Cofib cofib1 -> {
          if (cofib1.isFalse()) return Cofib.known(false);
          cofib = cofib.conj(cofib1);
          continue;
        }
        default -> { assert atom instanceof Term.Ref; }
      }
      cofib = cofib.conj(Cofib.from(atom));
    }
    return cofib;
  }

  private @NotNull ImmutableSeq<Tuple2<Cofib.Conj, Term>> partEl(@NotNull ImmutableSeq<Tuple2<Cofib.Conj, Term>> elems) {
    return elems.flatMap(tup ->
      term(tup.component1()).conjs().map(conj -> Tuple.of(conj, term(tup.component2()))));
  }

  public @NotNull Normalizer derive() {
    return new Normalizer(MutableMap.from(rho), unification.derive());
  }

  record Renamer(MutableMap<LocalVar, LocalVar> map) {
    /** @implNote Make sure to rename param before bodying */
    public Term term(Term term) {
      return switch (term) {
        case Term.Lam lam -> {
          var param = param(lam.x());
          yield new Term.Lam(param, term(lam.body()));
        }
        case Term.Lit u -> u;
        case Term.Ref ref -> new Term.Ref(vv(ref.var()));
        case Term.Pi dt -> new Term.Pi(param(dt.param()), term(dt.cod()));
        case Term.Sigma dt -> new Term.Sigma(param(dt.param()), term(dt.cod()));
        case Term.App(var f, var a) -> new Term.App(term(f), term(a));
        case Term.Pair(var a, var b) -> new Term.Pair(term(a), term(b));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.FnCall fnCall -> new Term.FnCall(fnCall.fn(), fnCall.args().map(this::term));
        case Term.ConCall conCall ->
          new Term.ConCall(conCall.fn(), conCall.args().map(this::term), conCall.dataArgs().map(this::term));
        case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
        case Cofib cofib -> new Cofib(cofib.conjs().map(this::term));
        case Cofib.Eq eq -> new Cofib.Eq(term(eq.lhs()), term(eq.rhs()));
        case Term.INeg t -> new Term.INeg(term(t));
        case Term.PartTy(var cof, var ty) -> new Term.PartTy(term(cof), term(ty));
        case Term.PartEl(var elems) ->
          new Term.PartEl(elems.map(tup -> Tuple.of(term(tup.component1()), term(tup.component2()))));
        case Term.Error error -> error;
        case Term.Coe(var r, var s, var A) -> new Term.Coe(term(r), term(s), term(A));
        case Term.Hcom(var r, var s, var A, var f, var partial) ->
          new Term.Hcom(term(r), term(s), term(A), term(f), term(partial));
        case Term.Ext<?> e -> ext(e);
        case Term.Path(var binders, var ext) -> new Term.Path(localVars(binders), ext(ext));
        case Term.Sub(var A, var partEl) -> new Term.Sub(term(A), term(partEl));
        case Term.InS(var phi, var of) -> new Term.InS(term(phi), term(of));
        case Term.OutS(var phi, var partEl, var of) -> new Term.OutS(term(phi), term(partEl), term(of));
      };
    }

    public <F extends Restr> Term.@NotNull Ext<F> ext(@NotNull Term.Ext<F> ext) {
      return new Term.Ext<>(term(ext.type()), (F) ext.restr().map(this::term, this::term));
    }

    public @NotNull Cofib.Conj term(@NotNull Cofib.Conj conj) {
      return new Cofib.Conj(conj.atoms().map(this::term));
    }

    private @NotNull LocalVar vv(@NotNull LocalVar var) {
      return map.getOrDefault(var, var);
    }

    private Param<Term> param(@NotNull Param<Term> param) {
      return new Param<>(param(param.x()), term(param.type()));
    }

    public ImmutableSeq<Param<Term>> params(@NotNull ImmutableSeq<Param<Term>> params) {
      return params.map(this::param);
    }

    private @NotNull LocalVar param(@NotNull LocalVar param) {
      var var = new LocalVar(param.name());
      map.put(param, var);
      return var;
    }

    private @NotNull ImmutableSeq<LocalVar> localVars(@NotNull ImmutableSeq<LocalVar> params) {
      return params.map(this::param);
    }
  }
}
