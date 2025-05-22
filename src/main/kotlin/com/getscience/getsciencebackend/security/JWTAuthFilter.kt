package com.getscience.getsciencebackend.security

import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.repository.AccountRepository
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.lang.NonNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * Фильтр для аутентификации пользователей по JWT-токену.
 * 
 * Перехватывает все HTTP-запросы, проверяет наличие и валидность JWT-токена в заголовке Authorization,
 * и при успешной валидации устанавливает аутентификацию пользователя в контексте безопасности Spring.
 */
@Component
class JWTAuthFilter internal constructor(private val jwtService: JWTService, private val accountRepository: AccountRepository) :
    OncePerRequestFilter() {

    /**
     * Обрабатывает HTTP-запрос и выполняет аутентификацию на основе JWT-токена.
     * 
     * Извлекает JWT-токен из заголовка Authorization, проверяет его валидность,
     * и при успешной валидации устанавливает аутентификацию пользователя.
     * 
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @param filterChain цепочка фильтров для дальнейшей обработки запроса
     * @throws ServletException при ошибках в сервлете
     * @throws IOException при ошибках ввода-вывода
     */
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        @NonNull response: HttpServletResponse,
        @NonNull filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        var token: String? = null
        var username: String? = null

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7)
            try {
                username = jwtService.extractUsername(token)
            } catch (e: ExpiredJwtException) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token expired")
                return
            }
        }
        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            val account: Account = accountRepository.findByEmail(username) ?: throw RuntimeException("User not found")
                if (jwtService.validateToken(token, account)) {
                    val authToken = UsernamePasswordAuthenticationToken(account, null, account.authorities)
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        filterChain.doFilter(request, response)
    }
}
