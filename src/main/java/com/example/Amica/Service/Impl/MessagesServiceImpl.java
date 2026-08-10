package com.example.Amica.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Amica.Entity.MessagesEntity;
import com.example.Amica.Mapper.MessagesMapper;
import com.example.Amica.Service.MessagesService;
import org.springframework.stereotype.Service;

@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, MessagesEntity> implements MessagesService{
}
