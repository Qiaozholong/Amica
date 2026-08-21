package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.ConversationDto;
import com.example.Amica.Service.ConversationService;
import com.example.Amica.Vo.ConversationVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversation")
public class ConversationController {
    private final ConversationService conversationService;
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    @PostMapping("/create")
    public Result<ConversationVo> create(@Valid @RequestBody ConversationDto dto){
        return conversationService.create(dto);
    }
}
