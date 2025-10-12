import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("com.github.ben-manes.versions") version "0.53.0"
}

group = "org.example.spring.demo"
version = "0.0.1-SNAPSHOT"
description = "spring-demo"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // spring bom
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.6"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Blaze-Persistence + QueryDSL (Jakarta)
    val blazeVersion = "1.6.17"
    val querydslVersion = "5.1.0"
    implementation("com.blazebit:blaze-persistence-core-api-jakarta:$blazeVersion")
    implementation("com.blazebit:blaze-persistence-core-impl-jakarta:$blazeVersion")
    implementation("com.blazebit:blaze-persistence-integration-hibernate-6.2:$blazeVersion")
    implementation("com.blazebit:blaze-persistence-integration-querydsl-expressions-jakarta:$blazeVersion")

    implementation("com.querydsl:querydsl-jpa:$querydslVersion:jakarta")
    ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:7.0")

    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/build/**")
    }
}

// Dependency Updates configuration (stable releases only)
// Run: ./gradlew dependencyUpdates --refresh-dependencies
tasks.withType<DependencyUpdatesTask> {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
        val regex = "^[0-9,.v-]+$".toRegex()
        return !(stableKeyword || regex.matches(version))
    }
    rejectVersionIf {
        isNonStable(candidate.version)
    }
    checkConstraints = true
    gradleReleaseChannel = "current"
}
