package com.example.Amica.Provider.model;

public record ChatMessage(Role role,String content) {
    public enum Role {USER,ASSISTANT}
    public String toApiRole(){
        return switch(role){
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }
}
