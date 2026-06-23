package com.h3late.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YtStatsServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(YtStatsServiceApplication.class, args);
		System.out.println("YT Stats Service is running...");
	}

}
