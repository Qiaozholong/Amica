package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ApiKeyDto;
import com.example.Amica.Dto.ModelRegister.ProviderDto;
import com.example.Amica.Entity.ProviderEntity;
import com.example.Amica.Vo.ApiKeyVo;
import com.example.Amica.Vo.ModelRegister.RegisteredProviderVo;
import com.example.Amica.Vo.ProviderVo;

import java.util.List;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    RegisteredProviderVo registerProvider(ProviderDto dto);
    //注册api密钥的方法
    Result<ApiKeyVo> apiKey(ApiKeyDto dto);
    //查询api密钥的方法
    String GetApiKey(Long providerId);
    //查询提供商的方法
    Result<List<ProviderVo>> getProvider();

}
