package com.example.aidemotest.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Mapper.ModelMapper;
import com.example.aidemotest.Service.ModelService;
import org.springframework.stereotype.Service;

@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements ModelService {

}
