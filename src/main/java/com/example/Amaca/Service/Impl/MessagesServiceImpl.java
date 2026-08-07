package com.example.Amaca.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amaca.Entity.MessagesEntity;
import com.example.Amaca.Mapper.MessagesMapper;
import com.example.Amaca.Service.MessagesService;
import org.springframework.stereotype.Service;

@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, MessagesEntity> implements MessagesService{
}
