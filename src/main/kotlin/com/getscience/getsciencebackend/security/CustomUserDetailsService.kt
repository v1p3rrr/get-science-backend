package com.getscience.getsciencebackend.security

import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.repository.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Сервис для загрузки пользовательских данных для аутентификации в Spring Security.
 * 
 * Извлекает данные пользователя из базы данных и преобразует их в объект UserDetails,
 * используемый Spring Security для аутентификации и авторизации.
 */
@Service
class CustomUserDetailsService @Autowired constructor(val accountRepository: AccountRepository) : UserDetailsService {

    /**
     * Загружает пользователя по email для аутентификации.
     * 
     * @param email email пользователя (используется вместо username)
     * @return объект UserDetails с данными пользователя
     * @throws UsernameNotFoundException если пользователь с указанным email не найден
     */
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): UserDetails {
        val account: Account = accountRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")

        return User.builder()
            .username(account.email)
            .password(account.passwordHash)
            .authorities(getAuthorities(account))
            .build()
    }

    /**
     * Получает список ролей пользователя для Spring Security.
     * 
     * @param account аккаунт пользователя
     * @return коллекция прав доступа на основе роли пользователя
     */
    private fun getAuthorities(account: Account): Collection<GrantedAuthority> {
        val authorities: MutableList<GrantedAuthority> = ArrayList()
        authorities.add(SimpleGrantedAuthority(account.profile?.role?.title?.name))
        return authorities
    }
}