package org.example.spring.demo.dao

import com.blazebit.persistence.CriteriaBuilderFactory
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.reflect.Method

/**
 * Unit tests for the `jsonTextArrayLiteral` private helper inside [OrderJdbcRepository].
 *
 * The helper is private, so it is accessed via reflection on a real (not mocked)
 * instance. The constructor dependencies are satisfied with Mockito mocks because
 * the method under test calls neither of them.
 *
 * Format contract:
 *   - Empty list  -> `'[]'`
 *   - Single item -> `'["a"]'`
 *   - Many items  -> `'["a","b","c"]'`
 *   Each value is wrapped in double-quotes; the outer array is wrapped in
 *   single-quotes so it embeds cleanly as a SQL literal.
 *
 * Full query-level verification (WHERE clause correctness against a real
 * PostgreSQL instance) lives in [OrderEntityTest].
 */
@ExtendWith(MockitoExtension::class)
class OrderJdbcRepositoryJsonLiteralTest {
    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var criteriaBuilderFactory: CriteriaBuilderFactory

    /** The reflected private method, made accessible once for the test class. */
    private val jsonTextArrayLiteralMethod: Method by lazy {
        OrderJdbcRepository::class.java
            .getDeclaredMethod("jsonTextArrayLiteral", List::class.java)
            .also { it.isAccessible = true }
    }

    /** Calls the private helper on a real repository instance via reflection. */
    private fun literal(values: List<String>): String {
        val repo = OrderJdbcRepository(entityManager, criteriaBuilderFactory)
        return jsonTextArrayLiteralMethod.invoke(repo, values) as String
    }

    // -------------------------------------------------------------------------
    // Empty list
    // -------------------------------------------------------------------------

    @Nested
    inner class EmptyList {
        @Test
        fun `empty list should produce empty JSON array wrapped in single quotes`() {
            assertThat(literal(emptyList())).isEqualTo("'[]'")
        }
    }

    // -------------------------------------------------------------------------
    // Single element
    // -------------------------------------------------------------------------

    @Nested
    inner class SingleElement {
        @Test
        fun `single element should be double-quoted inside the single-quoted array`() {
            assertThat(literal(listOf("a"))).isEqualTo("'[\"a\"]'")
        }

        @Test
        fun `element value containing hyphens is preserved verbatim`() {
            assertThat(literal(listOf("alloc-123"))).isEqualTo("'[\"alloc-123\"]'")
        }

        @Test
        fun `single element UUID-style value is preserved verbatim`() {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            assertThat(literal(listOf(uuid))).isEqualTo("'[\"$uuid\"]'")
        }
    }

    // -------------------------------------------------------------------------
    // Multiple elements
    // -------------------------------------------------------------------------

    @Nested
    inner class MultipleElements {
        @Test
        fun `two elements should be comma-separated without spaces`() {
            assertThat(literal(listOf("a", "b"))).isEqualTo("'[\"a\",\"b\"]'")
        }

        @Test
        fun `three elements should all appear in insertion order`() {
            assertThat(literal(listOf("x", "y", "z"))).isEqualTo("'[\"x\",\"y\",\"z\"]'")
        }

        @Test
        fun `result should be wrapped in outer single quotes`() {
            val result = literal(listOf("a", "b"))
            assertThat(result).startsWith("'")
            assertThat(result).endsWith("'")
        }

        @Test
        fun `result should not contain a trailing comma before the closing bracket`() {
            val result = literal(listOf("a", "b", "c"))
            assertThat(result).doesNotContain(",]")
        }

        @Test
        fun `five elements are all present in the literal`() {
            val ids = listOf("p", "q", "r", "s", "t")
            val result = literal(ids)
            ids.forEach { id -> assertThat(result).contains("\"$id\"") }
        }
    }
}
