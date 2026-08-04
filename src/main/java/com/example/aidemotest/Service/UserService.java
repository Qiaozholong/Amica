package com.example.aidemotest.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.Auth.LoginDto;
import com.example.aidemotest.Dto.Auth.RegisterDto;
import com.example.aidemotest.Entity.UserEntity;

import java.util.List;


public interface UserService extends IService<UserEntity> {
    //注册新用户
    Result<RegisterDto> register(RegisterDto dto);
    //用户登录
    Result<LoginDto> login(LoginDto dto);
    //用户列表查询，测试用
    Result<List<UserEntity>> showUsers();
}
