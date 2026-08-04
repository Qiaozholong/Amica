package com.example.aidemotest.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ProviderEntity;
import com.example.aidemotest.Vo.ProviderVo;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    Result<ProviderVo> registerProvider(ModelDto dto);
}
