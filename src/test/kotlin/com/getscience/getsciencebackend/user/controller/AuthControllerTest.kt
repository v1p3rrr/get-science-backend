package com.getscience.getsciencebackend.user.controller

import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.security.JWTService
import com.getscience.getsciencebackend.security.RefreshTokenService
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.dto.ChangePasswordRequest
import com.getscience.getsciencebackend.user.data.dto.JwtResponse
import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.service.AccountService
import com.getscience.getsciencebackend.user.service.ProfileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.net.URL

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {

    @Mock
    private lateinit var accountService: AccountService
    @Mock
    private lateinit var profileService: ProfileService
    @Mock
    private lateinit var refreshTokenService: RefreshTokenService
    @Mock
    private lateinit var jwtService: JWTService
    @Mock
    private lateinit var s3Service: S3Service

    @InjectMocks
    private lateinit var authController: AuthController

    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val mockAccount = Account(accountId = 1L, email = testEmail, passwordHash = "hash")
    private val mockRole = Role(roleId = 1L, title = RoleType.USER)
    private val mockProfile = Profile(profileId = 1L, firstName = "Test", lastName = "User", account = mockAccount, role = mockRole)
    private val mockProfileResponse = ProfileResponse.fromEntity(mockProfile)

    private fun mockSecurityContext(email: String) {
        val authentication = mock(Authentication::class.java)
        val securityContext = mock(SecurityContext::class.java)
        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.name).thenReturn(email)
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `loginUser should call AccountService and return JwtResponse`() {
        val loginRequest = LoginRequest(testEmail, testPassword)
        val jwtResponse = JwtResponse("access", "refresh")
        `when`(accountService.login(loginRequest)).thenReturn(jwtResponse)

        val response = authController.loginUser(loginRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(jwtResponse, response.body)
    }

    @Test
    fun `changePassword should call AccountService and return NoContent`() {
        mockSecurityContext(testEmail)
        val changePasswordRequest = ChangePasswordRequest("current", "newPassword")
        `when`(accountService.changePassword(testEmail, "current", "newPassword")).thenReturn(true)

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(accountService).changePassword(testEmail, "current", "newPassword")
        SecurityContextHolder.clearContext() // Clean up context
    }

     @Test
    fun `changePassword should return BadRequest if service fails`() {
        mockSecurityContext(testEmail)
        val changePasswordRequest = ChangePasswordRequest("current", "newPassword")
        `when`(accountService.changePassword(testEmail, "current", "newPassword")).thenReturn(false)

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        SecurityContextHolder.clearContext()
    }
    
    @Test
    fun `uploadAvatar should call ProfileService and return updated profile`() {
        mockSecurityContext(testEmail)
        val mockFile = MockMultipartFile("file", "avatar.jpg", "image/jpeg", "someImageData".toByteArray())
        val updatedProfileResponse = mockProfileResponse.copy(avatarUrl = "new_avatar.jpg")

        `when`(profileService.uploadAvatar(mockFile, testEmail)).thenReturn(updatedProfileResponse)

        val response = authController.uploadAvatar(mockFile)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(updatedProfileResponse, response.body)
        verify(profileService).uploadAvatar(mockFile, testEmail)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `getAvatarPresignedUrl should return URL when avatar exists`() {
        mockSecurityContext(testEmail)
        val presignedUrl = URL("http://s3.com/avatar.jpg")
        val profileWithAvatar = mockProfileResponse.copy(avatarUrl = "avatar.key")
        `when`(profileService.findByAccountEmail(testEmail)).thenReturn(profileWithAvatar)
        `when`(s3Service.generatePresignedUrl("avatar.key")).thenReturn(presignedUrl)

        val response = authController.getAvatarPresignedUrl()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(mapOf("url" to presignedUrl.toString()), response.body)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `getAvatarPresignedUrl should return NotFound when avatar does not exist`() {
        mockSecurityContext(testEmail)
        val profileWithoutAvatar = mockProfileResponse.copy(avatarUrl = null)
        `when`(profileService.findByAccountEmail(testEmail)).thenReturn(profileWithoutAvatar)

        val response = authController.getAvatarPresignedUrl()

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `verifyEmail should call service and return OK`() {
        val token = "verificationToken"
        doNothing().`when`(accountService).verifyEmail(token)

        val response = authController.verifyEmail(token)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Email verified successfully", response.body)
        verify(accountService).verifyEmail(token)
    }

    @Test
    fun `verifyEmail should return BadRequest on IllegalArgumentException`() {
        val token = "invalidToken"
        val errorMessage = "Token expired or already used"
        `when`(accountService.verifyEmail(token)).thenThrow(IllegalArgumentException(errorMessage))

        val response = authController.verifyEmail(token)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(errorMessage, response.body)
    }

}