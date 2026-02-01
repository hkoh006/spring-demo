plugins {
    alias(libs.plugins.kotlin.jvm)
}

description = "crypto-exchange-server"

dependencies {
    implementation(libs.kotlin.reflect)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
