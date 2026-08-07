package com.example.Aimaca.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`provider`")
//Model的上级
public class ProviderEntity {
    //对应提供商Id
    private Long id;
    //对应提供商名称(如:DeepSeek)
    private String name;
    //对应请求体样式(如:OpenAi/AnthropicAi)
    private String protocol;
    //提供商地址
    private String baseUrl;
    //api密钥
    private String apiKey;
    //创建时间
    private LocalDateTime createTime;
    //更新时间
    private LocalDateTime updateTime;
}
