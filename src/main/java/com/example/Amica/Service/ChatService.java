package com.example.Amica.Service;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.Messages.MessagesDto;
import com.example.Amica.Entity.MessagesEntity;
import com.example.Amica.Provider.model.ChatResponse;

import java.util.List;

public interface ChatService{
    ChatResponse sendMessage(Long conversationId, MessagesDto dto);
    Result<List<MessagesEntity>> getMessage(Long conversationId);
}
