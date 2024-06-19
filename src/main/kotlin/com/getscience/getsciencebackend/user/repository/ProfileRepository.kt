package com.getscience.getsciencebackend.user.repository

import com.getscience.getsciencebackend.user.data.model.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileRepository : JpaRepository<Profile, Long> {
    fun findByAccountEmail(email: String): Profile?
    fun findProfileByProfileId(profileId: Long): Profile?
    fun existsByAccountEmail(email: String): Boolean
}