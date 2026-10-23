import org.gradle.api.tasks.JavaExec

plugins {
    application
    alias(libs.plugins.lombok)
}

group = "io.allitov"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(libs.log4j.slf4j)

    testImplementation(libs.assertj)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

application {
    mainClass.set("io.allitov.plt.Main")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}
