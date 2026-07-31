package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Entity.ConversationEntity;
import com.example.aidemotest.Mapper.ConversationMapper;
import com.example.aidemotest.Service.ConversationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationEntity> implements ConversationService {
}
