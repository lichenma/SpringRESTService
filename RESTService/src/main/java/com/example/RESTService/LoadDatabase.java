package com.example.RESTService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class LoadDatabase {
    @Bean
    CommandLineRunner initDatabase(EmployeeRepository repository){
        return args -> {
            log.info("Preloading "+repository.save(new Employee("Tony Stark", "IT Technician")));
            log.info("Preloading "+repository.save(new Employee("Bruce Wayne", "Business Analyst")));

        };
    }
}
