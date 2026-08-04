package com.example.aidemotest.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Vo.ModelVo;

import java.util.List;

public interface ModelService extends IService<ModelEntity> {
    //注册model的方法
    Result<ModelVo> registerModel(ModelDto dto);
}
