package com.example.Amica.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Mapper.AssistantMapper;
import com.example.Amica.Service.AssistantService;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
}
