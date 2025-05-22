package com.getscience.getsciencebackend.user.service.init

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Класс конфигурации для учетных данных модератора.
 * 
 * Содержит email и пароль для создания учетной записи модератора.
 * Используется при инициализации системы для создания аккаунтов модераторов.
 */
data class ModeratorAccountConfig(
    var email: String = "",
    var password: String = ""
)

/**
 * Класс для загрузки и хранения конфигурации модераторов из application.properties.
 * 
 * Свойства конфигурации должны быть определены с префиксом "moderators" в файле properties.
 * Например: moderators.accounts[0].email=admin@example.com
 */
@Component
@ConfigurationProperties(prefix = "moderators")
class ModeratorProperties {
    var accounts: List<ModeratorAccountConfig> = emptyList()
}