package com.luizreal.ai_example.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/rag")
@Tag(name = "RAG")
public class RagFileDbController {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(RagFileDbController.class);
    @Value("classpath:/rag-prompt-template.st")
    private Resource ragPromptTemplate;
    private final RagConfiguration ragConfiguration;

    public RagFileDbController(ChatClient.Builder builder, RagConfiguration ragConfiguration) {
        this.ragConfiguration = ragConfiguration;
        this.chatClient = builder.build();
    }

    @GetMapping("/generate")
    public ResponseEntity<String> generate(@RequestParam(value = "message", defaultValue = "Qual o código do status de Bad Request?") String message) throws IOException {

        List<Document> similarDocuments = ragConfiguration.getSimpleVectorStore().similaritySearch(SearchRequest.query(message).withTopK(2));
        List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate.getContentAsString(StandardCharsets.UTF_8));
        var promptParameters = new HashMap<String,Object>();
        promptParameters.put("input",message);
        promptParameters.put("documents",String.join("\n",contentList));
        var prompt = promptTemplate.create(promptParameters);

        log.info(prompt.getContents());

        return new ResponseEntity<>(chatClient.prompt(prompt).call().content(), HttpStatus.OK);
    }

}
