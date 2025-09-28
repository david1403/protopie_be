package com.protopie.service

import com.protopie.controller.dto.LoginResponse
import com.protopie.controller.dto.LoginRequest
import com.protopie.controller.dto.UserModificationRequest
import com.protopie.controller.dto.UserRegistrationRequest
import com.protopie.controller.dto.UserResponse
import com.protopie.dto.exception.DuplicateEmailException
import com.protopie.dto.exception.LoginFailureException
import com.protopie.entity.User
import com.protopie.repository.UserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordConverter: PasswordConverter,
) {
    suspend fun register(userRegistrationRequest: UserRegistrationRequest): User {
        checkDuplicateEmail(userRegistrationRequest.email)

        return userRepository.save(
            User(
                email = userRegistrationRequest.email,
                userName = userRegistrationRequest.userName,
                password = passwordConverter.encrypt(userRegistrationRequest.password),
                role = userRegistrationRequest.role,
            )
        )
    }

    suspend fun login(userLoginDto: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(
            email = userLoginDto.email,
        )?.takeIf { it.password == passwordConverter.encrypt(userLoginDto.password) } ?: throw LoginFailureException("Login failure. check email or password")

        val token = jwtTokenProvider.createToken(user.id!!)
        return LoginResponse(userResponse = UserResponse.convertFromEntity(user), jwtToken = token)
    }

    suspend fun deleteUserById(userId: Long) {
        val user = getUserByIdOrThrow(userId)
        userRepository.delete(user)
    }

    suspend fun modifyUser(userId: Long, userModificationRequest: UserModificationRequest): User {
        val user = getUserByIdOrThrow(userId)
        var updatedUser = user
        userModificationRequest.userName?.let { updatedUser = updatedUser.updateUserName(it) }
        userModificationRequest.password?.let { updatedUser = updatedUser.updatePassword(passwordConverter.encrypt(it)) }
        userModificationRequest.role?.let { updatedUser = updatedUser.updateRole(it) }
        return userRepository.save(user)
    }

    suspend fun getUserByIdOrThrow(userId: Long): User {
        return userRepository.findById(userId) ?: throw IllegalArgumentException("user not found by id: $userId")
    }

    suspend fun findAllWithPagination(pageable: Pageable): Page<UserResponse> {
        val contents = userRepository.findAllBy(pageable).toList()
        val totalCount = userRepository.countAll()

        return PageImpl(contents.map { UserResponse.convertFromEntity(it) }, pageable, totalCount)
    }

    private suspend fun checkDuplicateEmail(email: String) {
        userRepository.findByEmail(email)?.let {
            throw DuplicateEmailException("email already exists")
        }
    }
}