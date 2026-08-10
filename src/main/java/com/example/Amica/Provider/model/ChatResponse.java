package com.example.Amica.Provider.model;

public record ChatResponse(
   String content,
   String model,
   int inputTokens,
   int outputtokens
) {}
