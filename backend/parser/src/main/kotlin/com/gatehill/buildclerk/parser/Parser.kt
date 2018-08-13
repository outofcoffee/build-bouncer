package com.gatehill.buildclerk.parser

import com.gatehill.buildclerk.api.model.Analysis
import com.gatehill.buildclerk.dsl.AbstractBlock
import com.gatehill.buildclerk.dsl.ConfigBlock
import com.gatehill.buildclerk.parser.inject.InstanceFactoryLocator
import java.nio.file.Path
import javax.script.ScriptEngineManager

/**
 * Parses a config file.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class Parser {
    private val engine by lazy { ScriptEngineManager().getEngineByExtension("kts")!! }

    fun parse(rulesFile: Path): ConfigBlock {
        val config = engine.eval(rulesFile.toFile().reader()) as? ConfigBlock
                ?: throw IllegalStateException("No configuration defined")

        config.body(config)
        return config
    }

    /**
     * Instantiate the block of type `B`, configure it, then invoke the `body` on it.
     */
    inline fun <reified B : AbstractBlock> invoke(
            analysis: Analysis,
            branchName: String,
            noinline blockConfigurer: ((B) -> Unit)? = null,
            noinline body: (B.() -> Unit)?
    ) {
        body?.let {
            val block = InstanceFactoryLocator.instance<B>()

            block.analysis = analysis
            block.branchName = branchName
            blockConfigurer?.let { configurer -> configurer(block) }

            block.body()
        }
    }
}
