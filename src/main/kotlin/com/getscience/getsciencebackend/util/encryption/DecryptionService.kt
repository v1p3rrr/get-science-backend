package com.getscience.getsciencebackend.util.encryption

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/** * Предназначен для дешифрования файлов или тела сообщения (текста) * при помощи транспортного ключа и вектора, зашифрованных публичным ключом  */
@Service
class DecryptionService(
    private val cryptCommonService: CryptCommonService
) {
    private val secretKey = cryptCommonService.secretKey

    private val log = KotlinLogging.logger {}

    /**
     * Расшифровка текста сообщения в виде строки
     * @param messageBody зашифрованное сообщение
     * @param key ключ, применявшийся при шифровании
     * @return расшифрованное содержимое сообщения
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
     * Расшифровка содержимого файла в виде массива байт
     * @param key ключ, применявшийся при шифровании
     * @param payload зашифрованное содержимое файла в виде массива байт
     * @return расшифрованное содержимое файла
     */
    fun decryptFile(payload: ByteArray, key: ByteArray? = null): ByteArray {
        val decryptedFile = decryptBytesWithKey(payload, key ?: secretKey.encoded)
        log.info { "Successfully decrypted file" }
        return decryptedFile
    }

    /**
     * Расшифровываем массив байтов (файл/сообщение)
     * @param key ключ, применявшийся при шифровании
     * @param payload полезная нагрузка, требующая расшифровки - сообщение или файл
     * @return расшифрованное содержимое
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