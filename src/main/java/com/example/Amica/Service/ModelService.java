package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Entity.ModelEntity;
import com.example.Amica.Vo.ModelRegister.AModelVo;
import com.example.Amica.Vo.ModelRegister.ModelVo;

import java.util.List;


public interface ModelService extends IService<ModelEntity> {
    //注册model的方法
    Result<AModelVo> registerModel(ModelDto dto);
    //查询提供商旗下model的方法
    Result<List<ModelVo>> getAllModels(Long ProviderId);
}
