package com.getscience.getsciencebackend.util.encryption

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec


@Service
class CryptCommonService {

    companion object {
        const val TAG_BIT_LENGTH = 128
        const val KEYSTORE_TYPE = "JCEKS"
        const val SYMMETRIC_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ALIAS = "private-key"
    }

    @Value("\${KEYSTORE_LOCATION}")
    private lateinit var keystore: String

    /** * Пароль JKS для чтения ключей.  */
    @Value("\${KEYSTORE_PASSWORD}")
    private lateinit var keyStorePassword: String

    /** * Пароль для приватного ключа  */
    @Value("\${KEY_PASSWORD}")
    private lateinit var keyPassword: String

    private val log = KotlinLogging.logger {}

    /** * Получает публичный ключ из хранилища. * * @param alias ключ jks для поиска публичного ключа * @return публичный ключ  */
    fun getPublicKey(alias: String?): PublicKey {
        val storePath: String = keystore
        try {
            FileInputStream(storePath).use { keyStoreStream ->
                val keyStore = KeyStore.getInstance("KEYSTORE_TYPE")
                keyStore.load(keyStoreStream, keyStorePassword.toCharArray())
                val certificate = keyStore.getCertificate(alias)
                return certificate.publicKey
            }
        } catch (e: Exception) {
            val messageTemplate = "Не удалось получить public из хранилища %s по alias '%s'"
            val message = String.format(messageTemplate, storePath, alias)
            throw FileNotFoundException(message)
        }
    }

    /** * Получает приватный ключ из хранилища  */
    val secretKey: SecretKey
        get() {
            try {
                FileInputStream(keystore).use { keyStoreStream ->
                    val keyStore = KeyStore.getInstance(KEYSTORE_TYPE) // Загружаем хранилище ключей
                    keyStore.load(keyStoreStream, keyStorePassword.toCharArray()) // Получаем ключевую пару
                    log.debug("Keystore loaded successfully")
                    val passwordProtection = PasswordProtection(keyPassword.toCharArray())
                    val keyEntry = keyStore.getEntry(ALIAS, passwordProtection) as KeyStore.SecretKeyEntry
                    log.debug("Key entry retrieved successfully")
                    return keyEntry.secretKey
                }
            } catch (e: Exception) {
                log.error("Failed to retrieve key from keystore", e)
                throw FileNotFoundException("Не удалось получить приватный ключ из хранилища")
            }
        }

    /** Инициализация шифра для заданного режима и ключа */
    fun initCipher(mode: Int, transformation: String?, key: Key?, spec: AlgorithmParameterSpec? = null): Cipher {
        val cipher = Cipher.getInstance(transformation)
        if (spec != null) {
            cipher.init(mode, key, spec)
        } else {
            cipher.init(mode, key)
        }
        return cipher
    }

    /** Генерация вектора инициализации для алгоритма шифрования AES-GCM  */
    fun createIvParameterSpec(): ByteArray {
        val secureRandom = SecureRandom()
        val iv = ByteArray(TAG_BIT_LENGTH / 4)
        secureRandom.nextBytes(iv)
        return IvParameterSpec(iv).iv
    }
}