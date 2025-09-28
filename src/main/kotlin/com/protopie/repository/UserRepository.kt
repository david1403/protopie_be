package com.protopie.repository

import com.protopie.entity.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<User, Long> {
    suspend fun findByEmail(email: String): User?
    @Query("SELECT * FROM users ORDER BY id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    suspend fun findAllBy(pageable: Pageable): Flow<User>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countAll(): Long
}