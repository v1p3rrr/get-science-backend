package com.getscience.getsciencebackend.util.address

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Реализация сервиса для работы с адресами, использующая Photon API.
 * 
 * Предоставляет функциональность для получения подсказок адресов через внешний API Photon.
 * Использует кэширование результатов запросов.
 */
@Service
class PhotonAddressService(
    @Qualifier("photonWebClient")
    private val webClient: WebClient
) : AddressService {
    private val logger = LoggerFactory.getLogger(PhotonAddressService::class.java)

    /**
     * {@inheritDoc}
     *
     * Выполняет запрос к Photon API, фильтрует результаты по типу (страна или город)
     * и формирует список подсказок. Результаты кэшируются.
     */
    @LogBusinessOperation(operationType = "ADDRESS_SUGGEST", description = "Получение подсказок адреса через Photon API")
    @Cacheable(cacheNames = ["addressSuggestions"], key = "#query")
    override fun suggest(query: String, limit: Int): List<AddressSuggestionDTO> {
        logger.info("Запрос к внешнему API Photon с параметрами: query='{}', limit={}", query, limit)
        
        try {
            val response = webClient.get()
                .uri { uri ->
                    uri
                        .path("/api")
                        .queryParam("q", query)
                        .queryParam("lang", "default")
                        .queryParam("limit", 25)
                        .build()
                }
                .retrieve()
                .bodyToMono(PhotonResponse::class.java)
                .onErrorResume { e ->
                    logger.error("Ошибка при запросе к API Photon: {}", e.message, e)
                    Mono.empty()
                }
                .block()
                
            if (response == null) {
                logger.warn("Нет ответа от API Photon")
                return emptyList()
            }
            
            logger.debug("Получен ответ от API Photon: {} объектов", response.features.size)

            val filteredEntries = response.features.filter { entry -> entry.properties.type == "country" || entry.properties.type == "city" }

            val rawSuggestions =  filteredEntries.map { feature ->
                val p = feature.properties
                val coords = feature.geometry.coordinates
                val label = buildLabel(p)
                AddressSuggestionDTO(label, coords[1], coords[0])
            }

            return rawSuggestions.distinctBy { it.label }.take(limit)

        } catch (e: WebClientResponseException) {
            logger.error("Ошибка HTTP при запросе к API Photon: {} {}", e.statusCode, e.statusText, e)
            return emptyList()
        } catch (e: Exception) {
            logger.error("Неожиданная ошибка при запросе к API Photon: {}", e.message, e)
            return emptyList()
        }
    }

    /**
     * Формирует метку (название) адреса на основе данных Photon API.
     *
     * @param p свойства объекта из Photon API
     * @return форматированная метка адреса (город, страна или только страна)
     */
    private fun buildLabel(p: PhotonProperties): String {
        // Если это сама страна (osmValue == "country"), оставляем только страну
        if (p.osmValue == "country") {
            return p.country ?: p.name.orEmpty()
        }
        // Иначе — город + страна
        val city = p.name.orEmpty()
        val country = p.country.orEmpty()
        return listOf(city, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PhotonResponse(
    val features: List<PhotonFeature>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PhotonFeature(
    val geometry: Geometry,
    val properties: PhotonProperties
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Geometry(
    val coordinates: List<Double>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PhotonProperties(
    val name: String?,
    val state: String?,
    val country: String?,
    val type: String?,
    @JsonProperty("osm_key") val osmKey: String?,
    @JsonProperty("osm_value") val osmValue: String?
)
