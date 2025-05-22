package com.getscience.getsciencebackend.util.address

/**
 * Сервис для работы с адресами.
 * 
 * Предоставляет функциональность для поиска и получения подсказок адресов.
 */
interface AddressService {
    /**
     * Получает список подсказок адресов на основе поискового запроса.
     *
     * @param query текстовый запрос для поиска адреса
     * @param limit максимальное количество возвращаемых подсказок (по умолчанию 7)
     * @return список подсказок адресов
     */
    fun suggest(query: String, limit: Int = 7): List<AddressSuggestionDTO>
}