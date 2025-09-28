package com.protopie.controller.dto

data class LoginResponse(
    val userResponse: UserResponse,
    val jwtToken: String,
)