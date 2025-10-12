package org.example.spring.demo.dao

import com.blazebit.persistence.spi.FunctionRenderContext
import com.blazebit.persistence.spi.JpqlFunction

/**
 * Blaze-Persistence function renderer for PostgreSQL jsonb contains operator @>.
 * Usage in JPQL/Criteria: jsonb_contains(left, rightJson)
 * Renders to: (left @> cast(rightJson as jsonb))
 */
class PgJsonbPathQueryArrayFunction : JpqlFunction {
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
        if (context.argumentsSize != 2) {
            throw IllegalArgumentException("jsonb_path_query_array expects exactly 2 arguments")
        }
        context.addChunk("jsonb_path_query_array(")
        context.addArgument(0)
        context.addChunk(", ")
        context.addArgument(1)
        context.addChunk(")")
    }
}
