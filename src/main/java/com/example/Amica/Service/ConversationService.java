package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ConversationDto;
import com.example.Amica.Entity.ConversationEntity;
import com.example.Amica.Vo.ConversationVo;

public interface ConversationService extends IService<ConversationEntity> {
    Result<ConversationVo> create(ConversationDto dto);
}
