package com.example.Amaca.Controller;

import com.example.Amaca.Common.Result;
import com.example.Amaca.Dto.ApiKeyDto;
import com.example.Amaca.Dto.ModelRegister.ModelDto;
import com.example.Amaca.Service.ModelService;
import com.example.Amaca.Service.ProviderService;
import com.example.Amaca.Vo.ApiKeyVo;
import com.example.Amaca.Vo.ModelRegister.AModelVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/model")
public class ModelController {
    final private ModelService modelService;
    private final ProviderService providerService;

    public ModelController(ModelService modelService, ProviderService providerService) {
        this.modelService = modelService;
        this.providerService = providerService;
    }
    //模型注册
    @PostMapping("/register")
    public Result<AModelVo> register(@Valid @RequestBody ModelDto dto) {
        return modelService.registerModel(dto);
    }
    //api密钥
    @PostMapping("/apikey")
    public Result<ApiKeyVo> apikey(@RequestBody ApiKeyDto dto) {
        return providerService.apiKey(dto);
    }

}
