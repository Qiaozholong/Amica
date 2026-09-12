package com.example.Amica.Vo.Auth;

import lombok.Data;

@Data
public class AuthVo {
    private Long id;
    private String account;
    private String nickname;
    private String token;
}
