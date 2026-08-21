package com.example.Amica.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssistantDto {

    //合理应该从jwt鉴权拿到这个字段，但现在我不想写jwt鉴权，所以暂且只从前端传
    @NotNull(message = "为必填项")
    private Long userId;
    //同样从前端获取，前端应该设计成获取二层表单，首成为provider选择，其次是model选择，选择后会填写模型对应Id，但现在没有前端，只好手传了
    @NotNull(message = "为必填项")
    private Long modelId;
    //助手名称
    @NotBlank(message = "请输入助手名称")
    private String name;
    //助手提示词
    private String prompt;

}
