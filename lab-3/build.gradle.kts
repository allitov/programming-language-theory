plugins {
    application
    alias(libs.plugins.lombok)
}

group = "io.allitov"
version = "1.0.0"

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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.run {
    mainClass.set("io.allitov.plt.Main")
    standardInput = System.`in`
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}
