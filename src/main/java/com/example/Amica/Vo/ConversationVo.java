package com.example.Amica.Vo;

import lombok.Data;

@Data
public class ConversationVo {
    //话题Id
    private Long id;
    //话题名
    private String title;
    //对话级prompt覆盖状态
    private String status;

}
