package com.linkcart

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class LinkcartBackendApplication

fun main(args: Array<String>) {
	runApplication<LinkcartBackendApplication>(*args)
}
