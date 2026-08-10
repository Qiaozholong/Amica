package com.example.Amica.Provider.model;

import java.util.ArrayList;
import java.util.List;

public record ChatRequest(
        String systemPrompt,
        List<ChatMessage> messages,
        String model,
        int maxTokens,
        ChatOptions chatOptions
){
    //接收相关参数传递
    public ChatRequest(String systemPrompt,String model,int maxtokens,ChatOptions options){
        this(systemPrompt,new ArrayList<>(),model,maxtokens,options);
    }
    //往数组注入role为user的信息
    public void addUserMessage(String content){
        messages.add(new ChatMessage(ChatMessage.Role.USER,content));
    }
    //同上，为assistant的信息
    public void addAssistantMessage(String content){
        messages.add(new ChatMessage(ChatMessage.Role.ASSISTANT,content));
    }
}
