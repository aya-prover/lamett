dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.aya-prover", "tools", version = deps.getProperty("version.aya"))
  testImplementation("org.junit.jupiter", "junit-jupiter", version = deps.getProperty("version.junit"))
  testImplementation("org.hamcrest", "hamcrest", version = deps.getProperty("version.hamcrest"))
  testImplementation(project(":cli"))
}

