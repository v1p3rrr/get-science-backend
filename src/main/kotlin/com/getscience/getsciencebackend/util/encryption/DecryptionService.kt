package com.getscience.getsciencebackend.util.encryption

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Сервис для дешифрования данных.
 * 
 * Предоставляет методы для дешифрования строк и файлов с использованием
 * секретного ключа и алгоритма AES/GCM.
 */
@Service
class DecryptionService(
    private val cryptCommonService: CryptCommonService
) {
    private val secretKey = cryptCommonService.secretKey

    private val log = KotlinLogging.logger {}

    /**
     * Расшифровывает текст сообщения в виде строки.
     *
     * @param messageBody зашифрованное сообщение в Base64
     * @param key ключ для дешифрования (если null, используется ключ по умолчанию)
     * @return расшифрованная строка в UTF-8
     */
    fun decryptString(messageBody: String, key: ByteArray? = null): String {
        val decryptedMessage = decryptBytesWithKey(
            Base64.getDecoder().decode(messageBody.toByteArray(StandardCharsets.UTF_8)),
            key ?: secretKey.encoded
        )
        log.info { "Successfully decrypted string" }
        return String(decryptedMessage, StandardCharsets.UTF_8)
    }

    /**
     * Расшифровывает содержимое файла в виде массива байт.
     *
     * @param payload зашифрованное содержимое файла
     * @param key ключ для дешифрования (если null, используется ключ по умолчанию)
     * @return расшифрованное содержимое файла
     */
    fun decryptFile(payload: ByteArray, key: ByteArray? = null): ByteArray {
        val decryptedFile = decryptBytesWithKey(payload, key ?: secretKey.encoded)
        log.info { "Successfully decrypted file" }
        return decryptedFile
    }

    /**
     * Расшифровывает массив байтов (файл/сообщение) с указанным ключом.
     *
     * @param payload зашифрованные данные
     * @param key ключ для дешифрования (если null, используется ключ по умолчанию)
     * @return расшифрованные данные
     */
    private fun decryptBytesWithKey(
        payload: ByteArray,
        key: ByteArray? = null
    ): ByteArray {
        val secretKeySpec = SecretKeySpec(key ?: secretKey.encoded, CryptCommonService.SYMMETRIC_ALGORITHM)
        val parameterSpec = GCMParameterSpec(CryptCommonService.TAG_BIT_LENGTH, key ?: secretKey.encoded)
        val fileCipher: Cipher = cryptCommonService.initCipher(
            Cipher.DECRYPT_MODE,
            CryptCommonService.TRANSFORMATION,
            secretKeySpec,
            parameterSpec
        )
        return fileCipher.doFinal(payload)
    }
}