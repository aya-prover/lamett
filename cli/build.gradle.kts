plugins { application }
application.mainClass.set("org.aya.lamett.cli.CliMain")

dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("info.picocli", "picocli", version = deps.getProperty("version.picocli"))
  implementation(project(":base"))
  implementation("org.aya-prover.upstream", "ij-parsing-core", version = deps.getProperty("version.build-util"))
  // implementation("org.aya-prover", "tools-repl", version = deps.getProperty("version.aya"))
}

val genDir = file("src/main/gen")
sourceSets["main"].java.srcDir(genDir)
idea.module {
  sourceDirs.add(genDir)
}

val lexer = tasks.register<JFlexTask>("lexer") {
  outputDir = genDir.resolve("org/aya/lamett/parser")
  val grammar = file("src/main/grammar")
  jflex = grammar.resolve("AyaPsiLexer.flex")
  skel = grammar.resolve("aya-jflex.skeleton")
}

val genVer = tasks.register<GenerateVersionTask>("genVer") {
  basePackage = "org.aya.lamett"
  outputDir = file(genDir).resolve("org/aya/lamett/prelude")
}
listOf(tasks.sourcesJar, tasks.compileJava).forEach { it.configure { dependsOn(genVer) } }
