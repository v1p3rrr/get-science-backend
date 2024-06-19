package com.getscience.getsciencebackend.config

import com.getscience.getsciencebackend.security.JWTAuthFilter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration


@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: UserDetailsService,
//                     private val corsConfigurationSource: CorsConfigurationSource
) {

//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .authorizeRequests { requests ->
//                requests
//                    .requestMatchers(AntPathRequestMatcher("/public/**")).permitAll()
//                    .anyRequest().permitAll()
//            }
//            .csrf { csrf -> csrf.disable() }
//            .cors { }
//        return http.build()
//    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthFilter: JWTAuthFilter?): SecurityFilterChain {
        return http
            .csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .cors { httpSecurityCorsConfigurer: CorsConfigurer<HttpSecurity?> ->
                httpSecurityCorsConfigurer.configurationSource { request: HttpServletRequest? -> CorsConfiguration().applyPermitDefaultValues() }
            }
            .authorizeHttpRequests { auth: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry ->
                auth
//                    .requestMatchers("/api/v1/applications/**").authenticated()
                    .requestMatchers("/api/v1/file-applications/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }


    @Bean
    fun authenticationProvider(
        passwordEncoder: PasswordEncoder?
    ): AuthenticationProvider {
        val authenticationProvider = DaoAuthenticationProvider()
        authenticationProvider.setUserDetailsService(userDetailsService)
        authenticationProvider.setPasswordEncoder(passwordEncoder)
        return authenticationProvider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

}