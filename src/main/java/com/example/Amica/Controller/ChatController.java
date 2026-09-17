package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.Messages.MessagesDto;
import com.example.Amica.Entity.MessagesEntity;
import com.example.Amica.Provider.model.ChatResponse;
import com.example.Amica.Service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{conversationId}/{assistantId}/send")
    public Result<ChatResponse> send(
            @PathVariable Long conversationId,
            @PathVariable Long assistantId,
            @Valid @RequestBody MessagesDto dto,
            @RequestAttribute("userId") Long userId
    ) {
        return Result.success(chatService.sendMessage(conversationId, userId, dto,assistantId ));
    }

    @GetMapping("/{conversationId}/{assistantId}/get")
    public Result<List<MessagesEntity>> get(
            @PathVariable Long conversationId,
            @PathVariable Long assistantId,
            @RequestAttribute("userId") Long userId
    ) {
        return chatService.getMessage(conversationId, userId, assistantId);
    }

}
