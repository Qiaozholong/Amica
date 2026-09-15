package com.example.Amica.Vo.ModelRegister;

import lombok.Data;

@Data
public class RegisteredProviderVo {
    //提供商Id
    private Long id;
    //提供商信息
    private String protocol;
    //URL地址
    private String baseUrl;

}
