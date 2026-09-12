package com.example.Amica.Dto.Messages;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class MessagesDto {
    //上下文
    @NotBlank(message = "文本不可为空,数据类型为content")
    private String content;
    //上限token数,方法体还没写好处理这个参数的功能
    private int maxtokens;
    //嵌套JSON
    private OptionsDto options;
}
