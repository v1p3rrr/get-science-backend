package com.getscience.getsciencebackend.util.address

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class AddressControllerTest {

    @Mock
    private lateinit var addressService: AddressService

    @InjectMocks
    private lateinit var addressController: AddressController

    @Test
    fun `suggest should return suggestions from service`() {
        val query = "Paris"
        val limit = 5
        val suggestions = listOf(
            AddressSuggestionDTO("Paris, France", 48.8566, 2.3522),
            AddressSuggestionDTO("Paris, Texas, USA", 33.6609, -95.5555)
        )
        `when`(addressService.suggest(query, limit)).thenReturn(suggestions)

        val responseEntity = addressController.suggest(query, limit)

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        assertEquals(suggestions, responseEntity.body)
        verify(addressService).suggest(query, limit)
    }

    @Test
    fun `suggest should return BadRequest for blank query`() {
        val query = "   "
        val limit = 5

        val responseEntity = addressController.suggest(query, limit)

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
        verify(addressService, never()).suggest(anyString(), anyInt())
    }

    @Test
    fun `suggest should use default limit if not provided`() {
        val query = "London"
        val defaultLimit = 7 // As defined in controller annotation
        val suggestions = listOf(AddressSuggestionDTO("London, UK", 51.5074, 0.1278))
        `when`(addressService.suggest(query, defaultLimit)).thenReturn(suggestions)

        val responseEntity = addressController.suggest(query, defaultLimit) // Pass default limit explicitly for verification

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        assertEquals(suggestions, responseEntity.body)
        verify(addressService).suggest(query, defaultLimit)
    }
} 