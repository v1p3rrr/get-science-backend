package com.getscience.getsciencebackend.util.encryption

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(
    private val cryptCommonService: CryptCommonService
) {
    private val secretKey: SecretKey = cryptCommonService.secretKey

    private val log = KotlinLogging.logger {}

    /**
     * Шифрование массива байтов с использованием заданного ключа
     * @param content массив байтов, требующий шифрования
     * @param key ключ для шифрования содержимого
     * @return зашифрованное содержимое файла  */
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
     * Шифрование строки с использованием заданного ключа
     * @param content строка с кодировкой UTF-8, требующая шифрации
     * @param key ключ для шифрования содержимого
     * @return зашифрованная строка  */
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