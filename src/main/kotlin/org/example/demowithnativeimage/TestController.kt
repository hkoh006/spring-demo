package org.example.demowithnativeimage

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

    @GetMapping("/api/v1/ping")
    fun ping() = "pong"
}
