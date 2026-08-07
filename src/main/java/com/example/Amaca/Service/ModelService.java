package com.example.Amaca.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amaca.Common.Result;
import com.example.Amaca.Dto.ModelRegister.ModelDto;
import com.example.Amaca.Entity.ModelEntity;
import com.example.Amaca.Vo.ModelRegister.AModelVo;



public interface ModelService extends IService<ModelEntity> {
    //注册model的方法

    Result<AModelVo> registerModel(ModelDto dto);
}
