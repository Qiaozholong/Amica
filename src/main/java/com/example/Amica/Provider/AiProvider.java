package com.example.Amica.Provider;
import com.example.Amica.Provider.model.ChatRequest;
import com.example.Amica.Provider.model.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface AiProvider {
    ChatResponse chat(ChatRequest request);
}
