package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.AssistantDto;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Vo.AssistantVo;

public interface AssistantService extends IService<AssistantEntity> {
    Result<AssistantVo> createAssistant(AssistantDto dto);
}
