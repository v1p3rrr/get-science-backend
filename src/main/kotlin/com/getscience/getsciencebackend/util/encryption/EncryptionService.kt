package com.getscience.getsciencebackend.util.encryption

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Сервис для шифрования данных.
 * 
 * Предоставляет методы для шифрования строк и файлов с использованием
 * секретного ключа и алгоритма AES/GCM.
 */
@Service
class EncryptionService(
    private val cryptCommonService: CryptCommonService
) {
    private val secretKey: SecretKey = cryptCommonService.secretKey

    private val log = KotlinLogging.logger {}

    /**
     * Шифрует массив байтов с использованием заданного ключа.
     *
     * @param content массив байтов для шифрования
     * @param key ключ для шифрования (если null, используется ключ по умолчанию)
     * @return зашифрованный массив байтов
     */
    fun encryptBytes(content: ByteArray, key: ByteArray? = null): ByteArray {
        val secretKeySpec = SecretKeySpec(secretKey.encoded, CryptCommonService.SYMMETRIC_ALGORITHM)
        val parameterSpec = GCMParameterSpec(CryptCommonService.TAG_BIT_LENGTH, key ?: secretKey.encoded)
        val fileCipher: Cipher = cryptCommonService.initCipher(
            Cipher.ENCRYPT_MODE,
            CryptCommonService.TRANSFORMATION,
            secretKeySpec,
            parameterSpec
        )
        val encryptedFile = fileCipher.doFinal(content)
        log.info { "Successfully encrypted file" }
        return encryptedFile
    }

    /**
     * Шифрует строку с использованием заданного ключа.
     *
     * @param content строка для шифрования в кодировке UTF-8
     * @param key ключ для шифрования (если null, используется ключ по умолчанию)
     * @return зашифрованная строка в формате Base64
     */
    fun encryptString(content: String, key: ByteArray? = null): String {
        val secretKeySpec = SecretKeySpec(key ?: secretKey.encoded, CryptCommonService.SYMMETRIC_ALGORITHM)
        val parameterSpec = GCMParameterSpec(CryptCommonService.TAG_BIT_LENGTH, key ?: secretKey.encoded)
        val contentCipher: Cipher = cryptCommonService.initCipher(
            Cipher.ENCRYPT_MODE,
            CryptCommonService.TRANSFORMATION,
            secretKeySpec,
            parameterSpec
        )
        val encryptedContent = contentCipher.doFinal(content.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedContent)
    }
}