import java.util.*

plugins {
  java
  groovy
}

repositories { mavenCentral() }

dependencies {
  val deps = Properties()
  deps.load(rootDir.resolve("../gradle/deps.properties").reader())
  api("org.aya-prover.upstream", "build-util", deps.getProperty("version.build-util"))
  api("org.aya-prover.upstream", "build-util-jflex", deps.getProperty("version.build-util"))
}
