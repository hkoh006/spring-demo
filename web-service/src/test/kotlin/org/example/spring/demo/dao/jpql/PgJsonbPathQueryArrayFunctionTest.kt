package org.example.spring.demo.dao.jpql

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure unit tests for [PgJsonbPathQueryArrayFunction].
 *
 * Verifies that the function renders the correct SQL fragment wrapping its
 * two arguments in a `jsonb_path_query_array(arg0, arg1)` call, that metadata
 * methods return the expected values, and that wrong argument counts produce
 * a clear [IllegalArgumentException].
 */
class PgJsonbPathQueryArrayFunctionTest {
    private lateinit var fn: PgJsonbPathQueryArrayFunction

    @BeforeEach
    fun setUp() {
        fn = PgJsonbPathQueryArrayFunction()
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
        fun `getReturnType should echo back the supplied class`() {
            assertThat(fn.getReturnType(String::class.java)).isEqualTo(String::class.java)
        }

        @Test
        fun `getReturnType should return null when null is passed`() {
            assertThat(fn.getReturnType(null)).isNull()
        }
    }

    // -------------------------------------------------------------------------
    // render — happy path
    // -------------------------------------------------------------------------

    @Nested
    inner class Render {
        @Test
        fun `should wrap both arguments in a jsonb_path_query_array call`() {
            val ctx = FakeRenderContext(args = listOf("t.data", "'$.ids[*]'"))

            fn.render(ctx)

            assertThat(ctx.output()).isEqualTo("jsonb_path_query_array(t.data, '$.ids[*]')")
        }

        @Test
        fun `output should start with jsonb_path_query_array(`() {
            val ctx = FakeRenderContext(args = listOf("col", "path"))

            fn.render(ctx)

            assertThat(ctx.output()).startsWith("jsonb_path_query_array(")
        }

        @Test
        fun `output should end with closing parenthesis`() {
            val ctx = FakeRenderContext(args = listOf("col", "path"))

            fn.render(ctx)

            assertThat(ctx.output()).endsWith(")")
        }

        @Test
        fun `first argument should appear before the comma separator`() {
            val ctx = FakeRenderContext(args = listOf("FIRST_ARG", "SECOND_ARG"))

            fn.render(ctx)

            val out = ctx.output()
            assertThat(out.indexOf("FIRST_ARG")).isLessThan(out.indexOf(", SECOND_ARG"))
        }

        @Test
        fun `second argument should appear after the comma separator`() {
            val ctx = FakeRenderContext(args = listOf("FIRST_ARG", "SECOND_ARG"))

            fn.render(ctx)

            val out = ctx.output()
            assertThat(out.indexOf("SECOND_ARG")).isGreaterThan(out.indexOf("FIRST_ARG, "))
        }

        @Test
        fun `arguments should be separated by a comma and space`() {
            val ctx = FakeRenderContext(args = listOf("a", "b"))

            fn.render(ctx)

            assertThat(ctx.output()).contains(", ")
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
