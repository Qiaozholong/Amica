package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.AssistantDto;
import com.example.Amica.Entity.AssistantEntity;
import com.example.Amica.Service.AssistantService;
import com.example.Amica.Vo.AssistantVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private final AssistantService assistantService;
    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/create")
    public Result<AssistantVo> createAssistant(@Valid @RequestBody AssistantDto dto,
                                               @RequestAttribute("userId") Long userId
    ) {
        return assistantService.createAssistant(dto,userId);
    }
    @GetMapping("/getAllAssistant")
    public Result<List<AssistantEntity>> getAllAssistant() {
        return assistantService.findAllAssistant();
    }
}
