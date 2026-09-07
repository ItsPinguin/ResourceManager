plugins {
  kotlin("jvm") version "2.4.0"
  id("com.gradleup.shadow") version "8.3.0"
  `maven-publish`
}

group = "fr.itspinguin.resourcemanager"
version = "1.0.0"

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  compileOnly(kotlin("stdlib"))
  implementation("com.google.code.gson:gson:2.14.0")

  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.build {
  dependsOn(tasks.shadowJar)
}

tasks.test {
  useJUnitPlatform()
}

tasks.shadowJar {
  archiveClassifier.set("")
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}