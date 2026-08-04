package com.example.aidemotest.Provider.Impl;

import com.example.aidemotest.Common.BusinessException;
import com.example.aidemotest.Entity.ModelEntity;
import com.example.aidemotest.Provider.AiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;

public class OpenAiProvider implements AiProvider {
    //成员变量，避免重复创建对象，确保线程安全
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    //从数据库中提取相关变量
    public OpenAiProvider(ModelEntity model){
        this.baseUrl = model.getBaseUrl();
        this.apiKey = model.getApiKey();
    }
    //重写方法一,发起一次对话
    @Override
    public JsonNode chat(JsonNode requestBody){
        try{
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization","Bearer "+apiKey)
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> resp=httpClient
                    .send(req,HttpResponse.BodyHandlers.ofString());
            return switch (resp.statusCode()){
                case 200->mapper.readTree(resp.body());
                case 401->throw new BusinessException(401,"鉴权失败");
                case 429->throw new BusinessException(429,"限流中");
                case 500->throw new BusinessException(500,"服务端错误");
                default -> throw new BusinessException("API错误"+resp.statusCode()+":"+resp.body());
            };
        }catch(Exception e){
            throw new BusinessException("请求失败:"+e);
        }
    }
    //从JSON中提取对话
    @Override
    public String extractContent(JsonNode body){
    return body.path("choices").get(0)
            .path("message").path("content").asText();
    }
}
