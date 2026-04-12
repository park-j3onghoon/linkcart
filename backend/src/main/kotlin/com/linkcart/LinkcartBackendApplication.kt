package com.linkcart

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LinkcartBackendApplication

fun main(args: Array<String>) {
	runApplication<LinkcartBackendApplication>(*args)
}
