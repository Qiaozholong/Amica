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
    private final AssistantService assistantService;
    private final UserService userService;

    public ConversationServiceImpl(AssistantService assistantService, UserService userService) {
        this.userService = userService;
        this.assistantService = assistantService;
    }

    @Override
    public Result<ConversationVo> create(ConversationDto dto, Long userId) {
        //检查登录合法性
        UserEntity userExist = userService.lambdaQuery()
                .eq(UserEntity::getId, userId)
                .one();
        if (userExist == null) {
            throw new BusinessException(401, "登录凭证已失效,请重新登录");
        }
        //双重索引锁定对应用户以及对应assistant防止越权
        AssistantEntity assistantexist = assistantService.lambdaQuery()
                .eq(AssistantEntity::getId, dto.getAssistantId())
                .eq(AssistantEntity::getUserId, userId)
                .one();
        if (assistantexist == null) {
            throw new BusinessException("助手不存在或无权限操作");
        }

        //创建新对象接收参数,不知道有没有差参数
        ConversationEntity conversationentity = new ConversationEntity();
        BeanUtils.copyProperties(dto, conversationentity);
        conversationentity.setUserId(userId);

        //进行未命名title的加工,暂且使用assistantId与userId锁定
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            long count = lambdaQuery()
                    .eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getAssistantId, dto.getAssistantId())
                    .count();
            conversationentity.setTitle("话题" + (count + 1));
        }
        save(conversationentity);
        //创建返回体
        ConversationVo vo = new ConversationVo();
        BeanUtils.copyProperties(conversationentity, vo);
        String sp = dto.getSystemPrompt();
        vo.setStatus(sp == null || sp.isBlank() ? "未覆盖" : "已覆盖");
        return Result.success(vo);
    }

    @Override
    public Result<List<ConversationEntity>> findAllConversation(Long assistantId, Long userId) {

        List<ConversationEntity> result = lambdaQuery()
                .eq(ConversationEntity::getUserId, userId)
                .eq(ConversationEntity::getAssistantId, assistantId)
                .list();

        return Result.success(result);
    }
}
