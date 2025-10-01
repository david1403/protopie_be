package com.protopie.controller

import com.ninjasquad.springmockk.MockkBean
import com.protopie.entity.Role
import com.protopie.entity.User
import com.protopie.repository.UserRepository
import com.protopie.service.JwtTokenProvider
import com.protopie.service.WithdrawMessageService
import io.mockk.coEvery
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Value("\${app.jwt.secret}")
    private lateinit var jwtSecret: String

    @MockkBean
    private lateinit var userRepository: UserRepository

    @MockkBean
    private lateinit var withdrawMessageService: WithdrawMessageService

    private lateinit var adminUser: User
    private lateinit var memberUser: User
    private lateinit var otherMemberUser: User
    private lateinit var adminToken: String
    private lateinit var memberToken: String
    private lateinit var otherMemberToken: String

    @BeforeEach
    fun setUp() {
        // Mock users
        adminUser = User(
            id = 1L,
            email = "admin@example.com",
            userName = "Admin User",
            password = "encrypted_password",
            role = Role.ADMIN
        )

        memberUser = User(
            id = 2L,
            email = "member@example.com",
            userName = "Member User",
            password = "encrypted_password",
            role = Role.MEMBER
        )

        otherMemberUser = User(
            id = 3L,
            email = "other@example.com",
            userName = "Other Member",
            password = "encrypted_password",
            role = Role.MEMBER
        )

        // Generate JWT tokens
        adminToken = jwtTokenProvider.createToken(adminUser.id!!)
        memberToken = jwtTokenProvider.createToken(memberUser.id!!)
        otherMemberToken = jwtTokenProvider.createToken(otherMemberUser.id!!)

        // Setup repository mocks
        coEvery { userRepository.findById(1L) } returns adminUser
        coEvery { userRepository.findById(2L) } returns memberUser
        coEvery { userRepository.findById(3L) } returns otherMemberUser
    }


    @Test
    fun `getUserById should return 401 when no authorization header`() {
        webTestClient.get()
            .uri("/users/2")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").isEqualTo("Unauthorized")
    }

    @Test
    fun `getUserById should return 401 when invalid JWT token`() {
        webTestClient.get()
            .uri("/users/2")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").isEqualTo("Unauthorized")
    }

    @Test
    fun `getUserById should return 401 when expired JWT token`() = runTest {
        // Create an expired token by using negative validity period
        val expiredTokenProvider = JwtTokenProvider(
            secretKey = jwtSecret,
            validityInMilliseconds = -1L  // Negative value creates already expired token
        )
        val expiredToken = expiredTokenProvider.createToken(memberUser.id!!)

        webTestClient.get()
            .uri("/users/2")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").isEqualTo("Unauthorized")
    }

    @Test
    fun `deleteUserById should return 401 when no authorization header`() {
        webTestClient.delete()
            .uri("/users/2")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `modifyUserById should return 401 when no authorization header`() {
        webTestClient.put()
            .uri("/users/2")
            .bodyValue(mapOf("userName" to "New Name"))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `getUsers should return 401 when no authorization header`() {
        webTestClient.get()
            .uri("/users")
            .exchange()
            .expectStatus().isUnauthorized
    }


    @Test
    fun `getUserById should return 403 when MEMBER tries to access other user's data`() {
        webTestClient.get()
            .uri("/users/3")  // memberUser (id=2) trying to access otherMemberUser (id=3)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
            .jsonPath("$.error").isEqualTo("FORBIDDEN")
            .jsonPath("$.message").isEqualTo("Forbidden")
    }

    @Test
    fun `deleteUserById should return 403 when MEMBER tries to delete other user`() {
        coEvery { userRepository.delete(any()) } returns Unit
        coEvery { withdrawMessageService.sendWithdrawMessage(any()) } returns Unit

        webTestClient.delete()
            .uri("/users/3")  // memberUser (id=2) trying to delete otherMemberUser (id=3)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
            .jsonPath("$.error").isEqualTo("FORBIDDEN")
            .jsonPath("$.message").isEqualTo("Forbidden")
    }

    @Test
    fun `modifyUserById should return 403 when MEMBER tries to modify other user`() {
        webTestClient.put()
            .uri("/users/3")  // memberUser (id=2) trying to modify otherMemberUser (id=3)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .bodyValue(mapOf("userName" to "New Name"))
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
            .jsonPath("$.error").isEqualTo("FORBIDDEN")
            .jsonPath("$.message").isEqualTo("Forbidden")
    }

    @Test
    fun `getUsers should return 403 when MEMBER tries to access admin endpoint`() {
        webTestClient.get()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
            .jsonPath("$.error").isEqualTo("FORBIDDEN")
            .jsonPath("$.message").isEqualTo("Forbidden")
    }

    // ==================== 200 SUCCESS Tests ====================

    @Test
    fun `getUserById should return 200 when ADMIN accesses any user's data`() {
        webTestClient.get()
            .uri("/users/2")  // Admin accessing member's data
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(2)
            .jsonPath("$.email").isEqualTo("member@example.com")
            .jsonPath("$.userName").isEqualTo("Member User")
            .jsonPath("$.role").isEqualTo("MEMBER")
    }

    @Test
    fun `getUserById should return 200 when MEMBER accesses their own data`() {
        webTestClient.get()
            .uri("/users/2")  // memberUser accessing their own data
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(2)
            .jsonPath("$.email").isEqualTo("member@example.com")
            .jsonPath("$.userName").isEqualTo("Member User")
            .jsonPath("$.role").isEqualTo("MEMBER")
    }

    @Test
    fun `deleteUserById should return 200 when ADMIN deletes any user`() {
        coEvery { userRepository.delete(any()) } returns Unit
        coEvery { withdrawMessageService.sendWithdrawMessage(any()) } returns Unit

        webTestClient.delete()
            .uri("/users/2")  // Admin deleting member
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `deleteUserById should return 200 when MEMBER deletes their own account`() {
        coEvery { userRepository.delete(any()) } returns Unit
        coEvery { withdrawMessageService.sendWithdrawMessage(any()) } returns Unit

        webTestClient.delete()
            .uri("/users/2")  // memberUser deleting themselves
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `modifyUserById should return 200 when ADMIN modifies any user`() {
        val updatedUser = memberUser.copy(userName = "Updated Name")
        coEvery { userRepository.save(any()) } returns updatedUser

        webTestClient.put()
            .uri("/users/2")  // Admin modifying member
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .bodyValue(mapOf("userName" to "Updated Name"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.userName").isEqualTo("Updated Name")
    }

    @Test
    fun `modifyUserById should return 200 when MEMBER modifies their own account`() {
        val updatedUser = memberUser.copy(userName = "My New Name")
        coEvery { userRepository.save(any()) } returns updatedUser

        webTestClient.put()
            .uri("/users/2")  // memberUser modifying themselves
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .bodyValue(mapOf("userName" to "My New Name"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.userName").isEqualTo("My New Name")
    }

    @Test
    fun `getUsers should return 200 when ADMIN accesses user list`() {
        coEvery { userRepository.findAllBy(any()) } returns kotlinx.coroutines.flow.flowOf(
            adminUser,
            memberUser,
            otherMemberUser
        )
        coEvery { userRepository.countAll() } returns 3L

        webTestClient.get()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isArray
            .jsonPath("$.totalElements").isEqualTo(3)
    }

}