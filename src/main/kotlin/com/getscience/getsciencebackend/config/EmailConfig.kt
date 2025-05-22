package com.getscience.getsciencebackend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

/**
 * Конфигурация для отправки электронных писем и обработки шаблонов.
 * Настраивает SMTP-клиент для отправки писем и Thymeleaf для шаблонизации содержимого.
 */
@Configuration
class EmailConfig {

    @Value("\${spring.mail.host:smtp.gmail.com}")
    private lateinit var host: String

    @Value("\${spring.mail.port:587}")
    private var port: Int = 587

    @Value("\${spring.mail.username:}")
    private lateinit var username: String

    @Value("\${spring.mail.password:}")
    private lateinit var password: String

    /**
     * Создает и настраивает отправитель почты JavaMailSender.
     * Использует настройки SMTP-сервера из конфигурации приложения.
     * Если учетные данные настроены, включает SMTP-аутентификацию и TLS.
     *
     * @return настроенный JavaMailSender для отправки электронных писем
     */
    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port
        
        if (username.isNotBlank() && password.isNotBlank()) {
            mailSender.username = username
            mailSender.password = password

            val props = mailSender.javaMailProperties
            props["mail.transport.protocol"] = "smtp"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.debug"] = "true"
            props["mail.smtp.connectiontimeout"] = "5000"
            props["mail.smtp.timeout"] = "5000"
            props["mail.smtp.writetimeout"] = "5000"

        }

        return mailSender
    }

    /**
     * Создает настройщик шаблонов Thymeleaf для загрузки шаблонов электронных писем.
     * Настраивает путь к шаблонам, формат файлов и кодировку.
     *
     * @return настройщик шаблонов для Thymeleaf
     */
    @Bean
    fun templateResolver(): ClassLoaderTemplateResolver {
        val resolver = ClassLoaderTemplateResolver()
        resolver.prefix = "templates/"
        resolver.suffix = ".html"
        resolver.templateMode = TemplateMode.HTML
        resolver.characterEncoding = "UTF-8"
        resolver.isCacheable = false
        return resolver
    }

    /**
     * Создает движок шаблонов Thymeleaf для обработки HTML-шаблонов.
     * Использует настроенный templateResolver для загрузки шаблонов.
     *
     * @return настроенный движок шаблонов Thymeleaf
     */
    @Bean
    fun templateEngine(): SpringTemplateEngine {
        val engine = SpringTemplateEngine()
        engine.setTemplateResolver(templateResolver())
        return engine
    }
} 