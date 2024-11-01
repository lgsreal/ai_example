package com.luizreal.ai_example.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAIChatService {
    private final ChatClient.Builder chatClientBuilder;

    public SpringAIChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public String run(String userPrompt) {

        var chatClient = chatClientBuilder.build();

        return chatClient
                .prompt()
                .user(userPrompt)
                .call()
                .content()
                .replace("\n", "");
    }
}
