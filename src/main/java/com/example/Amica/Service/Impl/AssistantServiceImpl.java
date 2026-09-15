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

import java.util.List;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
    private final UserService userService;
    private final ModelService modelService;

    public AssistantServiceImpl(UserService userService, ModelService modelService) {
        this.userService = userService;
        this.modelService = modelService;
    }

    @Override
    public Result<AssistantVo> createAssistant(AssistantDto dto, Long userId) {
        //根据传回的userId查询对应用户，赋值给existUser用于查询数据库中是否有对应对象
        UserEntity existUser = userService.lambdaQuery().eq(UserEntity::getId, userId).one();
        if (existUser == null) {
            throw new BusinessException(401,"登录状态已失效，请重新登录");
        }
        //双重索引条件锁定对应model
        ModelEntity existModel = modelService.lambdaQuery()
                .eq(ModelEntity::getId, dto.getModelId())
                .eq(ModelEntity::getUserId, userId)
                .one();
        if (existModel == null) {
            throw new BusinessException("模型不存在");
        }
        //生成一个空对象用于接收参数以及存储
        AssistantEntity assistantEntity = new AssistantEntity();
        assistantEntity.setUserId(userId);
        assistantEntity.setModelId(dto.getModelId());
        assistantEntity.setName(dto.getName());
        assistantEntity.setPrompt(dto.getPrompt());
        save(assistantEntity);
        //中间参数调整所需参数
        AssistantVo vo = new AssistantVo();
        BeanUtils.copyProperties(assistantEntity, vo);
        return Result.success(vo);
    }

    @Override
    public Result<List<AssistantVo>> findByUserId(Long userId) {
        List<AssistantEntity> entities = lambdaQuery()
                .eq(AssistantEntity::getUserId,userId)
                .list();
        List<AssistantVo> result = entities.stream().map(e->{
            AssistantVo vo = new AssistantVo();
            BeanUtils.copyProperties(e,vo);
            return vo;
        }).toList();
        return Result.success(result);
    }
}
