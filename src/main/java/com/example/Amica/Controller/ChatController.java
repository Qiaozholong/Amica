package com.example.Amica.Controller;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.MessagesDto;
import com.example.Amica.Provider.model.ChatResponse;
import com.example.Amica.Service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    @PostMapping("/{conversationId}/send")
    public Result<ChatResponse> send(
            @PathVariable Long conversationId,
            @RequestBody MessagesDto dto
    ){
        return Result.success(chatService.sendMessage(conversationId,dto));
    }


}
