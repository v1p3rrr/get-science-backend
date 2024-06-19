package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.model.Profile

interface ProfileService {
    fun findByAccountEmail(email: String): ProfileResponse?
    fun findByProfileId(profileId: Long): ProfileResponse?
    fun updateProfile(profile: ProfileRequest, email: String): ProfileResponse
}
