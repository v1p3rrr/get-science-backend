package com.getscience.getsciencebackend.util.address

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/address")
class AddressController(
    private val addressService: AddressService
) {

    /**
     * Получает список подсказок адресов на основе поискового запроса.
     * 
     * @param query текстовый запрос для поиска адреса
     * @param limit максимальное количество возвращаемых подсказок (по умолчанию 7)
     * @return список подсказок адресов или HTTP 400, если запрос пустой
     */
    @GetMapping("/suggest")
    fun suggest(
        @RequestParam query: String,
        @RequestParam(defaultValue = "7") limit: Int
    ): ResponseEntity<List<AddressSuggestionDTO>> {
        if (query.isBlank()) return ResponseEntity.badRequest().build()
        val suggestions = addressService.suggest(query, limit)
        return ResponseEntity.ok(suggestions)
    }
}