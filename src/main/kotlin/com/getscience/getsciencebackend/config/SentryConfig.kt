package com.getscience.getsciencebackend.config


import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.UserBuilder;
import io.sentry.Sentry
import io.sentry.SentryClient
import io.sentry.SentryClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class SentryConfig {

    @Value("\${SENTRY_DSN}")
    private lateinit var dsn: String

    @Bean
    fun sentry(): SentryClient = SentryClientFactory.sentryClient(dsn)

    fun setUserContext(email: String) {
        val context = Sentry.getContext()
        context.user = UserBuilder().setEmail(email).build()
    }

    fun setBreadcrumb(message: String) {
        val context = Sentry.getContext()
        context.recordBreadcrumb(
            BreadcrumbBuilder().setMessage(message).build()
        )
    }
}