package org.aya.lamett.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.lamett.util.Distiller;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Context restriction. Aka cofibration in the context of cubical type theory.
 */
sealed public interface Restr extends Docile {
  /** the all-in-one map, maybe there's a better way */
  default @NotNull Restr map(@NotNull UnaryOperator<Term> mapTerm, @NotNull UnaryOperator<Term.Cofib.Conj> mapConj) {
    return switch (this) {
      case Cubical(var bdry) -> new Cubical(bdry.map(t ->
        Tuple.of(mapConj.apply(t.component1()), mapTerm.apply(t.component2()))));
      case Unfolding(var really, var unfolded) -> new Unfolding(really, mapTerm.apply(unfolded));
      case Class(var fields) -> new Class(fields.map(t -> Tuple.of(t.component1(), mapTerm.apply(t.component2()))));
      case Sigma sigma -> sigma;
    };
  }

  @Override default @NotNull Doc toDoc() {
    return Distiller.restr(this, Distiller.Prec.Free);
  }

  record Cubical(@NotNull ImmutableSeq<Tuple2<Term.Cofib.Conj, Term>> boundaries) implements Restr {
    public static @NotNull Cubical fromPartial(@NotNull Term.PartEl partial) {
      return new Cubical(partial.elems());
    }
  }
  record Unfolding(@NotNull DefVar<Def.Fn> defVar, @NotNull Term unfolded) implements Restr {}
  record Sigma() implements Restr {}
  record Class(@NotNull ImmutableSeq<Tuple2<String, Term>> fields) implements Restr {}
}
