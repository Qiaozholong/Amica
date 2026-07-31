package com.example.aidemotest.Provider;
import com.fasterxml.jackson.databind.JsonNode;

public interface AiProvider {
    //发起一次对话
    JsonNode chat(JsonNode requestBody);
    //从JSON中提取对话
    String extractContent(JsonNode requestBody);
}
