package com.johnmanko.portfolio.microserviceperformance;

import org.springframework.boot.SpringApplication;

public class TestMicroservicePerformanceApplication {

	public static void main(String[] args) {
		SpringApplication.from(MicroservicePerformanceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
