package com.example.Amica.Vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProviderVo {
    //对应提供商Id
    private Long id;
    //对应提供商名称(如:DeepSeek)
    private String name;
    //对应请求体样式(如:OpenAi/AnthropicAi)
    private String protocol;
    //提供商地址
    private String baseUrl;
    //创建时间
    private LocalDateTime createTime;
    //更新时间
    private LocalDateTime updateTime;
}
