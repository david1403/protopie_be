package com.protopie.controller.advice

import com.protopie.controller.UserController
import com.protopie.dto.exception.DuplicateEmailException
import com.protopie.dto.exception.LoginFailureException
import com.protopie.dto.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [UserController::class])
class UserControllerAdvice {

    @ExceptionHandler(
        value = [
            DuplicateEmailException::class,
            LoginFailureException::class,
            UserNotFoundException::class,
        ]
    )
    fun handleUserException(ex: RuntimeException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)
    }

    @ExceptionHandler(Exception::class)
    fun handleServerException(ex: Exception): ResponseEntity<String> {
        //add logging for ex.message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase)
    }
}