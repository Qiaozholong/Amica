package com.example.Amica.Vo;

import lombok.Data;

@Data
public class AssistantVo {
    //助手Id
    private Long id;
    //对应用户Id
    private Long userId;
    //旗下模型Id
    private Long modelId;
    //助手名称
    private String name;
    //助手提示词，应该为可空
    private String prompt;

}
