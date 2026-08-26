package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.Auth.LoginDto;
import com.example.Amica.Dto.Auth.RegisterDto;
import com.example.Amica.Service.UserService;
import com.example.Amica.Vo.UserInfoVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    //注册用户
    @PostMapping("/register")
    public Result<RegisterDto> register(@Valid @RequestBody RegisterDto dto) {
        return userService.register(dto);
    }
    //用户登录
    @PostMapping("/login")
    public Result<LoginDto> login(@Valid @RequestBody LoginDto dto) {
        return userService.login(dto);
    }
    //单用户查询
    @GetMapping("/get/{id}")
    public Result<UserInfoVo> getUser(@PathVariable Long id) { return userService.getInfo(id);}
    //多用户查询
    @GetMapping("/get")
    public Result<List<UserInfoVo>> getAllUsers() {return userService.getInfos();}



}
