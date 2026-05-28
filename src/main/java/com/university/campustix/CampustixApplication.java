package com.university.campustix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CampustixApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampustixApplication.class, args);
	}

}
