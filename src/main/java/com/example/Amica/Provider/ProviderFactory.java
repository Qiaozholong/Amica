//package com.example.Amica.Provider;
//
//import com.example.Amica.Common.BusinessException;
//import com.example.Amica.Entity.ProviderEntity;
//import com.example.Amica.Provider.Impl.AnthropicProvider;
//import com.example.Amica.Provider.Impl.OpenAiProvider;
//
//public class ProviderFactory {
//    public AiProvider get(ProviderEntity provider) {
//        return switch(provider.getProtocol()){
//            case"openai" ->new OpenAiProvider(provider);
//            case"anthropic"->new AnthropicProvider(provider);
//            default -> throw new BusinessException(401,provider.getProtocol());
//        }
//    }
//}
