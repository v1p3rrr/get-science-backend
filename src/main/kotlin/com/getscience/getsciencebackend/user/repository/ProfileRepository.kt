package com.getscience.getsciencebackend.user.repository

import com.getscience.getsciencebackend.user.data.model.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProfileRepository : JpaRepository<Profile, Long> {
    fun findByAccountEmail(email: String): Profile?
    fun findProfileByProfileId(profileId: Long): Profile?
    fun existsByAccountEmail(email: String): Boolean

@Query("""
        SELECT p FROM Profile p
        JOIN p.account a
        WHERE (:email IS NULL OR LOWER(a.email) LIKE :email )
          AND (:firstName IS NULL OR LOWER(p.firstName) LIKE :firstName)
          AND (:lastName IS NULL OR LOWER(p.lastName) LIKE :lastName)
    """)
    fun searchProfiles(
        @Param("email") email: String?,
        @Param("firstName") firstName: String?,
        @Param("lastName") lastName: String?
    ): List<Profile>
}