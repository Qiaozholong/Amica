package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ApiKeyDto;
import com.example.Amica.Dto.ModelRegister.ModelDto;
import com.example.Amica.Service.ModelService;
import com.example.Amica.Service.ProviderService;
import com.example.Amica.Vo.ApiKeyVo;
import com.example.Amica.Vo.ModelRegister.AModelVo;
import com.example.Amica.Vo.ModelRegister.ModelVo;
import com.example.Amica.Vo.ProviderVo;
import jakarta.servlet.http.HttpServletRequest;
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
    //模型注册
    @PostMapping("/register")
    public Result<AModelVo> register(@Valid @RequestBody ModelDto dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return modelService.registerModel(dto, userId);
    }
    //api密钥
    @PostMapping("/apikey")
    public Result<ApiKeyVo> apikey(@RequestBody ApiKeyDto dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return providerService.apiKey(dto, userId);
    }
    //提供商查询(按当前登录用户过滤)
    @GetMapping("/getallprovider")
    public Result<List<ProviderVo>> getProvider(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        return providerService.getProvider(userId);
    }
    //模型查询
    @GetMapping("/getAllModel/{providerId}")
    public Result<List<ModelVo>> getAllModel(
            @PathVariable Long providerId,
            HttpServletRequest request
    ){
        Long userId = (Long) request.getAttribute("userId");
        return modelService.getAllModels(providerId, userId);
    }

}
