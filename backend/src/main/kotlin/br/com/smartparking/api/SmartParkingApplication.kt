package br.com.smartparking.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SmartParkingApplication

fun main(args: Array<String>) {
    runApplication<SmartParkingApplication>(*args)
}
