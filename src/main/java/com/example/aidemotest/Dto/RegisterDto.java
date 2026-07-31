package com.example.aidemotest.Dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDto {
    //用户账号
    @NotBlank(message ="账号不能为空")
    private String account;
    //用户密码
    @NotBlank(message = "密码不能为空")
    private String password;
    //用户昵称
    @NotBlank(message = "请输入昵称")
    private String nickname;
}
