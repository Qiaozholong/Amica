package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Dto.Model_PDto;
import com.example.aidemotest.Entity.ProviderEntity;
import com.example.aidemotest.Mapper.ProviderMapper;
import com.example.aidemotest.Service.ProviderService;
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
    public ProviderVo registerProvider(Model_PDto dto){
        //检验运营商是否已存在
        ProviderEntity exist =lambdaQuery()
                .eq(ProviderEntity::getBaseUrl,dto.getBaseUrl())
                .eq(ProviderEntity::getProtocol,dto.getProtocol())
                .one();
        //存在,直接返回查询到的内容
        if(exist!=null){
            ProviderVo Vo = new ProviderVo();
            BeanUtils.copyProperties(exist,Vo);
            return Vo;
        }
        //不存在,接收数据进行创建,再进行返回
        ProviderEntity provider = new ProviderEntity();
        BeanUtils.copyProperties(dto,provider);
        provider.setName(dto.getProtocol());
        save(provider);
        ProviderVo Vo = new ProviderVo();
        BeanUtils.copyProperties(provider,Vo);
        Vo.setProviderId(provider.getId());
        return  Vo;
    }

}
