package com.protopie.controller.dto

import com.protopie.entity.Role

data class UserModificationRequest(
    val userName: String? = null,
    val password: String? = null,
    val role: Role? = null,
)