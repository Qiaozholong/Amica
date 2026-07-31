package com.example.aidemotest.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`conversation`")
public class ConversationEntity {
    //话题Id
    private Long id;
    //话题所属user
    private Long userId;
    //话题所属assistant
    private Long assistantId;
    //话题标题
    private String title;
    //系统级prompt
    private String systemPrompt;
    //对应model
    private String model;
    //JSON字段
    private String metadata;
    //创建时间
    private LocalDateTime createTime;
    //上传时间
    private LocalDateTime updateTime;

}
