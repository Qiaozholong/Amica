package com.example.Aimaca.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Aimaca.Common.Result;
import com.example.Aimaca.Dto.Auth.LoginDto;
import com.example.Aimaca.Dto.Auth.RegisterDto;
import com.example.Aimaca.Entity.UserEntity;

import java.util.List;


public interface UserService extends IService<UserEntity> {
    //注册新用户
    Result<RegisterDto> register(RegisterDto dto);
    //用户登录
    Result<LoginDto> login(LoginDto dto);
    //用户列表查询，测试用
    Result<List<UserEntity>> showUsers();
}
