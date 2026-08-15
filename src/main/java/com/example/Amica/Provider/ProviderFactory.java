package com.example.Amica.Provider;

import com.example.Amica.Common.BusinessException;
import com.example.Amica.Entity.ProviderEntity;
import com.example.Amica.Provider.Impl.AnthropicProvider;
import com.example.Amica.Provider.Impl.OpenAiProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class ProviderFactory {
    private final TextEncryptor apiKeyEncryptor;
    public ProviderFactory(@Qualifier("apiKeyEncryptor") TextEncryptor apiKeyEncryptor) {
        this.apiKeyEncryptor = apiKeyEncryptor;
    }
    public AiProvider get(ProviderEntity provider) {
        return switch(provider.getProtocol()){
            case"openai" ->new OpenAiProvider(provider, apiKeyEncryptor);
//            case"anthropic"->new AnthropicProvider(provider);
            default -> throw new BusinessException(401,provider.getProtocol());
        };
    }
}
