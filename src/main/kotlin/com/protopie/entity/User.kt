package com.protopie.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "users")
data class User(
    @Id val id: Long? = null,
    val email: String,
    val userName: String,
    val password: String,
    val role: Role,
) {
    fun updateUserName(newName: String): User {
        return this.copy(userName = newName)
    }

    fun updatePassword(newPassword: String): User {
        return this.copy(password = newPassword)
    }

    fun updateRole(newRole: Role): User {
        return this.copy(role = newRole)
    }
}

enum class Role {
    ADMIN, MEMBER
}