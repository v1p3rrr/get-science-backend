package com.getscience.getsciencebackend.user.service.init

import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import com.getscience.getsciencebackend.user.repository.RoleRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Компонент для автоматического создания учетных записей модераторов при запуске приложения.
 * 
 * Создает аккаунты и профили модераторов на основе конфигурации из ModeratorProperties.
 * Выполняется после инициализации ролей (Order 2).
 */
@Component
@Order(2)
class ModeratorSetupRunner(
    private val moderatorProps: ModeratorProperties,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    /**
     * Выполняет создание учетных записей модераторов при запуске приложения.
     * 
     * Для каждой конфигурации из moderatorProps создает аккаунт и профиль модератора, 
     * если аккаунта с указанным email еще не существует.
     * 
     * @param args аргументы командной строки (не используются)
     * @throws IllegalStateException если роль MODERATOR не найдена в базе данных
     */
    @Transactional
    override fun run(vararg args: String?) {

        val moderatorRole = roleRepository.findByTitle(RoleType.MODERATOR)
            ?: throw IllegalStateException("MODERATOR role not found")

        moderatorProps.accounts.forEach { config ->
            val existing = accountRepository.findByEmail(config.email)
            if (existing == null) {
                val encodedPassword = passwordEncoder.encode(config.password)
                val account = accountRepository.save(
                    Account(email = config.email, passwordHash = encodedPassword, emailConfirmed = true)
                )
                profileRepository.save(
                    Profile(
                        firstName = "Moderator",
                        lastName = "Account",
                        account = account,
                        role = moderatorRole
                    )
                )
                println("Created moderator: ${config.email}")
            }
        }
    }
}