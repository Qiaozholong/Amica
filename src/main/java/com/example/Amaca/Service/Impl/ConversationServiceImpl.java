package com.example.Amaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amaca.Entity.ConversationEntity;
import com.example.Amaca.Mapper.ConversationMapper;
import com.example.Amaca.Service.ConversationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationEntity> implements ConversationService {
}
