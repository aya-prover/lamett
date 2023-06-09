package org.aya.lamett.util;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
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
    Free, Cod, BinOp, UOp, AppHead, AppSpine, UOpSpine, ProjHead
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
        var doc = Doc.sep(expr(conj.lhs(), BinOp), Doc.plain("∧"), expr(conj.rhs(), BinOp));
        yield envPrec.ordinal() > BinOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibDisj disj -> {
        var doc = Doc.sep(expr(disj.lhs(), BinOp), Doc.plain("∨"), expr(disj.rhs(), BinOp));
        yield envPrec.ordinal() > BinOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibEq eq -> {
        var doc = Doc.sep(expr(eq.lhs(), BinOp), Doc.plain("="), expr(eq.rhs(), BinOp));
        yield envPrec.ordinal() > BinOp.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.CofibForall forall -> {
        var doc = Doc.sep(Doc.plain("∀"), Doc.cat(Doc.plain(forall.i().name()), Doc.plain(".")), expr(forall.body(), Cod));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
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
      case Term.Tuple two -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), Free), term(two.a(), Free))));
      case Term.FnCall fnCall -> call(envPrec, fnCall.args().view(), fnCall.fn());
      case Term.ConCall conCall -> call(envPrec, conCall.args().view(), conCall.fn());
      case Term.DataCall dataCall -> call(envPrec, dataCall.args().view(), dataCall.fn());
      case Term.INeg neg -> Doc.sep(Doc.symbol("¬"), term(neg.body(), UOpSpine));
      case Term.Cofib cofib -> {
        var fst = cofib.params().isNotEmpty()
          ? Doc.sep(
              Doc.plain("∀"),
              Doc.cat(cofib.params().map(var -> Doc.plain(var.name())).fold(Doc.empty(), Doc::sep), Doc.plain(".")))
          : Doc.empty();
        var inner = cofib.conjs().map(
          conj -> conj.eqs()
            .map(eq -> Doc.sep(term(eq.lhs(), BinOp), Doc.plain("="), term(eq.rhs(), BinOp)))
            .fold(Doc.empty(), (doc1, doc2) -> doc1.isEmpty() ? doc2 : Doc.sep(doc1, Doc.plain("∧"), doc2))
        ).fold(Doc.empty(), (doc1, doc2) -> doc1.isEmpty() ? doc2 : Doc.sep(Doc.parened(doc1), Doc.plain("∨"), Doc.parened(doc2)));
        var snd = cofib.isFalse() ? Doc.plain("⊥") :
          cofib.isTrue() ? Doc.plain("⊤") :
            fst.isNotEmpty() ? Doc.parened(inner) : inner;
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(Doc.sep(fst, snd)) : Doc.sep(fst, snd);
      }
      case Term.Error(var msg) -> Doc.plain(msg);
    };
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
