package com.hsf.e_comerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EComerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EComerceApplication.class, args);
	}

}
