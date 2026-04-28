# AI Example - Spring AI, LangChain4j e Ollama

Projeto de exemplo para demonstrar o uso de IA generativa em Java com Spring Boot, Spring AI, LangChain4j e Ollama. A ideia é servir como material didático para comparar duas formas de conversar com um modelo local e também mostrar um exemplo simples de RAG usando um arquivo de texto.

## Tecnologias usadas

- Java 21
- Spring Boot 3.5.9
- Gradle
- Spring AI 1.1.4
- LangChain4j 1.12.2
- Ollama
- Thymeleaf, para a página web de chat
- Springdoc OpenAPI, para documentação Swagger
- SimpleVectorStore do Spring AI, para o exemplo de RAG

## Como preparar o Ollama

Instale o Ollama e baixe o modelo usado no projeto:

```bash
ollama pull llama3.2:latest
```

Depois deixe o Ollama rodando:

```bash
ollama serve
```

Em outro terminal, você também pode testar o modelo diretamente:

```bash
ollama run llama3.2:latest
```

Por padrão, o Ollama fica disponível em:

```text
http://localhost:11434
```

## Como rodar o projeto

No Windows:

```bash
gradlew.bat bootRun
```

No Linux ou macOS:

```bash
./gradlew bootRun
```

Depois acesse:

```text
http://localhost:8080
```

## Página web

A aplicação possui uma página web em:

```text
GET /
```

Essa página abre um chat simples feito com Thymeleaf e JavaScript. Ela usa os endpoints do Spring AI:

- `POST /springai/chat`, para enviar mensagens.
- `POST /springai/chat/reset`, para limpar a conversa.
- `GET /springai/health`, para verificar se o Ollama está acessível.

O chat mantém um histórico curto em memória por `sessionId`, para simular memória de conversa.

## Endpoints disponíveis

### Spring AI

```text
GET /springai/generate?message=Sua pergunta
```

Envia uma pergunta simples para o modelo usando Spring AI.

Exemplo:

```bash
curl "http://localhost:8080/springai/generate?message=Explique%20Spring%20AI"
```

```text
POST /springai/chat
```

Envia uma mensagem para o chat com memória.

Corpo da requisição:

```json
{
  "sessionId": "",
  "message": "Explique o que e Spring AI"
}
```

Resposta:

```json
{
  "sessionId": "id-da-sessao",
  "response": "resposta do modelo"
}
```

```text
POST /springai/chat/reset
```

Remove o histórico de uma sessão.

Corpo da requisição:

```json
{
  "sessionId": "id-da-sessao"
}
```

```text
GET /springai/health
```

Verifica se o Ollama está respondendo e mostra o modelo configurado.

### LangChain4j

```text
GET /langchain4j/generate?message=Sua pergunta
```

Envia uma pergunta simples para o modelo usando LangChain4j.

Exemplo:

```bash
curl "http://localhost:8080/langchain4j/generate?message=Explique%20LangChain4j"
```

### RAG com Spring AI

```text
GET /rag/generate?message=Sua pergunta
```

Executa uma pergunta usando RAG. O projeto carrega o arquivo `src/main/resources/rfc7231.txt`, divide o texto em partes, grava ou reutiliza o `vectorstore.json` e busca trechos relacionados para montar o prompt.

Exemplo:

```bash
curl "http://localhost:8080/rag/generate?message=Qual%20o%20codigo%20do%20status%20Bad%20Request?"
```

## Swagger

A documentação dos endpoints fica disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

## Como mudar o modelo

### Spring AI

No arquivo `src/main/resources/application.properties`, altere:

```properties
spring.ai.ollama.chat.model=llama3.2
spring.ai.ollama.embedding.model=llama3.2
```

Para usar explicitamente a tag `latest`, você pode deixar assim:

```properties
spring.ai.ollama.chat.model=llama3.2:latest
spring.ai.ollama.embedding.model=llama3.2:latest
```

Se quiser usar outro modelo, primeiro baixe com o Ollama:

```bash
ollama pull nome-do-modelo:tag
```

Depois altere as propriedades para o mesmo nome.

### LangChain4j

No arquivo `src/main/java/com/luizreal/ai_example/langchain4j/Lang4jChatService.java`, altere o valor de `modelName`:

```java
return OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama3.2")
        .build();
```

Para usar a tag `latest`:

```java
return OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama3.2:latest")
        .build();
```

Se trocar para outro modelo, use o mesmo nome baixado pelo comando `ollama pull`.

## Observações para aula

- O endpoint `/springai/generate` mostra o uso direto do Spring AI.
- O endpoint `/langchain4j/generate` mostra o uso direto do LangChain4j.
- A página `/` usa Spring AI com memória simples em memória da aplicação.
- O endpoint `/rag/generate` demonstra uma consulta com RAG usando um documento local e um vector store simples.
- O projeto depende do Ollama em execução local para responder as chamadas de IA.
