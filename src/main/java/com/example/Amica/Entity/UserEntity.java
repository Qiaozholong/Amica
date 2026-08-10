package com.example.Amica.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class UserEntity {
    //用户id
    private Long id;
    //用户账号
    private String account;
    //用户密码
    private String password;
    //用户昵称
    private String nickname;
    //用户状态
    private Integer status;
    //创建时间
    private LocalDateTime createTime;
    //上传时间
    private LocalDateTime updateTime;
}
