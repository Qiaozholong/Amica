package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.AssistantDto;
import com.example.Amica.Service.AssistantService;
import com.example.Amica.Vo.AssistantVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private final AssistantService assistantService;
    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }
    @PostMapping("/create")
    public Result<AssistantVo> createAssistant(@Valid @RequestBody AssistantDto dto) {
        return assistantService.createAssistant(dto);
    }

}
