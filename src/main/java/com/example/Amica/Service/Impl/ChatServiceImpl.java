package com.example.Amica.Service.Impl;

import com.example.Amica.Dto.MessagesDto;
import com.example.Amica.Entity.*;
import com.example.Amica.Provider.AiProvider;
import com.example.Amica.Provider.ProviderFactory;
import com.example.Amica.Provider.model.ChatMessage;
import com.example.Amica.Provider.model.ChatOptions;
import com.example.Amica.Provider.model.ChatRequest;
import com.example.Amica.Provider.model.ChatResponse;
import com.example.Amica.Service.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    private final ConversationService conversationService;
    private final AssistantService assistantService;
    private final ModelService modelService;
    private final ProviderService providerService;
    private final MessagesService messagesService;
    private final ProviderFactory providerFactory;

    //构建方法注入
    public ChatServiceImpl(
            ConversationService conversationService,
            AssistantService assistantService,
            ModelService modelService,
            ProviderService providerService,
            MessagesService messagesService,
            ProviderFactory providerFactory
    ) {
        this.conversationService = conversationService;
        this.assistantService = assistantService;
        this.modelService = modelService;
        this.providerService = providerService;
        this.messagesService = messagesService;
        this.providerFactory = providerFactory;
    }

    //请求体
    @Override
    public ChatResponse sendMessage(Long conversationId, MessagesDto dto) {
        //检测传回的tokens是否为零，默认为1024
        int maxTokens = dto.getMaxtokens()!= 0 ? dto.getMaxtokens() : 1024;
        //查询对话
        ConversationEntity conv = conversationService.getById(conversationId);
        //链路查询
        AssistantEntity assistant = assistantService.getById(conv.getAssistantId());
        ModelEntity model = modelService.getById(assistant.getModelId());
        ProviderEntity providerEntity = providerService.getById(model.getProviderId());
        //拼接上下文

        List<MessagesEntity> history = messagesService
                .lambdaQuery()
                .eq(MessagesEntity::getConversationId, conversationId)
                .orderByAsc(MessagesEntity::getSeq)
                .list();
        ChatRequest req = new ChatRequest(conv.getSystemPrompt(), model.getModelId(), dto.getMaxtokens(), ChatOptions.none());
        for (MessagesEntity m : history) {
            req.messages().add(new ChatMessage(
                    toRole(m.getRole()),
                    m.getContent()));
        }
        req.addUserMessage(dto.getContent());
        //发送请求
        AiProvider provider = providerFactory.get(providerEntity);
        ChatResponse resp = provider.chat(req);
        return resp;

    }

    //toRole方法,这是role的补丁
    private ChatMessage.Role toRole(String role) {
        return switch (role) {
            case "user" -> ChatMessage.Role.USER;
            case "assistant" -> ChatMessage.Role.ASSISTANT;
            default -> throw new RuntimeException("未知角色: " + role);
        };
    }
}
