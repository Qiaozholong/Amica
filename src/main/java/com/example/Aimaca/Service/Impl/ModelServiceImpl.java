package com.example.Aimaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Aimaca.Common.BusinessException;
import com.example.Aimaca.Common.Result;
import com.example.Aimaca.Dto.ModelRegister.ModelDto;
import com.example.Aimaca.Dto.ModelRegister.ProviderDto;
import com.example.Aimaca.Entity.ModelEntity;
import com.example.Aimaca.Mapper.ModelMapper;
import com.example.Aimaca.Service.ModelService;
import com.example.Aimaca.Service.ProviderService;
import com.example.Aimaca.Vo.ModelRegister.AModelVo;
import com.example.Aimaca.Vo.ModelRegister.ModelVo;
import com.example.Aimaca.Vo.ModelRegister.ProviderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service

public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements ModelService {
    private final ProviderService providerService;

    public ModelServiceImpl(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    @Transactional
    public Result<AModelVo> registerModel(ModelDto dto) {
        //查询数据库中是否有ModelId相同字段，避免重复创建
        ModelEntity exist = lambdaQuery()
                .eq(ModelEntity::getModelId, dto.getModelId())
                .one();
        if (exist != null) {
            throw new BusinessException("模型已存在");
        }
        //把得到的数据传给providerImpl,
        ProviderDto provider = new ProviderDto();
        BeanUtils.copyProperties(dto, provider);
        ProviderVo PVo = providerService.registerProvider(provider);
        //进行创建模型
        ModelEntity model = new ModelEntity();
        BeanUtils.copyProperties(dto, model);
        model.setProviderId(PVo.getProviderId());
        //规避并发异常
        try {
            save(model);
        }catch(DuplicateKeyException e) {
            throw new BusinessException("模型已存在");
        }

        ModelVo MVo = new ModelVo();
        BeanUtils.copyProperties(model, MVo);
        AModelVo AVo = new AModelVo();
        BeanUtils.copyProperties(MVo, AVo);
        BeanUtils.copyProperties(PVo, AVo);
        return Result.success(AVo);
    }
}