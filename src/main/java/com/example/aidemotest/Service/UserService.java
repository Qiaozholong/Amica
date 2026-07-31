package com.example.aidemotest.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.LoginDto;
import com.example.aidemotest.Dto.RegisterDto;
import com.example.aidemotest.Entity.UserEntity;


public interface UserService extends IService<UserEntity> {
    //注册新用户
    Result<RegisterDto> register(RegisterDto dto);
    //用户登录
    Result<LoginDto> login(LoginDto dto);
}
