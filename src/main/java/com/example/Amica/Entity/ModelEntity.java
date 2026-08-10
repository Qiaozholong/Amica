package com.example.Amica.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`model`")
//Provider的下级
public class ModelEntity {
    //对应模型Id
    private Long id;
    //模型名称
    private String name;
    //模型提供商Id(用于绑定Provider)
    private Long providerId;
    //模型对应编号(如:DeepSeek-V4-Pro)
    private String modelId;
    //创建时间
    private LocalDateTime createTime;
    //更新时间
    private LocalDateTime updateTime;
}
