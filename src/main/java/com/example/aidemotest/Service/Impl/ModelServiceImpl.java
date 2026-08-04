package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Common.BusinessException;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Mapper.ModelMapper;
import com.example.aidemotest.Service.ModelService;
import com.example.aidemotest.Vo.ModelVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements ModelService {
    @Override
    public Result<ModelVo> registerModel(ModelDto dto) {
        ModelEntity exist = lambdaQuery()
                .eq(ModelEntity::getModelId,dto.getModelId())
                .one();
        if (exist != null) {
            throw new BusinessException("模型已存在");
        }
        ModelEntity model  = new ModelEntity();
        BeanUtils.copyProperties(dto, model);
        save(model);
        ModelVo modelVo = new ModelVo();
        BeanUtils.copyProperties(model, modelVo);
        return Result.success(modelVo);
    }
}