package com.example.aidemotest.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelDto {

    //模型提供商(使用provider表)
    @NotBlank(message = "提供商(如openai/anthropicai/orther)不能为空")
    private String protocol;
    //URL地址(使用provider表)
    @NotBlank(message = "URL地址不能为空")
    private String baseUrl;
    //模型名称(使用model表)
    @NotBlank(message = "模型名称不能为空")
    private String name;
    //模型对应Id(使用model表)
    @NotBlank(message = "模型(如DeepSeek-V4-Pro)不能为空")
    private String modelId;
}
