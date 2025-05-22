package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.multipart.MultipartFile
import java.net.URL

@ExtendWith(MockitoExtension::class)
class ProfileServiceImplTest {

    @Mock
    private lateinit var profileRepository: ProfileRepository
    @Mock
    private lateinit var accountRepository: AccountRepository
    @Mock
    private lateinit var s3Service: S3Service

    @InjectMocks
    private lateinit var profileService: ProfileServiceImpl

    private val testEmail = "test@example.com"
    private val testProfileId = 1L
    private val mockAccount = Account(accountId = 1L, email = testEmail, passwordHash = "hash")
    private val mockRole = Role(roleId = 1L, title = RoleType.USER)
    private val mockProfile = Profile(
        profileId = testProfileId, 
        firstName = "Test", 
        lastName = "User", 
        account = mockAccount, 
        role = mockRole, 
        avatarUrl = "avatar.jpg"
    )
    private val presignedUrl = URL("http://example.com/avatar.jpg")

    @Test
    fun `findByAccountEmail should return profile with presigned URL when avatar exists`() {
        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(mockProfile)
        `when`(s3Service.generatePresignedUrl(mockProfile.avatarUrl!!)).thenReturn(presignedUrl)

        val profileResponse = profileService.findByAccountEmail(testEmail)

        assertNotNull(profileResponse)
        assertEquals(mockProfile.profileId, profileResponse?.profileId)
        assertEquals(presignedUrl.toString(), profileResponse?.avatarPresignedUrl)
    }

    @Test
    fun `findByAccountEmail should throw IllegalArgumentException when profile not found`() {
        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(null)
        assertThrows<IllegalArgumentException> {
            profileService.findByAccountEmail(testEmail)
        }
    }

    @Test
    fun `updateProfile should keep existing avatar if new one is null`() {
        val profileRequest = ProfileRequest(firstName = "Updated", lastName = "Name", avatarUrl = null)
        val existingProfileWithAvatar = mockProfile.copy(avatarUrl = "existing_avatar.jpg")
        `when`(accountRepository.findByEmail(testEmail)).thenReturn(mockAccount)
        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(existingProfileWithAvatar)
        `when`(profileRepository.save(any(Profile::class.java))).thenAnswer { it.arguments[0] }
        `when`(s3Service.generatePresignedUrl(existingProfileWithAvatar.avatarUrl!!)).thenReturn(presignedUrl)

        val response = profileService.updateProfile(profileRequest, testEmail)

        assertEquals(existingProfileWithAvatar.avatarUrl, response.avatarUrl)
        verify(profileRepository).save(argThat { it.avatarUrl == "existing_avatar.jpg" })
    }

    @Test
    fun `uploadAvatar should upload new avatar, delete old one, and return profile`() {
        val mockFile = mock(MultipartFile::class.java)
        val oldAvatarKey = "old_avatar.jpg"
        val newAvatarKey = "new_avatar.jpg"
        val existingProfileWithOldAvatar = mockProfile.copy(avatarUrl = oldAvatarKey)

        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(existingProfileWithOldAvatar)
        `when`(s3Service.uploadAvatar(mockFile, existingProfileWithOldAvatar.profileId)).thenReturn(newAvatarKey)
        `when`(profileRepository.save(any(Profile::class.java))).thenAnswer { it.arguments[0] }
        `when`(s3Service.deleteFile(oldAvatarKey)).thenReturn(true)
        `when`(s3Service.generatePresignedUrl(newAvatarKey)).thenReturn(presignedUrl)

        val profileResponse = profileService.uploadAvatar(mockFile, testEmail)

        assertNotNull(profileResponse)
        assertEquals(newAvatarKey, profileResponse.avatarUrl)
        assertEquals(presignedUrl.toString(), profileResponse.avatarPresignedUrl)
        verify(s3Service).uploadAvatar(mockFile, existingProfileWithOldAvatar.profileId)
        verify(profileRepository).save(argThat { it.avatarUrl == newAvatarKey })
        verify(s3Service).deleteFile(oldAvatarKey)
    }

    @Test
    fun `uploadAvatar should proceed even if deleting old avatar fails`() {
        val mockFile = mock(MultipartFile::class.java)
        val oldAvatarKey = "old_avatar.jpg"
        val newAvatarKey = "new_avatar.jpg"
        val existingProfileWithOldAvatar = mockProfile.copy(avatarUrl = oldAvatarKey)

        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(existingProfileWithOldAvatar)
        `when`(s3Service.uploadAvatar(mockFile, existingProfileWithOldAvatar.profileId)).thenReturn(newAvatarKey)
        `when`(profileRepository.save(any(Profile::class.java))).thenAnswer { it.arguments[0] }
        `when`(s3Service.deleteFile(oldAvatarKey)).thenReturn(false) // Deletion fails
        `when`(s3Service.generatePresignedUrl(newAvatarKey)).thenReturn(presignedUrl)

        val profileResponse = profileService.uploadAvatar(mockFile, testEmail)

        assertNotNull(profileResponse)
        assertEquals(newAvatarKey, profileResponse.avatarUrl)
        verify(s3Service).deleteFile(oldAvatarKey) // Attempt to delete is still made
    }

     @Test
    fun `uploadAvatar should not attempt to delete if old avatar url is null`() {
        val mockFile = mock(MultipartFile::class.java)
        val newAvatarKey = "new_avatar.jpg"
        val existingProfileNoAvatar = mockProfile.copy(avatarUrl = null)

        `when`(profileRepository.findByAccountEmail(testEmail)).thenReturn(existingProfileNoAvatar)
        `when`(s3Service.uploadAvatar(mockFile, existingProfileNoAvatar.profileId)).thenReturn(newAvatarKey)
        `when`(profileRepository.save(any(Profile::class.java))).thenAnswer { it.arguments[0] }
        `when`(s3Service.generatePresignedUrl(newAvatarKey)).thenReturn(presignedUrl)

        profileService.uploadAvatar(mockFile, testEmail)

        verify(s3Service, never()).deleteFile(anyString())
    }
}