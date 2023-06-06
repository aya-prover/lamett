package org.aya.lamett.parse;

import com.intellij.lexer.FlexLexer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.LineColumn;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.lamett.parser.LamettPsiParser;
import org.aya.lamett.syntax.Decl;
import org.aya.lamett.syntax.DefVar;
import org.aya.lamett.syntax.Expr;
import org.aya.lamett.syntax.Pat;
import org.aya.lamett.util.Constants;
import org.aya.lamett.util.LocalVar;
import org.aya.lamett.util.Param;
import org.aya.util.error.InternalException;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static org.aya.lamett.parser.LamettPsiElementTypes.*;

public record LamettProducer(
  @NotNull SourceFile source,
  @NotNull Reporter reporter
  ) {

  public static final @NotNull TokenSet DECL = LamettPsiParser.EXTENDS_SETS_[0];
  public static final @NotNull TokenSet EXPR = LamettPsiParser.EXTENDS_SETS_[1];

  public @NotNull Decl decl(@NotNull GenericNode<?> node) {
    if (node.is(DATA_DECL)) {
      return dataDecl(node);
    }

    if (node.is(FN_DECL)) {
      return fnDecl(node);
    }

    if (node.is(CLASS_DECL)) {
      return todo();
    }

    return unreachable(node);
  }

  public @NotNull Expr expr(@NotNull GenericNode<?> node) {
    // expr ::= newExpr
    //   | piExpr
    //   | forallExpr
    //   | sigmaExpr
    //   | lambdaExpr
    //   | selfExpr
    //   | pathExpr
    //   | atomExpr
    //   | arrowExpr
    //   | appExpr
    //   | projExpr

    // TODO: (EXPR : TokenSet) also contains (EXPR : IElementType), should we handle it?

    return todo();
  }

  public @NotNull Decl.Data dataDecl(@NotNull GenericNode<?> node) {
    // dataDecl ::= KW_DATA weakId?
    //  tele* type? dataBody*
    var pos = sourcePosOf(node);
    var id = weakIdMaybe(node.peekChild(WEAK_ID), pos);
    var telescope = telescope(node.childrenOfType(TELE).map(x -> x));
    var type = node.peekChild(TYPE);    // FIXME
    var bodies = node.childrenOfType(DATA_BODY).map(x -> x.child(DATA_CTOR));
    var ctors = bodies.map(this::dataCtor).toImmutableSeq();

    return new Decl.Data(
      new DefVar<>(id.data()),
      telescope,
      ctors
    );
  }

  public @NotNull Decl.Cons dataCtor(@NotNull GenericNode<?> node) {
    // dataCtor ::= weakId tele* type? partialBlock?
    var id = weakId(node.child(WEAK_ID));
    var telescope = telescope(node.childrenOfType(TELE).map(x -> x));
    var type = node.peekChild(TYPE);    // FIXME
    var partialBlock = node.peekChild(PARTIAL_BLOCK);   // FIXME

    return new Decl.Cons(
      new DefVar<>(id.data()),
      telescope
    );
  }

  public @NotNull Decl.Fn fnDecl(@NotNull GenericNode<?> node) {
    // fnDecl ::=
    //   KW_DEF weakId
    // tele* type? fnBody

    var pos = sourcePosOf(node);
    var id = weakId(node.child(WEAK_ID));
    var telescope = telescopeOf(node);
    var type = typeOrHole(node.peekChild(TYPE), telescope.scope().map(Param::x), pos);
    var fnBody = fnBody(node.child(FN_BODY));

    if (fnBody == null) {
      error(node, "Expect a function body");
      return null;
    }

    return new Decl.Fn(
      new DefVar<>(id.data()),
      telescope,
      type,
      fnBody
    );
  }

  public @Nullable Either<Expr, Either<Pat.ClauseSet<Expr>, ImmutableSeq<Pat.UnresolvedClause>>> fnBody(@NotNull GenericNode<?> node) {
    var expr = node.peekChild(EXPR);
    var implies = node.peekChild(IMPLIES);
    if (expr == null && implies != null) return error(implies, "Expect function body");
    if (expr != null) return Either.left(expr(expr));
    var clauses = barredClauses(node.childrenOfType(BARRED_CLAUSE).map(x -> x));
    return Either.right(Either.right(clauses));
  }

  public @NotNull ImmutableSeq<Pat.UnresolvedClause> barredClauses(@NotNull SeqView<GenericNode<?>> nodes) {
    return nodes.map(this::bareOrBarredClause).toImmutableSeq();
  }

  public @NotNull Pat.UnresolvedClause bareOrBarredClause(@NotNull GenericNode<?> node) {
    return clause(node.child(CLAUSE));
  }

  public @NotNull Pat.UnresolvedClause clause(@NotNull GenericNode<?> node) {
    // clause ::= patterns (IMPLIES expr)?
    return todo();
  }

  public @NotNull ImmutableSeq<Pat.Unresolved> patterns(@NotNull GenericNode<?> node) {
    return todo();
  }

  public @NotNull Pat.Unresolved unitPattern(@NotNull GenericNode<?> node) {
    // TODO: ice1000 will fix this
    // unitPattern ::= <<paren patterns>>
    //               | LPAREN RPAREN
    //               | weakId

    var paren = node.peekChild(PAREN);
    if (paren != null) {
      var pos = sourcePosOf(paren);
      return new Pat.Unresolved(pos, null, paren(paren, PATTERNS, this::patterns));
    }

    var id = node.peekChild(WEAK_ID);
    if (id != null) {
      var weakId = weakId(id);
      return new Pat.Unresolved(weakId.sourcePos(), weakId.data(), ImmutableSeq.of());
    }

    return new Pat.Unresolved(sourcePosOf(node), null, ImmutableSeq.of());
  }

  public @NotNull WithPos<String> weakId(@NotNull GenericNode<?> node) {
    return new WithPos<>(sourcePosOf(node), node.tokenText().toString());
  }

  /**
   * @param pos used when nodeMaybe == null
   */
  public @NotNull WithPos<String> weakIdMaybe(@Nullable GenericNode<?> nodeMaybe, @NotNull SourcePos pos) {
    if (nodeMaybe == null) return new WithPos<>(pos, Constants.randomName(pos));
    return weakId(nodeMaybe);
  }

  public @NotNull Decl.Tele telescopeOf(@NotNull GenericNode<?> node) {
    return telescope(node.childrenOfType(TELE).map(x -> x));
  }

  public @NotNull Decl.Tele telescope(@NotNull SeqView<GenericNode<?>> node) {
    return new Decl.Tele(node.flatMap(this::tele).toImmutableSeq());
  }

  public @NotNull ImmutableSeq<Param<Expr>> tele(@NotNull GenericNode<?> node) {
    // tele ::= literal | <<paren teleBinder>>
    var tele = node.peekChild(PAREN);
    if (tele != null) return paren(tele, TELE_BINDER, this::teleBinder);
    var type = expr(node.child(EXPR));
    var pos = sourcePosOf(node);
    return ImmutableSeq.of(new Param<>(Constants.randomlyNamed(pos), type));
  }

  public @NotNull Expr type(@NotNull GenericNode<?> node) {
    return expr(node.child(EXPR));
  }

  public @NotNull Expr typeOrHole(
    @Nullable GenericNode<?> node,
    @NotNull ImmutableSeq<LocalVar> ctx,
    @NotNull SourcePos holePos
  ) {
    if (node != null) return type(node);
    return new Expr.Hole(holePos, ctx);
  }

  public @NotNull <R> R paren(
    @NotNull GenericNode<?> node,
    @NotNull IElementType type,
    @NotNull Function<GenericNode<?>, R> ctor
  ) {
    return ctor.apply(node.child(type));
  }

  public @NotNull ImmutableSeq<Param<Expr>> teleBinder(@NotNull GenericNode<?> node) {
    var typed = node.peekChild(TELE_BINDER_TYPED);
    if (typed != null) return teleBinderTyped(typed);
    var anonymous = node.peekChild(TELE_BINDER_ANONYMOUS);
    if (anonymous != null) return ImmutableSeq.of(teleBinderAnonymous(anonymous));
    return unreachable(node);
  }

  private @NotNull ImmutableSeq<Param<Expr>> teleBinderTyped(@NotNull GenericNode<?> node) {
    // teleBinderTyped ::= teleBinderUntyped type
    var ids = teleBinderUntyped(node.child(TELE_BINDER_UNTYPED));
    var type = type(node.child(TYPE));
    return ids.map(i -> new Param<>(LocalVar.from(i), type));
  }

  private @NotNull ImmutableSeq<WithPos<String>> teleBinderUntyped(@NotNull GenericNode<?> node) {
    // teleBinderUntyped ::= weakId+
    return node.childrenOfType(WEAK_ID).map(this::weakId).toImmutableSeq();
  }

  private @NotNull Param<Expr> teleBinderAnonymous(@NotNull GenericNode<?> node) {
    return new Param<>(
      Constants.randomlyNamed(sourcePosOf(node)),
      expr(node.child(EXPR))
    );
  }

  // TODO: delete this
  @Deprecated(forRemoval = true)
  private <T> T todo() {
    throw new UnsupportedOperationException("TODO");
  }

  private <T> @Nullable T error(@NotNull GenericNode<?> node, @NotNull String message) {
    reporter.report(new ParseError(sourcePosOf(node), message));
    return null;
  }

  private <T> T unreachable(@NotNull GenericNode<?> node) {
    throw new InternalException(node.elementType() + ": " + node.tokenText());
  }

  private @NotNull SourcePos sourcePosOf(@NotNull GenericNode<?> node) {
    return sourcePosOf(node, source);
  }

  public static @NotNull SourcePos sourcePosOf(@NotNull GenericNode<?> node, @NotNull SourceFile file) {
    return sourcePosOf(node.range(), file, node.isTerminalNode());
  }

  public static @NotNull SourcePos sourcePosOf(@NotNull FlexLexer.Token token, @NotNull SourceFile file) {
    return sourcePosOf(token.range(), file, true);
  }

  public static @NotNull SourcePos sourcePosOf(@NotNull TextRange range, @NotNull SourceFile file, boolean isTerminal) {
    var start = StringUtil.offsetToLineColumn(file.sourceCode(), range.getStartOffset());
    var length = range.getLength();
    var endOffset = range.getEndOffset() - (length == 0 ? 0 : 1);
    var end = isTerminal || length == 0
      ? LineColumn.of(start.line, start.column + length - 1)
      : StringUtil.offsetToLineColumn(file.sourceCode(), endOffset);
    return new SourcePos(file, range.getStartOffset(), endOffset,
      start.line + 1, start.column, end.line + 1, end.column);
  }
}
