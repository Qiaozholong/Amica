package com.example.Amaca.Dto.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDto {
    //用户账号
    @NotBlank(message ="账号不能为空")
    private String account;
    //用户密码
    @NotBlank(message = "密码不能为空")
    private String password;
}
