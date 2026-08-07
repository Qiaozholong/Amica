package com.example.Amaca.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amaca.Common.Result;
import com.example.Amaca.Dto.ApiKeyDto;
import com.example.Amaca.Dto.ModelRegister.ProviderDto;
import com.example.Amaca.Entity.ProviderEntity;
import com.example.Amaca.Vo.ApiKeyVo;
import com.example.Amaca.Vo.ModelRegister.ProviderVo;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    ProviderVo registerProvider(ProviderDto dto);
    //注册api密钥的方法
    Result<ApiKeyVo> apiKey(ApiKeyDto dto);
}
