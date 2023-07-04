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
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.intellij.GenericNode;
import org.aya.lamett.parser.LamettPsiParser;
import org.aya.lamett.parser.LamettPsiTokenType;
import org.aya.lamett.syntax.*;
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
  @NotNull Either<SourceFile, SourcePos> source,
  @NotNull Reporter reporter
) {

  public static final @NotNull TokenSet DECL = LamettPsiParser.EXTENDS_SETS_[0];
  public static final @NotNull TokenSet EXPR = LamettPsiParser.EXTENDS_SETS_[1];

  public @NotNull ImmutableSeq<Decl> program(@NotNull GenericNode<?> node) {
    return node.childrenOfType(DECL)
      .map(this::decl)
      .filterNotNull()
      .toImmutableSeq();
  }

  public @Nullable Decl decl(@NotNull GenericNode<?> node) {
    if (node.is(DATA_DECL)) return dataDecl(node);
    if (node.is(FN_DECL)) return fnDecl(node);
    if (node.is(CLASS_DECL)) return todo();
    if (node.is(PRINT_DECL)) return printDecl(node);

    return unreachable(node);
  }

  private Decl printDecl(GenericNode<?> node) {
    return new Decl.Print(telescopeOf(node), type(node.child(TYPE)), expr(node.child(EXPR)));
  }

  public @NotNull Expr expr(@NotNull GenericNode<?> node) {
    var pos = sourcePosOf(node);
    if (node.is(NEW_EXPR)) return todo();
    if (node.is(PI_EXPR)) {
      // piExpr ::= KW_PI tele+ TO expr

      var tele = telescopeOf(node);
      var expr = expr(node.child(EXPR));
      return tele.scope().foldRight(expr, (l, r) -> {
        // l : GenericNode<?> in TELE type
        // r : Expr
        // Goal : l -> r
        // TODO: subpos
        return new Expr.Pi(pos, l, r);
      });
    }

    if (node.is(FORALL_EXPR)) {
      // forallExpr ::= KW_FORALL lambdaTele+ TO expr
      var tele = lambdaTelescopeOf(node);
      var expr = expr(node.child(EXPR));
      return tele.scope().foldRight(expr, (l, r) -> new Expr.Lam(pos, l.x(), r));
    }

    if (node.is(SIGMA_EXPR)) {
      // sigmaExpr ::= KW_SIGMA tele SUCHTHAT expr
      var tele = tele(node.child(TELE));
      if (tele.sizeGreaterThan(1)) {
        throw new IllegalArgumentException("Sigma is binary");
      }
      var such = expr(node.child(EXPR));

      return tele.foldRight(such, (l, r) -> new Expr.Sigma(pos, l, r));
    }

    if (node.is(LAMBDA_EXPR)) {
      // lambdaExpr ::= KW_LAMBDA lambdaTele+ (IMPLIES expr)?
      var tele = lambdaTelescopeOf(node);
      var bodyNode = node.peekChild(EXPR);
      var body = bodyNode != null
        ? expr(bodyNode)
        : new Expr.Hole(pos, ImmutableSeq.empty()); // TODO: accessible
      return tele.scope().foldRight(body, (l, r) -> new Expr.Lam(pos, l.x(), r));
    }

    if (node.is(SELF_EXPR)) return todo();

    if (node.is(PATH_EXPR)) {
      // pathExpr ::= LPATH weakId+ RPATH expr partialBlock?
      return todo();
    }

    if (node.is(ARROW_EXPR)) {
      var app = node.childrenOfType(EXPR);
      assert app.sizeEquals(2);
      var of = unnamedParam(app.get(0));
      var arg = expr(app.get(1));

      return new Expr.Pi(pos, of, arg);
    }

    if (node.is(APP_EXPR)) {
      // appExpr ::= expr argument+
      var of = expr(node.child(EXPR));
      var arg = node.childrenOfType(ARGUMENT).map(this::argument);
      return arg.foldLeft(of, (l, r) -> new Expr.App(pos, l, r));
    }

    if (node.is(PROJ_EXPR)) {
      // projExpr ::= expr projFix
      return projFix(expr(node.child(EXPR)), pos, node.child(PROJ_FIX));
    }

    if (node.is(REF_EXPR)) {
      var id = weakId(node.child(WEAK_ID));
      return new Expr.Unresolved(id.sourcePos(), id.data());
    }

    if (node.is(HOLE_EXPR)) {
      return new Expr.Hole(pos, ImmutableSeq.empty());
    }

    if (node.is(CONST_EXPR)) {
      // constExpr ::= KW_TYPE | KW_ISET | KW_SET | KW_INTERVAL | NUMBER | KW_F
      if (node.peekChild(KW_TYPE) != null) return new Expr.Kw(pos, Keyword.U);
      if (node.peekChild(KW_ISET) != null) return new Expr.Kw(pos, Keyword.ISet);
      if (node.peekChild(KW_SET) != null) return new Expr.Kw(pos, Keyword.Set);
      if (node.peekChild(KW_INTERVAL) != null) return new Expr.Kw(pos, Keyword.I);
      if (node.peekChild(KW_F) != null) return new Expr.Kw(pos, Keyword.F);
      if (node.peekChild(NUMBER) != null) {
        var text = node.child(NUMBER).tokenText();
        if (text.contentEqualsIgnoreCase("1")) return new Expr.Kw(pos, Keyword.One);
        if (text.contentEqualsIgnoreCase("0")) return new Expr.Kw(pos, Keyword.Zero);
      }
      if (node.peekChild(KW_PARTIAL) != null) return new Expr.PrimCall(pos, Expr.PrimType.Partial);
      if (node.peekChild(KW_COE) != null) return new Expr.PrimCall(pos, Expr.PrimType.Coe);
      if (node.peekChild(KW_SUB) != null) return new Expr.PrimCall(pos, Expr.PrimType.Sub);
      if (node.peekChild(KW_INS) != null) return new Expr.PrimCall(pos, Expr.PrimType.InS);
      if (node.peekChild(KW_OUTS) != null) return new Expr.PrimCall(pos, Expr.PrimType.OutS);

      return unreachable(node);
    }

    /// region atomExpr
    if (node.is(TUPLE_ATOM)) {
      // tupleAtom ::= LPAREN exprList RPAREN
      var exprs = exprListOf(node);
      // No unary tuples
      if (exprs.sizeEquals(1)) return exprs.first();
      // Now it is correct
      return exprs.reduceRight((l, r) -> new Expr.Pair(l.pos().union(r.pos()), l, r));
    }

    if (node.is(PARTIAL_ATOM)) return partial(node, pos);
    /// endregion atomExpr

    /// region cubical cofibration
    if (node.is(IFORALL_EXPR)) {
      var ids = node.childrenOfType(WEAK_ID).map(i -> new LocalVar(i.tokenText().toString())).toImmutableSeq();
      var expr = expr(node.child(EXPR));
      return ids.foldRight(expr, (l, r) -> new Expr.CofibForall(pos, l, r));
    }

    if (node.is(IEQ_EXPR)) {
      var exprs = node.childrenOfType(EXPR).map(this::expr).toImmutableSeq();
      assert exprs.sizeEquals(2);
      return new Expr.CofibEq(pos, exprs.get(0), exprs.get(1));
    }

    if (node.is(DISJ_EXPR)) {
      var exprs = node.childrenOfType(EXPR).map(this::expr).toImmutableSeq();
      assert exprs.sizeEquals(2);
      return new Expr.CofibDisj(pos, exprs.get(0), exprs.get(1));
    }

    if (node.is(CONJ_EXPR)) {
      var exprs = node.childrenOfType(EXPR).map(this::expr).toImmutableSeq();
      assert exprs.sizeEquals(2);
      return new Expr.CofibConj(pos, exprs.get(0), exprs.get(1));
    }

    if (node.is(INEG_EXPR)) return new Expr.INeg(pos, expr(node.child(EXPR)));
    /// endregion cubical cofibration

    return unreachable(node);
  }

  public @NotNull Expr argument(@NotNull GenericNode<?> node) {
    // argument ::= atomExpr projFix*
    var expr = expr(node.child(EXPR));
    return node.childrenOfType(PROJ_FIX).foldLeft(expr,
      (on, proj) -> projFix(on, sourcePosOf(proj), proj));
  }

  public @NotNull Expr projFix(@NotNull Expr on, @NotNull SourcePos pos, @NotNull GenericNode<?> node) {
    // projFix ::= DOT (NUMBER | projFixId)
    if (node.peekChild(NUMBER) != null) {
      var isOne = node.child(NUMBER).tokenText().contentEqualsIgnoreCase("1");
      return new Expr.Proj(pos, on, isOne);
    } else return todo(); // TODO: projFixId
  }

  public @NotNull Expr.PartEl partial(@Nullable GenericNode<?> partial, @NotNull SourcePos fallbackPos) {
    if (partial == null) return new Expr.PartEl(fallbackPos, ImmutableSeq.empty());
    var sub = partial.childrenView()
      .filter(c -> c.elementType() == BARE_SUB_SYSTEM || c.elementType() == BARRED_SUB_SYSTEM)
      .map(this::bareOrBarredSubSystem)
      .toImmutableSeq();
    return new Expr.PartEl(sourcePosOf(partial), sub);
  }

  public @NotNull Tuple2<Expr, Expr> bareOrBarredSubSystem(@NotNull GenericNode<?> node) {
    return subSystem(node.child(SUB_SYSTEM));
  }

  public @NotNull Tuple2<Expr, Expr> subSystem(@NotNull GenericNode<?> node) {
    var exprs = node.childrenOfType(EXPR).map(this::expr);
    return Tuple.of(exprs.get(0), exprs.get(1));
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

  public @Nullable Decl.Fn fnDecl(@NotNull GenericNode<?> node) {
    // fnDecl ::=
    //   KW_DEF weakId
    // tele* type? fnBody

    var pos = sourcePosOf(node);
    var id = weakId(node.child(WEAK_ID));
    var tele = telescopeOf(node);
    var type = typeOrHole(node.peekChild(TYPE), tele.scope().map(Param::x), pos);
    var fnBody = fnBody(node.child(FN_BODY));

    if (fnBody == null) {
      error(node, "Expect a function body");
      return null;
    }

    return new Decl.Fn(new DefVar<>(id.data()), tele, type, fnBody);
  }

  public @Nullable Either<Expr, Either<Pat.ClauseSet<Expr>, ImmutableSeq<Pat.UnresolvedClause>>>
  fnBody(@NotNull GenericNode<?> node) {
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
    // clause ::= <<commaSep patterns>> (IMPLIES expr)?
    return new Pat.UnresolvedClause(
      node.child(COMMA_SEP).childrenOfType(PATTERNS)
        .map(this::patterns).toImmutableSeq(),
      node.peekChild(IMPLIES) == null ? null : expr(node.child(EXPR))
    );
  }

  private @NotNull SeqView<Pat.Unresolved> patternsRaw(@NotNull GenericNode<?> node) {
    return node.childrenOfType(PATTERN).map(this::pattern);
  }

  public @NotNull Pat.Unresolved patterns(@NotNull GenericNode<?> node) {
    var raw = patternsRaw(node);
    var head = raw.first();
    if (head.pats().isNotEmpty())
      throw new IllegalStateException("Unsupported syntax: " + node.tokenText());
    var tail = raw.drop(1).toImmutableSeq();
    return new Pat.Unresolved(sourcePosOf(node), head.name(), tail);
  }

  public @NotNull Pat.Unresolved pattern(@NotNull GenericNode<?> node) {
    // pattern ::= <<paren patterns>>
    //         | LPAREN RPAREN
    //         | weakId

    var paren = node.peekChild(PAREN);
    if (paren != null) return paren(paren, PATTERNS, this::patterns);

    var id = node.peekChild(WEAK_ID);
    if (id != null) {
      var weakId = weakId(id);
      return new Pat.Unresolved(weakId.sourcePos(), weakId.data(), ImmutableSeq.empty());
    }

    // TODO: absurd patterns
    return todo();
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
    return telescope(node.childrenOfType(TELE));
  }

  public @NotNull Decl.Tele telescope(@NotNull SeqView<? extends GenericNode<?>> node) {
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

  public @NotNull <R> R paren(@NotNull GenericNode<?> node, @NotNull IElementType type, @NotNull Function<GenericNode<?>, R> ctor) {
    return ctor.apply(node.child(type));
  }

  public @NotNull ImmutableSeq<Expr> exprListOf(@NotNull GenericNode<?> node) {
    return node.child(COMMA_SEP).childrenOfType(EXPR).map(this::expr).toImmutableSeq();
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
    return unnamedParam(node.child(EXPR));
  }

  public @NotNull Decl.Tele lambdaTelescopeOf(@NotNull GenericNode<?> node) {
    return lambdaTelescope(node.childrenOfType(LAMBDA_TELE).map(x -> x));
  }

  public @NotNull Decl.Tele lambdaTelescope(SeqView<? extends GenericNode<?>> telescope) {
    return new Decl.Tele(telescope.flatMap(this::lambdaTele).toImmutableSeq());
  }

  public @NotNull ImmutableSeq<Param<Expr>> lambdaTele(@NotNull GenericNode<?> node) {
    var teleParamName = node.peekChild(WEAK_ID);
    if (teleParamName != null) {
      var id = weakId(teleParamName);
      var type = typeOrHole(null, ImmutableSeq.empty(), id.sourcePos());
      return ImmutableSeq.of(new Param<>(LocalVar.from(id), type));
    }

    var paren = node.child(PAREN);
    return paren(paren, LAMBDA_TELE_BINDER, this::lambdaTeleBinder);
  }


  public @NotNull ImmutableSeq<Param<Expr>> lambdaTeleBinder(@NotNull GenericNode<?> node) {
    // | teleBinderTyped
    var typed = node.peekChild(TELE_BINDER_TYPED);
    if (typed != null) return teleBinderTyped(typed);

    // | teleBinderUntyped
    var ids = node.child(TELE_BINDER_UNTYPED);
    return teleBinderUntyped(ids).view()
      .map(id -> new Param<>(LocalVar.from(id), typeOrHole(null, ImmutableSeq.empty(), id.sourcePos())))   // TODO: ctx
      .toImmutableSeq();
  }

  public @NotNull Param<Expr> unnamedParam(@NotNull GenericNode<?> node) {
    var pos = sourcePosOf(node);
    return new Param<>(Constants.randomlyNamed(pos), expr(node));
  }

  // TODO: delete this
  @Deprecated
  public <T> T todo() {
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
    return source.fold(file -> sourcePosOf(node, file), pos -> pos);
  }

  public static @NotNull SourcePos sourcePosOf(@NotNull GenericNode<?> node, @NotNull SourceFile file) {
    return sourcePosOf(node.range(), file, isTerminalNode(node));
  }

  public static boolean isTerminalNode(@NotNull GenericNode<?> node) {
    return node.elementType() instanceof LamettPsiTokenType;
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
