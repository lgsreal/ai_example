package com.luizreal.ai_example;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info =
@Info(title = "Spring Ollama API", version = "0.0.1", description = "Exemplos de Spring AI com Ollama"))
public class AiExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiExampleApplication.class, args);
    }

}
