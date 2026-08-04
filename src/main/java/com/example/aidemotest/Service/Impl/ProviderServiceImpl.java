package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ProviderEntity;
import com.example.aidemotest.Mapper.ProviderMapper;
import com.example.aidemotest.Service.ProviderService;
import com.example.aidemotest.Vo.ModelVo;
import com.example.aidemotest.Vo.ProviderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class ProviderServiceImpl extends ServiceImpl<ProviderMapper, ProviderEntity> implements ProviderService {
    private final TextEncryptor apiKeyEncryptor;

    public ProviderServiceImpl(@Qualifier("apiKeyEncryptor")TextEncryptor apiKeyEncryptor) {
        this.apiKeyEncryptor = apiKeyEncryptor;
    }

    @Override
    public Result<ProviderVo> registerProvider(ModelDto dto){
        ProviderEntity exist =lambdaQuery()
                .eq(ProviderEntity::getBaseUrl,dto.getBaseUrl())
                .eq(ProviderEntity::getProtocol,dto.getProtocol())
                .one();
        if(exist!=null){
            ProviderVo Vo = new ProviderVo();
            BeanUtils.copyProperties(exist,Vo);
            return Result.success(Vo);
        }
        ProviderEntity provider = new ProviderEntity();
        BeanUtils.copyProperties(dto,provider);
        save(provider);
        ProviderVo Vo = new ProviderVo();
        BeanUtils.copyProperties(provider,Vo);
        return  Result.success(Vo);
    }

}
