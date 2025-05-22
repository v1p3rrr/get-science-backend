package com.getscience.getsciencebackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class GetScienceBackendApplication

fun main(args: Array<String>) {
    runApplication<GetScienceBackendApplication>(*args)
}
