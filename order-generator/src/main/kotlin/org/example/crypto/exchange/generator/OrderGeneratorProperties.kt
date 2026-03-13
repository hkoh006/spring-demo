package org.example.crypto.exchange.generator

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

@ConfigurationProperties(prefix = "order-generator")
data class OrderGeneratorProperties(
    val enabled: Boolean = true,
    val intervalMs: Long = 500,
    val users: List<String> = listOf("alice", "bob", "charlie"),
    val minPrice: BigDecimal = BigDecimal("90.00"),
    val maxPrice: BigDecimal = BigDecimal("110.00"),
    val minQuantity: BigDecimal = BigDecimal("1.00"),
    val maxQuantity: BigDecimal = BigDecimal("10.00"),
    val topic: String = "orders",
)
