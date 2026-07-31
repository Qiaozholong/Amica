package com.example.aidemotest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.aidemotest.Mapper")
public class AiDemoTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDemoTestApplication.class, args);
    }

}
