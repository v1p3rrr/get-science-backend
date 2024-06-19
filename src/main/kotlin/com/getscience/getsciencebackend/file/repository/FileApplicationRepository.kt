package com.getscience.getsciencebackend.file.repository

import com.getscience.getsciencebackend.file.data.model.FileApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FileApplicationRepository : JpaRepository<FileApplication, Long> {
    fun findByApplicationApplicationId(applicationId: Long): List<FileApplication>
}