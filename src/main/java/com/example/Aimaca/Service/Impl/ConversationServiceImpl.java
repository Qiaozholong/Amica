package com.example.Aimaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Aimaca.Entity.ConversationEntity;
import com.example.Aimaca.Mapper.ConversationMapper;
import com.example.Aimaca.Service.ConversationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationEntity> implements ConversationService {
}
