package com.example.Amica.Dto.Messages;

import lombok.Data;

@Data
public class OptionsDto {
    //模型温度
    private Double temperature;
    //topP值
    private Double topP;
    //思考深度
    private String reasoningEffort;
    //流式传输
    private Boolean stream;
}
