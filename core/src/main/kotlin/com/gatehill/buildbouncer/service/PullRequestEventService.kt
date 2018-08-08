package com.gatehill.buildbouncer.service

import com.gatehill.buildbouncer.api.model.BuildStatus
import com.gatehill.buildbouncer.api.service.BuildOutcomeService
import com.gatehill.buildbouncer.config.Settings
import com.gatehill.buildbouncer.model.PullRequestMergedEvent
import com.gatehill.buildbouncer.service.scm.ScmService
import kotlinx.coroutines.experimental.async
import org.apache.logging.log4j.LogManager

class PullRequestEventService(
        private val buildOutcomeService: BuildOutcomeService,
        private val scmService: ScmService
) {
    private val logger = LogManager.getLogger(PullRequestEventService::class.java)

    fun verify(event: PullRequestMergedEvent) {
        logger.debug("Processing PR merge event: $event")

        val prInfo = "merge event for PR #${event.pullRequest.id} [author: ${event.pullRequest.author.username}]"
        if (event.repository.name != Settings.repoName) {
            logger.info("Ignoring $prInfo because repository name: ${event.repository.name} does not match")
            return
        }
        val branchName = event.pullRequest.destination.branch.name
        if (branchName != Settings.branchName) {
            logger.info("Ignoring $prInfo because branch name: $branchName does not match")
            return
        }

        async {
            buildOutcomeService.fetchStatus(branchName)?.let { buildOutcome ->
                val branchStatusInfo =
                        "branch name: $branchName status is currently: ${buildOutcome.build.status}"

                when (buildOutcome.build.status) {
                    BuildStatus.SUCCESS -> logger.info("Verified $prInfo because $branchStatusInfo")
                    BuildStatus.FAILED -> {
                        logger.warn("Failed to validate $prInfo because $branchStatusInfo - reverting merge")
                        scmService.revertCommit(
                                commit = event.pullRequest.mergeCommit.hash,
                                branchName = event.pullRequest.destination.branch.name
                        )
                    }
                }

            } ?: run {
                logger.warn("Skipped validation of $prInfo because status of branch: $branchName is unknown")
            }
        }
    }
}