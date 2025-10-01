package com.protopie.controller

import com.protopie.controller.dto.LoginRequest
import com.protopie.controller.dto.LoginResponse
import com.protopie.controller.dto.UserModificationRequest
import com.protopie.controller.dto.UserRegistrationRequest
import com.protopie.controller.dto.UserResponse
import com.protopie.dto.exception.LoginFailureException
import com.protopie.entity.Role
import com.protopie.entity.User
import com.protopie.service.UserService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class UserControllerTest {

    private lateinit var userController: UserController
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userController = UserController(userService)
    }

    @Test
    fun `signup should return UserResponse when successful`() = runTest {
        // given
        val request = UserRegistrationRequest(
            email = "test@example.com",
            password = "password123",
            userName = "Test User",
            role = Role.MEMBER
        )
        val user = User(
            id = 1L,
            email = request.email,
            userName = request.userName,
            password = "encrypted",
            role = request.role
        )

        coEvery { userService.register(request) } returns user

        // when
        val result = userController.signup(request)

        // then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(request.email, result.email)
        assertEquals(request.userName, result.userName)
        assertEquals(request.role, result.role)

        coVerify { userService.register(request) }
    }

    @Test
    fun `login should return LoginResponse when credentials are valid`() = runTest {
        // given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )
        val userResponse = UserResponse(
            id = 1L,
            email = loginRequest.email,
            userName = "Test User",
            role = Role.MEMBER
        )
        val loginResponse = LoginResponse(
            userResponse = userResponse,
            jwtToken = "jwt_token"
        )

        coEvery { userService.login(loginRequest) } returns loginResponse

        // when
        val result = userController.login(loginRequest)

        // then
        assertNotNull(result)
        assertEquals("jwt_token", result.jwtToken)
        assertEquals(1L, result.userResponse.id)
        assertEquals(loginRequest.email, result.userResponse.email)

        coVerify { userService.login(loginRequest) }
    }

    @Test
    fun `login should throw LoginFailureException when credentials are invalid`() = runTest {
        // given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "wrong_password"
        )

        coEvery { userService.login(loginRequest) } throws LoginFailureException("Login failure. check email or password")

        // when & then
        assertThrows<LoginFailureException> {
            userController.login(loginRequest)
        }

        coVerify { userService.login(loginRequest) }
    }

    @Test
    fun `deleteUserById should complete successfully`() = runTest {
        // given
        val userId = 1L

        coEvery { userService.deleteUserById(userId) } returns Unit

        // when
        userController.deleteUserById(userId)

        // then
        coVerify { userService.deleteUserById(userId) }
    }

    @Test
    fun `modifyUserById should return updated UserResponse`() = runTest {
        // given
        val userId = 1L
        val modificationRequest = UserModificationRequest(
            userName = "Updated Name",
            password = null,
            role = null
        )
        val updatedUser = User(
            id = userId,
            email = "test@example.com",
            userName = "Updated Name",
            password = "encrypted",
            role = Role.MEMBER
        )

        coEvery { userService.modifyUser(userId, modificationRequest) } returns updatedUser

        // when
        val result = userController.modifyUserById(userId, modificationRequest)

        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("Updated Name", result.userName)

        coVerify { userService.modifyUser(userId, modificationRequest) }
    }

    @Test
    fun `getUserById should return UserResponse`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Test User",
            password = "encrypted",
            role = Role.MEMBER
        )

        coEvery { userService.getUserByIdOrThrow(userId) } returns user

        // when
        val result = userController.getUserById(userId)

        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("Test User", result.userName)

        coVerify { userService.getUserByIdOrThrow(userId) }
    }

    @Test
    fun `getUserById should throw exception when user not found`() = runTest {
        // given
        val userId = 999L

        coEvery { userService.getUserByIdOrThrow(userId) } throws IllegalArgumentException("user not found by id: $userId")

        // when & then
        assertThrows<IllegalArgumentException> {
            userController.getUserById(userId)
        }

        coVerify { userService.getUserByIdOrThrow(userId) }
    }

    @Test
    fun `getUsers should return paginated UserResponse list`() = runTest {
        // given
        val userResponses = listOf(
            UserResponse(1L, "user1@example.com", "User 1", Role.MEMBER),
            UserResponse(2L, "user2@example.com", "User 2", Role.ADMIN),
            UserResponse(3L, "user3@example.com", "User 3", Role.MEMBER)
        )
        val pageable = PageRequest.of(0, 10, Sort.by("id"))
        val page = PageImpl(userResponses, pageable, 3L)

        coEvery { userService.findAllWithPagination(pageable) } returns page

        // when
        val result = userController.getUsers(pageable)

        // then
        assertNotNull(result)
        assertEquals(3, result.content.size)
        assertEquals(3L, result.totalElements)
        assertEquals("user1@example.com", result.content[0].email)
        assertEquals("user2@example.com", result.content[1].email)
        assertEquals("user3@example.com", result.content[2].email)

        coVerify { userService.findAllWithPagination(pageable) }
    }
}