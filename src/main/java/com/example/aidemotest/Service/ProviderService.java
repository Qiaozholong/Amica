package com.example.aidemotest.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.aidemotest.Dto.Model_PDto;
import com.example.aidemotest.Entity.ProviderEntity;
import com.example.aidemotest.Vo.ProviderVo;


public interface ProviderService extends IService<ProviderEntity> {
    //注册提供商的方法
    ProviderVo registerProvider(Model_PDto dto);
}
