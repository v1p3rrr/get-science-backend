package com.getscience.getsciencebackend.user.controller

import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.dto.RegisterRequest
import com.getscience.getsciencebackend.user.service.AccountService
import com.getscience.getsciencebackend.user.service.ProfileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/auth")
class AuthController @Autowired constructor(
    private val accountService: AccountService,
    private val profileService: ProfileService
) {

    @PostMapping("/register")
    fun registerUser(@RequestBody registerRequest: RegisterRequest): ResponseEntity<String> {
        val account = accountService.registerUser(registerRequest)
        return ResponseEntity.ok("User registered with ID: ${account.accountId}")
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<String> {
        val token: String = accountService.login(loginRequest)
        return ResponseEntity.ok(token)
    }

    @GetMapping("/profile")
    fun getProfileByEmail(@RequestParam email: String): ResponseEntity<ProfileResponse> {
        val profile = profileService.findByAccountEmail(email)
        return ResponseEntity.ok(profile)
    }

    @GetMapping("/profile/me")
    fun getProfileByJwt(): ResponseEntity<ProfileResponse> {
        val email = getEmailFromToken()
        val profile = profileService.findByAccountEmail(email)
        return ResponseEntity.ok(profile)
    }

    @GetMapping("/profile/{id}")
    fun getProfileById(@PathVariable id: Long): ResponseEntity<ProfileResponse> {
        val profile = profileService.findByProfileId(id)
        return ResponseEntity.ok(profile)
    }

    @PutMapping("/profile/{id}")
    @PostMapping("/profile/{id}/update")
    fun updateProfile(@PathVariable id: Long, @RequestBody profile: ProfileRequest): ResponseEntity<ProfileResponse> {
        val updatedProfile = profileService.updateProfile(profile, getEmailFromToken())
        return ResponseEntity.ok(updatedProfile)
    }


    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }

}