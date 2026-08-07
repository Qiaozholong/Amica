package com.example.Aimaca.Dto.ModelRegister;

import lombok.Data;

@Data
public class ProviderDto {
    //模型提供商(使用provider表)
    private String protocol;
    //URL地址(使用provider表)
    private String baseUrl;
}
