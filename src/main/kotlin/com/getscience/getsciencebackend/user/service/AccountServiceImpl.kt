package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.application.data.dto.ProfileDTO
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.user.data.model.token.Token
import com.getscience.getsciencebackend.user.data.model.token.TokenType
import com.getscience.getsciencebackend.user.repository.TokenRepository
import com.getscience.getsciencebackend.email.EmailService
import com.getscience.getsciencebackend.security.JWTService
import com.getscience.getsciencebackend.security.RefreshTokenService
import com.getscience.getsciencebackend.user.data.dto.JwtResponse
import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.RegisterRequest
import com.getscience.getsciencebackend.user.data.model.*
import com.getscience.getsciencebackend.user.repository.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val tokenRepository: TokenRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JWTService,
    private val refreshTokenService: RefreshTokenService
) : AccountService {
    private val logger = LoggerFactory.getLogger(AccountServiceImpl::class.java)

    /**
     * {@inheritDoc}
     * 
     * Создает новую учетную запись пользователя, профиль и отправляет email для подтверждения.
     */
    @LogBusinessOperation(operationType = "USER_REGISTRATION", description = "Регистрация нового пользователя")
    @Transactional
    override fun registerUser(registerRequest: RegisterRequest, locale: Locale): Account {
        accountRepository.findByEmail(registerRequest.email)?.let {
            throw IllegalArgumentException("User with email ${registerRequest.email} already exists")
        }

        val encodedPassword = passwordEncoder.encode(registerRequest.password)

        val account = Account(
            email = registerRequest.email,
            passwordHash = encodedPassword
        )

        val savedAccount = accountRepository.save(account)

        val verificationToken = UUID.randomUUID().toString()
            tokenRepository.save(
                Token(
                    token = verificationToken,
                    account = savedAccount,
                    type = TokenType.EMAIL_VERIFICATION,
                    expiresAt = Date(System.currentTimeMillis() + 1000 * 60 * 60) // 1 час
                )
            )
        emailService.sendVerificationEmail(savedAccount.email, verificationToken, locale)

        // Fetch the role from the database
        val role = roleRepository.findByTitle(registerRequest.role) ?: Role(title = registerRequest.role).also { roleRepository.save(it) }

        val profile = Profile(
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            role = role,
            account = savedAccount
        )

        savedAccount.profile = profile

        return savedAccount
    }

    /**
     * {@inheritDoc}
     * 
     * Аутентифицирует пользователя, генерирует JWT-токен доступа и токен обновления.
     */
    @LogBusinessOperation(operationType = "USER_LOGIN", description = "Аутентификация пользователя")
    @Transactional
    override fun login(loginRequest: LoginRequest): JwtResponse {
        val email = loginRequest.email
        val password = loginRequest.password
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, password)
        )

        if (!authentication.isAuthenticated)
            throw UsernameNotFoundException("Invalid credentials")

        val accessToken = jwtService.generateToken(email, authentication.authorities)
        val refreshToken = refreshTokenService.createRefreshToken(email)
        logger.error("Access token issued for $email")
        logger.error("Refresh token created for $email with expiry ${refreshToken.expiryDate}")

        return JwtResponse(accessToken, refreshToken.token)
    }

    /**
     * {@inheritDoc}
     */
    override fun findProfileByEmail(email: String): Profile? {
        return profileRepository.findByAccountEmail(email)
    }

    /**
     * {@inheritDoc}
     *
     * Результат кэшируется с использованием email в качестве ключа.
     */
    @Cacheable("profile", key = "#email")
    override fun findProfileDTOByEmail(email: String): ProfileDTO? {
        return profileRepository.findByAccountEmail(email)?.let { ProfileDTO.fromEntity(it) }
    }

    /**
     * {@inheritDoc}
     * 
     * Проверяет текущий пароль перед установкой нового.
     */
    @LogBusinessOperation(operationType = "PASSWORD_CHANGE", description = "Изменение пароля пользователя")
    @Transactional
    override fun changePassword(email: String, currentPassword: String, newPassword: String): Boolean {
        val account = accountRepository.findByEmail(email)
            ?: throw NoSuchElementException("Account not found for $email")

        if (!passwordEncoder.matches(currentPassword, account.password)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        val updatedAccount = account.copy(passwordHash = passwordEncoder.encode(newPassword))
        accountRepository.save(updatedAccount)
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun checkIfProfileExists(email: String): Boolean {
        return profileRepository.existsByAccountEmail(email)
    }

    /**
     * {@inheritDoc}
     * 
     * Проверяет валидность токена и устанавливает флаг emailConfirmed для аккаунта.
     */
    @LogBusinessOperation(operationType = "EMAIL_VERIFICATION", description = "Подтверждение email пользователя")
    @Transactional
    override fun verifyEmail(token: String) {
        val verification = tokenRepository.findByTokenAndType(token, TokenType.EMAIL_VERIFICATION)
            ?: throw IllegalArgumentException("Invalid token")

        if (verification.used || verification.expiresAt.before(Date())) {
            throw IllegalArgumentException("Token expired or already used")
        }

        val account = verification.account
        val accountUpdated = account.copy(emailConfirmed = true)
        accountRepository.save(accountUpdated)

        val verificationUpdated = verification.copy(used = true)
        tokenRepository.save(verificationUpdated)
    }

    /**
     * {@inheritDoc}
     * 
     * Создает токен сброса пароля и отправляет его на email пользователя.
     * Если пользователь с указанным email не найден, метод завершается без ошибки 
     * для предотвращения раскрытия информации о наличии аккаунта.
     */
    @LogBusinessOperation(operationType = "PASSWORD_RESET_REQUEST", description = "Запрос на сброс пароля")
    @Transactional
    override fun requestPasswordReset(email: String, locale: Locale) {
        val account = accountRepository.findByEmail(email) ?: return

        val token = UUID.randomUUID().toString()

        val resetToken = Token(
            token = token,
            account = account,
            type = TokenType.PASSWORD_RESET,
            expiresAt = Date(System.currentTimeMillis() + 1000 * 60 * 15)
        )

        tokenRepository.save(resetToken)
        emailService.sendResetPasswordEmail(account.email, token, locale)
    }

    /**
     * {@inheritDoc}
     * 
     * Проверяет валидность токена сброса пароля и устанавливает новый пароль для аккаунта.
     */
    @LogBusinessOperation(operationType = "PASSWORD_RESET", description = "Сброс пароля пользователя")
    @Transactional
    override fun resetPassword(token: String, newPassword: String) {
        val resetToken = tokenRepository.findByTokenAndType(token, TokenType.PASSWORD_RESET)
            ?: throw IllegalArgumentException("Invalid token")

        if (resetToken.used || resetToken.expiresAt.before(Date())) {
            throw IllegalArgumentException("Token expired or already used")
        }

        val account = resetToken.account
        val updatedAccount = account.copy(passwordHash = passwordEncoder.encode(newPassword))
        accountRepository.save(updatedAccount)

        val updatedResetToken = resetToken.copy(used = true)
        tokenRepository.save(updatedResetToken)
    }
}
