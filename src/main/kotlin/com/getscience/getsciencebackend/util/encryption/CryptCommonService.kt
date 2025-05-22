package com.getscience.getsciencebackend.util.encryption

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Сервис для общих операций с шифрованием.
 * 
 * Предоставляет методы для работы с хранилищем ключей, инициализации шифров
 * и создания векторов инициализации для алгоритмов шифрования.
 */
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

    /**
     * Универсальный способ получить InputStream из keystore.
     * 
     * Сначала пытается загрузить keystore с диска, если не найден — из classpath.
     * 
     * @return поток для чтения хранилища ключей
     * @throws FileNotFoundException если хранилище не найдено ни на диске, ни в classpath
     */
    private fun getKeyStoreInputStream(): InputStream {
        val file = File(keystore)
        return if (file.exists()) {
            log.info { "Loading keystore from file path: ${file.absolutePath}" }
            FileInputStream(file)
        } else {
            log.info { "Keystore not found on disk, trying classpath: $keystore" }
            this::class.java.classLoader.getResourceAsStream(keystore)
                ?: throw FileNotFoundException("Keystore not found on disk or in classpath: $keystore")
        }
    }

    /**
     * Проверяет возможность загрузки хранилища ключей при инициализации сервиса.
     * 
     * Выполняется автоматически после создания бина сервиса.
     */
    @PostConstruct
    fun verifyKeystoreLoading() {
        try {
            secretKey // заставим выполнить инициализацию
            log.info { "Keystore successfully loaded from $keystore" }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to load keystore during startup from $keystore" }
        }
    }

    /**
     * Получает публичный ключ из хранилища.
     *
     * @param alias ключ jks для поиска публичного ключа
     * @return публичный ключ
     * @throws FileNotFoundException если сертификат с указанным alias не найден
     */
    fun getPublicKey(alias: String?): PublicKey {
        try {
            getKeyStoreInputStream().use { keyStoreStream ->
                val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
                keyStore.load(keyStoreStream, keyStorePassword.toCharArray())
                val certificate = keyStore.getCertificate(alias)
                    ?: throw FileNotFoundException("Certificate with alias '$alias' not found")
                return certificate.publicKey
            }
        } catch (e: Exception) {
            val message = "Не удалось получить public из хранилища '$keystore' по alias '$alias'"
            log.error(e) { message }
            throw FileNotFoundException(message)
        }
    }

    /**
     * Получает секретный ключ из хранилища.
     * 
     * @return секретный ключ для шифрования/дешифрования
     * @throws FileNotFoundException если не удалось получить ключ из хранилища
     */
    val secretKey: SecretKey
        get() {
            try {
                getKeyStoreInputStream().use { keyStoreStream ->
                    val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
                    keyStore.load(keyStoreStream, keyStorePassword.toCharArray())
                    log.debug { "Keystore loaded successfully" }

                    val passwordProtection = PasswordProtection(keyPassword.toCharArray())
                    val keyEntry = keyStore.getEntry(ALIAS, passwordProtection) as? KeyStore.SecretKeyEntry
                        ?: throw IllegalStateException("SecretKeyEntry with alias '$ALIAS' not found in keystore")

                    log.debug { "Key entry retrieved successfully" }
                    return keyEntry.secretKey
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to retrieve key from keystore" }
                throw FileNotFoundException("Не удалось получить приватный ключ из хранилища")
            }
        }

    /**
     * Инициализирует шифр для заданного режима и ключа.
     * 
     * @param mode режим шифра (Cipher.ENCRYPT_MODE или Cipher.DECRYPT_MODE)
     * @param transformation строка трансформации шифра (например, "AES/GCM/NoPadding")
     * @param key ключ для шифрования/дешифрования
     * @param spec параметры алгоритма (опционально)
     * @return инициализированный объект Cipher
     */
    fun initCipher(mode: Int, transformation: String?, key: Key?, spec: AlgorithmParameterSpec? = null): Cipher {
        val cipher = Cipher.getInstance(transformation)
        if (spec != null) {
            cipher.init(mode, key, spec)
        } else {
            cipher.init(mode, key)
        }
        return cipher
    }

    /**
     * Генерирует вектор инициализации для алгоритма шифрования.
     * 
     * @return массив байтов, представляющий вектор инициализации
     */
    fun createIvParameterSpec(): ByteArray {
        val secureRandom = SecureRandom()
        val iv = ByteArray(TAG_BIT_LENGTH / 4)
        secureRandom.nextBytes(iv)
        return IvParameterSpec(iv).iv
    }
}