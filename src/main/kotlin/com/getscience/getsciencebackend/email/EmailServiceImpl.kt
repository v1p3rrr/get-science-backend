package com.getscience.getsciencebackend.email

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.context.MessageSource
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.MimeMessageHelper
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.*

/**
 * Реализация сервиса отправки электронных писем.
 * Использует JavaMailSender для отправки, TemplateEngine для генерации HTML-контента
 * и MessageSource для локализации сообщений.
 */
@Service
class EmailServiceImpl @Autowired constructor(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val messageSource: MessageSource
) : EmailService {

    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)

    init {
        if (System.getProperty("mail.debug") == null) {
            System.setProperty("mail.debug", "false")
        }
    }

    @Value("\${spring.mail.username}")
    private lateinit var fromEmail: String

    @Value("\${frontend.base-url}")
    private lateinit var frontendBaseUrl: String

    /**
     * Отправляет простое текстовое электронное письмо.
     * Использует SimpleMailMessage для создания и отправки письма.
     * 
     * @param to адрес электронной почты получателя
     * @param subject тема письма
     * @param content текстовое содержимое письма
     */
    @LogBusinessOperation(operationType = "EMAIL_SEND_SIMPLE", description = "Отправка простого текстового email")
    override fun sendEmail(to: String, subject: String, content: String) {
        logger.debug("Sending simple email to: $to with subject: $subject")
        val message = SimpleMailMessage().apply {
            setFrom(fromEmail)
            setTo(to)
            setSubject(subject)
            setText(content)
        }
        mailSender.send(message)
    }

    /**
     * Отправляет письмо для подтверждения адреса электронной почты.
     * Генерирует HTML-содержимое с использованием шаблона "verify-email"
     * и отправляет его на указанный адрес.
     * 
     * @param to адрес электронной почты получателя
     * @param token токен верификации для формирования ссылки подтверждения
     * @param locale языковая локаль для локализации содержимого письма
     */
    @LogBusinessOperation(operationType = "EMAIL_SEND_VERIFICATION", description = "Отправка email для подтверждения почты")
    override fun sendVerificationEmail(to: String, token: String, locale: Locale) {
        val subject = messageSource.getMessage("email.verification.subject", null, locale)
        val link = "${ensureHttpsUrl(frontendBaseUrl)}/verify-email?token=$token"
        val html = buildHtmlContent("verify-email", mapOf("link" to link), locale)
        sendHtmlMessage(to, subject, html)
    }

    /**
     * Отправляет письмо для сброса пароля.
     * Генерирует HTML-содержимое с использованием шаблона "reset-password"
     * и отправляет его на указанный адрес.
     * 
     * @param to адрес электронной почты получателя
     * @param token токен для формирования ссылки сброса пароля
     * @param locale языковая локаль для локализации содержимого письма
     */
    @LogBusinessOperation(operationType = "EMAIL_SEND_RESET_PASSWORD", description = "Отправка email для сброса пароля")
    override fun sendResetPasswordEmail(to: String, token: String, locale: Locale) {
        val subject = messageSource.getMessage("email.reset.subject", null, locale)
        val link = "${ensureHttpsUrl(frontendBaseUrl)}/reset-password?token=$token"
        val html = buildHtmlContent("reset-password", mapOf("link" to link), locale)
        sendHtmlMessage(to, subject, html)
    }

    /**
     * Формирует HTML-содержимое письма на основе шаблона Thymeleaf.
     * 
     * @param template имя шаблона Thymeleaf
     * @param variables карта переменных для подстановки в шаблон
     * @param locale языковая локаль для локализации шаблона
     * @return HTML-содержимое письма
     */
    private fun buildHtmlContent(template: String, variables: Map<String, Any>, locale: Locale): String {
        val context = Context(locale)
        context.setVariables(variables)
        return templateEngine.process(template, context)
    }

    /**
     * Отправляет HTML-письмо с использованием MimeMessage.
     * 
     * @param to адрес электронной почты получателя
     * @param subject тема письма
     * @param html HTML-содержимое письма
     */
    private fun sendHtmlMessage(to: String, subject: String, html: String) {
        logger.debug("Sending HTML email to: $to with subject: $subject")
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(html, true)
        mailSender.send(mimeMessage)
    }

    /**
     * Обеспечивает корректный формат URL, добавляя протокол https://, если он отсутствует.
     * 
     * @param url исходный URL
     * @return URL с добавленным протоколом https://, если он отсутствовал
     */
    private fun ensureHttpsUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
    }
}