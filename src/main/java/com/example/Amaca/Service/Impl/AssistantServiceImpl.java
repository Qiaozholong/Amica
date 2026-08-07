package com.example.Amaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amaca.Entity.AssistantEntity;
import com.example.Amaca.Mapper.AssistantMapper;
import com.example.Amaca.Service.AssistantService;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl extends ServiceImpl<AssistantMapper, AssistantEntity> implements AssistantService {
}
