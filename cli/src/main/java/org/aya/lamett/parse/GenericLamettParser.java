// Copyright (c) 2020-2022 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.lamett.parse;

import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.syntax.Decl;
import org.aya.lamett.syntax.Expr;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public interface GenericLamettParser {
  @NotNull ImmutableSeq<Decl> program(@NotNull SourceFile sourceFile);
  @NotNull Expr expr(@NotNull String code, @NotNull SourcePos pos);
  @NotNull Reporter reporter();
}
