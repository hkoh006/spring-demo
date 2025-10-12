package org.example.spring.demo.dao

import org.assertj.core.api.Assertions.assertThat
import org.example.spring.demo.dao.model.Allocation
import org.example.spring.demo.dao.model.OrderDetails
import org.example.spring.demo.dao.model.OrderEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
    lateinit var orderJdbcRepository: OrderJdbcRepository

    @Autowired
    lateinit var orderJpaRepository: OrderJpaRepository

    companion object {
        @Container
        val postgres =
            PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
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

    @BeforeEach
    fun setup() {
        orderJpaRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        orderJpaRepository.deleteAll()
    }

    private fun newOrder(
        id: Long,
        vararg allocationIds: String,
    ): OrderEntity =
        OrderEntity(
            id = id,
            orderDetails =
                OrderDetails(
                    allocations = allocationIds.mapIndexed { idx, s -> Allocation(id = s, quantity = idx + 1) },
                ),
        )

    @Test
    fun `persist and read order with jsonb`() {
        val order = newOrder(1L, "a1", "a2")
        orderJpaRepository.save(order)

        val all = orderJdbcRepository.findAll()
        assertThat(all).hasSize(1)
        assertThat(all.first().id).isEqualTo(1L)
        assertThat(
            all
                .first()
                .orderDetails.allocations
                .map { it.id },
        ).containsExactlyInAnyOrder("a1", "a2")

        // Any-of filter
        val anyA1 = orderJdbcRepository.findAllWithIdsMatchingAny(listOf("a1"))
        assertThat(anyA1.map { it.id }).containsExactly(1L)

        val anyA3 = orderJdbcRepository.findAllWithIdsMatchingAny(listOf("a3"))
        assertThat(anyA3).isEmpty()

        // All-of filter
        val allA1A2 = orderJdbcRepository.findAllWithIdsMatchingAll(listOf("a1", "a2"))
        assertThat(allA1A2.map { it.id }).containsExactly(1L)

        val allA1A3 = orderJdbcRepository.findAllWithIdsMatchingAll(listOf("a1", "a3"))
        assertThat(allA1A3).isEmpty()

        // Empty filters should not restrict results
        val anyEmpty = orderJdbcRepository.findAllWithIdsMatchingAny(emptyList())
        val allEmpty = orderJdbcRepository.findAllWithIdsMatchingAll(emptyList())
        assertThat(anyEmpty.map { it.id }).containsExactly(1L)
        assertThat(allEmpty.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `find all with allocation filters across multiple orders`() {
        val o1 = newOrder(1L, "a1", "a2")
        val o2 = newOrder(2L, "b1")
        val o3 = newOrder(3L, "a2", "b1", "c1")
        orderJpaRepository.saveAll(listOf(o1, o2, o3))

        // Any-of queries
        val anyA1 = orderJdbcRepository.findAllWithIdsMatchingAny(listOf("a1"))
        assertThat(anyA1.map { it.id }).containsExactly(1L)

        val anyA1B1 = orderJdbcRepository.findAllWithIdsMatchingAny(listOf("a1", "b1"))
        assertThat(anyA1B1.map { it.id }).containsExactlyInAnyOrder(1L, 2L, 3L)

        val anyZ = orderJdbcRepository.findAllWithIdsMatchingAny(listOf("z"))
        assertThat(anyZ).isEmpty()

        // All-of queries
        val allA1A2 = orderJdbcRepository.findAllWithIdsMatchingAll(listOf("a1", "a2"))
        assertThat(allA1A2.map { it.id }).containsExactly(1L)

        val allB1C1 = orderJdbcRepository.findAllWithIdsMatchingAll(listOf("b1", "c1"))
        assertThat(allB1C1.map { it.id }).containsExactly(3L)

        val allA1B1 = orderJdbcRepository.findAllWithIdsMatchingAll(listOf("a1", "b1"))
        assertThat(allA1B1).isEmpty()

        // Empty filters
        val anyEmpty = orderJdbcRepository.findAllWithIdsMatchingAny(emptyList())
        val allEmpty = orderJdbcRepository.findAllWithIdsMatchingAll(emptyList())
        assertThat(anyEmpty.map { it.id }).containsExactlyInAnyOrder(1L, 2L, 3L)
        assertThat(allEmpty.map { it.id }).containsExactlyInAnyOrder(1L, 2L, 3L)
    }
}
