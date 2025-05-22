package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.application.data.dto.ProfileDTO
import com.getscience.getsciencebackend.user.data.dto.*
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import java.util.Locale

/**
 * Сервис для управления аккаунтами пользователей.
 *
 * Предоставляет методы для регистрации, аутентификации, смены пароля,
 * подтверждения email и сброса пароля пользователей.
 */
interface AccountService {
    /**
     * Регистрирует нового пользователя в системе.
     * 
     * Создает аккаунт, профиль пользователя и отправляет email для подтверждения.
     *
     * @param registerRequest данные для регистрации
     * @param locale локаль для отправки email
     * @return созданный аккаунт пользователя
     * @throws IllegalArgumentException если пользователь с таким email уже существует
     */
    fun registerUser(registerRequest: RegisterRequest, locale: Locale): Account
    
    /**
     * Аутентифицирует пользователя и возвращает JWT-токены.
     *
     * @param loginRequest данные для входа (email и пароль)
     * @return JWT-токены доступа и обновления
     * @throws UsernameNotFoundException если пользователь не найден или учетные данные неверны
     */
    fun login(loginRequest: LoginRequest): JwtResponse
    
    /**
     * Находит профиль пользователя по email.
     *
     * @param email email пользователя
     * @return профиль пользователя или null, если профиль не найден
     */
    fun findProfileByEmail(email: String): Profile?

    /**
     * Находит DTO профиль пользователя по email.
     *
     * @param email email пользователя
     * @return DTO профиль пользователя или null, если профиль не найден
     */
    fun findProfileDTOByEmail(email: String): ProfileDTO?
    
    /**
     * Изменяет пароль пользователя.
     *
     * @param email email пользователя
     * @param currentPassword текущий пароль
     * @param newPassword новый пароль
     * @return true, если пароль успешно изменен, иначе false
     * @throws NoSuchElementException если пользователь не найден
     * @throws IllegalArgumentException если текущий пароль неверен
     */
    fun changePassword(email: String, currentPassword: String, newPassword: String): Boolean
    
    /**
     * Проверяет существование профиля пользователя с указанным email.
     *
     * @param email email пользователя
     * @return true, если профиль существует, иначе false
     */
    fun checkIfProfileExists(email: String): Boolean
    
    /**
     * Подтверждает email пользователя по токену верификации.
     *
     * @param token токен верификации email
     * @throws IllegalArgumentException если токен недействителен, истек или уже использован
     */
    fun verifyEmail(token: String)
    
    /**
     * Запрашивает сброс пароля для указанного email.
     * 
     * Создает токен сброса пароля и отправляет его на email пользователя.
     *
     * @param email email пользователя
     * @param locale локаль для отправки email
     */
    fun requestPasswordReset(email: String, locale: Locale)
    
    /**
     * Сбрасывает пароль пользователя по токену сброса.
     *
     * @param token токен сброса пароля
     * @param newPassword новый пароль
     * @throws IllegalArgumentException если токен недействителен, истек или уже использован
     */
    fun resetPassword(token: String, newPassword: String)
}
