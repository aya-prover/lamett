// This is a generated file. Not intended for manual editing.
package org.aya.lamett.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.aya.lamett.parser.LamettPsiElementTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class LamettPsiParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return program(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(CLASS_DECL, DATA_DECL, DECL, FN_DECL,
      PRINT_DECL),
    create_token_set_(APP_EXPR, ARROW_EXPR, ATOM_EXPR, CALM_FACE_EXPR,
      CONJ_EXPR, CONST_EXPR, DISJ_EXPR, EXPR,
      FORALL_EXPR, GOAL_EXPR, HOLE_EXPR, IEQ_EXPR,
      IFORALL_EXPR, INEG_EXPR, LAMBDA_EXPR, LITERAL,
      NEW_EXPR, PARTIAL_ATOM, PATH_EXPR, PI_EXPR,
      PROJ_EXPR, REF_EXPR, SELF_EXPR, SIGMA_EXPR,
      TUPLE_ATOM),
  };

  /* ********************************************************** */
  // atomExpr projFix*
  public static boolean argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = atomExpr(b, l + 1);
    r = r && argument_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // projFix*
  private static boolean argument_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!projFix(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "argument_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // subSystem
  public static boolean bareSubSystem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bareSubSystem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BARE_SUB_SYSTEM, "<bare sub system>");
    r = subSystem(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BAR clause
  public static boolean barredClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "barredClause")) return false;
    if (!nextTokenIs(b, BAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BAR);
    r = r && clause(b, l + 1);
    exit_section_(b, m, BARRED_CLAUSE, r);
    return r;
  }

  /* ********************************************************** */
  // BAR subSystem
  public static boolean barredSubSystem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "barredSubSystem")) return false;
    if (!nextTokenIs(b, BAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BAR);
    r = r && subSystem(b, l + 1);
    exit_section_(b, m, BARRED_SUB_SYSTEM, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACE <<param>> RBRACE
  static boolean braced(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "braced")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && _param.parse(b, l);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // CALM_FACE
  public static boolean calmFaceExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "calmFaceExpr")) return false;
    if (!nextTokenIs(b, CALM_FACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CALM_FACE);
    exit_section_(b, m, CALM_FACE_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // KW_CLASS weakId (KW_EXTENDS <<commaSep weakId>>)?
  //       (BAR classMember)*
  public static boolean classDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDecl")) return false;
    if (!nextTokenIs(b, KW_CLASS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KW_CLASS);
    r = r && weakId(b, l + 1);
    r = r && classDecl_2(b, l + 1);
    r = r && classDecl_3(b, l + 1);
    exit_section_(b, m, CLASS_DECL, r);
    return r;
  }

  // (KW_EXTENDS <<commaSep weakId>>)?
  private static boolean classDecl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDecl_2")) return false;
    classDecl_2_0(b, l + 1);
    return true;
  }

  // KW_EXTENDS <<commaSep weakId>>
  private static boolean classDecl_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDecl_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KW_EXTENDS);
    r = r && commaSep(b, l + 1, LamettPsiParser::weakId);
    exit_section_(b, m, null, r);
    return r;
  }

  // (BAR classMember)*
  private static boolean classDecl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDecl_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!classDecl_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "classDecl_3", c)) break;
    }
    return true;
  }

  // BAR classMember
  private static boolean classDecl_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDecl_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BAR);
    r = r && classMember(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // KW_CLASSIFIYING? weakId tele* type IMPLIES expr
  public static boolean classMember(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classMember")) return false;
    if (!nextTokenIs(b, "<class member>", ID, KW_CLASSIFIYING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CLASS_MEMBER, "<class member>");
    r = classMember_0(b, l + 1);
    r = r && weakId(b, l + 1);
    r = r && classMember_2(b, l + 1);
    r = r && type(b, l + 1);
    r = r && consumeToken(b, IMPLIES);
    r = r && expr(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // KW_CLASSIFIYING?
  private static boolean classMember_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classMember_0")) return false;
    consumeToken(b, KW_CLASSIFIYING);
    return true;
  }

  // tele*
  private static boolean classMember_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classMember_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "classMember_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // <<commaSep patterns>> (IMPLIES expr)?
  public static boolean clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause")) return false;
    if (!nextTokenIs(b, "<clause>", ID, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CLAUSE, "<clause>");
    r = commaSep(b, l + 1, LamettPsiParser::patterns);
    r = r && clause_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (IMPLIES expr)?
  private static boolean clause_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_1")) return false;
    clause_1_0(b, l + 1);
    return true;
  }

  // IMPLIES expr
  private static boolean clause_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IMPLIES);
    r = r && expr(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // <<param>> (',' <<param>>) *
  public static boolean commaSep(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "commaSep")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _param.parse(b, l);
    r = r && commaSep_1(b, l + 1, _param);
    exit_section_(b, m, COMMA_SEP, r);
    return r;
  }

  // (',' <<param>>) *
  private static boolean commaSep_1(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "commaSep_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!commaSep_1_0(b, l + 1, _param)) break;
      if (!empty_element_parsed_guard_(b, "commaSep_1", c)) break;
    }
    return true;
  }

  // ',' <<param>>
  private static boolean commaSep_1_0(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "commaSep_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && _param.parse(b, l);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // KW_TYPE | KW_ISET | KW_SET | KW_INTERVAL | NUMBER | KW_F
  public static boolean constExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONST_EXPR, "<const expr>");
    r = consumeToken(b, KW_TYPE);
    if (!r) r = consumeToken(b, KW_ISET);
    if (!r) r = consumeToken(b, KW_SET);
    if (!r) r = consumeToken(b, KW_INTERVAL);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, KW_F);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BAR (dataCtorClause | dataCtor)
  public static boolean dataBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataBody")) return false;
    if (!nextTokenIs(b, BAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DATA_BODY, null);
    r = consumeToken(b, BAR);
    p = r; // pin = 1
    r = r && dataBody_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // dataCtorClause | dataCtor
  private static boolean dataBody_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataBody_1")) return false;
    boolean r;
    r = dataCtorClause(b, l + 1);
    if (!r) r = dataCtor(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // weakId tele* type? partialBlock?
  public static boolean dataCtor(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataCtor")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    r = r && dataCtor_1(b, l + 1);
    r = r && dataCtor_2(b, l + 1);
    r = r && dataCtor_3(b, l + 1);
    exit_section_(b, m, DATA_CTOR, r);
    return r;
  }

  // tele*
  private static boolean dataCtor_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataCtor_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "dataCtor_1", c)) break;
    }
    return true;
  }

  // type?
  private static boolean dataCtor_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataCtor_2")) return false;
    type(b, l + 1);
    return true;
  }

  // partialBlock?
  private static boolean dataCtor_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataCtor_3")) return false;
    partialBlock(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // patterns IMPLIES dataCtor
  public static boolean dataCtorClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataCtorClause")) return false;
    if (!nextTokenIs(b, "<data ctor clause>", ID, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DATA_CTOR_CLAUSE, "<data ctor clause>");
    r = patterns(b, l + 1);
    r = r && consumeToken(b, IMPLIES);
    r = r && dataCtor(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // KW_DATA weakId tele* dataBody*
  public static boolean dataDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataDecl")) return false;
    if (!nextTokenIs(b, KW_DATA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KW_DATA);
    r = r && weakId(b, l + 1);
    r = r && dataDecl_2(b, l + 1);
    r = r && dataDecl_3(b, l + 1);
    exit_section_(b, m, DATA_DECL, r);
    return r;
  }

  // tele*
  private static boolean dataDecl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataDecl_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "dataDecl_2", c)) break;
    }
    return true;
  }

  // dataBody*
  private static boolean dataDecl_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dataDecl_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!dataBody(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "dataDecl_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // fnDecl | classDecl | dataDecl | printDecl
  public static boolean decl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "decl")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, DECL, "<decl>");
    r = fnDecl(b, l + 1);
    if (!r) r = classDecl(b, l + 1);
    if (!r) r = dataDecl(b, l + 1);
    if (!r) r = printDecl(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<commaSep expr>>
  static boolean exprList(PsiBuilder b, int l) {
    return commaSep(b, l + 1, expr_parser_);
  }

  /* ********************************************************** */
  // simpleBody
  //          | barredClause*
  public static boolean fnBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fnBody")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FN_BODY, "<fn body>");
    r = simpleBody(b, l + 1);
    if (!r) r = fnBody_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // barredClause*
  private static boolean fnBody_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fnBody_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!barredClause(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "fnBody_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // KW_DEF weakId
  //  tele* type fnBody
  public static boolean fnDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fnDecl")) return false;
    if (!nextTokenIs(b, KW_DEF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KW_DEF);
    r = r && weakId(b, l + 1);
    r = r && fnDecl_2(b, l + 1);
    r = r && type(b, l + 1);
    r = r && fnBody(b, l + 1);
    exit_section_(b, m, FN_DECL, r);
    return r;
  }

  // tele*
  private static boolean fnDecl_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fnDecl_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "fnDecl_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LGOAL expr? RGOAL
  public static boolean goalExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "goalExpr")) return false;
    if (!nextTokenIs(b, LGOAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LGOAL);
    r = r && goalExpr_1(b, l + 1);
    r = r && consumeToken(b, RGOAL);
    exit_section_(b, m, GOAL_EXPR, r);
    return r;
  }

  // expr?
  private static boolean goalExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "goalExpr_1")) return false;
    expr(b, l + 1, -1);
    return true;
  }

  /* ********************************************************** */
  // goalExpr | calmFaceExpr
  public static boolean holeExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "holeExpr")) return false;
    if (!nextTokenIs(b, "<hole expr>", CALM_FACE, LGOAL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, HOLE_EXPR, "<hole expr>");
    r = goalExpr(b, l + 1);
    if (!r) r = calmFaceExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // weakId | <<paren lambdaTeleBinder>>
  public static boolean lambdaTele(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaTele")) return false;
    if (!nextTokenIs(b, "<lambda tele>", ID, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LAMBDA_TELE, "<lambda tele>");
    r = weakId(b, l + 1);
    if (!r) r = paren(b, l + 1, LamettPsiParser::lambdaTeleBinder);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // teleBinderTyped
  //                    | teleBinderUntyped
  public static boolean lambdaTeleBinder(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaTeleBinder")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = teleBinderTyped(b, l + 1);
    if (!r) r = teleBinderUntyped(b, l + 1);
    exit_section_(b, m, LAMBDA_TELE_BINDER, r);
    return r;
  }

  /* ********************************************************** */
  // refExpr | holeExpr | constExpr
  public static boolean literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, LITERAL, "<literal>");
    r = refExpr(b, l + 1);
    if (!r) r = holeExpr(b, l + 1);
    if (!r) r = constExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BAR newArgField weakId* IMPLIES expr
  public static boolean newArg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newArg")) return false;
    if (!nextTokenIs(b, BAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BAR);
    r = r && newArgField(b, l + 1);
    r = r && newArg_2(b, l + 1);
    r = r && consumeToken(b, IMPLIES);
    r = r && expr(b, l + 1, -1);
    exit_section_(b, m, NEW_ARG, r);
    return r;
  }

  // weakId*
  private static boolean newArg_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newArg_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!weakId(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "newArg_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // weakId
  public static boolean newArgField(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newArgField")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    exit_section_(b, m, NEW_ARG_FIELD, r);
    return r;
  }

  /* ********************************************************** */
  // <<braced newArg*>>
  public static boolean newBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newBody")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced(b, l + 1, LamettPsiParser::newBody_0_0);
    exit_section_(b, m, NEW_BODY, r);
    return r;
  }

  // newArg*
  private static boolean newBody_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newBody_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!newArg(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "newBody_0_0", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LPAREN <<param>> RPAREN
  public static boolean paren(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "paren")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && _param.parse(b, l);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, PAREN, r);
    return r;
  }

  /* ********************************************************** */
  // LPARTIAL partialInner? RPARTIAL
  public static boolean partialAtom(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialAtom")) return false;
    if (!nextTokenIs(b, LPARTIAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPARTIAL);
    r = r && partialAtom_1(b, l + 1);
    r = r && consumeToken(b, RPARTIAL);
    exit_section_(b, m, PARTIAL_ATOM, r);
    return r;
  }

  // partialInner?
  private static boolean partialAtom_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialAtom_1")) return false;
    partialInner(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<braced partialInner>>
  public static boolean partialBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialBlock")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced(b, l + 1, LamettPsiParser::partialInner);
    exit_section_(b, m, PARTIAL_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // bareSubSystem? barredSubSystem*
  static boolean partialInner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialInner")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = partialInner_0(b, l + 1);
    r = r && partialInner_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // bareSubSystem?
  private static boolean partialInner_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialInner_0")) return false;
    bareSubSystem(b, l + 1);
    return true;
  }

  // barredSubSystem*
  private static boolean partialInner_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "partialInner_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!barredSubSystem(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "partialInner_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // <<paren patterns>>
  //               | LPAREN RPAREN
  //               | weakId
  public static boolean pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern")) return false;
    if (!nextTokenIs(b, "<pattern>", ID, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PATTERN, "<pattern>");
    r = paren(b, l + 1, LamettPsiParser::patterns);
    if (!r) r = parseTokens(b, 0, LPAREN, RPAREN);
    if (!r) r = weakId(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // pattern+
  public static boolean patterns(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "patterns")) return false;
    if (!nextTokenIs(b, "<patterns>", ID, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PATTERNS, "<patterns>");
    r = pattern(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!pattern(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "patterns", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // KW_PRINT tele* type simpleBody
  public static boolean printDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "printDecl")) return false;
    if (!nextTokenIs(b, KW_PRINT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KW_PRINT);
    r = r && printDecl_1(b, l + 1);
    r = r && type(b, l + 1);
    r = r && simpleBody(b, l + 1);
    exit_section_(b, m, PRINT_DECL, r);
    return r;
  }

  // tele*
  private static boolean printDecl_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "printDecl_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "printDecl_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // stmts
  static boolean program(PsiBuilder b, int l) {
    return stmts(b, l + 1);
  }

  /* ********************************************************** */
  // DOT (NUMBER | projFixId)
  public static boolean projFix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "projFix")) return false;
    if (!nextTokenIs(b, DOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && projFix_1(b, l + 1);
    exit_section_(b, m, PROJ_FIX, r);
    return r;
  }

  // NUMBER | projFixId
  private static boolean projFix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "projFix_1")) return false;
    boolean r;
    r = consumeToken(b, NUMBER);
    if (!r) r = projFixId(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // weakId
  public static boolean projFixId(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "projFixId")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    exit_section_(b, m, PROJ_FIX_ID, r);
    return r;
  }

  /* ********************************************************** */
  // weakId
  public static boolean refExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "refExpr")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    exit_section_(b, m, REF_EXPR, r);
    return r;
  }

  /* ********************************************************** */
  // IMPLIES expr
  static boolean simpleBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleBody")) return false;
    if (!nextTokenIs(b, IMPLIES)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, IMPLIES);
    p = r; // pin = 1
    r = r && expr(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // DOC_COMMENT | KW_DEF | KW_CLASS | KW_DATA | KW_PRINT
  static boolean stmt_first(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_first")) return false;
    boolean r;
    r = consumeToken(b, DOC_COMMENT);
    if (!r) r = consumeToken(b, KW_DEF);
    if (!r) r = consumeToken(b, KW_CLASS);
    if (!r) r = consumeToken(b, KW_DATA);
    if (!r) r = consumeToken(b, KW_PRINT);
    return r;
  }

  /* ********************************************************** */
  // !(stmt_first)
  static boolean stmt_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !stmt_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (stmt_first)
  private static boolean stmt_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = stmt_first(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(<<eof>>) decl
  static boolean stmt_with_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_with_recover")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = stmt_with_recover_0(b, l + 1);
    p = r; // pin = 1
    r = r && decl(b, l + 1);
    exit_section_(b, l, m, r, p, LamettPsiParser::stmt_recover);
    return r || p;
  }

  // !(<<eof>>)
  private static boolean stmt_with_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmt_with_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !stmt_with_recover_0_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<eof>>
  private static boolean stmt_with_recover_0_0(PsiBuilder b, int l) {
    return eof(b, l + 1);
  }

  /* ********************************************************** */
  // stmt_with_recover*
  static boolean stmts(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stmts")) return false;
    while (true) {
      int c = current_position_(b);
      if (!stmt_with_recover(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "stmts", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // expr DEFINE_AS expr
  public static boolean subSystem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subSystem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SUB_SYSTEM, "<sub system>");
    r = expr(b, l + 1, -1);
    r = r && consumeToken(b, DEFINE_AS);
    r = r && expr(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // literal | <<paren teleBinder>>
  public static boolean tele(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tele")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TELE, "<tele>");
    r = literal(b, l + 1);
    if (!r) r = paren(b, l + 1, LamettPsiParser::teleBinder);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // teleBinderTyped
  //              | teleBinderAnonymous
  public static boolean teleBinder(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "teleBinder")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TELE_BINDER, "<tele binder>");
    r = teleBinderTyped(b, l + 1);
    if (!r) r = teleBinderAnonymous(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expr
  public static boolean teleBinderAnonymous(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "teleBinderAnonymous")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TELE_BINDER_ANONYMOUS, "<tele binder anonymous>");
    r = expr(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // teleBinderUntyped type
  public static boolean teleBinderTyped(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "teleBinderTyped")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = teleBinderUntyped(b, l + 1);
    r = r && type(b, l + 1);
    exit_section_(b, m, TELE_BINDER_TYPED, r);
    return r;
  }

  /* ********************************************************** */
  // weakId+
  public static boolean teleBinderUntyped(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "teleBinderUntyped")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!weakId(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "teleBinderUntyped", c)) break;
    }
    exit_section_(b, m, TELE_BINDER_UNTYPED, r);
    return r;
  }

  /* ********************************************************** */
  // LPAREN exprList RPAREN
  public static boolean tupleAtom(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tupleAtom")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && exprList(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, TUPLE_ATOM, r);
    return r;
  }

  /* ********************************************************** */
  // COLON expr
  public static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    if (!nextTokenIs(b, COLON)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE, null);
    r = consumeToken(b, COLON);
    p = r; // pin = 1
    r = r && expr(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ID
  public static boolean weakId(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "weakId")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ID);
    exit_section_(b, m, WEAK_ID, r);
    return r;
  }

  /* ********************************************************** */
  // Expression root: expr
  // Operator priority table:
  // 0: PREFIX(newExpr)
  // 1: PREFIX(piExpr)
  // 2: PREFIX(forallExpr)
  // 3: PREFIX(sigmaExpr)
  // 4: ATOM(lambdaExpr)
  // 5: ATOM(selfExpr)
  // 6: PREFIX(pathExpr)
  // 7: ATOM(atomExpr)
  // 8: BINARY(arrowExpr)
  // 9: POSTFIX(appExpr)
  // 10: POSTFIX(projExpr)
  // 11: BINARY(disjExpr)
  // 12: BINARY(conjExpr)
  // 13: PREFIX(iforallExpr)
  // 14: BINARY(ieqExpr)
  // 15: PREFIX(inegExpr)
  public static boolean expr(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expr")) return false;
    addVariant(b, "<expr>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expr>");
    r = newExpr(b, l + 1);
    if (!r) r = piExpr(b, l + 1);
    if (!r) r = forallExpr(b, l + 1);
    if (!r) r = sigmaExpr(b, l + 1);
    if (!r) r = lambdaExpr(b, l + 1);
    if (!r) r = selfExpr(b, l + 1);
    if (!r) r = pathExpr(b, l + 1);
    if (!r) r = atomExpr(b, l + 1);
    if (!r) r = iforallExpr(b, l + 1);
    if (!r) r = inegExpr(b, l + 1);
    p = r;
    r = r && expr_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean expr_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expr_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 8 && consumeTokenSmart(b, TO)) {
        r = expr(b, l, 7);
        exit_section_(b, l, m, ARROW_EXPR, r, true, null);
      } else if (g < 9 && appExpr_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, APP_EXPR, r, true, null);
      } else if (g < 10 && projFix(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, PROJ_EXPR, r, true, null);
      } else if (g < 11 && consumeTokenSmart(b, KW_DISJ)) {
        r = expr(b, l, 11);
        exit_section_(b, l, m, DISJ_EXPR, r, true, null);
      } else if (g < 12 && consumeTokenSmart(b, KW_CONJ)) {
        r = expr(b, l, 12);
        exit_section_(b, l, m, CONJ_EXPR, r, true, null);
      } else if (g < 14 && consumeTokenSmart(b, KW_EQ)) {
        r = expr(b, l, 14);
        exit_section_(b, l, m, IEQ_EXPR, r, true, null);
      } else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  public static boolean newExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpr")) return false;
    if (!nextTokenIsSmart(b, KW_NEW)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, KW_NEW);
    p = r;
    r = p && expr(b, l, 0);
    r = p && report_error_(b, newExpr_1(b, l + 1)) && r;
    exit_section_(b, l, m, NEW_EXPR, r, p, null);
    return r || p;
  }

  // newBody?
  private static boolean newExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpr_1")) return false;
    newBody(b, l + 1);
    return true;
  }

  public static boolean piExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "piExpr")) return false;
    if (!nextTokenIsSmart(b, KW_PI)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = piExpr_0(b, l + 1);
    p = r;
    r = p && expr(b, l, 1);
    exit_section_(b, l, m, PI_EXPR, r, p, null);
    return r || p;
  }

  // KW_PI tele+ TO
  private static boolean piExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "piExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_PI);
    r = r && piExpr_0_1(b, l + 1);
    r = r && consumeToken(b, TO);
    exit_section_(b, m, null, r);
    return r;
  }

  // tele+
  private static boolean piExpr_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "piExpr_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = tele(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!tele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "piExpr_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean forallExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forallExpr")) return false;
    if (!nextTokenIsSmart(b, KW_FORALL)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = forallExpr_0(b, l + 1);
    p = r;
    r = p && expr(b, l, 2);
    exit_section_(b, l, m, FORALL_EXPR, r, p, null);
    return r || p;
  }

  // KW_FORALL lambdaTele+ TO
  private static boolean forallExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forallExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_FORALL);
    r = r && forallExpr_0_1(b, l + 1);
    r = r && consumeToken(b, TO);
    exit_section_(b, m, null, r);
    return r;
  }

  // lambdaTele+
  private static boolean forallExpr_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forallExpr_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = lambdaTele(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!lambdaTele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "forallExpr_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean sigmaExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "sigmaExpr")) return false;
    if (!nextTokenIsSmart(b, KW_SIGMA)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = sigmaExpr_0(b, l + 1);
    p = r;
    r = p && expr(b, l, 3);
    exit_section_(b, l, m, SIGMA_EXPR, r, p, null);
    return r || p;
  }

  // KW_SIGMA tele SUCHTHAT
  private static boolean sigmaExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "sigmaExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_SIGMA);
    r = r && tele(b, l + 1);
    r = r && consumeToken(b, SUCHTHAT);
    exit_section_(b, m, null, r);
    return r;
  }

  // KW_LAMBDA lambdaTele+ (IMPLIES expr)?
  public static boolean lambdaExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaExpr")) return false;
    if (!nextTokenIsSmart(b, KW_LAMBDA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_LAMBDA);
    r = r && lambdaExpr_1(b, l + 1);
    r = r && lambdaExpr_2(b, l + 1);
    exit_section_(b, m, LAMBDA_EXPR, r);
    return r;
  }

  // lambdaTele+
  private static boolean lambdaExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaExpr_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = lambdaTele(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!lambdaTele(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "lambdaExpr_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // (IMPLIES expr)?
  private static boolean lambdaExpr_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaExpr_2")) return false;
    lambdaExpr_2_0(b, l + 1);
    return true;
  }

  // IMPLIES expr
  private static boolean lambdaExpr_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambdaExpr_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, IMPLIES);
    r = r && expr(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // KW_SELF (AT weakId)?
  public static boolean selfExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selfExpr")) return false;
    if (!nextTokenIsSmart(b, KW_SELF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_SELF);
    r = r && selfExpr_1(b, l + 1);
    exit_section_(b, m, SELF_EXPR, r);
    return r;
  }

  // (AT weakId)?
  private static boolean selfExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selfExpr_1")) return false;
    selfExpr_1_0(b, l + 1);
    return true;
  }

  // AT weakId
  private static boolean selfExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selfExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, AT);
    r = r && weakId(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean pathExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pathExpr")) return false;
    if (!nextTokenIsSmart(b, LPATH)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = pathExpr_0(b, l + 1);
    p = r;
    r = p && expr(b, l, 6);
    r = p && report_error_(b, pathExpr_1(b, l + 1)) && r;
    exit_section_(b, l, m, PATH_EXPR, r, p, null);
    return r || p;
  }

  // LPATH weakId+ RPATH
  private static boolean pathExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pathExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LPATH);
    r = r && pathExpr_0_1(b, l + 1);
    r = r && consumeToken(b, RPATH);
    exit_section_(b, m, null, r);
    return r;
  }

  // weakId+
  private static boolean pathExpr_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pathExpr_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!weakId(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pathExpr_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // partialBlock?
  private static boolean pathExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pathExpr_1")) return false;
    partialBlock(b, l + 1);
    return true;
  }

  // tupleAtom | partialAtom | literal
  public static boolean atomExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "atomExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, ATOM_EXPR, "<atom expr>");
    r = tupleAtom(b, l + 1);
    if (!r) r = partialAtom(b, l + 1);
    if (!r) r = literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // argument+
  private static boolean appExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "appExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = argument(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!argument(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "appExpr_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean iforallExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iforallExpr")) return false;
    if (!nextTokenIsSmart(b, KW_FORALL)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = iforallExpr_0(b, l + 1);
    p = r;
    r = p && expr(b, l, 13);
    exit_section_(b, l, m, IFORALL_EXPR, r, p, null);
    return r || p;
  }

  // KW_FORALL weakId+ IMPLIES
  private static boolean iforallExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iforallExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, KW_FORALL);
    r = r && iforallExpr_0_1(b, l + 1);
    r = r && consumeToken(b, IMPLIES);
    exit_section_(b, m, null, r);
    return r;
  }

  // weakId+
  private static boolean iforallExpr_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iforallExpr_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = weakId(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!weakId(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "iforallExpr_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean inegExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inegExpr")) return false;
    if (!nextTokenIsSmart(b, KW_INEG)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, KW_INEG);
    p = r;
    r = p && expr(b, l, -1);
    exit_section_(b, l, m, INEG_EXPR, r, p, null);
    return r || p;
  }

  static final Parser expr_parser_ = (b, l) -> expr(b, l + 1, -1);
}
