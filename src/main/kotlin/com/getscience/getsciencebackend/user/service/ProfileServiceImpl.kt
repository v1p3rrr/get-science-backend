package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository
) : ProfileService {

    override fun findByAccountEmail(email: String): ProfileResponse? {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")
        return ProfileResponse.fromEntity(profile)
    }

    override fun findByProfileId(profileId: Long): ProfileResponse? {
        val profile = profileRepository.findProfileByProfileId(profileId)
            ?: throw IllegalArgumentException("Profile not found")
        return ProfileResponse.fromEntity(profile)
    }

    @CachePut("profile", key = "#email")
    override fun updateProfile(profile: ProfileRequest, email: String): ProfileResponse {
        val account = accountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Account not found")
        val existingProfile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")

        val updatedProfile = existingProfile.copy(
            firstName = profile.firstName,
            lastName = profile.lastName
        )

        val savedProfile = profileRepository.save(updatedProfile)
        account.profile = savedProfile
        accountRepository.save(account)
        return ProfileResponse.fromEntity(savedProfile)
    }
}
