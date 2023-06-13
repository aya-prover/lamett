package org.aya.lamett.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple2;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public Term term(Term term) {
    return switch (term) {
      case Term.Ref ref -> rho.getOption(ref.var())
        .map(Normalizer::rename)
        .map(this::term).getOrDefault(ref);
      case Term.Lit u -> u;
      case Term.Lam(var x, var body) -> new Term.Lam(x, term(body));
      case Term.DT dt -> dt.make(param(dt.param()), term(dt.cod()));
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
        var bud = fn.body().fold(this::term, cls ->
          Matchy.unfold(cls, args).map(this::term).getOrElse(() -> new Term.FnCall(call.fn(), args)));
        fn.teleVars().forEach(rho::remove);
        yield bud;
      }
      case Term.ConCall conCall -> new Term.ConCall(conCall.fn(),
        conCall.args().map(this::term), conCall.dataArgs().map(this::term));
      case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
      case Term.INeg t -> term(t.body()).neg();
      case Term.Cofib cofib -> term(cofib);
      case Term.Partial partial -> new Term.Partial(term(partial.cofib()), term(partial.type()));
      case Term.PartialElem elem -> new Term.PartialElem(elem.elems().map(tup -> new Tuple2<>(term(tup.component1()), term(tup.component2()))));
      case Term.Error error -> error;
    };
  }

  public @NotNull Term.Cofib term(Term.Cofib cofib) {
    return new Term.Cofib(ImmutableSeq.empty(), cofib.conjs().mapNotNull(conj -> term(cofib.params(), conj)));
  }

  public @NotNull Term.Cofib.Conj term(Term.Cofib.Conj conj) {
    var res = term(ImmutableSeq.empty(), conj);
    return res == null ? new Term.Cofib.Conj(ImmutableSeq.empty()) : res;
  }

  public @Nullable Term.Cofib.Conj term(ImmutableSeq<LocalVar> params, Term.Cofib.Conj conj) {
    // We don't really need to normalize the cofib,
    // just have to normalize those containing `Lit`s and bounded `Ref`s
    var eqs = conj.eqs().foldLeft(ImmutableSeq.<Term.Cofib.Eq>empty(),
      (acc, eq) -> {
        if (acc == null) return null;
        var lhs = term(eq.lhs());
        var rhs = term(eq.rhs());

        if (lhs instanceof Term.INeg) {
          lhs = lhs.neg();
          rhs = rhs.neg();
        } else if (lhs instanceof Term.Lit li) {
          if (rhs instanceof Term.Lit ri) {
            return li.keyword() == ri.keyword() ? acc : null;
          } else {
            var tmp = lhs;
            lhs = rhs;
            rhs = tmp;
            if (lhs instanceof Term.INeg) {
              lhs = lhs.neg();
              rhs = rhs.neg();
            }
          }
        }

        assert lhs instanceof Term.Ref;
        LocalVar var = ((Term.Ref) lhs).var();
        if (params.contains(var)) return null;
        switch(rhs) {
          case Term.Ref ref -> {
            if (var == ref.var()) return acc;
            if (params.contains(ref.var())) return null;
          }
          case Term.INeg neg when neg.body() instanceof Term.Ref ref -> {
            if (var == ref.var()) return acc;
            if (params.contains(ref.var())) return null;
          }
          default -> {}
        }
        return acc.appended(new Term.Cofib.Eq(lhs, rhs));
      }
    );
    return eqs == null ? null : new Term.Cofib.Conj(eqs);
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
        case Term.DT dt -> {
          var param = param(dt.param());
          yield dt.make(param, term(dt.cod()));
        }
        case Term.App(var f, var a) -> new Term.App(term(f), term(a));
        case Term.Tuple(var a, var b) -> new Term.Tuple(term(a), term(b));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.FnCall fnCall -> new Term.FnCall(fnCall.fn(), fnCall.args().map(this::term));
        case Term.ConCall conCall ->
          new Term.ConCall(conCall.fn(), conCall.args().map(this::term), conCall.dataArgs().map(this::term));
        case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
        case Term.Cofib cofib -> term(cofib);
        case Term.INeg t -> new Term.INeg(term(t));
        case Term.Partial partial -> new Term.Partial(term(partial.cofib()), term(partial.type()));
        case Term.PartialElem elem -> new Term.PartialElem(elem.elems().map(tup -> new Tuple2<>(term(tup.component1()), term(tup.component2()))));
        case Term.Error error -> error;
      };
    }

    public @NotNull Term.Cofib term(Term.Cofib cofib) {
      var params = cofib.params().map(this::param);
      return new Term.Cofib(params, cofib.conjs().map(this::term));
    }

    public @NotNull Term.Cofib.Conj term(Term.Cofib.Conj conj) {
      return new Term.Cofib.Conj(conj.eqs().map(
        eq -> new Term.Cofib.Eq(term(eq.lhs()), term(eq.rhs()))
      ));
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
