package org.example.spring.demo.dao.jpql

import com.blazebit.persistence.spi.FunctionRenderContext
import com.blazebit.persistence.spi.JpqlFunction

/**
 * Blaze-Persistence function renderer for PostgreSQL jsonb contains operator `@>`.
 * Usage in JPQL/Criteria: jsonb_contains(left, rightJson)
 * Renders to: left @> rightJson
 */
class PgJsonbContainsFunction : JpqlFunction {
    override fun hasArguments(): Boolean = true

    override fun hasParenthesesIfNoArguments(): Boolean = true

    override fun getReturnType(p0: Class<*>?): Class<*> = Boolean::class.java

    override fun render(context: FunctionRenderContext) {
        if (context.argumentsSize != 2) {
            throw IllegalArgumentException("jsonb_contains expects exactly 2 arguments")
        }
        context.addArgument(0)
        context.addChunk(" @> ")
        context.addArgument(1)
    }
}
