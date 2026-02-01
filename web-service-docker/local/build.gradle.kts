plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.testcontainers)
    implementation(libs.testcontainers.postgresql)

    implementation(project(":web-service"))

    // OpenAPI + Scalar UI (via springdoc)
    implementation(libs.springdoc.openapi.starter.webmvc.scalar)

    runtimeOnly(libs.postgresql)
}
