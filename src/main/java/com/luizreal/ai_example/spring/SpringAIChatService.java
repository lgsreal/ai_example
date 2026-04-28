package com.luizreal.ai_example.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SpringAIChatService {
    private static final int MAX_TURNS_PER_SESSION = 10;
    private final ChatClient.Builder chatClientBuilder;
    private final Map<String, Deque<ChatTurn>> conversations = new ConcurrentHashMap<>();

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

    public ChatResponse runWithMemory(String sessionId, String userPrompt) {
        String normalizedSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId.trim();

        Deque<ChatTurn> turns = conversations.computeIfAbsent(normalizedSessionId, ignored -> new ArrayDeque<>());
        String response;

        synchronized (turns) {
            String promptWithMemory = buildPromptWithMemory(turns, userPrompt);
            response = chatClientBuilder.build()
                    .prompt()
                    .user(promptWithMemory)
                    .call()
                    .content();

            String safeResponse = response == null ? "" : response;
            turns.addLast(new ChatTurn("user", userPrompt));
            turns.addLast(new ChatTurn("assistant", safeResponse));
            trimHistory(turns);
            response = safeResponse;
        }

        return new ChatResponse(normalizedSessionId, response.replace("\n", " "));
    }

    public void resetConversation(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            conversations.remove(sessionId.trim());
        }
    }

    private static String buildPromptWithMemory(Deque<ChatTurn> turns, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful assistant. Keep context from previous messages.\n");
        prompt.append("Conversation history:\n");

        if (turns.isEmpty()) {
            prompt.append("(no previous messages)\n");
        } else {
            for (ChatTurn turn : turns) {
                prompt.append(turn.role()).append(": ").append(turn.content()).append('\n');
            }
        }

        prompt.append("user: ").append(userPrompt).append('\n');
        prompt.append("assistant:");
        return prompt.toString();
    }

    private static void trimHistory(Deque<ChatTurn> turns) {
        int maxEntries = MAX_TURNS_PER_SESSION * 2;
        while (turns.size() > maxEntries) {
            turns.removeFirst();
        }
    }

    public record ChatResponse(String sessionId, String response) {
    }

    private record ChatTurn(String role, String content) {
    }
}
