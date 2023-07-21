import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DistillerTest {
  @Test
  public void ext() {
    assertEquals("", parseAndPretty("""
      [| i j |] A j i { i = 0 := j | j = 0 := i }
      """));
  }

  public static @NotNull String parseAndPretty(@NotNull @Language("TEXT") String code) {
    return ExprsTest.parse(code).toDoc().debugRender();
  }
}
