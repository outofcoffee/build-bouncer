package com.gatehill.buildbouncer.parser

import com.gatehill.buildbouncer.api.model.Analysis
import com.gatehill.buildbouncer.api.model.BuildOutcome
import com.gatehill.buildbouncer.dsl.ConfigWrapper
import java.nio.file.Path
import javax.script.ScriptEngineManager

/**
 * Parses a config file.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class Parser {
    private val engine by lazy { ScriptEngineManager().getEngineByExtension("kts")!! }

    fun parse(rulesFile: Path, outcome: BuildOutcome, analysis: Analysis): ConfigWrapper {
        val config = engine.eval(rulesFile.toFile().reader()) as? ConfigWrapper
                ?: throw IllegalStateException("No configuration defined")

        config.buildOutcome = outcome
        config.analysis = analysis
        config.eval()

        return config
    }
}
