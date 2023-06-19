package org.aya.lamett.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.aya.lamett.parser.LamettPsiElementTypes.*;

%%

%{
  public _LamettPsiLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _LamettPsiLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

KW_ULIFT=ulift|\u2191
KW_SIGMA=Sig|\u03a3
KW_FORALL=forall|\u2200
KW_EQ=eq|=
KW_LAMBDA=fn|\u03bb
KW_INEG=ineg|¬
KW_DISJ="\\/"|\u2228
KW_CONJ="/\\"|\u2227
ID=[α-ωa-zA-Z_][α-ωa-zA-Z0-9_'-]*
NUMBER=[0-9]+

%%
<YYINITIAL> {
  {WHITE_SPACE}       { return WHITE_SPACE; }

  "ISet"              { return KW_ISET; }
  "Set"               { return KW_SET; }
  "U"                 { return KW_TYPE; }
  "I"                 { return KW_INTERVAL; }
  "F"                 { return KW_F; }
  "def"               { return KW_DEF; }
  "classifying"       { return KW_CLASSIFIYING; }
  "class"             { return KW_CLASS; }
  "print"             { return KW_PRINT; }
  "data"              { return KW_DATA; }
  "coe"               { return KW_COE; }
  "extends"           { return KW_EXTENDS; }
  "new"               { return KW_NEW; }
  "self"              { return KW_SELF; }
  "override"          { return KW_OVERRIDE; }
  "codata"            { return KW_CODATA; }
  "Partial"           { return KW_PARTIAL; }
  "fn"                { return KW_LAMBDA; }
  "Fn"                { return KW_PI; }
  "->"                { return TO; }
  "<-"                { return LARROW; }
  "=>"                { return IMPLIES; }
  ":="                { return DEFINE_AS; }
  "**"                { return SUCHTHAT; }
  "."                 { return DOT; }
  "|"                 { return BAR; }
  ","                 { return COMMA; }
  ":"                 { return COLON; }
  "{"                 { return LBRACE; }
  "}"                 { return RBRACE; }
  "("                 { return LPAREN; }
  ")"                 { return RPAREN; }
  "(|"                { return LIDIOM; }
  "|)"                { return RIDIOM; }
  "{?"                { return LGOAL; }
  "?}"                { return RGOAL; }
  "{|"                { return LPARTIAL; }
  "|}"                { return RPARTIAL; }
  "[|"                { return LPATH; }
  "|]"                { return RPATH; }
  "@"                 { return AT; }
  "_"                 { return CALM_FACE; }
  "DOC_COMMENT"       { return DOC_COMMENT; }

  {KW_ULIFT}          { return KW_ULIFT; }
  {KW_LAMBDA}         { return KW_LAMBDA; }
  {KW_SIGMA}          { return KW_SIGMA; }
  {KW_FORALL}         { return KW_FORALL; }
  {KW_EQ}             { return KW_EQ; }
  {KW_DISJ}           { return KW_DISJ; }
  {KW_CONJ}           { return KW_CONJ; }
  {KW_INEG}           { return KW_INEG; }
  {ID}                { return ID; }
  {NUMBER}            { return NUMBER; }
}

[^] { return BAD_CHARACTER; }
