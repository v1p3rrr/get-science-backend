package com.getscience.getsciencebackend.user.controller

import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.security.JWTService
import com.getscience.getsciencebackend.security.RefreshTokenService
import com.getscience.getsciencebackend.user.data.dto.*
import com.getscience.getsciencebackend.user.service.AccountService
import com.getscience.getsciencebackend.user.service.ProfileService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("api/v1/auth")
class AuthController @Autowired constructor(
    private val accountService: AccountService,
    private val profileService: ProfileService,
    private val refreshTokenService: RefreshTokenService,
    private val jwtService: JWTService,
    private val s3Service: S3Service
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * Регистрирует нового пользователя в системе.
     * 
     * @param registerRequest данные для регистрации пользователя
     * @param languageHeader языковой заголовок для локализации писем
     * @return сообщение об успешной регистрации с ID аккаунта
     */
    @PostMapping("/register")
    fun registerUser(@RequestBody @Valid registerRequest: RegisterRequest, @RequestHeader("Accept-Language") languageHeader: String?): ResponseEntity<String> {
        val locale = languageHeader?.let { Locale.forLanguageTag(it) } ?: Locale("ru", "RU")
        val account = accountService.registerUser(registerRequest, locale)
        return ResponseEntity.ok("User registered with ID: ${account.accountId}")
    }

    /**
     * Аутентифицирует пользователя и возвращает JWT-токены.
     * 
     * @param loginRequest данные для входа (email и пароль)
     * @return JWT-токены доступа и обновления
     */
    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<JwtResponse> {
        val jwtResponse = accountService.login(loginRequest)
        return ResponseEntity.ok(jwtResponse)
    }

    /**
     * Выходит из системы, делая токен обновления недействительным.
     * 
     * @param refreshToken токен обновления, который нужно аннулировать
     * @return пустой ответ с кодом 204
     */
    @PostMapping("/logout")
    fun logout(@RequestParam refreshToken: String): ResponseEntity<Void> {
        refreshTokenService.deleteByToken(refreshToken)
        return ResponseEntity.noContent().build()
    }

    /**
     * Выходит из всех сессий пользователя, аннулируя все его токены обновления.
     * 
     * @return пустой ответ с кодом 204
     */
    @DeleteMapping("/logout/all")
    fun logoutAllSessions(): ResponseEntity<Void> {
        val email = getEmailFromToken()
        refreshTokenService.deleteAllTokensByEmail(email)
        return ResponseEntity.noContent().build()
    }

    /**
     * Обновляет JWT-токен доступа с помощью токена обновления.
     * 
     * @param refreshToken токен обновления
     * @return новые JWT-токены доступа и обновления
     */
    @PostMapping("/refresh-token", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun refreshAccessToken(@RequestBody refreshToken: String): ResponseEntity<JwtResponse> {
        val tokenEntity = refreshTokenService.validateRefreshToken(refreshToken)
        val email = tokenEntity.account.email
        val account = tokenEntity.account
        val authorities = account.authorities

        val newAccessToken = jwtService.generateToken(email, authorities)
        logger.debug("New access token generated for user $email")

        return ResponseEntity.ok(JwtResponse(accessToken = newAccessToken, refreshToken = refreshToken))
    }

    /**
     * Изменяет пароль аутентифицированного пользователя.
     * 
     * @param request запрос с текущим и новым паролями
     * @return пустой ответ с кодом 204 при успехе, иначе 400
     */
    @PutMapping("/change-password")
    fun changePassword(
        @RequestBody @Valid request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        val email = getEmailFromToken()
        val success = accountService.changePassword(email, request.currentPassword, request.newPassword)
        return if (success) ResponseEntity.noContent().build() else ResponseEntity.badRequest().build()
    }

    /**
     * Получает профиль пользователя по email.
     * 
     * @param email email пользователя
     * @return данные профиля пользователя
     */
    @GetMapping("/profile")
    fun getProfileByEmail(@RequestParam email: String): ResponseEntity<ProfileResponse> {
        val profile = profileService.findByAccountEmail(email)
        return ResponseEntity.ok(profile)
    }

    /**
     * Получает профиль текущего аутентифицированного пользователя.
     * 
     * @return данные профиля текущего пользователя
     */
    @GetMapping("/profile/me")
    fun getProfileByJwt(): ResponseEntity<ProfileResponse> {
        val email = getEmailFromToken()
        val profile = profileService.findByAccountEmail(email)
        return ResponseEntity.ok(profile)
    }

    /**
     * Получает профиль пользователя по идентификатору.
     * 
     * @param id идентификатор профиля
     * @return данные профиля пользователя
     */
    @GetMapping("/profile/{id}")
    fun getProfileById(@PathVariable id: Long): ResponseEntity<ProfileResponse> {
        val profile = profileService.findByProfileId(id)
        return ResponseEntity.ok(profile)
    }

    /**
     * Обновляет профиль текущего аутентифицированного пользователя.
     * 
     * @param profile новые данные профиля
     * @return обновленные данные профиля
     */
    @PutMapping("/profile")
    fun updateProfile(@RequestBody profile: ProfileRequest): ResponseEntity<ProfileResponse> {
        val updatedProfile = profileService.updateProfile(profile, getEmailFromToken())
        return ResponseEntity.ok(updatedProfile)
    }

    /**
     * Ищет профили пользователей по заданным критериям.
     * 
     * @param email часть email для поиска (опционально)
     * @param firstName часть имени для поиска (опционально)
     * @param lastName часть фамилии для поиска (опционально)
     * @return список найденных профилей
     */
    @GetMapping("/users")
    fun searchProfiles(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?
    ) = profileService.searchProfiles(email, firstName, lastName)

    /**
     * Получает профиль пользователя вместе с информацией о его мероприятиях.
     * 
     * @param profileId идентификатор профиля
     * @return данные профиля с информацией о мероприятиях
     */
    @GetMapping("/users/{profileId}")
    fun getProfileWithEvents(@PathVariable profileId: Long) = profileService.getProfileWithEvents(profileId)

    /**
     * Загружает аватар для текущего аутентифицированного пользователя.
     * 
     * @param file файл аватара
     * @return обновленные данные профиля с URL аватара
     */
    @PostMapping("/profile/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(@RequestParam("file") file: MultipartFile): ResponseEntity<ProfileResponse> {
        val email = getEmailFromToken()
        val updatedProfile = profileService.uploadAvatar(file, email)
        return ResponseEntity.ok(updatedProfile)
    }

    /**
     * Получает временный URL для доступа к аватару текущего пользователя.
     * 
     * @return временный URL для доступа к аватару
     */
    @GetMapping("/profile/avatar")
    fun getAvatarPresignedUrl(): ResponseEntity<Map<String, String>> {
        val email = getEmailFromToken()
        val profile = profileService.findByAccountEmail(email)
        
        return if (profile?.avatarUrl != null) {
            val presignedUrl = s3Service.generatePresignedUrl(profile.avatarUrl).toString()
            ResponseEntity.ok(mapOf("url" to presignedUrl))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Получает временный URL для доступа к аватару указанного пользователя.
     * 
     * @param profileId идентификатор профиля
     * @return временный URL для доступа к аватару
     */
    @GetMapping("/profile/avatar/{profileId}")
    fun getAvatarPresignedUrlByProfileId(@PathVariable profileId: Long): ResponseEntity<Map<String, String>> {
        val profile = profileService.findByProfileId(profileId)
        
        return if (profile?.avatarUrl != null) {
            val presignedUrl = s3Service.generatePresignedUrl(profile.avatarUrl).toString()
            ResponseEntity.ok(mapOf("url" to presignedUrl))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Подтверждает email пользователя после регистрации.
     * 
     * @param token токен верификации email
     * @return сообщение об успешном подтверждении или ошибке
     */
    @PostMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<String> {
        return try {
            accountService.verifyEmail(token)
            ResponseEntity.ok("Email verified successfully")
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ex.message)
        }
    }

    /**
     * Запрашивает сброс пароля для указанного email.
     * 
     * @param email email пользователя
     * @param languageHeader языковой заголовок для локализации письма
     * @return сообщение о результате операции
     */
    @PostMapping("/request-password-reset")
    fun requestPasswordReset(@RequestParam email: String, @RequestHeader("Accept-Language") languageHeader: String?): ResponseEntity<String> {
        val locale = languageHeader?.let { Locale.forLanguageTag(it) } ?: Locale("ru", "RU")
        accountService.requestPasswordReset(email, locale)
        return ResponseEntity.ok("If this email exists, reset instructions were sent.")
    }

    /**
     * Сбрасывает пароль пользователя по токену сброса.
     * 
     * @param token токен сброса пароля
     * @param newPassword новый пароль
     * @return сообщение о результате операции
     */
    @PostMapping("/reset-password")
    fun resetPassword(@RequestParam token: String, @RequestParam newPassword: String): ResponseEntity<String> {
        return try {
            accountService.resetPassword(token, newPassword)
            ResponseEntity.ok("Password updated")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    /**
     * Извлекает email текущего аутентифицированного пользователя из токена JWT.
     * 
     * @return email пользователя
     */
    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }
}