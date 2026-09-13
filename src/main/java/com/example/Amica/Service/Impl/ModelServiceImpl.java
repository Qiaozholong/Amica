package com.example.Amica.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amica.Common.BusinessException;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Dto.ModelRegister.ProviderDto;
import com.example.Amica.Entity.ModelEntity;
import com.example.Amica.Mapper.ModelMapper;
import com.example.Amica.Service.ModelService;
import com.example.Amica.Service.ProviderService;
import com.example.Amica.Vo.ModelRegister.AModelVo;
import com.example.Amica.Vo.ModelRegister.ModelVo;
import com.example.Amica.Vo.ModelRegister.RegisteredProviderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service

public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements ModelService {
    private final ProviderService providerService;

    public ModelServiceImpl(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    @Transactional
    public Result<AModelVo> registerModel(ModelDto dto, Long userId) {
        //查询该用户下是否已有相同 modelId, 避免重复创建(唯一键已改为 (user_id, model_id))
        ModelEntity exist = lambdaQuery()
                .eq(ModelEntity::getUserId, userId)
                .eq(ModelEntity::getModelId, dto.getModelId())
                .one();
        if (exist != null) {
            throw new BusinessException("模型已存在");
        }
        //把得到的数据传给providerImpl,
        ProviderDto provider = new ProviderDto();
        BeanUtils.copyProperties(dto, provider);
        RegisteredProviderVo PVo = providerService.registerProvider(provider, userId);
        //进行创建模型
        ModelEntity model = new ModelEntity();
        BeanUtils.copyProperties(dto, model);
        model.setUserId(userId);
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
    @Override
    public Result<List<ModelVo>> getAllModels(Long providerId, Long userId) {
        List<ModelEntity> entitys = lambdaQuery()
                .eq(ModelEntity::getProviderId, providerId)
                .eq(ModelEntity::getUserId, userId)
                .list();
        List<ModelVo> result = entitys.stream().map(e->{
            ModelVo vo = new ModelVo();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        return Result.success(result);
    }
}