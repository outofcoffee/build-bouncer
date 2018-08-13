package com.gatehill.buildclerk.service

import com.gatehill.buildclerk.api.model.Analysis
import com.gatehill.buildclerk.api.model.BuildOutcome
import com.gatehill.buildclerk.api.model.BuildStatus
import com.gatehill.buildclerk.api.model.PullRequestMergedEvent
import com.gatehill.buildclerk.api.service.BuildOutcomeService
import com.gatehill.buildclerk.config.Settings
import com.gatehill.buildclerk.dsl.AbstractBuildBlock
import com.gatehill.buildclerk.parser.Parser
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

class AnalysisService @Inject constructor(
        private val parser: Parser,
        private val buildOutcomeService: BuildOutcomeService
) {
    private val logger = LogManager.getLogger(AnalysisService::class.java)

    fun analyseBuild(outcome: BuildOutcome): Analysis {
        val analysis = Analysis("Build ${outcome.build.number} on ${outcome.build.scm.branch}", logger)

        val config = parser.parse(Settings.Rules.configFile)

        val previousBuildStatus = buildOutcomeService.fetchBuildStatus(
                branchName = outcome.build.scm.branch,
                buildNumber = outcome.build.number - 1
        )

        val blockConfigurer = { block: AbstractBuildBlock ->
            block.outcome = outcome
        }

        when (outcome.build.status) {
            BuildStatus.SUCCESS -> {
                logger.info("Build passed: $outcome")
                parser.invoke(
                        analysis = analysis,
                        branchName = outcome.build.scm.branch,
                        blockConfigurer = blockConfigurer,
                        body = config.bodyHolder.buildPassed
                )

                if (previousBuildStatus == BuildStatus.FAILED) {
                    logger.info("Branch started passing: $outcome")
                    parser.invoke(
                            analysis = analysis,
                            branchName = outcome.build.scm.branch,
                            blockConfigurer = blockConfigurer,
                            body = config.bodyHolder.branchStartsPassing
                    )
                }
            }
            BuildStatus.FAILED -> {
                logger.info("Build failed: $outcome")
                parser.invoke(
                        analysis = analysis,
                        branchName = outcome.build.scm.branch,
                        blockConfigurer = blockConfigurer,
                        body = config.bodyHolder.buildFailed
                )

                if (previousBuildStatus == BuildStatus.SUCCESS) {
                    logger.info("Branch started failing: $outcome")
                    parser.invoke(
                            analysis = analysis,
                            branchName = outcome.build.scm.branch,
                            blockConfigurer = blockConfigurer,
                            body = config.bodyHolder.branchStartsFailing
                    )
                }
            }
        }

        // runs every time
        parser.invoke(
                analysis = analysis,
                branchName = outcome.build.scm.branch,
                body = config.bodyHolder.repository
        )

        analysis.log("Analysis complete")
        return analysis
    }

    fun analysePullRequest(
            mergeEvent: PullRequestMergedEvent,
            currentBranchStatus: BuildStatus
    ): Analysis {

        val analysis = Analysis("Pull request ${mergeEvent.pullRequest.id} merged into ${mergeEvent.pullRequest.destination.branch.name}", logger)

        val config = parser.parse(Settings.Rules.configFile)

        parser.invoke(
                analysis = analysis,
                branchName = mergeEvent.pullRequest.destination.branch.name,
                body = config.bodyHolder.pullRequestMerged,
                blockConfigurer = { block ->
                    block.mergeEvent = mergeEvent
                    block.currentBranchStatus = currentBranchStatus
                }
        )

        analysis.log("Analysis complete")
        return analysis
    }
}
