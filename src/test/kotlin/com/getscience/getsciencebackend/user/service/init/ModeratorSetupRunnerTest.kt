package com.getscience.getsciencebackend.user.service.init

import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import com.getscience.getsciencebackend.user.repository.RoleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class ModeratorSetupRunnerTest {

    @Mock
    private lateinit var moderatorProps: ModeratorProperties

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var profileRepository: ProfileRepository

    @Mock
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var moderatorSetupRunner: ModeratorSetupRunner

    @Captor
    private lateinit var roleCaptor: ArgumentCaptor<Role>

    @Captor
    private lateinit var accountCaptor: ArgumentCaptor<Account>

    @Captor
    private lateinit var profileCaptor: ArgumentCaptor<Profile>

    @Test
    fun `run should throw IllegalStateException if MODERATOR role not found`() {
        // Arrange
        // Simulate MODERATOR role is not found, even after RoleDataInitializer should have run
        `when`(roleRepository.findByTitle(RoleType.MODERATOR)).thenReturn(null)

        // Act & Assert
        assertThrows<IllegalStateException> {
            moderatorSetupRunner.run()
        }
    }

    @Test
    fun `run should create moderator accounts if they do not exist`() {
        // Arrange
        val moderatorRole = Role(roleId = 1L, title = RoleType.MODERATOR)
        `when`(roleRepository.findByTitle(RoleType.MODERATOR)).thenReturn(moderatorRole)

        val accountConfig1 = ModeratorAccountConfig("mod1@example.com", "pass1")
        val accountConfig2 = ModeratorAccountConfig("mod2@example.com", "pass2")
        `when`(moderatorProps.accounts).thenReturn(listOf(accountConfig1, accountConfig2))

        `when`(accountRepository.findByEmail(accountConfig1.email)).thenReturn(null)
        `when`(accountRepository.findByEmail(accountConfig2.email)).thenReturn(null)

        `when`(passwordEncoder.encode(accountConfig1.password)).thenReturn("encodedPass1")
        `when`(passwordEncoder.encode(accountConfig2.password)).thenReturn("encodedPass2")

        `when`(accountRepository.save(any(Account::class.java))).thenAnswer { it.arguments[0] as Account }

        // Act
        moderatorSetupRunner.run()

        // Assert
        verify(accountRepository, times(2)).save(accountCaptor.capture())
        verify(profileRepository, times(2)).save(profileCaptor.capture())

        val savedAccounts = accountCaptor.allValues
        val savedProfiles = profileCaptor.allValues

        assertEquals(2, savedAccounts.size)
        assertEquals("mod1@example.com", savedAccounts[0].email)
        assertEquals("encodedPass1", savedAccounts[0].passwordHash)
        assertEquals(true, savedAccounts[0].emailConfirmed)

        assertEquals("mod2@example.com", savedAccounts[1].email)
        assertEquals("encodedPass2", savedAccounts[1].passwordHash)
        assertEquals(true, savedAccounts[1].emailConfirmed)

        assertEquals(2, savedProfiles.size)
        assertEquals("Moderator", savedProfiles[0].firstName)
        assertEquals("Account", savedProfiles[0].lastName)
        assertEquals(moderatorRole, savedProfiles[0].role)
        assertEquals(savedAccounts[0], savedProfiles[0].account)

        assertEquals("Moderator", savedProfiles[1].firstName)
        assertEquals(moderatorRole, savedProfiles[1].role)
        assertEquals(savedAccounts[1], savedProfiles[1].account)
    }

    @Test
    fun `run should not create moderator accounts if they already exist`() {
        // Arrange
        val moderatorRole = Role(roleId = 1L, title = RoleType.MODERATOR)
        `when`(roleRepository.findByTitle(RoleType.MODERATOR)).thenReturn(moderatorRole)

        val accountConfig = ModeratorAccountConfig("mod1@example.com", "pass1")
        `when`(moderatorProps.accounts).thenReturn(listOf(accountConfig))

        `when`(accountRepository.findByEmail(accountConfig.email)).thenReturn(Account(email = accountConfig.email, passwordHash = "someHash"))

        // Act
        moderatorSetupRunner.run()

        // Assert
        verify(accountRepository, never()).save(any(Account::class.java))
        verify(profileRepository, never()).save(any(Profile::class.java))
    }
} 