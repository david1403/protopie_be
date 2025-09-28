package com.protopie

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProtopieApplication

fun main(args: Array<String>) {
    runApplication<ProtopieApplication>(*args)
}
