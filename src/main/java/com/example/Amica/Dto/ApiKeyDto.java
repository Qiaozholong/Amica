package com.example.Amica.Dto;

import lombok.Data;

@Data
public class ApiKeyDto {
    //api密钥
    private String apiKey;
    //提供商Id
    private Long providerId;
}
