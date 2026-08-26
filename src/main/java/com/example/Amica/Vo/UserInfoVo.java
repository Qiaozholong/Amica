package com.example.Amica.Vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVo {
    //用户id
    private Long id;
    //用户账号
    private String account;
    //用户昵称
    private String nickname;
    //用户状态,哈哈（8/27日）我彻底不知道这行是干嘛的了
    private Integer status;
    //创建时间
    private LocalDateTime createTime;
    //更新时间
    private LocalDateTime updateTime;
}
