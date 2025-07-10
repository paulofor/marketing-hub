# Success Product Worker

Este projeto executa em segundo plano para analisar novos produtos cadastrados no Marketing Hub e enriquecê-los com características de marketing. Ele utiliza as entidades do **ads-service** publicadas no GitHub Packages e roda tarefas agendadas a cada hora.

## Pré-requisitos
- Java 21
- Maven configurado com as variáveis `GITHUB_ACTOR` e `GITHUB_TOKEN` para acessar o repositório do GitHub Packages.
- MySQL em execução conforme as configurações de `application.properties`.

## Como compilar

```bash
mvn -s settings.xml package
```

## Como testar

```bash
mvn -s settings.xml test
```

## Como executar

```bash
mvn spring-boot:run
```

A aplicação agenda a tarefa `SuccessProductScheduler` para rodar a cada hora (`0 0 * * * *`). O método `analyzeNewProducts` busca registros com `novo=true`, chama `ChatGptClient` para preencher os campos e persiste o resultado. Agora a implementação padrão utiliza a API da OpenAI (`OpenAiChatGptClient`). Caso queira utilizar a versão de testes sem chamadas externas, ative o perfil `dummy`.

Para que a integração funcione é necessário definir a variável de ambiente `OPENAI_API_KEY` ou a propriedade `openai.api-key` com o token de acesso. O modelo utilizado pode ser configurado pela propriedade `openai.model` (padrão `o3`).

Durante a execução, o worker registra logs informando o início e o término da tarefa, além de detalhes sobre cada produto processado. Verifique o console para acompanhar o andamento.

Edite `src/main/resources/application.properties` caso precise alterar as credenciais ou a URL do banco de dados.
