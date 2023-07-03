package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;

public class Normalizer {
  private final @NotNull MutableMap<LocalVar, Term> rho;

  public MutableMap<LocalVar, Term> rho() {return rho;}

  private final @NotNull Unifier unifier;

  /** @apiNote Arguments should agree */
  public Normalizer(@NotNull Unifier unifier) {
    this.unifier = unifier;
    this.rho = unifier.unification().toSubst();
  }

  public Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
    this.rho = rho;
    this.unifier = new Unifier();
  }

  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public Term term(Term term) {
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
      case Term.Cofib cofib -> term(cofib);
      case Term.Cofib.Eq eq -> {
        eq = eq.map(this::term);

        if (eq.lhs() instanceof Term.INeg) {
          eq = eq.neg();
        } else if (eq.lhs() instanceof Term.Lit li) {
          if (eq.rhs() instanceof Term.Lit ri) {
            yield Term.Cofib.known(li.keyword() == ri.keyword());
          } else {
            eq = new Term.Cofib.Eq(eq.rhs(), eq.lhs());
            if (eq.lhs() instanceof Term.INeg) eq = eq.neg();
          }
        }

        assert eq.lhs() instanceof Term.Ref;
        var var = ((Term.Ref) eq.lhs()).var();

        if (eq.rhs() instanceof Term.Ref(var ref) && var == ref) yield Term.Cofib.known(true);
        yield eq;
      }
      case Term.Partial partial -> new Term.Partial(term(partial.cofib()), term(partial.type()));
      case Term.PartEl elem -> new Term.PartEl(partEl(elem.elems()));
      case Term.Error error -> error;
      case Term.Coe(var r, var s, var A) -> {
        if (unifier.untyped(r, s)) yield identity("c");

        var varI = new LocalVar("i");
        var codom = term(A.app(new Term.Ref(varI)));

        yield switch (codom) {
          case Term.Sigma sigma -> KanPDF.coeSigma(sigma, varI, r, s);
          case Term.Pi pi -> KanPDF.coePi(pi, new Term.Coe(r, s, A), varI);
          case Term.Lit(var lit) when lit == Keyword.U -> identity("u");
          default -> term;
        };
      }
      case Term.Hcom(var r, var s, var A, var i, var el) -> {
        if (unifier.untyped(r, s)) yield identity("u");

        yield switch (term(A)) {
          case Term.Sigma sigma -> KanPDF.hcomSigma(sigma, r, s, i, el);
          case Term.Pi pi -> KanPDF.hcomPi(pi, r, s, i, el);
          case Term.Lit(var lit) when lit == Keyword.U -> identity("u");
          default -> term;
        };
      }
      case Term.Ext<?>(var type, var faces) -> new Term.Ext<>(
        term(type),
        faces.map(face -> face.map(this::term, null)) // cubical ext type should be Path
      );
      case Term.Path path -> {
        var faces = partEl(path.carryingPartEl());
        yield new Term.Path(
          path.binders(),
          new Term.Ext<>(term(path.ext().type()), faces.map(Term.Restr.Cubical::from))
        );
      }
    };
  }

  private @NotNull Term identity(String u) {
    var x = new LocalVar(u);
    return Term.mkLam(ImmutableSeq.of(x).view(), new Term.Ref(x));
  }

  public @NotNull Term.Cofib term(Term.Cofib cofib) {
    var res = Term.Cofib.known(false);
    for (var conj : cofib.conjs())
      res = res.disj(term(cofib.params(), conj));
    return res;
  }

  public @NotNull Term.Cofib term(ImmutableSeq<LocalVar> params, Term.Cofib.Conj conj) {
    var cofib = Term.Cofib.known(true);
    for (var atom : conj.atoms()) {
      atom = term(atom);
      switch (atom) {
        case Term.Cofib.Eq eq -> {
          if (eq.freeVars().anyMatch(params::contains)) return Term.Cofib.known(false);
        }
        case Term.Cofib cofib1 -> {
          cofib = cofib.conj(cofib1);
          if (cofib.isFalse()) break;
          continue;
        }
        default -> {
        }
      }
      cofib = cofib.conj(Term.Cofib.atom(atom));
    }
    assert cofib.params().isEmpty();
    return cofib;
  }

  public @NotNull Term.Cofib term(Term.Cofib.Conj conj) {
    return term(ImmutableSeq.empty(), conj);
  }

  private @NotNull ImmutableSeq<Tuple2<Term.Cofib.Conj, Term>> partEl(@NotNull ImmutableSeq<Tuple2<Term.Cofib.Conj, Term>> elems) {
    return elems.flatMap(tup ->
      term(tup.component1()).conjs().map(conj -> Tuple.of(conj, term(tup.component2()))));
  }

  public @NotNull Normalizer derive() {
    return new Normalizer(MutableMap.from(rho));
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
        case Term.Cofib cofib -> term(cofib);
        case Term.Cofib.Eq eq -> new Term.Cofib.Eq(term(eq.lhs()), term(eq.rhs()));
        case Term.INeg t -> new Term.INeg(term(t));
        case Term.Partial(var cof, var ty) -> new Term.Partial(term(cof), term(ty));
        case Term.PartEl(var elems) ->
          new Term.PartEl(elems.map(tup -> Tuple.of(term(tup.component1()), term(tup.component2()))));
        case Term.Error error -> error;
        case Term.Coe(var r, var s, var A) -> new Term.Coe(term(r), term(s), term(A));
        case Term.Hcom(var r, var s, var A, var i, var partial) -> {
          var paramI = param(i);
          yield new Term.Hcom(term(r), term(s), term(A), paramI, (Term.PartEl) term(partial));
        }
        case Term.Ext<?> e -> ext(e);
        case Term.Path(var binders, var ext) -> new Term.Path(localVars(binders), ext(ext));
      };
    }

    public <F extends Term.Restr> Term.@NotNull Ext<F> ext(@NotNull Term.Ext<F> ext) {
      return new Term.Ext<>(term(ext.type()), ext.faces().map(face ->
        (F) face.map(this::term, this::term)));
    }

    public @NotNull Term.Cofib term(@NotNull Term.Cofib cofib) {
      var params = cofib.params().map(this::param);
      return new Term.Cofib(params, cofib.conjs().map(this::term));
    }

    public @NotNull Term.Cofib.Conj term(@NotNull Term.Cofib.Conj conj) {
      return new Term.Cofib.Conj(conj.atoms().map(this::term));
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
