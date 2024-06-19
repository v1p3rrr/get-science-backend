package com.getscience.getsciencebackend.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config {

    @Value("\${S3_ACCESS_KEY}")
    private lateinit var s3AccessKey: String

    @Value("\${S3_SECRET_KEY}")
    private lateinit var s3SecretKey: String

    @Value("\${S3_ENDPOINT}")
    private lateinit var s3Endpoint: String

    @Value("\${S3_REGION}")
    private lateinit var s3Region: String

    private val log = KotlinLogging.logger {}

    @Bean
    fun s3Client(): S3Client {
        log.info { "Initializing S3Client with endpoint: $s3Endpoint" }

        val credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretKey)
        return S3Client.builder()
            .region(Region.of(s3Region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(s3Endpoint))
            .serviceConfiguration(S3Configuration.builder().build())
            .build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        log.info("Initializing S3Presigner with endpoint: $s3Endpoint")

        val credentials = AwsBasicCredentials.create(s3AccessKey, s3SecretKey)
        return S3Presigner.builder()
            .region(Region.of(s3Region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(s3Endpoint))
            .build()
    }
}