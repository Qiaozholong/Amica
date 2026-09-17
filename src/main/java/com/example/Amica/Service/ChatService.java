package com.example.Amica.Service;

import com.example.Amica.Common.Result;
import com.example.Amica.Dto.Messages.MessagesDto;
import com.example.Amica.Entity.MessagesEntity;
import com.example.Amica.Provider.model.ChatResponse;

import java.util.List;

public interface ChatService{
    //发送信息的方法
    ChatResponse sendMessage(Long conversationId,Long userId ,MessagesDto dto,Long assistantId);
    //查询对应对话的上下文的方法
    Result<List<MessagesEntity>> getMessage(Long conversationId,Long userId,Long assistantId);
}
