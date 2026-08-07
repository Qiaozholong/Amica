package com.example.Aimaca.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Aimaca.Common.Result;
import com.example.Aimaca.Dto.ModelRegister.ModelDto;
import com.example.Aimaca.Entity.ModelEntity;
import com.example.Aimaca.Vo.ModelRegister.AModelVo;



public interface ModelService extends IService<ModelEntity> {
    //注册model的方法

    Result<AModelVo> registerModel(ModelDto dto);
}
