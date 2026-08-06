package com.example.aidemotest.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Model_PDto {
    //模型提供商(使用provider表)
    private String protocol;
    //URL地址(使用provider表)
    private String baseUrl;
}
