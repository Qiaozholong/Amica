package com.example.Amica.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiKeyDto {
    //api密钥
    @NotBlank(message = "密钥不能为空,请填充api密钥")
    private String apiKey;
    //提供商Id
    @NotNull(message = "提供商编号为空")
    private Long providerId;
}
