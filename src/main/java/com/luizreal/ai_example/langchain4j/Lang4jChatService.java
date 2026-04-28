package com.luizreal.ai_example.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
public class Lang4jChatService {
    private static ChatModel connectModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2")
                .build();
    }

    public String run(String userPrompt) {
        var model = connectModel();
        return model.chat(userPrompt);
    }
}
