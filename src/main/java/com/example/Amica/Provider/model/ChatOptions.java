package com.example.Amica.Provider.model;

public record ChatOptions(
        Double temperature,
        Double topP,
        String reasoningEffort,
        Boolean stream
) {
    public static ChatOptions none(){
        return new ChatOptions(null,null,null,null);
    }
    public ChatOptions withTemperature(Double t){
        return new ChatOptions(t,topP,reasoningEffort,stream);
    }
    public ChatOptions withTopP(Double t){
        return new ChatOptions(temperature,t,reasoningEffort,stream);
    }
    public ChatOptions withReasoningEffort(String t){
        return new ChatOptions(temperature,topP,t,stream);
    }
    public ChatOptions withStream(Boolean t){
        return new ChatOptions(temperature,topP,reasoningEffort,t);
    }
}
