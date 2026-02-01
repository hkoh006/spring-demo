package org.example.spring.demo

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<SpringDemoApplication>()
        .with(org.example.spring.demo.config.TestcontainersDatabaseConfig::class)
        .run(*args)
}
