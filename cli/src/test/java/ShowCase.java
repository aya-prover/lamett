import org.aya.lamett.cli.CliMain;
import org.aya.lamett.parse.LamettParserImpl;

public class ShowCase {
  public static void main(String[] args) {
    var ast = new LamettParserImpl(CliMain.newReporter()).parseNode("""
      def id : U => U
      print : U => U
      """);
    System.out.println(ast.toDebugString());
  }
}
