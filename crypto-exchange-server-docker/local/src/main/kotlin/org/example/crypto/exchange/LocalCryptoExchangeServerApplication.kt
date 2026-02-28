package org.example.crypto.exchange

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LocalCryptoExchangeServerApplication

fun main(args: Array<String>) {
    runApplication<LocalCryptoExchangeServerApplication>(*args)
}
