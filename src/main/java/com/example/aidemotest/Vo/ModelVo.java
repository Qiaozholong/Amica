package com.example.aidemotest.Vo;

import lombok.Data;

@Data
public class ModelVo {
    //模型名称
    private String name;
    //模型提供商
    private String providerId;
    //模型API端点
    private String baseUrl;
    //模型对应Id
    private String modelId;
    //对应modelId
    private Long id;
}
