package org.aya.lamett.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.util.AnyVar;
import org.aya.lamett.util.Distiller;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr extends Docile {
  @NotNull SourcePos pos();
  @Override default @NotNull Doc toDoc() {
    return Distiller.expr(this, Distiller.Prec.Free);
  }
  record Unresolved(@Override @NotNull SourcePos pos, String name) implements Expr {}
  record Resolved(@Override @NotNull SourcePos pos, AnyVar ref) implements Expr {}
  sealed interface Two extends Expr {
    @NotNull Expr f();
    @NotNull Expr a();
    @NotNull default Two make(@NotNull SourcePos pos, @NotNull Expr f, @NotNull Expr a) {
      return switch (this) {
        case Tuple $ -> new Tuple(pos, f, a);
        case App $ -> new App(pos, f, a);
      };
    }
  }
  record Tuple(@Override @NotNull SourcePos pos, Expr f, Expr a) implements Two {}
  record App(@Override @NotNull SourcePos pos, Expr f, Expr a) implements Two {}
  record Lam(@Override @NotNull SourcePos pos, LocalVar x, Expr a) implements Expr {}

  /** @param isOne it's a second projection if false */
  record Proj(@Override @NotNull SourcePos pos, @NotNull Expr t, boolean isOne) implements Expr {}
  record Kw(@Override @NotNull SourcePos pos, @NotNull Keyword keyword) implements Expr {}
  record Hole(@Override @NotNull SourcePos pos, ImmutableSeq<LocalVar> accessible) implements Expr {}
  sealed interface DT extends Expr {
    Param<Expr> param();
    Expr cod();
    @NotNull default DT make(@NotNull SourcePos pos, Param<Expr> param, Expr cod) {
      return switch (this) {
        case Pi $ -> new Pi(pos, param, cod);
        case Sigma $ -> new Sigma(pos, param, cod);
      };
    }
  }
  record Pi(@Override @NotNull SourcePos pos, Param<Expr> param, Expr cod) implements DT {}
  record Sigma(@Override @NotNull SourcePos pos, Param<Expr> param, Expr cod) implements DT {}

  sealed interface Cofib extends Expr {}
  record CofibForall(@Override @NotNull SourcePos pos, Expr i, Expr body) implements Cofib {}
  record CofibConj(@Override @NotNull SourcePos pos, Expr lhs, Expr rhs) implements Cofib {}
  record CofibDisj(@Override @NotNull SourcePos pos, Expr lhs, Expr rhs) implements Cofib {}
  record CofibEq(@Override @NotNull SourcePos pos, Expr lhs, Expr rhs) implements Cofib {}
  record INeg(@Override @NotNull SourcePos pos, Expr body) implements Expr {}
}
