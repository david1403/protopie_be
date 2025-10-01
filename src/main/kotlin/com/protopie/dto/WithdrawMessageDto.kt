package com.protopie.dto

import java.time.ZonedDateTime

data class WithdrawMessageDto(
    val userId: Long,
    val withdrawAt: ZonedDateTime,
)