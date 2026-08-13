package com.example.Amica.Dto;

import lombok.Data;

@Data
public class MessagesDto {
    //上下文
    private String content;
    //上限token数,方法体还没写好处理这个参数的功能
    private int maxtokens;
    //嵌套JSON
    private OptionsDto options;
}
