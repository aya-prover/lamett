// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.lamett.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public abstract class LamettParserDefinitionBase implements ParserDefinition {
  @Override public @NotNull Lexer createLexer(Project project) {
    return new FlexAdapter(new _LamettPsiLexer());
  }

  @Override public @NotNull PsiParser createParser(Project project) {
    return new LamettPsiParser();
  }

  @Override public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }

  @Override public @NotNull TokenSet getCommentTokens() {
    // Remark needs DOC_COMMENT, do not skip it.
    return SKIP_COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  public static final @NotNull LamettPsiTokenType LINE_COMMENT = new LamettPsiTokenType("LINE_COMMENT");
  public static final @NotNull LamettPsiTokenType BLOCK_COMMENT = new LamettPsiTokenType("BLOCK_COMMENT");
  public static final @NotNull TokenSet COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT);
  public static final @NotNull TokenSet SKIP_COMMENTS = COMMENTS;
  /** non-text symbols that should be highlighted like keywords */
  public static final @NotNull TokenSet MARKERS = TokenSet.create(
    LamettPsiElementTypes.TO,
    LamettPsiElementTypes.LARROW,
    LamettPsiElementTypes.IMPLIES,
    LamettPsiElementTypes.DEFINE_AS,
    LamettPsiElementTypes.SUCHTHAT,
    LamettPsiElementTypes.DOT,
    LamettPsiElementTypes.BAR,
    LamettPsiElementTypes.COLON
  );
  /** text keywords */
  public static final @NotNull TokenSet KEYWORDS = TokenSet.create(
    LamettPsiElementTypes.KW_CODATA,
    LamettPsiElementTypes.KW_DATA,
    LamettPsiElementTypes.KW_DEF,
    LamettPsiElementTypes.KW_SELF,
    LamettPsiElementTypes.KW_OVERRIDE,
    LamettPsiElementTypes.KW_EXTENDS,
    LamettPsiElementTypes.KW_FORALL,
    LamettPsiElementTypes.KW_LAMBDA,
    LamettPsiElementTypes.KW_NEW,
    LamettPsiElementTypes.KW_PI,
    LamettPsiElementTypes.KW_SIGMA,
    LamettPsiElementTypes.KW_CLASS,
    LamettPsiElementTypes.KW_CLASSIFIYING,
    LamettPsiElementTypes.KW_TYPE,
    LamettPsiElementTypes.KW_ISET,
    LamettPsiElementTypes.KW_ULIFT
  );
  /** Anything that has a unicode variant. Keep in touch with LamettPsiLexer.flex */
  public static final @NotNull TokenSet UNICODES = TokenSet.create(
    LamettPsiElementTypes.KW_ULIFT,
    LamettPsiElementTypes.KW_SIGMA,
    LamettPsiElementTypes.KW_LAMBDA,
    LamettPsiElementTypes.KW_PI,
    LamettPsiElementTypes.KW_FORALL,
    LamettPsiElementTypes.TO,
    LamettPsiElementTypes.LARROW,
    LamettPsiElementTypes.IMPLIES,
    LamettPsiElementTypes.LIDIOM,
    LamettPsiElementTypes.RIDIOM,
    LamettPsiElementTypes.LPARTIAL,
    LamettPsiElementTypes.RPARTIAL,
    LamettPsiElementTypes.LPATH,
    LamettPsiElementTypes.RPATH
  );

  public static final @NotNull TokenSet DELIMITERS = TokenSet.create(
    LamettPsiElementTypes.LPAREN,
    LamettPsiElementTypes.RPAREN,
    LamettPsiElementTypes.LBRACE,
    LamettPsiElementTypes.RBRACE
  );
}
