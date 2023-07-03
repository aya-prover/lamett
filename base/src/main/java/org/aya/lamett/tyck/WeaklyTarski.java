package org.aya.lamett.tyck;

import org.aya.lamett.syntax.Keyword;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.syntax.Type;
import org.aya.lamett.util.Param;
import org.jetbrains.annotations.NotNull;

public record WeaklyTarski(@NotNull Normalizer n) {
  public @NotNull Type el(@NotNull Term ty) {
    return switch (n.term(ty)) {
      case Term.Lit(var lit) when lit == Keyword.F -> Type.Lit.F;
      case Term.Lit(var lit) when lit == Keyword.ISet -> Type.Lit.ISet;
      case Term.Lit(var lit) when lit == Keyword.Set -> Type.Lit.Set;
      case Term.Lit(var lit) when lit == Keyword.U -> Type.Lit.U;
      case Term.Lit(var lit) when lit == Keyword.I -> Type.Lit.I;
      case Term.Pi(var param, var cod) -> new Type.Pi(param(param), el(cod));
      case Term.Sigma(var param, var cod) -> new Type.Sigma(param(param), el(cod));
      case Term.Path(var binders, var ext) -> {
        var sub = new Type.Sub(el(ext.type()), ext.restr().boundaries());
        yield Type.mkPi(binders.map(i -> new Param<>(i, Type.Lit.I)), sub);
      }
      case Term misc -> new Type.El(misc);
    };
  }

  private @NotNull Param<Type> param(@NotNull Param<Term> param) {
    return new Param<>(param.x(), el(param.type()));
  }
}
