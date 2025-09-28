package com.protopie.controller

import com.protopie.controller.dto.LoginResponse
import com.protopie.controller.dto.LoginRequest
import com.protopie.controller.dto.UserModificationRequest
import com.protopie.controller.dto.UserRegistrationRequest
import com.protopie.controller.dto.UserResponse
import com.protopie.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService
) {
    @PostMapping("/signup")
    suspend fun signup(@RequestBody userRegistrationRequest: UserRegistrationRequest): UserResponse {
        return UserResponse.convertFromEntity(
            userService.register(userRegistrationRequest)
        )
    }

    @PostMapping("/signin")
    suspend fun login(@RequestBody userLoginDto: LoginRequest): LoginResponse {
        return userService.login(userLoginDto)
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MEMBER') and #userId == authentication.principal.id)")
    suspend fun deleteUserById(@PathVariable userId: Long) {
        return userService.deleteUserById(userId)
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MEMBER') and #userId == authentication.principal.id)")
    suspend fun modifyUserById(
        @PathVariable userId: Long,
        @RequestBody userModificationRequest: UserModificationRequest
    ): UserResponse {
        return UserResponse.convertFromEntity(
            userService.modifyUser(userId, userModificationRequest)
        )
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MEMBER') and #userId == authentication.principal.id)")
    suspend fun getUserById(@PathVariable userId: Long): UserResponse {
        return UserResponse.convertFromEntity(
            userService.getUserByIdOrThrow(userId)
        )
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun getUsers(
        @PageableDefault(
            sort = ["id"],
            direction = Sort.Direction.ASC
        ) pageable: Pageable,
    ): Page<UserResponse> {
        return userService.findAllWithPagination(pageable)
    }
}