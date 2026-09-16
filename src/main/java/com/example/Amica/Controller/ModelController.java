package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ApiKeyDto;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Service.ModelService;
import com.example.Amica.Service.ProviderService;
import com.example.Amica.Vo.ApiKeyVo;
import com.example.Amica.Vo.ModelRegister.ModelVo;
import com.example.Amica.Vo.ProviderVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/model")
public class ModelController {
    final private ModelService modelService;
    private final ProviderService providerService;

    public ModelController(ModelService modelService, ProviderService providerService) {
        this.modelService = modelService;
        this.providerService = providerService;
    }

    //模型注册，model与provider模块耦合
    @PostMapping("/register")
    public Result<ModelVo> register(@Valid @RequestBody ModelDto dto,
                                    @RequestAttribute("userId") Long userId) {
        return modelService.registerModel(dto, userId);
    }

    //api密钥,单provider模块
    @PostMapping("/apikey")
    public Result<ApiKeyVo> apikey(@Valid @RequestBody ApiKeyDto dto,
                                   @RequestAttribute("userId") Long userId) {
        return providerService.apiKey(dto, userId);
    }

    //提供商查询(按当前登录用户过滤),单provider模块
    @GetMapping("/getallprovider")
    public Result<List<ProviderVo>> getProvider(@RequestAttribute("userId") Long userId) {
        return providerService.getProvider(userId);
    }

    //用户名下提供商旗下模型清单查询,单model模块
    @GetMapping("/getAllModel/{providerId}")
    public Result<List<ModelVo>> getAllModel(
            @PathVariable Long providerId,
            @RequestAttribute("userId") Long userId
    ) {
        return modelService.getAllModels(providerId, userId);
    }

}
