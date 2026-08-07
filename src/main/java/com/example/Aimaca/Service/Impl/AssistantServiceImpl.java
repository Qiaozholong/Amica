package com.example.Aimaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Aimaca.Entity.AssistantEntity;
import com.example.Aimaca.Mapper.AssistantMapper;
import com.example.Aimaca.Service.AssistantService;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
}
