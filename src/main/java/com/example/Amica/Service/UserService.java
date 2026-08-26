package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.Auth.LoginDto;
import com.example.Amica.Dto.Auth.RegisterDto;
import com.example.Amica.Entity.UserEntity;
import com.example.Amica.Vo.UserInfoVo;

import java.util.List;


public interface UserService extends IService<UserEntity> {
    //注册新用户
    Result<RegisterDto> register(RegisterDto dto);
    //用户登录
    Result<LoginDto> login(LoginDto dto);
    //单用户查询（应该是用于登录后查询信息？或者管理员查询，但未引入jwt故暂且歇置）
    Result<UserInfoVo> getInfo(Long id);
    //用户总体查询
    Result<List<UserInfoVo>> getInfos();
}
