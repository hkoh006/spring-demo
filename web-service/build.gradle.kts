import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.benmanes.versions)
}

description = "web-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


val versionCatalog = the<VersionCatalogsExtension>().named("libs")
val querydslVersion = versionCatalog.findVersion("querydsl").get().requiredVersion

dependencies {
    // Spring BOM (manages versions for many dependencies)
    implementation(platform(libs.spring.boot.dependencies))

    // Bundled Spring starters and common Kotlin/Jackson
    implementation(libs.bundles.spring.starters)

    // Spring Boot Testcontainers support (for runtime DB via containers)
    implementation(libs.spring.boot.testcontainers)

    // Hypersistence utilities
    implementation(libs.hypersistence.utils.hibernate63)

    // Blaze-Persistence + QueryDSL (Jakarta)
    implementation(libs.bundles.blaze.querydsl)
    implementation(variantOf(libs.querydsl.jpa) { classifier("jakarta") })
    ksp(libs.querydsl.ksp.codegen)

    runtimeOnly(libs.postgresql)
    // Testcontainers PostgreSQL at runtime (so app can start without local DB)
    implementation(libs.testcontainers.postgresql)

    // Testing bundles
    testImplementation(libs.bundles.testing.core)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.bundles.testcontainers)

    // OpenAPI + Scalar UI (via springdoc)
    implementation(libs.springdoc.openapi.starter.webmvc.scalar)
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
        // Exclude generated code and QueryDSL Q-classes
        exclude("**/build/**")
        exclude("**/Q*.kt")
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
