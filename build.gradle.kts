import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
}

group = "st.orm.demo"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    // mavenLocal first so locally built Storm versions (not yet on Central)
    // resolve during development against the framework.
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform("st.orm:storm-bom:1.12.0"))
    annotationProcessor(platform("st.orm:storm-bom:1.12.0"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    implementation("st.orm:storm-spring-boot-starter")
    implementation("st.orm:storm-java21")
    implementation("st.orm:storm-jackson3")
    runtimeOnly("st.orm:storm-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    annotationProcessor("st.orm:storm-metamodel-processor")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("st.orm:storm-test")
    testRuntimeOnly("st.orm:storm-h2")
    testRuntimeOnly("com.h2database:h2:2.3.232")
    testImplementation("com.microsoft.playwright:playwright:1.61.0")
}

// Storm's Java API is built on JDK String Templates (JEP 430), a Java 21
// preview feature that was removed after JDK 22 — so the toolchain is pinned
// to Java 21 above and preview features are enabled for every compile and run.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

tasks.withType<BootRun>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

// Playwright interface tests run against the live application: start the
// app (./gradlew bootRun), then run ./gradlew e2eTest.
tasks.register<Test>("e2eTest") {
    description = "Runs Playwright interface tests against the running application."
    group = "verification"
    useJUnitPlatform {
        includeTags("e2e")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    systemProperty("app.baseUrl", System.getProperty("app.baseUrl") ?: "http://localhost:8080")
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("installPlaywrightBrowsers") {
    description = "Downloads the Chromium browser used by the Playwright tests."
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args("install", "chromium")
}
