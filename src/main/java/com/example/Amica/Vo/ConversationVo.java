package com.example.Amica.Vo;

import lombok.Data;

@Data
public class ConversationVo {
    //话题Id
    private Long id;
    //话题所属userId
    private Long userId;
    //话题所属assistantId
    private Long assistantId;
    //话题所属prompt
    private String systemPrompt;
    //话题名
    private String title;
    //对话级prompt覆盖状态
    private String status;

}
