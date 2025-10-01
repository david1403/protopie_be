package com.protopie.service

import com.protopie.dto.WithdrawMessageDto
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class WithdrawMessageService(
    private val kafkaTemplate: KafkaTemplate<String, WithdrawMessageDto>,
) {
    companion object {
        const val USER_WITHDRAW_TOPIC = "user-withdraw-topic"
    }

    fun sendWithdrawMessage(userId: Long) {
        kafkaTemplate.send(USER_WITHDRAW_TOPIC, WithdrawMessageDto(userId, ZonedDateTime.now()))
    }
}