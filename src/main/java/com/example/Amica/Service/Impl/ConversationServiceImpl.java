package com.example.Amica.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amica.Common.BusinessException;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ConversationDto;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Entity.ConversationEntity;
import com.example.Amica.Entity.UserEntity;
import com.example.Amica.Mapper.ConversationMapper;
import com.example.Amica.Service.AssistantService;
import com.example.Amica.Service.ConversationService;
import com.example.Amica.Service.UserService;
import com.example.Amica.Vo.ConversationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationEntity> implements ConversationService {
    private final UserService userService;
    private final AssistantService assistantService;
    public ConversationServiceImpl(UserService userService, AssistantService assistantService) {
        this.userService = userService;
        this.assistantService = assistantService;
    }
    @Override
    public Result<ConversationVo> create(ConversationDto dto){
        //依旧检查验证
        UserEntity userexist =userService.lambdaQuery().eq(UserEntity::getId,dto.getUserId()).one();
        if(userexist==null){
            throw new BusinessException("用户不存在");
        }
        AssistantEntity assistantexist =assistantService.lambdaQuery().eq(AssistantEntity::getId,dto.getAssistantId()).one();
        if(assistantexist==null){
            throw new BusinessException("助手不存在");
        }
        //创建新对象接收参数
        ConversationEntity conversationentity = new ConversationEntity();
        BeanUtils.copyProperties(dto,conversationentity);
        //进行未命名title的加工
        if(dto.getTitle()==null || dto.getTitle().isBlank()){
            long count = lambdaQuery().eq(ConversationEntity::getUserId,dto.getUserId()).count();
            conversationentity.setTitle("话题"+(count+1));
        }
        save(conversationentity);
        //创建返回体
        ConversationVo vo = new ConversationVo();
        vo.setId(conversationentity.getId());
        vo.setTitle(conversationentity.getTitle());
        vo.setStatus(dto.getSystemPrompt()==null?"未覆盖":"已覆盖");
        return Result.success(vo);

    }
}
