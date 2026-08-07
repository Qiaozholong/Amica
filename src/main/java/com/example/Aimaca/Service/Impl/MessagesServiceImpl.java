package com.example.Aimaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Aimaca.Entity.MessagesEntity;
import com.example.Aimaca.Mapper.MessagesMapper;
import com.example.Aimaca.Service.MessagesService;
import org.springframework.stereotype.Service;

@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, MessagesEntity> implements MessagesService{
}
