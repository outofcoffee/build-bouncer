package com.gatehill.buildclerk

import com.gatehill.buildclerk.api.dao.BuildReportDao
import com.gatehill.buildclerk.api.dao.PendingActionDao
import com.gatehill.buildclerk.api.dao.PullRequestEventDao
import com.gatehill.buildclerk.api.service.BuildReportService
import com.gatehill.buildclerk.api.service.BuildRunnerService
import com.gatehill.buildclerk.api.service.NotificationService
import com.gatehill.buildclerk.config.Settings
import com.gatehill.buildclerk.dao.inmem.InMemoryBuildReportDaoImpl
import com.gatehill.buildclerk.dao.inmem.InMemoryPendingActionDaoImpl
import com.gatehill.buildclerk.dao.inmem.InMemoryPullRequestEventDaoImpl
import com.gatehill.buildclerk.parser.Parser
import com.gatehill.buildclerk.parser.inject.InstanceFactory
import com.gatehill.buildclerk.parser.inject.InstanceFactoryLocator
import com.gatehill.buildclerk.server.Server
import com.gatehill.buildclerk.api.service.AnalysisService
import com.gatehill.buildclerk.service.AnalysisServiceImpl
import com.gatehill.buildclerk.service.CommandExecutorService
import com.gatehill.buildclerk.service.PendingActionService
import com.gatehill.buildclerk.service.builder.BuildEventService
import com.gatehill.buildclerk.service.builder.BuildReportServiceImpl
import com.gatehill.buildclerk.service.builder.jenkins.JenkinsApiClientBuilder
import com.gatehill.buildclerk.service.builder.jenkins.JenkinsBuildRunnerServiceImpl
import com.gatehill.buildclerk.service.notify.slack.SlackApiService
import com.gatehill.buildclerk.service.notify.slack.SlackNotificationServiceImpl
import com.gatehill.buildclerk.service.notify.slack.SlackOperationsService
import com.gatehill.buildclerk.api.service.PullRequestEventService
import com.gatehill.buildclerk.service.scm.ScmService
import com.gatehill.buildclerk.service.scm.bitbucket.BitbucketApiClientBuilder
import com.gatehill.buildclerk.service.scm.bitbucket.BitbucketOperationsService
import com.gatehill.buildclerk.service.scm.bitbucket.BitbucketPullRequestEventServiceImpl
import com.gatehill.buildclerk.service.scm.bitbucket.BitbucketScmServiceImpl
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Singleton
import com.google.inject.binder.ScopedBindingBuilder
import org.apache.logging.log4j.LogManager

private val logger = LogManager.getLogger("com.gatehill.buildclerk.Clerk")

fun main(args: Array<String>) {
    logger.info("Starting Build Clerk")

    val injector = Guice.createInjector(object : AbstractModule() {
        override fun configure() {
            bind(PendingActionService::class.java).asSingleton()
            bind(CommandExecutorService::class.java).asSingleton()

            // server
            bind(Server::class.java).asSingleton()

            // event processors
            bind(BuildReportService::class.java).to(BuildReportServiceImpl::class.java).asSingleton()
            bind(BuildEventService::class.java).asSingleton()
            bind(AnalysisService::class.java).to(AnalysisServiceImpl::class.java).asSingleton()

            // daos
            bind(BuildReportDao::class.java).to(storeImplementation).asSingleton()
            bind(PullRequestEventDao::class.java).to(InMemoryPullRequestEventDaoImpl::class.java).asSingleton()
            bind(PendingActionDao::class.java).to(InMemoryPendingActionDaoImpl::class.java).asSingleton()

            // slack
            bind(NotificationService::class.java).to(SlackNotificationServiceImpl::class.java).asSingleton()
            bind(SlackOperationsService::class.java).asSingleton()
            bind(SlackApiService::class.java).asSingleton()

            // bitbucket
            bind(ScmService::class.java).to(BitbucketScmServiceImpl::class.java).asSingleton()
            bind(BitbucketOperationsService::class.java).asSingleton()
            bind(BitbucketApiClientBuilder::class.java).asSingleton()
            bind(PullRequestEventService::class.java).to(BitbucketPullRequestEventServiceImpl::class.java).asSingleton()

            // jenkins
            bind(BuildRunnerService::class.java).to(JenkinsBuildRunnerServiceImpl::class.java).asSingleton()
            bind(JenkinsApiClientBuilder::class.java).asSingleton()

            // parser
            bind(Parser::class.java).asSingleton()
        }
    })


    InstanceFactoryLocator.instanceFactory = object : InstanceFactory {
        override fun <T : Any> instance(clazz: Class<T>) = injector.getInstance(clazz)
    }

    val server = injector.getInstance(Server::class.java)
    server.startServer()
}

private val storeImplementation: Class<out BuildReportDao>
    get() {
        val impl = Settings.Store.implementation ?: InMemoryBuildReportDaoImpl::class.java
        logger.debug("Using ${impl.simpleName} store implementation")
        return impl
    }

/**
 * Syntactic sugar for binding Guice singletons.
 */
private fun ScopedBindingBuilder.asSingleton() = this.`in`(Singleton::class.java)
