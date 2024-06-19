package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.user.data.model.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByTitle(title: String): Role?
}