package org.example.spring.demo.dao.jpql

import com.blazebit.persistence.spi.FunctionRenderContext
import com.blazebit.persistence.spi.JpqlFunction

/**
 * Blaze-Persistence function renderer for PostgreSQL jsonb path any-of operator `??|` with array.
 * Usage in JPQL/Criteria: jsonb_contains_any_of(leftJsonPathResult, rightTextArrayLiteral)
 * Renders to: left ??| array ['a','b']
 */
class PgJsonbContainsAnyOfFunction : JpqlFunction {
    override fun hasArguments(): Boolean = true

    override fun hasParenthesesIfNoArguments(): Boolean = true

    override fun getReturnType(p0: Class<*>?): Class<*> = Boolean::class.java

    override fun render(context: FunctionRenderContext) {
        if (context.argumentsSize != 2) {
            throw IllegalArgumentException("jsonb_contains_any_of expects exactly 2 arguments")
        }
        context.addArgument(0)
        context.addChunk(" ??| array ")
        context.addChunk(
            context
                .getArgument(1)
                .replace("\"", "'")
                .removePrefix("'")
                .removeSuffix("'"),
        )
    }
}
