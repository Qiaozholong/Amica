package com.example.aidemotest.Vo;

import lombok.Data;

@Data
public class AModelVo {
    //模型名称
    private String name;
    //模型对应编号
    private String modelId;
    //提供商信息
    private String protocol;
    //URL地址
    private String baseUrl;
    //提供商Id
    private Long providerId;
}
