package com.getscience.getsciencebackend.config

import com.getscience.getsciencebackend.security.JWTAuthFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.http.HttpMethod

/**
 * Конфигурация безопасности приложения.
 * Настраивает авторизацию, аутентификацию, CORS и фильтры безопасности.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: UserDetailsService
) {

    @Value("\${frontend.base-url}")
    private lateinit var frontendBaseUrl: String

    /**
     * Настраивает цепочку фильтров безопасности.
     * Отключает CSRF, настраивает CORS для фронтенда, определяет правила доступа к API,
     * и добавляет JWT фильтр для аутентификации по токену.
     * 
     * @param http объект HttpSecurity для настройки
     * @param jwtAuthFilter фильтр для JWT аутентификации
     * @return настроенная цепочка фильтров безопасности
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthFilter: JWTAuthFilter?): SecurityFilterChain {
        return http
            .csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .cors { cors -> cors.configurationSource {
                val config = CorsConfiguration()
                val host = extractHost(frontendBaseUrl)
                config.allowedOrigins = listOf(host, "https://$host", "http://$host")
                config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                config.allowedHeaders = listOf("*")
                config.allowCredentials = true
                config
            }
        }
            .authorizeHttpRequests { auth: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/ws/chat/**").permitAll()
                    .requestMatchers("/api/v1/files/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    /**
     * Создает и настраивает провайдер аутентификации.
     * Использует сервис для загрузки данных пользователей и кодировщик паролей.
     * 
     * @param passwordEncoder кодировщик паролей
     * @return настроенный провайдер аутентификации
     */
    @Bean
    fun authenticationProvider(
        passwordEncoder: PasswordEncoder?
    ): AuthenticationProvider {
        val authenticationProvider = DaoAuthenticationProvider()
        authenticationProvider.setUserDetailsService(userDetailsService)
        authenticationProvider.setPasswordEncoder(passwordEncoder)
        return authenticationProvider
    }

    /**
     * Создает менеджер аутентификации.
     * 
     * @param config конфигурация аутентификации
     * @return менеджер аутентификации
     */
    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    /**
     * Извлекает имя хоста из полного URL.
     * Удаляет префиксы протокола и конечные слеши.
     * 
     * @param url полный URL, из которого нужно извлечь хост
     * @return строка с именем хоста
     */
    private fun extractHost(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/") // на случай если с хвостом
    }
}