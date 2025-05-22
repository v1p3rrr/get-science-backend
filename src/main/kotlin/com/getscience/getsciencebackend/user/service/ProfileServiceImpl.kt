package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import mu.KotlinLogging

@Service
@Transactional
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val s3Service: S3Service
) : ProfileService {

    private val logger = KotlinLogging.logger {}

    /**
     * {@inheritDoc}
     * 
     * Также получает временный URL для аватара, если он есть.
     */
    @LogBusinessOperation(operationType = "PROFILE_GET", description = "Получение профиля по email")
    override fun findByAccountEmail(email: String): ProfileResponse? {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")
        
        // Получаем presigned URL для аватарки
        val presignedUrl = if (profile.avatarUrl != null) {
            s3Service.generatePresignedUrl(profile.avatarUrl).toString()
        } else {
            null
        }
        
        return ProfileResponse.fromEntity(profile, presignedUrl)
    }

    /**
     * {@inheritDoc}
     * 
     * Также получает временный URL для аватара, если он есть.
     */
    @LogBusinessOperation(operationType = "PROFILE_GET_BY_ID", description = "Получение профиля по ID")
    override fun findByProfileId(profileId: Long): ProfileResponse? {
        val profile = profileRepository.findProfileByProfileId(profileId)
            ?: throw IllegalArgumentException("Profile not found")
        
        // Получаем presigned URL для аватарки
        val presignedUrl = if (profile.avatarUrl != null) {
            s3Service.generatePresignedUrl(profile.avatarUrl).toString()
        } else {
            null
        }
        
        return ProfileResponse.fromEntity(profile, presignedUrl)
    }

    /**
     * {@inheritDoc}
     * 
     * Обновляет профиль и кэширует результат. Сохраняет существующий URL аватара, 
     * если в запросе он не указан.
     */
    @LogBusinessOperation(operationType = "PROFILE_UPDATE", description = "Обновление профиля пользователя")
    @CachePut("profile", key = "#email")
    override fun updateProfile(profile: ProfileRequest, email: String): ProfileResponse {
        val account = accountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Account not found")
        val existingProfile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")

        val updatedProfile = existingProfile.copy(
            firstName = profile.firstName,
            lastName = profile.lastName,
            aboutMe = profile.aboutMe,
            avatarUrl = profile.avatarUrl ?: existingProfile.avatarUrl
        )

        val savedProfile = profileRepository.save(updatedProfile)
        account.profile = savedProfile
        accountRepository.save(account)
        
        // Получаем presigned URL для аватарки
        val presignedUrl = if (savedProfile.avatarUrl != null) {
            s3Service.generatePresignedUrl(savedProfile.avatarUrl).toString()
        } else {
            null
        }
        
        return ProfileResponse.fromEntity(savedProfile, presignedUrl)
    }

    /**
     * {@inheritDoc}
     * 
     * Нормализует параметры поиска, добавляя к ним символы подстановки для частичного совпадения,
     * и получает временный URL для аватара каждого найденного профиля.
     */
    @LogBusinessOperation(operationType = "PROFILE_SEARCH", description = "Поиск профилей пользователей")
    override fun searchProfiles(email: String?, firstName: String?, lastName: String?): List<ProfileResponse> {
        val normalizedEmail = email?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        val normalizedFirstName = firstName?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        val normalizedLastName = lastName?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        
        return profileRepository.searchProfiles(normalizedEmail, normalizedFirstName, normalizedLastName).map { profile ->
            // Для каждого профиля получаем presigned URL для аватарки
            val presignedUrl = if (profile.avatarUrl != null) {
                s3Service.generatePresignedUrl(profile.avatarUrl).toString()
            } else {
                null
            }
            
            ProfileResponse.fromEntity(profile, presignedUrl)
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Также получает временный URL для аватара, если он есть.
     */
    @LogBusinessOperation(operationType = "PROFILE_GET_WITH_EVENTS", description = "Получение профиля с событиями")
    override fun getProfileWithEvents(profileId: Long): ProfileResponse {
        val profile = profileRepository.findProfileByProfileId(profileId)
            ?: throw IllegalArgumentException("Profile not found")
        
        // Получаем presigned URL для аватарки
        val presignedUrl = if (profile.avatarUrl != null) {
            s3Service.generatePresignedUrl(profile.avatarUrl).toString()
        } else {
            null
        }
        
        return ProfileResponse.fromEntity(profile, presignedUrl)
    }

    /**
     * {@inheritDoc}
     * 
     * Загружает новый аватар в S3, удаляет предыдущий аватар из S3 (если был),
     * обновляет URL аватара в профиле и кэширует результат.
     */
    @LogBusinessOperation(operationType = "PROFILE_AVATAR_UPLOAD", description = "Загрузка аватара профиля")
    @CachePut("profile", key = "#email")
    override fun uploadAvatar(file: MultipartFile, email: String): ProfileResponse {
        val existingProfile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")
            
        // Сохраняем ссылку на старую аватарку
        val oldAvatarUrl = existingProfile.avatarUrl
            
        val avatarKey = s3Service.uploadAvatar(file, existingProfile.profileId)
        
        val updatedProfile = existingProfile.copy(
            avatarUrl = avatarKey
        )
        
        val savedProfile = profileRepository.save(updatedProfile)
        
        // Удаляем старую аватарку из S3, если она существует
        if (oldAvatarUrl != null) {
            try {
                val deleteResult = s3Service.deleteFile(oldAvatarUrl)
                if (deleteResult) {
                    logger.info("Старая аватарка удалена: $oldAvatarUrl")
                } else {
                    logger.warn("Не удалось удалить старую аватарку: $oldAvatarUrl")
                }
            } catch (e: Exception) {
                logger.error("Ошибка при удалении старой аватарки: $oldAvatarUrl", e)
                // Продолжаем работу, несмотря на ошибку удаления
            }
        }
        
        // Получаем presigned URL для новой аватарки
        val presignedUrl = s3Service.generatePresignedUrl(avatarKey).toString()
        
        return ProfileResponse.fromEntity(savedProfile, presignedUrl)
    }
}
