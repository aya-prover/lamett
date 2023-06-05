module aya.lamett.cli {
  requires static org.jetbrains.annotations;
  requires aya.lamett.base;
  requires info.picocli;
  requires transitive aya.ij.parsing.core;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive aya.pretty;
  requires transitive aya.util;

  exports org.aya.lamett.cli;
  exports org.aya.lamett.parser;
}
