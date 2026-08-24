package com.example.Amica.Provider.Impl;

import com.example.Amica.Common.BusinessException;
import com.example.Amica.Entity.ProviderEntity;
import com.example.Amica.Provider.AiProvider;
import com.example.Amica.Provider.model.ChatMessage;
import com.example.Amica.Provider.model.ChatRequest;
import com.example.Amica.Provider.model.ChatResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAiProvider implements AiProvider {
    private final String baseUrl;
    private final String apiKey;
    private final TextEncryptor apiKeyEncryptor;
    private final HttpClient httpClient =HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    public OpenAiProvider(ProviderEntity provider,TextEncryptor apiKeyEncryptor) {
        this.baseUrl = provider.getBaseUrl();
        this.apiKey = apiKeyEncryptor.decrypt(provider.getApiKey());
        this.apiKeyEncryptor = apiKeyEncryptor;
    }
    //固定格式，除Exception外无业务耦合
    @Override
    public ChatResponse chat(ChatRequest request){
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildBody(request).toString()))
                    .build();
            HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return switch (resp.statusCode()) {
                case 200 -> parse(resp.body());
                case 401 -> throw new BusinessException(401, "鉴权失败");
                case 429 -> throw new BusinessException(429, "限流中");
                case 500 -> throw new BusinessException(500, "服务端错误");
                default -> throw new BusinessException("API错误" + resp.statusCode() + ":" + resp.body());
            };
        } catch (Exception e) {
            throw new BusinessException("请求失败:" + e.getMessage());
        }
    }

    // OpenAI 特有：system 作为第一条 message，可选参数映射
    private ObjectNode buildBody(ChatRequest req) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode msgs = root.putArray("messages");
        //检测是否存在prompt，存在就套入
        if (req.systemPrompt() != null && !req.systemPrompt().isBlank()) {
            msgs.addObject().put("role", "system").put("content", req.systemPrompt());
        }

        for (ChatMessage m : req.messages()) {
            msgs.addObject().put("role", m.toApiRole()).put("content", m.content());
        }

        root.put("model", req.model());
        root.put("max_tokens", req.maxTokens());

        if (req.options() != null) {
            if (req.options().temperature() != null) root.put("temperature", req.options().temperature());
            if (req.options().topP() != null) root.put("top_p", req.options().topP());
            if (req.options().reasoningEffort() != null) root.put("reasoning_effort", req.options().reasoningEffort());
            if (req.options().stream() != null) root.put("stream", req.options().stream());
        }
        return root;
    }

    private ChatResponse parse(String body) throws Exception {
        JsonNode root = mapper.readTree(body);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        String model = root.path("model").asText();
        int input = root.path("usage").path("prompt_tokens").asInt();
        int output = root.path("usage").path("completion_tokens").asInt();
        return new ChatResponse(content, model, input, output);
    }

}
