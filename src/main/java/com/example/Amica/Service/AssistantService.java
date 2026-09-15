package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.AssistantDto;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Vo.AssistantVo;

import java.util.List;

public interface AssistantService extends IService<AssistantEntity> {
    //用于创建新的助手
    Result<AssistantVo> createAssistant(AssistantDto dto,Long userId);
    //用于使用userId快捷查询对应对应用户名下助手清单
    Result<List<AssistantVo>> findByUserId(Long userId);
}
