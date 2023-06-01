module aya.anqur.base {
  requires static org.jetbrains.annotations;

  requires transitive aya.pretty;
  requires transitive aya.util;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.aya.lamett.syntax;
  exports org.aya.lamett.tyck;
  exports org.aya.lamett.util;
}
