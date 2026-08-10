package com.example.Amica;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.Amica.Mapper")
public class Amica {

    public static void main(String[] args) {
        SpringApplication.run(Amica.class, args);
    }

}
