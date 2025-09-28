package com.protopie.controller.dto

import com.protopie.entity.Role

data class UserRegistrationRequest(
    val email: String,
    val password: String,
    val userName: String,
    val role: Role,
)