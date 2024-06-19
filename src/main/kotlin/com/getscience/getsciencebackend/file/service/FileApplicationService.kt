package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.data.model.FileApplication
import org.springframework.web.multipart.MultipartFile
import java.net.URL

interface FileApplicationService {
     fun uploadFiles(files: List<MultipartFile>, application: Application, isEncryptionEnabled: Boolean): List<FileApplication>
    fun createFileApplication(fileApplicationRequest: FileApplicationRequest, applicationId: Long, email: String): FileApplication
    fun getFileApplicationsByApplication(applicationId: Long, email: String): List<FileApplicationResponse>
    fun getPresignedUrlForFile(fileId: Long, email: String): URL
     fun downloadFile(fileId: Long, email: String): ByteArray
    fun getApplication(fileId: Long, email: String): Application
    fun getFileApplication(fileId: Long, email: String): FileApplication
}