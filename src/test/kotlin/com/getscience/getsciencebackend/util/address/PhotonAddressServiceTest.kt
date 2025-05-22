package com.getscience.getsciencebackend.util.address

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import java.util.function.Function
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.nio.charset.Charset

// Re-declaring DTOs here for test clarity if they are not complex, 
// or ensure they are accessible. Assuming they are simple data classes.

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestPhotonResponse(val features: List<TestPhotonFeature>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestPhotonFeature(val geometry: TestGeometry, val properties: TestPhotonProperties)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestGeometry(val coordinates: List<Double>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestPhotonProperties(
    val name: String?,
    val state: String?,
    val country: String?,
    val type: String?,
    @JsonProperty("osm_key") val osmKey: String?,
    @JsonProperty("osm_value") val osmValue: String?
)

@ExtendWith(MockitoExtension::class)
class PhotonAddressServiceTest {

    @Mock
    private lateinit var webClient: WebClient
    @Mock
    private lateinit var requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*>
    @Mock
    private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>
    @Mock
    private lateinit var responseSpec: WebClient.ResponseSpec
    @Mock
    private lateinit var restTemplate: RestTemplate
    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var photonAddressService: PhotonAddressService

    @BeforeEach
    fun setUp() {
        `when`(webClient.get()).thenReturn(requestHeadersUriSpec)
        `when`(requestHeadersUriSpec.uri(any<Function<UriBuilder, URI>>())).thenReturn(requestHeadersSpec)
        `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `suggest should return mapped and filtered suggestions on successful API call`() {
        val query = "Berlin"
        val limit = 5
        val mockPhotonResponse = PhotonResponse(
            features = listOf(
                PhotonFeature(
                    Geometry(listOf(13.4050, 52.5200)),
                    PhotonProperties("Berlin", null, "Germany", "city", "place", "city")
                ),
                PhotonFeature(
                    Geometry(listOf(2.3522, 48.8566)),
                    PhotonProperties("Paris", null, "France", "city", "place", "city")
                ),
                 PhotonFeature( // This one should be filtered out by type
                    Geometry(listOf(10.0, 10.0)),
                    PhotonProperties("Some Street", null, "Germany", "street", "highway", "residential")
                ),
                PhotonFeature( // This is a country type
                    Geometry(listOf(10.0, 10.0)),
                    PhotonProperties("Germany", null, "Germany", "country", "place", "country")
                )
            )
        )
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.just(mockPhotonResponse))

        val suggestions = photonAddressService.suggest(query, limit)

        assertEquals(3, suggestions.size) // Berlin, Paris, Germany
        assertEquals("Berlin, Germany", suggestions[0].label)
        assertEquals(52.5200, suggestions[0].latitude)
        assertEquals(13.4050, suggestions[0].longitude)
        assertEquals("Paris, France", suggestions[1].label)
        assertEquals("Germany", suggestions[2].label) // Country only label
    }

    @Test
    fun `suggest should return empty list on WebClientResponseException`() {
        val query = "ErrorCity"
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error", HttpHeaders(), byteArrayOf(), Charset.defaultCharset())))

        val suggestions = photonAddressService.suggest(query, 2)

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `suggest should return empty list on generic exception during API call`() {
        val query = "ExceptionCity"
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.error(RuntimeException("Unexpected error")))

        val suggestions = photonAddressService.suggest(query, 2)

        assertTrue(suggestions.isEmpty())
    }
    
    @Test
    fun `suggest should return empty list when Photon API returns null response`() {
        val query = "NullResponseCity"
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.empty()) // Simulates null block()

        val suggestions = photonAddressService.suggest(query, 2)

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `suggest should correctly build labels`() {
        val query = "TestLabels"
        val mockPhotonResponse = PhotonResponse(
            features = listOf(
                PhotonFeature( // City with country
                    Geometry(listOf(1.0, 1.0)),
                    PhotonProperties("CityA", "StateA", "CountryA", "city", "place", "city")
                ),
                PhotonFeature( // Country only
                    Geometry(listOf(2.0, 2.0)),
                    PhotonProperties("CountryB", null, "CountryB", "country", "place", "country")
                ),
                PhotonFeature( // City with no country in properties, but name might be country like
                    Geometry(listOf(3.0, 3.0)),
                    PhotonProperties("CityC_CountryC", null, null, "city", "place", "city")
                )
            )
        )
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.just(mockPhotonResponse))

        val suggestions = photonAddressService.suggest(query, 3)

        assertEquals(3, suggestions.size)
        assertEquals("CityA, CountryA", suggestions[0].label)
        assertEquals("CountryB", suggestions[1].label)
        assertEquals("CityC_CountryC", suggestions[2].label) // name used if country is null
    }

     @Test
    fun `suggest should handle distinct labels and limit`() {
        val query = "DistinctTest"
        val mockPhotonResponse = PhotonResponse(
            features = listOf(
                PhotonFeature(Geometry(listOf(1.0, 1.0)), PhotonProperties("Berlin", null, "Germany", "city", "place", "city")),
                PhotonFeature(Geometry(listOf(2.0, 2.0)), PhotonProperties("Berlin", null, "Germany", "city", "place", "city")), // Duplicate
                PhotonFeature(Geometry(listOf(3.0, 3.0)), PhotonProperties("Paris", null, "France", "city", "place", "city")),
                PhotonFeature(Geometry(listOf(4.0, 4.0)), PhotonProperties("London", null, "UK", "city", "place", "city"))
            )
        )
        `when`(responseSpec.bodyToMono(PhotonResponse::class.java)).thenReturn(Mono.just(mockPhotonResponse))

        val suggestions = photonAddressService.suggest(query, 2) // Limit to 2

        assertEquals(2, suggestions.size)
        assertEquals("Berlin, Germany", suggestions[0].label)
        assertEquals("Paris, France", suggestions[1].label) // London should be cut off by limit
    }

} 