package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Entity.AssistantEntity;
import com.example.aidemotest.Mapper.AssistantMapper;
import com.example.aidemotest.Service.AssistantService;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
}
