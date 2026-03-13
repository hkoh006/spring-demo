package org.example.spring.demo.dao.jpql

import com.blazebit.persistence.spi.FunctionRenderContext

/**
 * Minimal in-memory [FunctionRenderContext] used by the JPQL-function unit tests.
 *
 * [addArgument] appends the pre-configured argument string at the given index.
 * [addChunk] appends a literal SQL fragment.
 * [output] returns the full rendered string for assertion.
 *
 * Only the methods exercised by the three PgJsonb* functions are implemented;
 * all other interface methods throw [UnsupportedOperationException].
 */
class FakeRenderContext(
    private val args: List<String>,
) : FunctionRenderContext {
    private val builder = StringBuilder()

    /** Returns the SQL string assembled so far. */
    fun output(): String = builder.toString()

    override fun getArgumentsSize(): Int = args.size

    override fun getArgument(index: Int): String = args[index]

    override fun addArgument(index: Int) {
        builder.append(args[index])
    }

    override fun addChunk(chunk: String) {
        builder.append(chunk)
    }
}
