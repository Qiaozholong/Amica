package com.example.Amica.Service.Impl;

import com.example.Amica.Dto.Messages.MessagesDto;
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
        int maxTokens = dto.getMaxtokens() != 0 ? dto.getMaxtokens() : 1024;
        //查询对话,确认话题id
        ConversationEntity conv = conversationService.getById(conversationId);
        //链路查询，检查通路是否通畅
        AssistantEntity assistant = assistantService.getById(conv.getAssistantId());
        ModelEntity model = modelService.getById(assistant.getModelId());
        ProviderEntity providerEntity = providerService.getById(model.getProviderId());
        //拼接上下文
        //查询历史消息，根据conversationId的检索与Seq的排序呈现
        List<MessagesEntity> history = messagesService
                .lambdaQuery()
                .eq(MessagesEntity::getConversationId, conversationId)
                .orderByAsc(MessagesEntity::getSeq)
                .list();
        //创建初始化ai请求体
        //这是助手级prompt与对话级prompt的补丁，如若未填写对话级prompt，自动补充助手级prompt
        String sysPrompt = conv.getSystemPrompt();
        if (sysPrompt == null || sysPrompt.isBlank()) {
            sysPrompt = assistant.getPrompt();   // 回退到助手默认 prompt
        }

        ChatRequest req = new ChatRequest(
                //初始prompt
                sysPrompt,
                //模型对象
                model.getModelId(),
                //此处接收上方的判断默认值取否
                maxTokens,
                //options的record类型中对应参数的开关状态
                ChatOptions.none()
        );
        //增强for循环中，遍历使数组history中参数类型为MessagesEntity的参数m，接收新创建的chatMessage对象
        for (MessagesEntity m : history) {
            req.messages().add(
                    new ChatMessage(
                            toRole(m.getRole()),
                            m.getContent()));
        }
        //加入用户目前的新消息
        req.addUserMessage(dto.getContent());
        //这个也是补丁，防止消息队列冲突，检查数组中索引最大位置状态，为空则直接填入，非空跳下一个位置
        int nextSeq = history.isEmpty() ? 0 : history.get(history.size() - 1).getSeq() + 1;
        //将用户输入的消息保存到上下文中
        MessagesEntity userMsg = new MessagesEntity();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(dto.getContent());
        userMsg.setSeq(nextSeq);
        messagesService.save(userMsg);
        //发送请求
        AiProvider provider = providerFactory.get(providerEntity);
        ChatResponse resp = provider.chat(req);
        //保存接收到的assistant信息
        MessagesEntity asstMsg = new MessagesEntity();
        asstMsg.setConversationId(conversationId);
        asstMsg.setRole("assistant");
        asstMsg.setContent(resp.content());
        asstMsg.setSeq(nextSeq + 1);
        messagesService.save(asstMsg);
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
