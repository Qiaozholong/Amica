package com.example.Amica.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`messages`")
public class MessagesEntity {
    //信息id
    private Long id;
    //所属话题Id
    private Long conversationId;
    //信息所属对象,user,assistant
    private String role;
    //消息正文
    private String content;
    //消息生成时间
    private LocalDateTime createTime;
    //消息上传时间，用于修改信息
    private LocalDateTime updateTime;
    //用于逻辑排序
    private int seq;
    //JSON字段
    private String metadata;

}
