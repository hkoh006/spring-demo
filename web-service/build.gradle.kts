plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ksp)
}

description = "web-service"

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(libs.bundles.spring.starters)

    implementation(libs.hypersistence.utils.hibernate71)
    implementation(libs.bundles.blaze.querydsl)
    implementation(variantOf(libs.querydsl.jpa) { classifier("jakarta") })
    ksp(libs.querydsl.ksp.codegen)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.webmvc.test)

    testRuntimeOnly(libs.postgresql)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
