package com.luizreal.ai_example.langchain4j;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/langchain4j")
@Tag(name = "Basic - langchain4j")
public class Lang4jChatController {
    private final Lang4jChatService chatService;

    public Lang4jChatController(Lang4jChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/generate")
    public ResponseEntity<Map> generate(@RequestParam(value = "message", defaultValue = "Java é a melhor linguagem de programação? Responda apenas sim ou não.") String message) {
        return new ResponseEntity<>(Map.of("ollama", chatService.run(message)), HttpStatus.OK);
    }
}
