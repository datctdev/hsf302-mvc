package com.hsf.e_comerce;

import com.hsf.e_comerce.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class EComerceApplication {

	public static void main(String[] args) {
		DotEnvConfig.loadDotEnv();
		SpringApplication.run(EComerceApplication.class, args);
	}

}
