package com.example.aidemotest.Controller;

import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Service.ModelService;
import com.example.aidemotest.Service.ProviderService;
import com.example.aidemotest.Vo.AModelVo;
import com.example.aidemotest.Vo.ModelVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/model")
public class ModelController {
    final private ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }
    //模型注册
    @PostMapping("/register")
    public Result<AModelVo> register(@Valid @RequestBody ModelDto dto) {
        return modelService.registerModel(dto);
    }

}
