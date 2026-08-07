package com.example.Aimaca;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.Aimaca.Mapper")
public class AimicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimicaApplication.class, args);
    }

}
