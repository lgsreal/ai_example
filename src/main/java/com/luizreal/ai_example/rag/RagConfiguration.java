package com.luizreal.ai_example.rag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Configuration
public class RagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagConfiguration.class);

    @Value("${app.rag.vector-store-file:vectorstore.json}")
    private String vectorStoreName;

    @Value("classpath:/rfc7231.txt")
    private Resource documentFromTXT;

    private final EmbeddingModel embeddingModel;

    public RagConfiguration(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public SimpleVectorStore getSimpleVectorStore() {

        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var vectorStoreFile = getVectorStoreFile();
        if (vectorStoreFile.exists()) {

            log.info("Usando arquivo do Vector Store");
            try {
                simpleVectorStore.load(vectorStoreFile);
            } catch (RuntimeException ex) {
                log.warn("Falha ao carregar Vector Store existente. Recriando arquivo no formato atual. Causa: {}", ex.getMessage());
                backupInvalidVectorStore(vectorStoreFile);
                rebuildVectorStore(simpleVectorStore, vectorStoreFile);
            }

        } else {
            rebuildVectorStore(simpleVectorStore, vectorStoreFile);
        }
        return simpleVectorStore;
    }

    private void rebuildVectorStore(SimpleVectorStore simpleVectorStore, File vectorStoreFile) {

        long startTime = System.currentTimeMillis();

        log.info("Criando Vector Store, carregando...");
        var textReader = new TextReader(documentFromTXT);
        textReader.getCustomMetadata().put("filename", "rfc7231.txt");

        List<Document> documents = textReader.get();
        log.info("Total de AI Documents: %d".formatted(documents.size()));

        var textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);
        simpleVectorStore.add(splitDocuments);

        log.info("Total de Tokens: %d".formatted(splitDocuments.size()));
        simpleVectorStore.save(vectorStoreFile);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        log.info("Tempo de execução : {} ms", executionTime);
    }

    private void backupInvalidVectorStore(File vectorStoreFile) {
        Path source = vectorStoreFile.toPath();
        Path backup = source.resolveSibling(source.getFileName() + ".bak");

        try {
            Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo antigo do Vector Store movido para {}", backup);
        } catch (IOException ioEx) {
            log.warn("Não foi possível criar backup do Vector Store antigo. Tentando sobrescrever o arquivo atual.");
        }
    }

    private File getVectorStoreFile() {

        return Paths.get(vectorStoreName).toFile().getAbsoluteFile();

    }

}
