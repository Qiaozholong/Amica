package com.example.Aimaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Aimaca.Common.BusinessException;
import com.example.Aimaca.Common.Result;
import com.example.Aimaca.Dto.Auth.LoginDto;
import com.example.Aimaca.Dto.Auth.RegisterDto;
import com.example.Aimaca.Entity.UserEntity;
import com.example.Aimaca.Mapper.UserMapper;
import com.example.Aimaca.Service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
    //构造方法传入PasswordEncoder,避免使用反射注入,先不加jwt验权
    private final PasswordEncoder passwordEncoder;
    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    //用户注册方法
    @Override
    public Result<RegisterDto> register(RegisterDto dto) {
        UserEntity exist=lambdaQuery().eq(UserEntity::getAccount,dto.getAccount()).one();
        if(exist!=null){
            throw new BusinessException("账号已存在");
        }
        UserEntity user =new UserEntity();
        BeanUtils.copyProperties(dto,user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        save(user);
        RegisterDto result=new RegisterDto();
        BeanUtils.copyProperties(user,result);
        return Result.success(result);
    }
    //用户登录方法
    @Override
    public Result<LoginDto> login(LoginDto dto) {
        UserEntity exist=lambdaQuery().eq(UserEntity::getAccount,dto.getAccount()).one();
        if(exist==null||!passwordEncoder.matches(dto.getPassword(),exist.getPassword())){
            throw new BusinessException(401,"账号或密码错误");
        }
        LoginDto result=new LoginDto();
        result.setAccount(exist.getAccount());
        return Result.success(result);
    }
    //用户查询入口，测试使用
    @Override
    public Result<List<UserEntity>> showUsers() {
        List<UserEntity> exist=list();
        return Result.success(exist);
    }

}
