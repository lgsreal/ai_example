package com.luizreal.ai_example.spring;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/springai")
@Tag(name = "Basic - Spring AI")
public class SpringAIChatController {

    private static final Logger log = LoggerFactory.getLogger(SpringAIChatController.class);
    private final SpringAIChatService chatService;
    private final long chatTimeoutSeconds;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public SpringAIChatController(
            SpringAIChatService chatService,
            @Value("${app.chat.timeout-seconds:45}") long chatTimeoutSeconds,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${spring.ai.ollama.chat.model:}") String ollamaModel
    ) {
        this.chatService = chatService;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
    }

    @GetMapping("/generate")
    public ResponseEntity<Map> generate(@RequestParam(value = "message", defaultValue = "Java é a melhor linguagem de programação? Responda apenas sim ou não.") String message) {
        return new ResponseEntity<>(Map.of("ollama", chatService.run(message)), HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            return ResponseEntity.ok(Map.of(
                    "reachable", ok,
                    "baseUrl", ollamaBaseUrl,
                    "model", ollamaModel,
                    "statusCode", response.statusCode()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "reachable", false,
                    "baseUrl", ollamaBaseUrl,
                    "model", ollamaModel,
                    "error", rootMessage(ex)
            ));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatMessageResponse> chat(@RequestBody ChatMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new ChatMessageResponse("", "Invalid request body"));
        }

        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(new ChatMessageResponse("", "Message cannot be empty"));
        }

        try {
            CompletableFuture<SpringAIChatService.ChatResponse> future =
                    CompletableFuture.supplyAsync(() -> chatService.runWithMemory(request.sessionId(), message));

            try {
                SpringAIChatService.ChatResponse response = future.get(chatTimeoutSeconds, TimeUnit.SECONDS);
                return ResponseEntity.ok(new ChatMessageResponse(response.sessionId(), response.response()));
            } catch (TimeoutException ex) {
                future.cancel(true);
                log.warn("Chat timeout after {}s. sessionId={}, model={}", chatTimeoutSeconds, request.sessionId(), ollamaModel);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                        .body(new ChatMessageResponse("", "Timed out waiting for model '" + ollamaModel + "' response."));
            }
        } catch (ExecutionException ex) {
            log.warn("Chat execution error: {}", rootMessage(ex));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatMessageResponse("", "Model unavailable at " + ollamaBaseUrl + ". Cause: " + rootMessage(ex)));
        } catch (Exception ex) {
            log.warn("Chat error: {}", rootMessage(ex));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatMessageResponse("", "Model unavailable at " + ollamaBaseUrl + ". Cause: " + rootMessage(ex)));
        }
    }

    @PostMapping("/chat/reset")
    public ResponseEntity<Void> reset(@RequestBody ResetChatRequest request) {
        chatService.resetConversation(request.sessionId());
        return ResponseEntity.noContent().build();
    }

    public record ChatMessageRequest(String sessionId, String message) {
    }

    public record ChatMessageResponse(String sessionId, String response) {
    }

    public record ResetChatRequest(String sessionId) {
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

}
