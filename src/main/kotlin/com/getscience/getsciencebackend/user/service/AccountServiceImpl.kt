package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.security.JWTService
import com.getscience.getsciencebackend.user.data.RoleTypes
import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.dto.RegisterRequest
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.AccountRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JWTService
) : AccountService {

    @Transactional
    override fun registerUser(registerRequest: RegisterRequest): Account {
        accountRepository.findByEmail(registerRequest.email)?.let {
            throw IllegalArgumentException("User with email ${registerRequest.email} already exists")
        }

        val encodedPassword = passwordEncoder.encode(registerRequest.password)

        val account = Account(
            email = registerRequest.email,
            passwordHash = encodedPassword
        )

        val savedAccount = accountRepository.save(account)

        // Fetch the role from the database
        val role = roleRepository.findByTitle(registerRequest.role.toString()) ?: Role(title = registerRequest.role.toString()).also { roleRepository.save(it) }

        val profile = Profile(
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            role = role,
            account = savedAccount
        )

        savedAccount.profile = profile

        return savedAccount
    }

    override fun login(loginRequest: LoginRequest): String {
        val email = loginRequest.email
        val password = loginRequest.password
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, password)
        )
        if (authentication.isAuthenticated) {
            return jwtService.generateToken(email, authentication.authorities)
        } else {
            throw UsernameNotFoundException("Invalid credentials")
        }
    }

    @Cacheable("profile", key = "#email", condition = "#result != null")
    override fun findProfileByEmail(email: String): Profile? {
        return profileRepository.findByAccountEmail(email)
    }

    override fun changePassword(email: String, newPassword: String) : Boolean {
        val account = accountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Account not found")
        val updatedAccount = account.copy(passwordHash = passwordEncoder.encode(newPassword))
        accountRepository.save(updatedAccount)
        return true
    }

    override fun checkIfProfileExists(email: String): Boolean {
        return profileRepository.existsByAccountEmail(email)
    }
}
