package com.qst.campus_errand_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//http://localhost:8080/swagger-ui/index.html
@SpringBootApplication
@MapperScan("com.qst.campus_errand_backend.mapper")
@EnableScheduling
public class CampusErrandBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusErrandBackendApplication.class, args);
    }

}
