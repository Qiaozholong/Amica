package com.example.Aimaca.Entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`assistant`")
//构思中，相当于自定义助手模板，与user对应，在系统设置修改
public class AssistantEntity {
    //助手ID
    private Long id;
    //创建用户ID
    private Long userId;
    //助手使用modelID
    private Long modelId;
    //助手名称
    private String name;
    //prompt模板
    private String prompt;
    //创建时间
    private LocalDateTime createTime;
    //上传时间
    private LocalDateTime updateTime;
}
