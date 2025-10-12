package org.example.spring.demo.dao

import org.assertj.core.api.Assertions
import org.example.spring.demo.dao.model.Allocation
import org.example.spring.demo.dao.model.OrderDetails
import org.example.spring.demo.dao.model.OrderEntity
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class OrderEntityTest() {
    @Autowired
     lateinit var orderJdbcRepository: OrderJdbcRepository

    @Autowired
    lateinit var orderJpaRepository: OrderJpaRepository

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun register(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.PostgreSQLDialect" }
        }
    }

    @Test
    fun `persist and read order with jsonb`() {
        val order = OrderEntity(
            id = 1L,
            orderDetails = OrderDetails(
                allocations = listOf(
                    Allocation(id = "a1", quantity = 3),
                    Allocation(id = "a2", quantity = 5)
                )
            )
        )
        orderJpaRepository.save(order)

        Assertions.assertThat(orderJpaRepository.findAll()).isNotEmpty

        println(orderJdbcRepository.findAll().map { it.orderDetails })

    }
}