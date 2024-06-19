package com.getscience.getsciencebackend.security


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

@Component
class JWTService(@Value("\${JWT_SECRET}") private val jwtSecret: String) {
    fun generateToken(username: String, authorities: Collection<GrantedAuthority>): String {
        val claims: MutableMap<String, Any> = HashMap()
        val roles = authorities.stream()
            .map { obj: GrantedAuthority -> obj.authority }
            .collect(Collectors.joining(","))
        claims["roles"] = roles
        return createToken(claims, username)
    }

    private fun createToken(claims: Map<String, Any>, username: String): String {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365))
            .signWith(signKey, SignatureAlgorithm.HS256).compact()
    }

    private val signKey: Key
        get() {
            val keyBytes: ByteArray = Decoders.BASE64.decode(jwtSecret)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    fun extractUsername(token: String?): String {
        return extractClaim<String>(token, Claims::getSubject)
    }

    fun extractExpiration(token: String?): Date {
        return extractClaim<Date>(token, Claims::getExpiration)
    }

    fun <T> extractClaim(token: String?, claimsResolver: Function<Claims, T>): T {
        val claims: Claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }

    fun extractRoles(token: String?): List<String> {
        val roles = extractClaim<String>(token) { claims: Claims ->
            claims["roles"] as String
        }
        return roles.split(",")
    }

    private fun extractAllClaims(token: String?): Claims {
        return Jwts
            .parserBuilder()
            .setSigningKey(signKey)
            .build()
            .parseClaimsJws(token)
            .getBody()
    }

    private fun isTokenExpired(token: String?): Boolean {
        return extractExpiration(token).before(Date())
    }

    fun validateToken(token: String?, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username && !isTokenExpired(token))
    }
}
