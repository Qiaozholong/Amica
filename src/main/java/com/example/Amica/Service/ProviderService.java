package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ApiKeyDto;
import com.example.Amica.Dto.ModelRegister.ProviderDto;
import com.example.Amica.Entity.ProviderEntity;
import com.example.Amica.Vo.ApiKeyVo;
import com.example.Amica.Vo.ModelRegister.ProviderVo;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    ProviderVo registerProvider(ProviderDto dto);
    //注册api密钥的方法
    Result<ApiKeyVo> apiKey(ApiKeyDto dto);
}
