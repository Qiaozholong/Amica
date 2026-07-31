package com.example.aidemotest.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`model`")
public class ModelEntity {
    //对应modelId
    private Long id;
    //模型名称
    private String name;
    //模型提供商
    private String providerId;
    //模型对应Id
    private String modelId;
    //模型API端点
    private String baseUrl;
    //apiKey存储
    private String apiKey;
    //创建时间
    private LocalDateTime createTime;
    //上传时间
    private LocalDateTime updateTime;
}
