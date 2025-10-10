package org.example.demowithnativeimage

import org.assertj.core.api.Assertions
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
class OrderEntityTest {

    @Autowired
    lateinit var orderRepository: OrderRepository

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
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.PostgreSQLDialect" }
        }
    }

    @Test
    fun persistAndReadOrderWithJsonb() {
        val order = OrderEntity(
            id = 1L,
            orderDetails = OrderDetails(
                allocations = listOf(
                    Allocation(id = "a1", quantity = 3),
                    Allocation(id = "a2", quantity = 5)
                )
            )
        )
        orderRepository.save(order)

        Assertions.assertThat(orderRepository.findAll()).isNotEmpty

        println(orderRepository.findAll().map { it.orderDetails })
    }
}