// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.lamett.parse;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.DefaultPsiParser;
import com.intellij.psi.TokenType;
import com.intellij.psi.builder.FleetPsiBuilder;
import com.intellij.psi.builder.MarkerNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.text.StringSlice;
import org.aya.lamett.parser.LamettLanguage;
import org.aya.lamett.parser.LamettParserDefinitionBase;
import org.aya.lamett.syntax.Decl;
import org.aya.lamett.syntax.Expr;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static org.aya.lamett.parser.LamettPsiElementTypes.PRINT_DECL;
import static org.aya.lamett.parser.LamettPsiElementTypes.TYPE;

public record LamettParserImpl(@NotNull Reporter reporter) implements GenericLamettParser {
  private static final @NotNull TokenSet ERROR = TokenSet.create(TokenType.ERROR_ELEMENT, TokenType.BAD_CHARACTER);

  public @NotNull GenericNode<?> parseNode(@NotNull String code) {
    var parser = new LamettFleetParser();
    return new NodeWrapper(code, parser.parse(code));
  }

  @Override public @NotNull ImmutableSeq<Decl> program(@NotNull SourceFile sourceFile) {
    return parse(sourceFile.sourceCode(), sourceFile);
  }

  @Override public @NotNull Expr expr(@NotNull String code, @NotNull SourcePos pos) {
    var node = parseNode("print : (" + code + ") => U");
    var bud = node.child(PRINT_DECL).child(TYPE);
    return new LamettProducer(Either.right(pos), reporter).type(bud);
  }

  private @NotNull ImmutableSeq<Decl> parse(@NotNull String code, @NotNull SourceFile errorReport) {
    var node = reportErrorElements(parseNode(code), errorReport);
    return new LamettProducer(Either.left(errorReport), reporter).program(node);
  }

  private static @NotNull SourceFile replSourceFile(@NotNull String text) {
    return new SourceFile("<stdin>", Path.of("stdin"), text);
  }

  private @NotNull GenericNode<?> reportErrorElements(@NotNull GenericNode<?> node, @NotNull SourceFile file) {
    // note: report syntax error here (instead of in Producer) bc
    // IJ plugin directly reports them through PsiErrorElements.
    node.childrenView()
      .filter(i -> ERROR.contains(i.elementType()))
      .forEach(e ->
        reporter.report(new ParseError(LamettProducer.sourcePosOf(e, file),
          "Cannot parse")
        ));
    return node;
  }

  private record NodeWrapper(
    @NotNull MarkerNode node,
    @Override @NotNull StringSlice tokenText
  ) implements GenericNode<NodeWrapper> {
    public NodeWrapper(@NotNull String code, @NotNull MarkerNode node) {
      this(node, StringSlice.of(code, node.range().getStartOffset(), node.range().getEndOffset()));
    }

    @Override public @NotNull IElementType elementType() {
      return node.elementType();
    }

    @SuppressWarnings("UnstableApiUsage") @Override public @NotNull SeqView<NodeWrapper> childrenView() {
      return node.children().view().map(c -> new NodeWrapper(tokenText.source(), c));
    }

    @Override public @NotNull TextRange range() {
      return node.range();
    }

    @Override public @NotNull String toDebugString() {
      return node.toDebugString("  ");
    }
  }

  private static class LamettFleetParser extends DefaultPsiParser {
    public LamettFleetParser() {
      super(new LamettFleetParserDefinition());
    }

    private static final class LamettFleetParserDefinition extends LamettParserDefinitionBase {
      private final @NotNull IFileElementType FILE = new IFileElementType(LamettLanguage.INSTANCE) {
        @Override public void parse(@NotNull FleetPsiBuilder<?> builder) {
        }
      };

      @Override public @NotNull IFileElementType getFileNodeType() {
        return FILE;
      }
    }
  }
}
