package org.example.spring.demo.dao.jpql

import com.blazebit.persistence.spi.FunctionRenderContext
import com.blazebit.persistence.spi.JpqlFunction

/**
 * Blaze-Persistence function renderer for PostgreSQL function jsonb_path_query_array(jsonb, jsonpath).
 * Usage in JPQL/Criteria: jsonb_path_query_array(targetJsonb, jsonPath)
 * Renders to: jsonb_path_query_array(targetJsonb, jsonPath)
 */
class PgJsonbPathQueryArrayFunction : JpqlFunction {
    override fun hasArguments(): Boolean = true

    override fun hasParenthesesIfNoArguments(): Boolean = true

    override fun getReturnType(p0: Class<*>?): Class<*>? = p0

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
