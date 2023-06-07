package org.aya.lamett.cli;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.lamett.parse.LamettParserImpl;
import org.aya.lamett.prelude.GeneratedVersion;
import org.aya.lamett.syntax.Decl;
import org.aya.lamett.syntax.Def;
import org.aya.lamett.tyck.Elaborator;
import org.aya.lamett.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.aya.util.reporter.Reporter;
import org.aya.util.reporter.ThrowingReporter;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "anqur",
  mixinStandardHelpOptions = true,
  version = "Anqur v" + GeneratedVersion.VERSION_STRING,
  showDefaultValues = true)
public class CliMain implements Callable<Integer> {
  @CommandLine.Parameters(paramLabel = "<input-file>", description = "File to compile")
  public String inputFile;
  @CommandLine.Option(names = {"--st"}, description = "Show the stack trace")
  public boolean stackTrace = false;

  public static void main(String @NotNull ... args) {
    System.exit(new CommandLine(new CliMain()).execute(args));
  }

  @Override public Integer call() throws Exception {
    if (inputFile == null) {
      System.err.println("Lamett " + GeneratedVersion.COMMIT_HASH);
      System.err.println("Use -h for help");
      return 1;
    }
    try {
      var ak = tyck(Files.readString(Paths.get(inputFile)), true, newReporter());
      System.out.println("Tycked " + ak.sigma().size() + " definitions, phew.");
      return 0;
    } catch (RuntimeException re) {
      System.err.println(re.getMessage());
      if (stackTrace) re.printStackTrace();
      return 2;
    }
  }

  public static @NotNull Reporter newReporter() {
    return new ThrowingReporter(new DebugPrettierOptions());
  }

  public static @NotNull Elaborator andrasKovacs() {
    return new Elaborator(MutableMap.create(), MutableMap.create());
  }

  public static @NotNull ImmutableSeq<Decl> def(String s, @NotNull Reporter reporter) {
    var program = new LamettParserImpl(reporter).program(new SourceFile("Mian", Path.of("/home/senpai/114514.lamett"), s));
    var resolver = new Resolver(MutableMap.create());
    return program.map(resolver::def);
  }

  public static @NotNull Elaborator tyck(String code, boolean verbose, @NotNull Reporter reporter) {
    var artifact = def(code, reporter);
    var akJr = andrasKovacs();
    for (var def : artifact) {
      var tycked = akJr.def(def);
      if (tycked instanceof Def.Print print) {
        System.out.println(print.body().toDoc().commonRender());
      } else {
        akJr.sigma().put(tycked.name(), tycked);
        if (verbose) System.out.println(tycked.name().name);
      }
    }
    return akJr;
  }
}
