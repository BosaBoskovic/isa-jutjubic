package com.jutjubic.jutjubic_backend;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


import org.springframework.scheduling.annotation.EnableAsync;

@EnableRabbit
@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableAsync
public class JutjubicBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JutjubicBackendApplication.class, args);
	}

}
