package com.getscience.getsciencebackend.user.service

import com.getscience.getsciencebackend.user.data.dto.LoginRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileRequest
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import com.getscience.getsciencebackend.user.data.dto.RegisterRequest
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile

interface AccountService {
    fun registerUser(registerRequest: RegisterRequest): Account
    fun login(loginRequest: LoginRequest): String
    fun findProfileByEmail(email: String): Profile?
    fun changePassword(email: String, newPassword: String): Boolean
    fun checkIfProfileExists(email: String): Boolean
}
