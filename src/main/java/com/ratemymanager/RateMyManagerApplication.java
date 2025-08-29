package com.ratemymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RateMyManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RateMyManagerApplication.class, args);
    }
}
