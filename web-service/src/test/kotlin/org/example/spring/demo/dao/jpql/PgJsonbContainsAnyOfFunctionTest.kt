package org.example.spring.demo.dao.jpql

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pure unit tests for [PgJsonbContainsAnyOfFunction].
 *
 * The key behaviour under test is the quote-transformation applied to the second
 * argument.  The argument arrives from the query builder as a JSON array literal
 * wrapped in single-quotes and using double-quoted strings, e.g.:
 *
 *   `'["a","b"]'`
 *
 * The render method must produce SQL suitable for the PostgreSQL `??|` operator:
 *
 *   `col ??| array ['a','b']`
 *
 * Transformation steps applied to the second argument:
 *   1. `.replace("\"", "'")` — swap double-quotes for single-quotes
 *   2. `.removePrefix("'")` — strip the leading single-quote wrapper
 *   3. `.removeSuffix("'")` — strip the trailing single-quote wrapper
 */
class PgJsonbContainsAnyOfFunctionTest {
    private lateinit var fn: PgJsonbContainsAnyOfFunction

    @BeforeEach
    fun setUp() {
        fn = PgJsonbContainsAnyOfFunction()
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
        fun `should emit left operand then ??| array then transformed right operand`() {
            // Second arg: single-quote-wrapped JSON with double-quoted values
            // After transform: outer single-quotes stripped, inner double-quotes → single-quotes
            val ctx = FakeRenderContext(args = listOf("expr", "'[\"a\",\"b\"]'"))

            fn.render(ctx)

            // Expected: expr ??| array ['a','b']
            assertThat(ctx.output()).isEqualTo("expr ??| array ['a','b']")
        }

        @Test
        fun `should place ??| array between the two arguments`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"a\"]'"))

            fn.render(ctx)

            assertThat(ctx.output()).contains(" ??| array ")
        }

        @Test
        fun `left argument should appear before ??| array`() {
            val ctx = FakeRenderContext(args = listOf("MY_COL", "'[\"v\"]'"))

            fn.render(ctx)

            val out = ctx.output()
            assertThat(out.indexOf("MY_COL")).isLessThan(out.indexOf(" ??| array "))
        }

        @Test
        fun `double-quotes in the array literal should be replaced by single-quotes`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"x\"]'"))

            fn.render(ctx)

            // The rendered chunk for the array must use single-quotes, not double-quotes
            val arrayPart = ctx.output().substringAfter(" ??| array ")
            assertThat(arrayPart).contains("'x'")
            assertThat(arrayPart).doesNotContain("\"x\"")
        }

        @Test
        fun `outer single-quote wrapper from the array argument should be stripped`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"a\"]'"))

            fn.render(ctx)

            // The array part must not start with a bare single-quote
            val arrayPart = ctx.output().substringAfter(" ??| array ")
            assertThat(arrayPart).startsWith("[")
        }

        @Test
        fun `single element array is rendered correctly`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"only\"]'"))

            fn.render(ctx)

            assertThat(ctx.output()).isEqualTo("col ??| array ['only']")
        }

        @Test
        fun `three element array is rendered with all elements`() {
            val ctx = FakeRenderContext(args = listOf("col", "'[\"a\",\"b\",\"c\"]'"))

            fn.render(ctx)

            assertThat(ctx.output()).isEqualTo("col ??| array ['a','b','c']")
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
