package com.example.Amica.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversationDto {
    //话题从属UserId,同时也应该是从jwt拿的，但我没写哈哈
    @NotNull(message = "不可为空")
    private Long userId;
    //话题从属AssistantId
    @NotNull(message = "不可为空")
    private Long assistantId;
    //话题名称，暂定为默认格式（“话题1”）依次类推，但后续想要加入自动总结话题名的功能
    private String title;
    //对话级prompt，可覆盖assistant自带prompt
    private String systemPrompt;
}
