package com.example.Aimaca.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Aimaca.Common.Result;
import com.example.Aimaca.Dto.ApiKeyDto;
import com.example.Aimaca.Dto.ModelRegister.ProviderDto;
import com.example.Aimaca.Entity.ProviderEntity;
import com.example.Aimaca.Vo.ApiKeyVo;
import com.example.Aimaca.Vo.ModelRegister.ProviderVo;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    ProviderVo registerProvider(ProviderDto dto);
    //注册api密钥的方法
    Result<ApiKeyVo> apiKey(ApiKeyDto dto);
}
