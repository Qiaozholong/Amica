package com.example.Amica.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Entity.ModelEntity;
import com.example.Amica.Vo.ModelRegister.ModelVo;

import java.util.List;


public interface ModelService extends IService<ModelEntity> {
    //注册model的方法,首次顺带注册provider，之后查重检测provider自动纳入
    Result<ModelVo> registerModel(ModelDto dto, Long userId);
    //查询提供商旗下model的方法
    Result<List<ModelVo>> getAllModels(Long providerId, Long userId);
}
