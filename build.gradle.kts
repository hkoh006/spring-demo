plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
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
    ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:6.12")

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
