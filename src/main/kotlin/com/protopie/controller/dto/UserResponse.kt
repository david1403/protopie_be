package com.protopie.controller.dto

import com.protopie.entity.Role
import com.protopie.entity.User

data class UserResponse(
    val id: Long,
    val email: String,
    val userName: String,
    val role: Role,
) {
    companion object {
        fun convertFromEntity(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                userName = user.userName,
                role = user.role,
            )
        }
    }
}