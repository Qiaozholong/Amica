package com.example.aidemotest.Controller;

import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.LoginDto;
import com.example.aidemotest.Dto.RegisterDto;
import com.example.aidemotest.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private UserService userService;
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public Result<RegisterDto> register(@Valid @RequestBody RegisterDto dto) {
        return userService.register(dto);
    }

    @PostMapping("/login")
    public Result<LoginDto> login(@Valid @RequestBody LoginDto dto) {
        return userService.login(dto);
    }
}
