package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ApiKeyDto;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Service.ModelService;
import com.example.Amica.Service.ProviderService;
import com.example.Amica.Vo.ApiKeyVo;
import com.example.Amica.Vo.ModelRegister.AModelVo;
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
