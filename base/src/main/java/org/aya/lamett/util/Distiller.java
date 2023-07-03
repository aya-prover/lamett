package org.aya.lamett.util;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.tuple.Tuple;
import org.aya.lamett.syntax.DefVar;
import org.aya.lamett.syntax.Expr;
import org.aya.lamett.syntax.Term;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static org.aya.lamett.util.Distiller.Prec.*;

public interface Distiller {
  @FunctionalInterface
  interface PP<E> extends BiFunction<E, Prec, Doc> {}
  enum Prec {
    Free, Cod, BinOp, BinOpSpine, UOp, AppHead, AppSpine, UOpSpine, ProjHead
  }
  static @NotNull Doc expr(@NotNull Expr expr, Prec envPrec) {
    return switch (expr) {
      case Expr.Kw u -> Doc.plain(u.keyword().name());
      case Expr.App two -> {
        var inner = Doc.sep(expr(two.f(), AppHead), expr(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
      case Expr.Pair two -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.a(), Free), expr(two.b(), Free))));
      case Expr.Lam lam -> {
        var doc = Doc.sep(
          Doc.plain("fn"),
          Doc.symbol(lam.x().name()),
          Doc.plain("=>"),
          expr(lam.a(), Free)
        );
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> {
        var doc = dependentType(dt instanceof Expr.Pi, dt.param(), expr(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.INeg neg -> {
        var doc = Doc.sep(Doc.symbol("¬"), expr(neg.body(), UOpSpine));
        yield envPrec.ordinal() > UOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibConj conj -> {
        var doc = Doc.sep(expr(conj.lhs(), BinOpSpine), Doc.plain("∧"), expr(conj.rhs(), BinOpSpine));
        yield envPrec.ordinal() > BinOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibDisj disj -> {
        var doc = Doc.sep(expr(disj.lhs(), BinOpSpine), Doc.plain("∨"), expr(disj.rhs(), BinOpSpine));
        yield envPrec.ordinal() > BinOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibEq eq -> {
        var doc = Doc.sep(expr(eq.lhs(), Free), Doc.plain("="), expr(eq.rhs(), Free));
        yield envPrec.ordinal() > BinOpSpine.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibForall forall -> {
        var doc = Doc.sep(Doc.plain("∀"), Doc.sep(Doc.plain(forall.i().name()), Doc.plain("=>")), expr(forall.body(), Cod));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.PrimCall partial -> Doc.plain(partial.type().prettyName);
      case Expr.PartEl elem -> {
        var clauses = elem.elems().map(tup -> Doc.sep(expr(tup.component1(), Free), Doc.plain(":="), expr(tup.component2(), Free)));
        var center = clauses.isEmpty() ? Doc.empty() : clauses.reduce((d1, d2) -> Doc.sep(Doc.cat(d1, Doc.plain("|")), d2));
        var doc = Doc.sep(Doc.plain("{|"), center, Doc.plain("|}"));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Hole ignored -> Doc.symbol("_");
    };
  }
  private static @NotNull Doc dependentType(boolean isPi, Param<?> param, Docile cod) {
    return Doc.sep(Doc.plain(isPi ? "Fn" : "Sig"),
      param.toDoc(), Doc.symbol(isPi ? "->" : "**"), cod.toDoc());
  }

  static @NotNull Doc term(@NotNull Term term, Prec envPrec) {
    return switch (term) {
      case Term.DT dt -> {
        var doc = dependentType(dt instanceof Term.Pi, dt.param(), term(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Lit ui -> Doc.plain(ui.keyword().name());
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Lam lam -> {
        var doc = Doc.sep(
          Doc.plain("fn"),
          Doc.symbol(lam.x().name()),
          Doc.plain("=>"),
          term(lam.body(), Free)
        );
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Proj proj -> Doc.cat(term(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.App two -> {
        var inner = Doc.sep(term(two.f(), AppHead), term(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
      case Term.Pair two -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), Free), term(two.a(), Free))));
      case Term.FnCall fnCall -> call(envPrec, fnCall.args().view(), fnCall.fn());
      case Term.ConCall conCall -> call(envPrec, conCall.args().view(), conCall.fn());
      case Term.DataCall dataCall -> call(envPrec, dataCall.args().view(), dataCall.fn());
      case Term.INeg neg -> Doc.sep(Doc.symbol("¬"), term(neg.body(), UOpSpine));
      case Term.Cofib cofib -> {
        var fst = cofib.params().isNotEmpty()
          ? Doc.sep(
          Doc.plain("∀"),
          Doc.cat(cofib.params().map(var -> Doc.plain(var.name())).fold(Doc.empty(), Doc::sep), Doc.plain("=>")))
          : Doc.empty();
        var conjs = cofib.conjs().mapNotNull(
          conj -> conj.atoms().isEmpty() ? null : conj.atoms()
            .map(atom -> term(atom, BinOpSpine))
            .reduce((doc1, doc2) -> Doc.sep(doc1, Doc.plain("∧"), doc2))
        );
        var inner = conjs.isEmpty() ? Doc.empty() :
          conjs.reduce((doc1, doc2) -> Doc.sep(Doc.parened(doc1), Doc.plain("∨"), Doc.parened(doc2)));
        var snd = cofib.isFalse() ? Doc.plain("⊥") :
          cofib.isTrue() ? Doc.plain("⊤") : inner;
        var doc = fst.isNotEmpty() ? Doc.sep(fst, snd) : snd;
        yield envPrec.ordinal() > Free.ordinal() && (fst.isNotEmpty() || !(cofib.isFalse() || cofib.isTrue()))
          ? Doc.parened(doc) : Doc.sep(doc);
      }
      case Term.Cofib.Eq eq -> {
        var doc = Doc.sep(term(eq.lhs(), BinOp), Doc.plain("="), term(eq.rhs(), BinOp));
        yield envPrec.ordinal() > BinOpSpine.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Partial(var cof, var ty) -> call(envPrec, "Partial", cof, ty);
      case Term.PartEl elem -> {
        var clauses = elem.elems().map(tup ->
          Doc.sep(term(new Term.Cofib(ImmutableSeq.empty(), ImmutableSeq.of(tup.component1())), Free), Doc.plain(":="), term(tup.component2(), Free)));
        var center = clauses.isEmpty() ? Doc.empty() : clauses.reduce((d1, d2) -> Doc.sep(Doc.cat(d1, Doc.plain("|")), d2));
        var doc = Doc.sep(Doc.plain("{|"), center, Doc.plain("|}"));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Error(var msg) -> Doc.plain(msg);
      case Term.Coe(var r, var s, var A) -> call(envPrec, "coe", r, s, A);
      case Term.Hcom(var r, var s, var A, var i, var el) -> Doc.sep(call(envPrec, "hcom", r, s, A),
        Doc.parened(Doc.sep(
          Doc.symbol(i.name()),
          Doc.plain("=>"),
          term(el, Free))));
      case Term.Ext<?> ext -> call(envPrec, "Ext", ext.type()); // TODO: ext.faces()
      case Term.Path path -> {
        var elems = path.ext().faces().map(face -> {
          var restr = face.component1().restr();
          return Tuple.of(restr, face.component2());
        });
        var asPartial = term(new Term.PartEl(elems), envPrec);
        yield Doc.sep(Doc.wrap("[|", "|]",
          Doc.commaList(path.binders().map(x -> Doc.plain(x.name())))), asPartial);
      }
    };
  }
  private static @NotNull Doc call(Prec envPrec, String kw, Term... args) {
    var docs = MutableList.of(Doc.plain(kw));
    for (var arg : args) docs.append(term(arg, AppSpine));
    var doc = Doc.sep(docs);
    return envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
  }
  private static @NotNull Doc call(Prec envPrec, SeqView<Term> args, DefVar<?> name) {
    var doc = Doc.sep(args
      .map(t -> term(t, AppSpine)).prepended(Doc.plain(name.name)));
    if (args.isEmpty()) return doc;
    return envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
  }

  static Doc args(ImmutableSeq<Arg<Term>> args) {
    return Doc.wrap("(", ")", Doc.commaList(args.map(
      arg -> term(arg.term(), Free))));
  }
}
