package org.example.spring.demo.dao

import com.blazebit.persistence.spi.FunctionRenderContext
import com.blazebit.persistence.spi.JpqlFunction

/**
 * Blaze-Persistence function renderer for PostgreSQL jsonb contains operator @>.
 * Usage in JPQL/Criteria: jsonb_contains(left, rightJson)
 * Renders to: (left @> cast(rightJson as jsonb))
 */
class PgJsonbContainsFunction : JpqlFunction {
    override fun hasArguments(): Boolean {
        return true
    }

    override fun hasParenthesesIfNoArguments(): Boolean {
        return true
    }

    override fun getReturnType(p0: Class<*>?): Class<*>? {
        return p0
    }

    override fun render(context: FunctionRenderContext) {
        if (context.getArgumentsSize() != 2) {
            throw IllegalArgumentException("jsonb_contains expects exactly 2 arguments")
        }
        context.addChunk("(")
        context.addArgument(0)
        context.addChunk(" @> ")
        // Ensure right-hand side is treated as jsonb
        context.addChunk("cast(")
        context.addArgument(1)
        context.addChunk(" as jsonb)")
        context.addChunk(")")
    }
}
