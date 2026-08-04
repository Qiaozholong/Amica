package com.example.aidemotest.Controller;

import com.example.aidemotest.Common.Result;
import com.example.aidemotest.Dto.ModelDto;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Service.ModelService;
import com.example.aidemotest.Service.ProviderService;
import com.example.aidemotest.Vo.ModelVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/model")
public class ModelController {
    final private ModelService modelService;
    final private ProviderService providerService;

    public ModelController(ModelService modelService, ProviderService providerService) {
        this.modelService = modelService;
        this.providerService = providerService;
    }
    //模型注册
    @PostMapping("/register")
    public String register(@Valid @RequestBody ModelDto dto) {
        return "使用的模型提供商为:"+providerService.registerProvider(dto)+",提供的模型为"+modelService.registerModel(dto);
    }

}
