import kala.collection.immutable.ImmutableSeq;
import org.aya.lamett.syntax.Cofib;
import org.aya.lamett.syntax.Term;
import org.aya.lamett.tyck.Normalizer;
import org.aya.lamett.tyck.Unification;
import org.aya.lamett.util.LocalVar;
import org.junit.jupiter.api.Test;

public class TermTest {
  @Test
  public void waitForice1000sGrammar() {
    var unification = new Unification();
    var nf = new Normalizer(unification);

    var phi = new LocalVar("φ");
    var A = new LocalVar("A");        // I → Partial (i = r ∨ φ) U
    var u0 = new LocalVar("u0");      // A r
    var refPhi = new Term.Ref(phi);
    var refA = new Term.Ref(A);
    var refU0 = new Term.Ref(u0);

    unification.addNFConj(new Cofib.Conj(ImmutableSeq.of(refPhi)));

    // coe (r ~> s) (λ i. A i) u0
    var halfCoe = new Term.Coe(Term.Lit.Zero, Term.Lit.One, Term.mkLam("i", i -> new Term.App(refA, new Term.Ref(i))));
    var ceiling = new Term.App(halfCoe, refU0);
    var box = new Term.Box(Term.Lit.Zero, Term.Lit.One, refPhi, ceiling, refU0);
    var cap = new Term.Cap(Term.Lit.Zero, Term.Lit.One, refPhi, box);

    var result = nf.term(cap);
    var box2 = new Term.Box(Term.Lit.Zero, Term.Lit.One, refPhi, ceiling, cap);
    var result2 = nf.term(box2);
  }
}
