package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.email.EmailService
import com.getscience.getsciencebackend.security.JWTService
import com.getscience.getsciencebackend.security.RefreshTokenService
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.RegisterRequest
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.data.model.token.RefreshToken
import com.getscience.getsciencebackend.user.data.model.token.Token
import com.getscience.getsciencebackend.user.data.model.token.TokenType
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import com.getscience.getsciencebackend.user.repository.RoleRepository
import com.getscience.getsciencebackend.user.repository.TokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class AccountServiceImplTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var profileRepository: ProfileRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var jwtService: JWTService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @InjectMocks
    private lateinit var accountService: AccountServiceImpl

    @Captor
    private lateinit var accountCaptor: ArgumentCaptor<Account>

    @Captor
    private lateinit var tokenCaptor: ArgumentCaptor<Token>

    @Captor
    private lateinit var profileCaptor: ArgumentCaptor<Profile>

    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testFirstName = "Test"
    private val testLastName = "User"
    private val testRoleType = RoleType.USER
    private val locale = Locale.ENGLISH

    @BeforeEach
    fun setUp() {
        // Common setup if needed, e.g. lenient stubs
        lenient().`when`(accountRepository.save(any(Account::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as Account }
        lenient().`when`(profileRepository.save(any(Profile::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as Profile }
        lenient().`when`(tokenRepository.save(any(Token::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as Token }
        lenient().`when`(roleRepository.save(any(Role::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as Role }
    }

    @Test
    fun `registerUser should create role if not exists`() {
        val registerRequest = RegisterRequest(testFirstName, testLastName, testRoleType, testEmail, testPassword)
        `when`(accountRepository.findByEmail(testEmail)).thenReturn(null)
        `when`(passwordEncoder.encode(testPassword)).thenReturn("encoded")
        `when`(roleRepository.findByTitle(testRoleType)).thenReturn(null) // Role does not exist

        accountService.registerUser(registerRequest, locale)

        verify(roleRepository).save(argThat { it.title == testRoleType })
    }

    @Test
    fun `login should return JwtResponse on successful authentication`() {
        // Arrange
        val loginRequest = LoginRequest(testEmail, testPassword)
        val authentication = mock(Authentication::class.java)
        val authorities = mutableListOf(GrantedAuthority { "ROLE_USER" })
        val accessToken = "accessToken"
        val refreshToken = RefreshToken(
            token = "refreshTokenString",
            account = Account(email = testEmail, passwordHash = ""),
            expiryDate = Date()
        )

        `when`(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java))).thenReturn(
            authentication
        )
        `when`(authentication.isAuthenticated).thenReturn(true)
        `when`(authentication.authorities).thenReturn(authorities)
        `when`(jwtService.generateToken(testEmail, authorities)).thenReturn(accessToken)
        `when`(refreshTokenService.createRefreshToken(testEmail)).thenReturn(refreshToken)

        // Act
        val jwtResponse = accountService.login(loginRequest)

        // Assert
        assertEquals(accessToken, jwtResponse.accessToken)
        assertEquals(refreshToken.token, jwtResponse.refreshToken)
    }

    @Test
    fun `login should throw UsernameNotFoundException on failed authentication`() {
        // Arrange
        val loginRequest = LoginRequest(testEmail, testPassword)
        val authentication = mock(Authentication::class.java)
        `when`(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java))).thenReturn(
            authentication
        )
        `when`(authentication.isAuthenticated).thenReturn(false)

        // Act & Assert
        assertThrows<UsernameNotFoundException> {
            accountService.login(loginRequest)
        }
    }

    @Test
    fun `verifyEmail should confirm email and mark token as used`() {
        val tokenString = "validToken"
        val account = Account(email = testEmail, passwordHash = "hash", emailConfirmed = false)
        val verificationToken = Token(
            token = tokenString,
            account = account,
            type = TokenType.EMAIL_VERIFICATION,
            expiresAt = Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
            used = false
        )

        `when`(tokenRepository.findByTokenAndType(tokenString, TokenType.EMAIL_VERIFICATION)).thenReturn(
            verificationToken
        )

        accountService.verifyEmail(tokenString)

        verify(accountRepository).save(accountCaptor.capture())
        assertTrue(accountCaptor.value.emailConfirmed)
        verify(tokenRepository).save(tokenCaptor.capture())
        assertTrue(tokenCaptor.value.used)
    }

    @Test
    fun `verifyEmail should throw IllegalArgumentException for invalid token`() {
        `when`(tokenRepository.findByTokenAndType("invalid", TokenType.EMAIL_VERIFICATION)).thenReturn(null)
        assertThrows<IllegalArgumentException> { accountService.verifyEmail("invalid") }
    }

    @Test
    fun `verifyEmail should throw IllegalArgumentException for used token`() {
        val account = Account(email = testEmail, passwordHash = "hash")
        val verificationToken = Token(
            token = "usedToken",
            account = account,
            type = TokenType.EMAIL_VERIFICATION,
            expiresAt = Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
            used = true
        )
        `when`(tokenRepository.findByTokenAndType("usedToken", TokenType.EMAIL_VERIFICATION)).thenReturn(
            verificationToken
        )
        assertThrows<IllegalArgumentException> { accountService.verifyEmail("usedToken") }
    }
}