package com.example.Amaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amaca.Common.BusinessException;
import com.example.Amaca.Common.Result;
import com.example.Amaca.Dto.ApiKeyDto;
import com.example.Amaca.Dto.ModelRegister.ProviderDto;
import com.example.Amaca.Entity.ProviderEntity;
import com.example.Amaca.Mapper.ProviderMapper;
import com.example.Amaca.Service.ProviderService;
import com.example.Amaca.Vo.ApiKeyVo;
import com.example.Amaca.Vo.ModelRegister.ProviderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class ProviderServiceImpl extends ServiceImpl<ProviderMapper, ProviderEntity> implements ProviderService {
    private final TextEncryptor apiKeyEncryptor;

    public ProviderServiceImpl(@Qualifier("apiKeyEncryptor") TextEncryptor apiKeyEncryptor) {
        this.apiKeyEncryptor = apiKeyEncryptor;
    }
    //
    @Override
    public ProviderVo registerProvider(ProviderDto dto) {
        //检验运营商是否已存在
        ProviderEntity exist = lambdaQuery()
                .eq(ProviderEntity::getBaseUrl, dto.getBaseUrl())
                .eq(ProviderEntity::getProtocol, dto.getProtocol())
                .one();
        //存在,直接返回查询到的内容
        if (exist != null) {
            ProviderVo Vo = new ProviderVo();
            BeanUtils.copyProperties(exist, Vo);
            return Vo;
        }
        //不存在,接收数据进行创建,再进行返回
        ProviderEntity provider = new ProviderEntity();
        BeanUtils.copyProperties(dto, provider);
        provider.setName(dto.getProtocol());
        try {
            save(provider);
        } catch (DuplicateKeyException e) {
            ProviderEntity ifExist = lambdaQuery()
                    .eq(ProviderEntity::getBaseUrl, dto.getBaseUrl())
                    .eq(ProviderEntity::getProtocol, dto.getProtocol())
                    .one();
            provider = ifExist;
        }
        ProviderVo Vo = new ProviderVo();
        BeanUtils.copyProperties(provider, Vo);
        Vo.setProviderId(provider.getId());
        return Vo;
    }
    //注册api密钥
    @Override
    public Result<ApiKeyVo> apiKey(ApiKeyDto dto){
        ProviderEntity exist = getById(dto.getProviderId());
        if (exist == null) {
            throw new BusinessException("提供商不存在");
        }
        exist.setApiKey(apiKeyEncryptor.encrypt(dto.getApiKey()));
        updateById(exist);
        ApiKeyVo Vo = new ApiKeyVo();
       Vo.setApiKey(mask(dto.getApiKey()));
        return Result.success(Vo);
    }
    //非接口使用，用于注册以及查询时返回脱敏密钥
    private String mask(String key){
        if(key==null || key.isBlank())return "";
        if(key.length()<=6)return "******";
        return key.substring(0,2)+"******"+key.substring(key.length()-2);
    }
}
