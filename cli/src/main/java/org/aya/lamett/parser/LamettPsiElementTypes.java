// This is a generated file. Not intended for manual editing.
package org.aya.lamett.parser;

import com.intellij.psi.tree.IElementType;

public interface LamettPsiElementTypes {

  IElementType APP_EXPR = new LamettPsiElementType("APP_EXPR");
  IElementType ARGUMENT = new LamettPsiElementType("ARGUMENT");
  IElementType ARROW_EXPR = new LamettPsiElementType("ARROW_EXPR");
  IElementType ATOM_EXPR = new LamettPsiElementType("ATOM_EXPR");
  IElementType BARE_SUB_SYSTEM = new LamettPsiElementType("BARE_SUB_SYSTEM");
  IElementType BARRED_CLAUSE = new LamettPsiElementType("BARRED_CLAUSE");
  IElementType BARRED_SUB_SYSTEM = new LamettPsiElementType("BARRED_SUB_SYSTEM");
  IElementType CALM_FACE_EXPR = new LamettPsiElementType("CALM_FACE_EXPR");
  IElementType CLASS_DECL = new LamettPsiElementType("CLASS_DECL");
  IElementType CLASS_MEMBER = new LamettPsiElementType("CLASS_MEMBER");
  IElementType CLAUSE = new LamettPsiElementType("CLAUSE");
  IElementType COMMA_SEP = new LamettPsiElementType("COMMA_SEP");
  IElementType CONJ_EXPR = new LamettPsiElementType("CONJ_EXPR");
  IElementType CONST_EXPR = new LamettPsiElementType("CONST_EXPR");
  IElementType DATA_BODY = new LamettPsiElementType("DATA_BODY");
  IElementType DATA_CTOR = new LamettPsiElementType("DATA_CTOR");
  IElementType DATA_CTOR_CLAUSE = new LamettPsiElementType("DATA_CTOR_CLAUSE");
  IElementType DATA_DECL = new LamettPsiElementType("DATA_DECL");
  IElementType DECL = new LamettPsiElementType("DECL");
  IElementType DISJ_EXPR = new LamettPsiElementType("DISJ_EXPR");
  IElementType EXPR = new LamettPsiElementType("EXPR");
  IElementType FN_BODY = new LamettPsiElementType("FN_BODY");
  IElementType FN_DECL = new LamettPsiElementType("FN_DECL");
  IElementType FORALL_EXPR = new LamettPsiElementType("FORALL_EXPR");
  IElementType GOAL_EXPR = new LamettPsiElementType("GOAL_EXPR");
  IElementType HOLE_EXPR = new LamettPsiElementType("HOLE_EXPR");
  IElementType IEQ_EXPR = new LamettPsiElementType("IEQ_EXPR");
  IElementType IFORALL_EXPR = new LamettPsiElementType("IFORALL_EXPR");
  IElementType INEG_EXPR = new LamettPsiElementType("INEG_EXPR");
  IElementType LAMBDA_EXPR = new LamettPsiElementType("LAMBDA_EXPR");
  IElementType LAMBDA_TELE = new LamettPsiElementType("LAMBDA_TELE");
  IElementType LAMBDA_TELE_BINDER = new LamettPsiElementType("LAMBDA_TELE_BINDER");
  IElementType LITERAL = new LamettPsiElementType("LITERAL");
  IElementType NEW_ARG = new LamettPsiElementType("NEW_ARG");
  IElementType NEW_ARG_FIELD = new LamettPsiElementType("NEW_ARG_FIELD");
  IElementType NEW_BODY = new LamettPsiElementType("NEW_BODY");
  IElementType NEW_EXPR = new LamettPsiElementType("NEW_EXPR");
  IElementType PAREN = new LamettPsiElementType("PAREN");
  IElementType PARTIAL_ATOM = new LamettPsiElementType("PARTIAL_ATOM");
  IElementType PARTIAL_BLOCK = new LamettPsiElementType("PARTIAL_BLOCK");
  IElementType PATH_EXPR = new LamettPsiElementType("PATH_EXPR");
  IElementType PATTERN = new LamettPsiElementType("PATTERN");
  IElementType PATTERNS = new LamettPsiElementType("PATTERNS");
  IElementType PI_EXPR = new LamettPsiElementType("PI_EXPR");
  IElementType PRINT_DECL = new LamettPsiElementType("PRINT_DECL");
  IElementType PROJ_EXPR = new LamettPsiElementType("PROJ_EXPR");
  IElementType PROJ_FIX = new LamettPsiElementType("PROJ_FIX");
  IElementType PROJ_FIX_ID = new LamettPsiElementType("PROJ_FIX_ID");
  IElementType REF_EXPR = new LamettPsiElementType("REF_EXPR");
  IElementType SELF_EXPR = new LamettPsiElementType("SELF_EXPR");
  IElementType SIGMA_EXPR = new LamettPsiElementType("SIGMA_EXPR");
  IElementType SUB_SYSTEM = new LamettPsiElementType("SUB_SYSTEM");
  IElementType TELE = new LamettPsiElementType("TELE");
  IElementType TELE_BINDER = new LamettPsiElementType("TELE_BINDER");
  IElementType TELE_BINDER_ANONYMOUS = new LamettPsiElementType("TELE_BINDER_ANONYMOUS");
  IElementType TELE_BINDER_TYPED = new LamettPsiElementType("TELE_BINDER_TYPED");
  IElementType TELE_BINDER_UNTYPED = new LamettPsiElementType("TELE_BINDER_UNTYPED");
  IElementType TUPLE_ATOM = new LamettPsiElementType("TUPLE_ATOM");
  IElementType TYPE = new LamettPsiElementType("TYPE");
  IElementType WEAK_ID = new LamettPsiElementType("WEAK_ID");

