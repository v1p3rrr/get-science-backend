package com.getscience.getsciencebackend.user.service.init

import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.RoleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ConfigurableApplicationContext

@ExtendWith(MockitoExtension::class)
class RoleDataInitializerTest {

    @Mock
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var configurableApplicationContext: ConfigurableApplicationContext

    @InjectMocks
    private lateinit var roleDataInitializer: RoleDataInitializer

    @Captor
    private lateinit var roleCaptor: ArgumentCaptor<Role>

    private lateinit var applicationReadyEvent: ApplicationReadyEvent

    @BeforeEach
    fun setUp() {
        // Mock ApplicationReadyEvent if specific context properties are needed, otherwise a simple mock is fine.
        // val mockContext = mock(ConfigurableApplicationContext::class.java) // Not used directly here
        // applicationReadyEvent = ApplicationReadyEvent(SpringApplication(RoleDataInitializer::class.java), emptyArray(), configurableApplicationContext) // Setup for event if needed for all tests
    }

    @Test
    fun `populateRoles should save new roles when they do not exist`() {
        // Arrange
        val roleTypes = RoleType.entries.toList()
        roleTypes.forEach {
            `when`(roleRepository.existsByTitle(it)).thenReturn(false)
        }

        // Act
        roleDataInitializer.run() // Called without arguments

        // Assert
        verify(roleRepository, times(roleTypes.size)).save(roleCaptor.capture())
        val savedRoles = roleCaptor.allValues
        assert(savedRoles.map { it.title }.containsAll(roleTypes))
        roleTypes.forEach { roleType ->
            verify(roleRepository).existsByTitle(roleType)
        }
    }

    @Test
    fun `populateRoles should not save roles when they already exist`() {
        // Arrange
        RoleType.entries.forEach {
            `when`(roleRepository.existsByTitle(it)).thenReturn(true)
        }

        // Act
        roleDataInitializer.run() // Called without arguments

        // Assert
        verify(roleRepository, never()).save(any(Role::class.java))
        RoleType.entries.forEach { roleType ->
            verify(roleRepository).existsByTitle(roleType)
        }
    }

    @Test
    fun `populateRoles saves USER role when it does not exist`() {
        `when`(roleRepository.existsByTitle(RoleType.USER)).thenReturn(false)
        roleDataInitializer.run()
        verify(roleRepository).save(argThat { it.title == RoleType.USER })
    }

    @Test
    fun `populateRoles does not save USER role when it exists`() {
        `when`(roleRepository.existsByTitle(RoleType.USER)).thenReturn(true)
        roleDataInitializer.run()
        verify(roleRepository, never()).save(argThat { it.title == RoleType.USER })
    }
} 