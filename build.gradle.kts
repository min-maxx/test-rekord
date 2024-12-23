plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
//    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.5")
//    implementation("com.thoughtworks.xstream:xstream:1.4.20")

    implementation("com.google.code.gson:gson:2.10")
    implementation("io.goodforgod:gson-configuration:2.0.0")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}
