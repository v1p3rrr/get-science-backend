package com.getscience.getsciencebackend.email

import com.getscience.getsciencebackend.config.CoroutineTest
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.MessageSource
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.util.ReflectionTestUtils
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.*


@CoroutineTest
@ExtendWith(MockitoExtension::class)
class EmailServiceImplTest {

    @Mock
    private lateinit var mailSender: JavaMailSender
    @Mock
    private lateinit var templateEngine: TemplateEngine
    @Mock
    private lateinit var messageSource: MessageSource
    @Mock
    private lateinit var mimeMessage: MimeMessage

    private lateinit var emailService: EmailServiceImpl

    @Captor
    private lateinit var simpleMailMessageCaptor: ArgumentCaptor<SimpleMailMessage>
    @Captor
    private lateinit var mimeMessageCaptor: ArgumentCaptor<MimeMessage>
    @Captor
    private lateinit var contextCaptor: ArgumentCaptor<Context>

    private val fromEmail = "noreply@example.com"
    private val frontendBaseUrl = "http://localhost:3000"
    private val testToEmail = "test@example.com"
    private val testToken = "testToken123"
    private val testLocale = Locale.ENGLISH

    @BeforeEach
    fun setUp() {
        emailService = EmailServiceImpl(mailSender, templateEngine, messageSource)
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail)
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", frontendBaseUrl)
    }

    @Test
    fun `sendEmail should send SimpleMailMessage`() = runTest {
        val subject = "Test Subject"
        val content = "Test Content"

        emailService.sendEmail(testToEmail, subject, content)

        verify(mailSender).send(any<SimpleMailMessage>())
    }

    @Test
    fun `sendVerificationEmail should send HTML email`() = runTest {
        val subject = "Verify Your Email"
        val verificationLink = "$frontendBaseUrl/verify-email?token=$testToken"
        val htmlContent = "<html><body>Verification Link: $verificationLink</body></html>"

        val mockMimeMessage = mock<MimeMessage>()
        val fromAddress = arrayOf(InternetAddress(fromEmail))
        val toAddress = arrayOf(InternetAddress(testToEmail))

        whenever(mailSender.createMimeMessage()).thenReturn(mockMimeMessage)
        whenever(mockMimeMessage.getFrom()).thenReturn(fromAddress)
        whenever(mockMimeMessage.getRecipients(Message.RecipientType.TO)).thenReturn(toAddress)
        whenever(mockMimeMessage.subject).thenReturn(subject)

        whenever(templateEngine.process(eq("verify-email"), any())).thenReturn(htmlContent)
        whenever(messageSource.getMessage(eq("email.verification.subject"), isNull(), eq(testLocale))).thenReturn(subject)

        emailService.sendVerificationEmail(testToEmail, testToken, testLocale)

        verify(mailSender).send(mockMimeMessage)
        assertEquals(fromEmail, mockMimeMessage.from[0].toString())
        assertEquals(testToEmail, mockMimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
        assertEquals(subject, mockMimeMessage.subject)
    }

    @Test
    fun `sendResetPasswordEmail should send HTML email`() = runTest {
        val subject = "Reset Your Password"
        val resetLink = "$frontendBaseUrl/reset-password?token=$testToken"
        val htmlContent = "<html><body>Reset Link: $resetLink</body></html>"

        val mockMimeMessage = mock<MimeMessage>()
        val fromAddress = arrayOf(InternetAddress(fromEmail))
        val toAddress = arrayOf(InternetAddress(testToEmail))

        whenever(mailSender.createMimeMessage()).thenReturn(mockMimeMessage)
        whenever(mockMimeMessage.getFrom()).thenReturn(fromAddress)
        whenever(mockMimeMessage.getRecipients(Message.RecipientType.TO)).thenReturn(toAddress)
        whenever(mockMimeMessage.subject).thenReturn(subject)

        whenever(templateEngine.process(eq("reset-password"), any())).thenReturn(htmlContent)
        whenever(messageSource.getMessage(eq("email.reset.subject"), isNull(), eq(testLocale))).thenReturn(subject)

        emailService.sendResetPasswordEmail(testToEmail, testToken, testLocale)

        verify(mailSender).send(mockMimeMessage)
        assertEquals(fromEmail, mockMimeMessage.from[0].toString())
        assertEquals(testToEmail, mockMimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
        assertEquals(subject, mockMimeMessage.subject)
    }
} 