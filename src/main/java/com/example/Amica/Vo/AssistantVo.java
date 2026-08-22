package com.example.Amica.Vo;

import lombok.Data;

@Data
public class AssistantVo {
    //助手Id
    private Long assistantId;
    //助手从属用户
    private String userName;
    //助手从属模型
    private String modelName;
    //助手名
    private String name;
    //助手提示词，应该为可空
    private String prompt;

}
