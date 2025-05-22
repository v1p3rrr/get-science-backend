package com.getscience.getsciencebackend.user.service.init

import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.RoleRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Компонент для автоматической инициализации ролей пользователей при запуске приложения.
 * 
 * Создает записи в базе данных для всех типов ролей, определенных в перечислении RoleType,
 * если они еще не существуют. Выполняется первым (Order 1) среди всех инициализаторов.
 */
@Component
@Order(1)
class RoleDataInitializer(private val roleRepository: RoleRepository) : CommandLineRunner {

    /**
     * Выполняет инициализацию ролей пользователей при запуске приложения.
     * 
     * Создает записи в базе данных для всех элементов перечисления RoleType,
     * если соответствующей роли еще нет в базе данных.
     *
     * @param args аргументы командной строки (не используются)
     */
    @Transactional
    override fun run(vararg args: String?) {
        RoleType.entries.forEach { roleType ->
            if (!roleRepository.existsByTitle(roleType)) {
                roleRepository.save(Role(title = roleType))
            }
        }
    }
}