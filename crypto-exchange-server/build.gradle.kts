plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.protobuf)
    id("org.openapi.generator") version "7.20.0"
}

description = "crypto-exchange-server"

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.springdoc.openapi.starter.webmvc.scalar)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    implementation(libs.confluent.kafka.protobuf.serializer)

    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.spring.kafka.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

protobuf {
    protoc {
        artifact =
            libs.protobuf.protoc
                .get()
                .toString()
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin {
            srcDir(tasks.openApiGenerate.map { it.outputDir.get() + "/src/main/kotlin" })
            exclude("org/openapitools/client/**")
        }
    }
}

openApiValidate {
    inputSpec.set(
        layout.projectDirectory
            .file("src/main/resources/crypto-exchange-openapi.yaml")
            .asFile.absolutePath,
    )
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set(
        layout.projectDirectory
            .file("src/main/resources/crypto-exchange-openapi.yaml")
            .asFile.absolutePath,
    )
    outputDir.set(layout.buildDirectory.dir("generated/openapi").map { it.asFile.absolutePath })
    apiPackage.set("org.example.crypto.exchange.api")
    modelPackage.set("org.example.crypto.exchange.model")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "UPPERCASE",
            "serializationLibrary" to "jackson",
            "collectionType" to "list",
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            "documentationProvider" to "none",
        ),
    )
}
