package com.example.Amica.Service;

import com.example.Amica.Dto.MessagesDto;
import com.example.Amica.Provider.model.ChatResponse;

public interface ChatService{
    ChatResponse sendMessage(Long conversationId, MessagesDto dto);
}
