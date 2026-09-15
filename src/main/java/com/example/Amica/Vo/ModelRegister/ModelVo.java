package com.example.Amica.Vo.ModelRegister;

import lombok.Data;

@Data
public class ModelVo {
    //模型Id
    private Long id;
    //对应用户Id
    private Long userId;
    //提供商Id
    private Long providerId;

    //模型名称
    private String name;
    //模型对应编号
    private String modelId;

}
