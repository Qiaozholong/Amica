package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ConversationDto;
import com.example.Amica.Entity.ConversationEntity;
import com.example.Amica.Vo.ConversationVo;

import java.util.List;

public interface ConversationService extends IService<ConversationEntity> {
    //用户创建话题
    Result<ConversationVo> create(ConversationDto dto,Long userId);
    //用户名下会话查询返回
    Result<List<ConversationEntity>> findAllConversation(Long assistantId,Long userId);
}
