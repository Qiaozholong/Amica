package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Entity.MessagesEntity;
import com.example.aidemotest.Mapper.MessagesMapper;
import com.example.aidemotest.Service.MessagesService;
import org.springframework.stereotype.Service;

@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, MessagesEntity> implements MessagesService{
}
