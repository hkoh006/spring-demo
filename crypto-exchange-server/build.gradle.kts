plugins {
    alias(libs.plugins.kotlin.jvm)
}

description = "crypto-exchange-server"

dependencies {
    implementation(libs.kotlin.reflect)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
