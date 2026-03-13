package org.example.spring.demo.dao.jpql

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure unit tests for [PgJsonbContainsFunction].
 *
 * Verifies the SQL fragment rendered for the PostgreSQL jsonb `@>` operator,
 * error handling when the wrong number of arguments is supplied, and the
 * metadata methods that Blaze-Persistence queries at configuration time.
 */
class PgJsonbContainsFunctionTest {
    private lateinit var fn: PgJsonbContainsFunction

    @BeforeEach
    fun setUp() {
        fn = PgJsonbContainsFunction()
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Nested
    inner class Metadata {
        @Test
        fun `hasArguments should return true`() {
            assertThat(fn.hasArguments()).isTrue()
        }

        @Test
        fun `hasParenthesesIfNoArguments should return true`() {
            assertThat(fn.hasParenthesesIfNoArguments()).isTrue()
        }

        @Test
        fun `getReturnType should return Boolean`() {
            assertThat(fn.getReturnType(Any::class.java)).isEqualTo(Boolean::class.java)
        }

        @Test
        fun `getReturnType should return Boolean even when null class is passed`() {
            assertThat(fn.getReturnType(null)).isEqualTo(Boolean::class.java)
        }
    }

    // -------------------------------------------------------------------------
    // render — happy path
    // -------------------------------------------------------------------------

    @Nested
    inner class Render {
        @Test
        fun `should emit left operand then ampersand right carrot operator then right operand`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"a\"]'"))

            fn.render(ctx)

            // Expected output: col @> '[\"a\"]'
            assertThat(ctx.output()).isEqualTo("col @> '[\"a\"]'")
        }

        @Test
        fun `should place ampersand right carrot between the two arguments`() {
            val ctx = FakeRenderContext(args = listOf("left_expr", "right_expr"))

            fn.render(ctx)

            assertThat(ctx.output()).contains(" @> ")
        }

        @Test
        fun `left argument should appear before ampersand right carrot`() {
            val ctx = FakeRenderContext(args = listOf("LEFT", "RIGHT"))

            fn.render(ctx)

            val out = ctx.output()
            assertThat(out.indexOf("LEFT")).isLessThan(out.indexOf(" @> "))
        }

        @Test
        fun `right argument should appear after ampersand right carrot`() {
            val ctx = FakeRenderContext(args = listOf("LEFT", "RIGHT"))

            fn.render(ctx)

            val out = ctx.output()
            assertThat(out.indexOf("RIGHT")).isGreaterThan(out.indexOf(" @> "))
        }
    }

    // -------------------------------------------------------------------------
    // render — error cases
    // -------------------------------------------------------------------------

    @Nested
    inner class RenderErrors {
        @Test
        fun `should throw IllegalArgumentException when zero arguments are supplied`() {
            val ctx = FakeRenderContext(args = emptyList())

            assertThatThrownBy { fn.render(ctx) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("2")
        }

        @Test
        fun `should throw IllegalArgumentException when only one argument is supplied`() {
            val ctx = FakeRenderContext(args = listOf("only_one"))

            assertThatThrownBy { fn.render(ctx) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should throw IllegalArgumentException when three arguments are supplied`() {
            val ctx = FakeRenderContext(args = listOf("a", "b", "c"))

            assertThatThrownBy { fn.render(ctx) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
