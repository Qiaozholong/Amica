package com.example.Amica.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amica.Common.BusinessException;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.AssistantDto;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Entity.ModelEntity;
import com.example.Amica.Entity.UserEntity;
import com.example.Amica.Mapper.AssistantMapper;
import com.example.Amica.Service.AssistantService;
import com.example.Amica.Service.ModelService;
import com.example.Amica.Service.UserService;
import com.example.Amica.Vo.AssistantVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
    private final UserService userService;
    private final ModelService modelService;

    public AssistantServiceImpl(UserService userService, ModelService modelService) {
        this.userService = userService;
        this.modelService = modelService;
    }

    @Override
    public Result<AssistantVo> createAssistant(AssistantDto dto) {
        UserEntity existUser = userService.lambdaQuery().eq(UserEntity::getId, dto.getUserId()).one();
        //两个if检查外键
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        ModelEntity existModel = modelService.lambdaQuery().eq(ModelEntity::getId, dto.getModelId()).one();
        if (existModel == null) {
            throw new BusinessException("模型不存在");
        }
        //生成一个空对象用于接收参数以及存储
        AssistantEntity assistantEntity = new AssistantEntity();
        assistantEntity.setUserId(dto.getUserId());
        assistantEntity.setModelId(dto.getModelId());
        assistantEntity.setName(dto.getName());
        assistantEntity.setPrompt(dto.getPrompt());
        save(assistantEntity);
        //中间参数调整所需参数
        AssistantVo vo = new AssistantVo();
        BeanUtils.copyProperties(assistantEntity, vo);
        vo.setAssistantId(assistantEntity.getId());
        vo.setUserName(existUser.getNickname());
        vo.setModelName(existModel.getName());
        return Result.success(vo);
    }
}
