package org.example.crypto.exchange.generator

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class GeneratorStatusDto(val paused: Boolean)

@RestController
@RequestMapping("/api/generator")
class OrderGeneratorController(
    private val service: OrderGeneratorService,
) {
    @GetMapping("/status")
    fun status() = GeneratorStatusDto(service.paused.get())

    @PostMapping("/pause")
    fun pause(): GeneratorStatusDto {
        service.paused.set(true)
        return GeneratorStatusDto(true)
    }

    @PostMapping("/resume")
    fun resume(): GeneratorStatusDto {
        service.paused.set(false)
        return GeneratorStatusDto(false)
    }
}
