package com.getscience.getsciencebackend.security


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Сервис для работы с JWT-токенами (JSON Web Tokens).
 * 
 * Предоставляет методы для генерации, валидации и извлечения данных из JWT-токенов,
 * используемых для аутентификации и авторизации пользователей.
 */
@Component
class JWTService(@Value("\${JWT_SECRET}") private val jwtSecret: String) {
    private val logger = LoggerFactory.getLogger(JWTService::class.java)

    /**
     * Генерирует JWT-токен для пользователя.
     * 
     * @param username имя пользователя (email)
     * @param authorities права доступа пользователя
     * @return JWT-токен в виде строки
     */
    fun generateToken(username: String, authorities: Collection<GrantedAuthority>): String {
        val claims: MutableMap<String, Any> = HashMap()
        val roles = authorities.stream()
            .map { obj: GrantedAuthority -> obj.authority }
            .collect(Collectors.joining(","))
        claims["roles"] = roles
        logger.debug("Generated new access token for user: $username, roles: $roles")
        return createToken(claims, username)
    }

    /**
     * Создает JWT-токен с указанными параметрами.
     * 
     * @param claims дополнительные данные для включения в токен
     * @param username имя пользователя (subject токена)
     * @return JWT-токен в виде строки
     */
    private fun createToken(claims: Map<String, Any>, username: String): String {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + 1000L * 60 * 15)) // 15 min
            .signWith(signKey, SignatureAlgorithm.HS256).compact()
    }

    /**
     * Возвращает ключ для подписи JWT-токенов.
     * 
     * @return ключ для подписи токенов
     */
    private val signKey: Key
        get() {
            val keyBytes: ByteArray = Decoders.BASE64.decode(jwtSecret)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    /**
     * Извлекает имя пользователя (subject) из JWT-токена.
     * 
     * @param token JWT-токен
     * @return имя пользователя
     */
    fun extractUsername(token: String?): String {
        return extractClaim<String>(token, Claims::getSubject)
    }

    /**
     * Извлекает дату истечения срока действия из JWT-токена.
     * 
     * @param token JWT-токен
     * @return дата истечения срока действия
     */
    fun extractExpiration(token: String?): Date {
        return extractClaim<Date>(token, Claims::getExpiration)
    }

    /**
     * Извлекает произвольное поле из JWT-токена.
     * 
     * @param token JWT-токен
     * @param claimsResolver функция для извлечения нужного поля из Claims
     * @return значение извлеченного поля
     */
    fun <T> extractClaim(token: String?, claimsResolver: Function<Claims, T>): T {
        val claims: Claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    /**
     * Извлекает роли пользователя из JWT-токена.
     * 
     * @param token JWT-токен
     * @return список ролей пользователя
     */
    fun extractRoles(token: String?): List<String> {
        val roles = extractClaim(token) { claims: Claims ->
            claims["roles"] as String
        }
        return roles.split(",")
    }

    /**
     * Извлекает все поля (claims) из JWT-токена.
     * 
     * @param token JWT-токен
     * @return объект Claims с полями токена
     */
    private fun extractAllClaims(token: String?): Claims {
        return Jwts
            .parserBuilder()
            .setSigningKey(signKey)
            .build()
            .parseClaimsJws(token)
            .getBody()
    }

    /**
     * Проверяет, истек ли срок действия JWT-токена.
     * 
     * @param token JWT-токен
     * @return true, если срок действия истек, иначе false
     */
    private fun isTokenExpired(token: String?): Boolean {
        return extractExpiration(token).before(Date())
    }

    /**
     * Проверяет валидность JWT-токена для указанного пользователя.
     * 
     * @param token JWT-токен
     * @param userDetails данные пользователя
     * @return true, если токен валиден для указанного пользователя, иначе false
     */
    fun validateToken(token: String?, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username && !isTokenExpired(token))
    }
}