  IElementType AT = new LamettPsiTokenType("@");
  IElementType BAR = new LamettPsiTokenType("|");
  IElementType CALM_FACE = new LamettPsiTokenType("_");
  IElementType COLON = new LamettPsiTokenType(":");
  IElementType COMMA = new LamettPsiTokenType(",");
  IElementType DEFINE_AS = new LamettPsiTokenType(":=");
  IElementType DOC_COMMENT = new LamettPsiTokenType("DOC_COMMENT");
  IElementType DOT = new LamettPsiTokenType(".");
  IElementType ID = new LamettPsiTokenType("ID");
  IElementType IMPLIES = new LamettPsiTokenType("=>");
  IElementType KW_CLASS = new LamettPsiTokenType("class");
  IElementType KW_CLASSIFIYING = new LamettPsiTokenType("classifying");
  IElementType KW_CODATA = new LamettPsiTokenType("codata");
  IElementType KW_CONJ = new LamettPsiTokenType("KW_CONJ");
  IElementType KW_DATA = new LamettPsiTokenType("data");
  IElementType KW_DEF = new LamettPsiTokenType("def");
  IElementType KW_DISJ = new LamettPsiTokenType("KW_DISJ");
  IElementType KW_EXTENDS = new LamettPsiTokenType("extends");
  IElementType KW_FORALL = new LamettPsiTokenType("KW_FORALL");
  IElementType KW_IEQ = new LamettPsiTokenType("KW_IEQ");
  IElementType KW_IFORALL = new LamettPsiTokenType("KW_IFORALL");
  IElementType KW_INEG = new LamettPsiTokenType("KW_INEG");
  IElementType KW_INTERVAL = new LamettPsiTokenType("KW_INTERVAL");
  IElementType KW_ISET = new LamettPsiTokenType("ISet");
  IElementType KW_LAMBDA = new LamettPsiTokenType("fn");
  IElementType KW_NEW = new LamettPsiTokenType("new");
  IElementType KW_OVERRIDE = new LamettPsiTokenType("override");
  IElementType KW_PI = new LamettPsiTokenType("Fn");
  IElementType KW_PRINT = new LamettPsiTokenType("KW_PRINT");
  IElementType KW_SELF = new LamettPsiTokenType("self");
  IElementType KW_SET = new LamettPsiTokenType("KW_SET");
  IElementType KW_SIGMA = new LamettPsiTokenType("KW_SIGMA");
  IElementType KW_TYPE = new LamettPsiTokenType("Type");
  IElementType KW_ULIFT = new LamettPsiTokenType("KW_ULIFT");
  IElementType LARROW = new LamettPsiTokenType("<-");
  IElementType LBRACE = new LamettPsiTokenType("{");
  IElementType LGOAL = new LamettPsiTokenType("{?");
  IElementType LIDIOM = new LamettPsiTokenType("(|");
  IElementType LPAREN = new LamettPsiTokenType("(");
  IElementType LPARTIAL = new LamettPsiTokenType("{|");
  IElementType LPATH = new LamettPsiTokenType("[|");
  IElementType NUMBER = new LamettPsiTokenType("NUMBER");
  IElementType RBRACE = new LamettPsiTokenType("}");
  IElementType RGOAL = new LamettPsiTokenType("?}");
  IElementType RIDIOM = new LamettPsiTokenType("|)");
  IElementType RPAREN = new LamettPsiTokenType(")");
  IElementType RPARTIAL = new LamettPsiTokenType("|}");
  IElementType RPATH = new LamettPsiTokenType("|]");
  IElementType SUCHTHAT = new LamettPsiTokenType("**");
  IElementType TO = new LamettPsiTokenType("->");
}
