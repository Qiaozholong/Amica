package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Common.BusinessException;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Dto.Model_PDto;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Mapper.ModelMapper;
import com.example.aidemotest.Service.ModelService;
import com.example.aidemotest.Service.ProviderService;
import com.example.aidemotest.Vo.AModelVo;
import com.example.aidemotest.Vo.ModelVo;
import com.example.aidemotest.Vo.ProviderVo;
import org.springframework.beans.BeanUtils;
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
                .eq(ModelEntity::getModelId,dto.getModelId())
                .one();
        if (exist != null) {
            throw new BusinessException("模型已存在");
        }
        //把得到的数据传给providerImpl,
        Model_PDto provider = new Model_PDto();
        BeanUtils.copyProperties(dto,provider);
        ProviderVo PVo= providerService.registerProvider(provider);
        //进行创建模型
        ModelEntity model  = new ModelEntity();
        BeanUtils.copyProperties(dto, model);
        model.setProviderId(PVo.getProviderId());
        save(model);
        ModelVo MVo = new ModelVo();
        BeanUtils.copyProperties(model, MVo);
        AModelVo AVo = new AModelVo();
        BeanUtils.copyProperties(MVo, AVo);
        BeanUtils.copyProperties(PVo, AVo);
        return Result.success(AVo);
    }
}