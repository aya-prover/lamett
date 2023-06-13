import kala.collection.mutable.MutableMap;
import org.aya.lamett.cli.CliMain;
import org.aya.lamett.parse.LamettParserImpl;
import org.aya.lamett.syntax.Expr;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.tyck.Resolver;
import org.aya.util.error.Global;
import org.aya.util.error.SourcePos;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExprsTest {
  @BeforeAll public static void init() {
    Global.NO_RANDOM_NAME = true;
  }
  @Test public void tyckUncurry() {
    var artifact = tyck("fn A B C t f => f (t.1) (t.2)",
      "Fn (A B C : U) -> Fn (t : Sig A ** B) -> Fn (f : A -> B -> C) -> C");
    assertNotNull(artifact);
  }

  @Test public void resolveId() {
    var e = (Expr.Lam) resolve("fn x => x");
    assertNotNull(e);
    assertSame(((Expr.Resolved) e.a()).ref(), e.x());
  }

  @Test public void distill() {
    assertEquals("a b c", distill("a b c"));
    assertEquals("a b c", distill("(a b) c"));
    assertEquals("a (b c)", distill("a (b c)"));
    assertEquals("a b c", distill("a (b) (c)"));
    assertEquals("a b.1 c.1", distill("a b.1 c.1"));
    assertEquals("a.1.2.1.2", distill("a.1.2.1.2"));
    assertEquals("fn a => b", distill("fn a => b"));
    assertEquals("fn a => _", distill("fn a"));
    assertEquals("Fn (_ : a b) -> c d", distill("a b -> c d"));
    assertEquals("Fn (_ : a b) -> Fn (_ : c d) -> e f", distill("a b -> c d -> e f"));
    assertEquals("Fn (_ : a b) -> Fn (_ : c d) -> e f", distill("a b -> (c d -> e f)"));
    assertEquals("Fn (_ : Fn (_ : a b) -> c d) -> e f", distill("(a b -> c d) -> e f"));
    assertEquals("(i = Zero ∧ ¬ j = One) ∨ k = ¬ k", distill("i = 0 ∧ ¬ j = 1 ∨ k = ¬ k"));
  }

  @Test public void parseFail() {
    assertThrows(RuntimeException.class, () -> parse("\\"));
  }

  private static @NotNull String distill(@Language("TEXT") String s) {
    return parse(s).toDoc().debugRender();
  }

  @Test public void tyckId() {
    var artifact = tyck("fn A x => x", "Fn (A : U) -> A -> A");
    assertNotNull(artifact);
  }

  @Test public void cubicalCof() {
    var artifact = tyck("∀ i => i = ¬ i", "F");
    assertNotNull(artifact);

    assertEquals("∀ i => i = ¬ i ∧ i = j", distill("∀ i => i = ¬ i ∧ i = j"));
    assertEquals("(∀ i => i = ¬ i) ∧ i = j", distill("(∀ i => i = ¬ i) ∧ i = j"));
    assertEquals("(i = Zero ∧ ¬ j = One) ∨ k = ¬ k", distill("i = Zero ∧ ¬ j = One ∨ k = ¬ k"));
  }

  private static @NotNull Term tyck(@Language("TEXT") String term, @Language("TEXT") String type) {
    var akJr = CliMain.andrasKovacs();
    var Id = akJr.synth(resolve(type));
    return akJr.inherit(resolve(term), Id.wellTyped());
  }

  private static @NotNull Expr resolve(String s) {
    return new Resolver(MutableMap.create()).expr(parse(s));
  }

  private static @NotNull Expr parse(String s) {
    var reporter = CliMain.newReporter();
    return new LamettParserImpl(reporter).expr(s, SourcePos.NONE);
  }
}
