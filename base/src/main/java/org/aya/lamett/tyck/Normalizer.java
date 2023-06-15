package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;

public record Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
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
      case Term.Tuple(var a, var b) -> new Term.Tuple(term(a), term(b));
      case Term.Proj proj -> {
        var t = term(proj.t());
        if (!(t instanceof Term.Tuple tup)) yield new Term.Proj(t, proj.isOne());
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

        if (eq.rhs() instanceof Term.Ref (var ref) && var == ref) yield Term.Cofib.known(true);
        yield eq;
      }
      case Term.Partial partial -> new Term.Partial(term(partial.cofib()), term(partial.type()));
      case Term.PartEl elem -> new Term.PartEl(elem.elems().flatMap(tup ->
        term(tup.component1()).conjs().map(conj -> Tuple.of(conj, term(tup.component2())))));
      case Term.Error error -> error;
    };
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
        case Term.Tuple(var a, var b) -> new Term.Tuple(term(a), term(b));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.FnCall fnCall -> new Term.FnCall(fnCall.fn(), fnCall.args().map(this::term));
        case Term.ConCall conCall ->
          new Term.ConCall(conCall.fn(), conCall.args().map(this::term), conCall.dataArgs().map(this::term));
        case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
        case Term.Cofib cofib -> term(cofib);
        case Term.Cofib.Eq eq -> new Term.Cofib.Eq(term(eq.lhs()), term(eq.rhs()));
        case Term.INeg t -> new Term.INeg(term(t));
        case Term.Partial partial -> new Term.Partial(term(partial.cofib()), term(partial.type()));
        case Term.PartEl elem ->
          new Term.PartEl(elem.elems().map(tup -> Tuple.of(term(tup.component1()), term(tup.component2()))));
        case Term.Error error -> error;
      };
    }

    public @NotNull Term.Cofib term(Term.Cofib cofib) {
      var params = cofib.params().map(this::param);
      return new Term.Cofib(params, cofib.conjs().map(this::term));
    }

    public @NotNull Term.Cofib.Conj term(Term.Cofib.Conj conj) {
      return new Term.Cofib.Conj(conj.atoms().map(this::term));
    }

    private @NotNull LocalVar vv(@NotNull LocalVar var) {
      return map.getOrDefault(var, var);
    }

    private Param<Term> param(Param<Term> param) {
      return new Param<>(param(param.x()), term(param.type()));
    }

    public ImmutableSeq<Param<Term>> params(ImmutableSeq<Param<Term>> params) {
      return params.map(this::param);
    }

    private LocalVar param(LocalVar param) {
      var var = new LocalVar(param.name());
      map.put(param, var);
      return var;
    }
  }
}
