package com.example.aidemotest.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelDto {
    //模型名称
    @NotBlank(message = "名称不能为空")
    private String name;
    //模型提供商
    @NotBlank(message ="提供商不能为空")
    private String providerId;
    //模型API端点
    @NotBlank(message = "API端点不能为空")
    private String baseUrl;
    //模型对应Id
    @NotBlank(message = "模型不能为空")
    private String modelId;
    //apiKey存储
    @NotBlank(message = "api密钥不能为空")
    private String apiKey;
}
