package com.example.Graduation_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class Graduation_projectApplication {

    public static void main(String[] args) {
        SpringApplication.run(Graduation_projectApplication.class, args);
    }
}
