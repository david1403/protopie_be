package com.protopie.service

import com.protopie.controller.dto.LoginRequest
import com.protopie.controller.dto.UserModificationRequest
import com.protopie.controller.dto.UserRegistrationRequest
import com.protopie.dto.exception.DuplicateEmailException
import com.protopie.dto.exception.LoginFailureException
import com.protopie.dto.exception.UserNotFoundException
import com.protopie.entity.Role
import com.protopie.entity.User
import com.protopie.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class UserServiceTest {
    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var passwordConverter: PasswordConverter
    private lateinit var withdrawMessageService: WithdrawMessageService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        jwtTokenProvider = mockk()
        passwordConverter = mockk()
        withdrawMessageService = mockk(relaxed = true)
        userService = UserService(userRepository, jwtTokenProvider, passwordConverter, withdrawMessageService)
    }

    @Test
    fun `register should create new user successfully`() = runTest {
        // given
        val request = UserRegistrationRequest(
            email = "test@example.com",
            password = "password123",
            userName = "Test User",
            role = Role.MEMBER
        )
        val encryptedPassword = "encrypted_password"
        val savedUser = User(
            id = 1L,
            email = request.email,
            userName = request.userName,
            password = encryptedPassword,
            role = request.role
        )

        coEvery { userRepository.findByEmail(request.email) } returns null
        coEvery { passwordConverter.encrypt(request.password) } returns encryptedPassword
        coEvery { userRepository.save(any()) } returns savedUser

        // when
        val result = userService.register(request)

        // then
        assertNotNull(result)
        assertEquals(savedUser.id, result.id)
        assertEquals(request.email, result.email)
        assertEquals(request.userName, result.userName)
        assertEquals(encryptedPassword, result.password)

        coVerify { userRepository.findByEmail(request.email) }
        coVerify { passwordConverter.encrypt(request.password) }
        coVerify { userRepository.save(any()) }
    }

    @Test
    fun `register should throw DuplicateEmailException when email already exists`() = runTest {
        // given
        val request = UserRegistrationRequest(
            email = "existing@example.com",
            password = "password123",
            userName = "Test User",
            role = Role.MEMBER
        )
        val existingUser = User(
            id = 1L,
            email = request.email,
            userName = "Existing User",
            password = "encrypted",
            role = Role.MEMBER
        )

        coEvery { userRepository.findByEmail(request.email) } returns existingUser

        // when & then
        assertThrows<DuplicateEmailException> {
            userService.register(request)
        }

        coVerify { userRepository.findByEmail(request.email) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login should return LoginResponse with valid credentials`() = runTest {
        // given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )
        val encryptedPassword = "encrypted_password"
        val user = User(
            id = 1L,
            email = loginRequest.email,
            userName = "Test User",
            password = encryptedPassword,
            role = Role.MEMBER
        )
        val token = "jwt_token"

        coEvery { passwordConverter.encrypt(loginRequest.password) } returns encryptedPassword
        coEvery { userRepository.findByEmail(loginRequest.email) } returns user
        coEvery { jwtTokenProvider.createToken(user.id!!) } returns token

        // when
        val result = userService.login(loginRequest)

        // then
        assertNotNull(result)
        assertEquals(token, result.jwtToken)
        assertEquals(user.id, result.userResponse.id)
        assertEquals(user.email, result.userResponse.email)

        coVerify { userRepository.findByEmail(loginRequest.email) }
        coVerify { passwordConverter.encrypt(loginRequest.password) }
        coVerify { jwtTokenProvider.createToken(user.id!!) }
    }

    @Test
    fun `login should throw LoginFailureException when user not found`() = runTest {
        // given
        val loginRequest = LoginRequest(
            email = "nonexistent@example.com",
            password = "password123"
        )

        coEvery { passwordConverter.encrypt(loginRequest.password) } returns "encrypted"
        coEvery { userRepository.findByEmail(loginRequest.email) } returns null

        // when & then
        assertThrows<LoginFailureException> {
            userService.login(loginRequest)
        }

        coVerify { userRepository.findByEmail(loginRequest.email) }
    }

    @Test
    fun `login should throw LoginFailureException when password is incorrect`() = runTest {
        // given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "wrong_password"
        )
        val user = User(
            id = 1L,
            email = loginRequest.email,
            userName = "Test User",
            password = "correct_encrypted_password",
            role = Role.MEMBER
        )

        coEvery { passwordConverter.encrypt(loginRequest.password) } returns "wrong_encrypted_password"
        coEvery { userRepository.findByEmail(loginRequest.email) } returns user

        // when & then
        assertThrows<LoginFailureException> {
            userService.login(loginRequest)
        }

        coVerify { userRepository.findByEmail(loginRequest.email) }
    }

    @Test
    fun `deleteUserById should delete user successfully`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Test User",
            password = "encrypted",
            role = Role.MEMBER
        )

        coEvery { userRepository.findById(userId) } returns user
        coEvery { userRepository.delete(user) } returns Unit
        coEvery { withdrawMessageService.sendWithdrawMessage(userId) } returns Unit

        // when
        userService.deleteUserById(userId)

        // then
        coVerify { userRepository.findById(userId) }
        coVerify { userRepository.delete(user) }
        coVerify { withdrawMessageService.sendWithdrawMessage(userId) }
    }

    @Test
    fun `deleteUserById should throw exception when user not found`() = runTest {
        // given
        val userId = 999L

        coEvery { userRepository.findById(userId) } returns null

        // when & then
        assertThrows<UserNotFoundException> {
            userService.deleteUserById(userId)
        }

        coVerify { userRepository.findById(userId) }
        coVerify(exactly = 0) { userRepository.delete(any()) }
    }

    @Test
    fun `modifyUser should update user name`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Old Name",
            password = "encrypted",
            role = Role.MEMBER
        )
        val modificationRequest = UserModificationRequest(
            userName = "New Name",
            password = null,
            role = null
        )

        coEvery { userRepository.findById(userId) } returns user
        coEvery { userRepository.save(any()) } returns user

        // when
        userService.modifyUser(userId, modificationRequest)

        // then
        coVerify { userRepository.findById(userId) }
        coVerify { userRepository.save(user) }
    }

    @Test
    fun `modifyUser should update password`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Test User",
            password = "old_encrypted",
            role = Role.MEMBER
        )
        val newPassword = "new_password"
        val newEncryptedPassword = "new_encrypted_password"
        val modificationRequest = UserModificationRequest(
            userName = null,
            password = newPassword,
            role = null
        )

        coEvery { userRepository.findById(userId) } returns user
        coEvery { passwordConverter.encrypt(newPassword) } returns newEncryptedPassword
        coEvery { userRepository.save(any()) } returns user

        // when
        userService.modifyUser(userId, modificationRequest)

        // then
        coVerify { userRepository.findById(userId) }
        coVerify { passwordConverter.encrypt(newPassword) }
        coVerify { userRepository.save(user) }
    }

    @Test
    fun `modifyUser should update role`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Test User",
            password = "encrypted",
            role = Role.MEMBER
        )
        val modificationRequest = UserModificationRequest(
            userName = null,
            password = null,
            role = Role.ADMIN
        )

        coEvery { userRepository.findById(userId) } returns user
        coEvery { userRepository.save(any()) } returns user

        // when
        userService.modifyUser(userId, modificationRequest)

        // then
        coVerify { userRepository.findById(userId) }
        coVerify { userRepository.save(user) }
    }

    @Test
    fun `getUserByIdOrThrow should return user when found`() = runTest {
        // given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            userName = "Test User",
            password = "encrypted",
            role = Role.MEMBER
        )

        coEvery { userRepository.findById(userId) } returns user

        // when
        val result = userService.getUserByIdOrThrow(userId)

        // then
        assertNotNull(result)
        assertEquals(user.id, result.id)
        assertEquals(user.email, result.email)

        coVerify { userRepository.findById(userId) }
    }

    @Test
    fun `getUserByIdOrThrow should throw exception when user not found`() = runTest {
        // given
        val userId = 999L

        coEvery { userRepository.findById(userId) } returns null

        // when & then
        assertThrows<UserNotFoundException> {
            userService.getUserByIdOrThrow(userId)
        }

        coVerify { userRepository.findById(userId) }
    }

    @Test
    fun `findAllWithPagination should return paginated users`() = runTest {
        // given
        val pageable = PageRequest.of(0, 10, Sort.by("id"))
        val users = listOf(
            User(1L, "user1@example.com", "User 1", "encrypted1", Role.MEMBER),
            User(2L, "user2@example.com", "User 2", "encrypted2", Role.ADMIN),
            User(3L, "user3@example.com", "User 3", "encrypted3", Role.MEMBER)
        )
        val totalCount = 3L

        coEvery { userRepository.findAllBy(pageable) } returns flowOf(*users.toTypedArray())
        coEvery { userRepository.countAll() } returns totalCount

        // when
        val result = userService.findAllWithPagination(pageable)

        // then
        assertNotNull(result)
        assertEquals(3, result.content.size)
        assertEquals(totalCount, result.totalElements)
        assertEquals("user1@example.com", result.content[0].email)
        assertEquals("user2@example.com", result.content[1].email)
        assertEquals("user3@example.com", result.content[2].email)

        coVerify { userRepository.findAllBy(pageable) }
        coVerify { userRepository.countAll() }
    }

    @Test
    fun `findAllWithPagination should return empty page when no users exist`() = runTest {
        // given
        val pageable = PageRequest.of(0, 10, Sort.by("id"))

        coEvery { userRepository.findAllBy(pageable) } returns flowOf()
        coEvery { userRepository.countAll() } returns 0L

        // when
        val result = userService.findAllWithPagination(pageable)

        // then
        assertNotNull(result)
        assertEquals(0, result.content.size)
        assertEquals(0L, result.totalElements)

        coVerify { userRepository.findAllBy(pageable) }
        coVerify { userRepository.countAll() }
    }
}