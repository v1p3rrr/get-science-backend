package com.getscience.getsciencebackend.user.repository

import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByTitle(title: RoleType): Role?
    fun existsByTitle(title: RoleType): Boolean
}