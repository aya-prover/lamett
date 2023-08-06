import org.aya.lamett.cli.CliMain;
import org.aya.lamett.syntax.Def;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.tyck.Elaborator;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeclsTest {
  @Test public void dontSayLazy() {
    var akJr = tyck("""
      def uncurry (A B C : U)
        (t : Sig A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)
      def uncurry' (A : U) (t : Sig A ** A)
       (f : A -> A -> A) : A => uncurry A A A t f
      """);
    akJr.sigma().valuesView().forEach(tycked -> {
      var body = ((Def.Fn) tycked).body();
      assertTrue(akJr.normalize(body.getLeftValue()) instanceof Term.App $);
    });
  }

  @Test public void leibniz() {
    tyck("""
      def Eq (A : U) (a b : A) : U => Fn (P : A -> U) -> P a -> P b
      def refl (A : U) (a : A) : Eq A a a => fn P pa => pa
      def sym (A : U) (a b : A) (e : Eq A a b) : Eq A b a =>
          e (fn b => Eq A b a) (refl A a)
      """);
  }

  @Test public void unitNatList() {
    tyck("""
      data Unit | unit
      def unnit : Unit => unit
      data Nat
      | zero
      | succ (n : Nat)

      def two : Nat => succ (succ zero)
      print : Nat => two

      data List (A : U)
      | nil
      | cons (x : A) (xs : List A)

      def lengthTwo (A : U) (a : A) : List A => cons A a (cons A a (nil A))
      print : List Nat => lengthTwo Nat two
      """);
  }

  @Test public void patternMatchingNat() {
    tyck("""
      data Nat
      | zero
      | succ (n : Nat)

      def plus (a : Nat) (b : Nat) : Nat
      | zero, b => b
      | succ a, b => succ (plus a b)

      def two : Nat => succ (succ zero)
      def four : Nat => plus two two
      def six : Nat => plus four two
      print : Nat => six
      """);
  }

  @Test public void cofibTest() {
    tyck("""
        def f (i j : I) (φ : F) : F => (i = 0) ∧ (j = 1) ∨ φ
      """);
  }

  @Test public void partialTest() {
    tyck("""
        data Nat
        | zero
        | succ (n : Nat)

        def plus (a : Nat) (b : Nat) : Nat
        | zero, b => b
        | succ a, b => succ (plus a b)
        def one : Nat => succ zero
        def two : Nat => succ (succ zero)
        
        def partial (i j : I) (φ : F) : Partial (φ ∨ i = 1) Nat => {| φ := plus one one | (i = 1) := two |}
      """);
    tyck("""
      def test (i j : I) (f : F) : F => (i = 0) ∧ (j = 1) ∨ f
      def test2 (i j : I) : U => Partial (test i j (i = 0)) U
      """);
  }

  @Test public void cubicalSubtype() {
    tyck("""
      def test1 (A : U) (φ : F) (a : A) : A
        => outS A φ {| φ := a |} (inS A φ a)
      """);
  }

  @Test public void whatTypeAmI() {
    tyck("""
      def typeOfMe (A : U) (φ : F) (of : A) : Sub A φ {| φ := of |}
        => inS A φ of
      """);
  }

  @Test public void coverageNat() {
    assertThrowsExactly(RuntimeException.class, () -> tyck("""
      data Nat
      | zero
      | succ (n : Nat)
      def plus-bad (a : Nat) (b : Nat) : Nat
      | succ a, b => succ (plus-bad a b)
      """));
  }

  @Test public void ext() {
    tyck("""
      def foo (A : U) (a : A) : ( [| i |] A { i = 0 := a | i = 1 := a } ) => (fn i => inS A ((i = 0) ∨ (i = 1)) a)
      """);
  }

  @Test public void coe() {
    tyck("""
      def foo (A : I -> U) (a : A 0) : [| i |] A 0 { i = 0 := coe 0 0 A a | i = 1 := a } => (fn i => inS (A 0) ((i = 0) ∨ (i = 1)) a)
      """);
  }

  private static @NotNull Elaborator tyck(@Language("TEXT") String s) {
    return CliMain.tyck(s, false, CliMain.newReporter());
  }
}
