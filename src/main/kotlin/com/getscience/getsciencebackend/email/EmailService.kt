package com.getscience.getsciencebackend.email

import java.util.*

/**
 * Сервис для отправки электронных писем.
 * Предоставляет методы для отправки различных типов писем: простых текстовых,
 * писем подтверждения регистрации и сброса пароля.
 */
interface EmailService {
    /**
     * Отправляет простое текстовое электронное письмо.
     * 
     * @param to адрес электронной почты получателя
     * @param subject тема письма
     * @param content текстовое содержимое письма
     */
    fun sendEmail(to: String, subject: String, content: String)
    
    /**
     * Отправляет письмо для подтверждения адреса электронной почты.
     * Использует шаблон HTML с ссылкой для подтверждения.
     * 
     * @param to адрес электронной почты получателя
     * @param token токен верификации
     * @param locale языковая локаль для локализации содержимого письма
     */
    fun sendVerificationEmail(to: String, token: String, locale: Locale)
    
    /**
     * Отправляет письмо для сброса пароля.
     * Использует шаблон HTML с ссылкой для сброса пароля.
     * 
     * @param to адрес электронной почты получателя
     * @param token токен для сброса пароля
     * @param locale языковая локаль для локализации содержимого письма
     */
    fun sendResetPasswordEmail(to: String, token: String, locale: Locale)
}