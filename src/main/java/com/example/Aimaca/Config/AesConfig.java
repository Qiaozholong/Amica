package com.example.Aimaca.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class AesConfig {
    @Value("${app.encrypt-key}")
    private String encryptKey;
    @Value("${app.salt}")
    private String salt;

    @Bean("apiKeyEncryptor")
    public TextEncryptor apiKeyEncryptor() {
        return Encryptors.text(encryptKey, salt);
    }
}
