# Success Product Worker

Este projeto executa em segundo plano para analisar novos produtos cadastrados no Marketing Hub e enriquecê-los com características de marketing. Ele utiliza as entidades do **ads-service** publicadas no GitHub Packages e roda tarefas agendadas a cada hora.

## Pré-requisitos
- Java 21
- Maven configurado com as variáveis `GITHUB_ACTOR` e `GITHUB_TOKEN` para acessar o repositório do GitHub Packages.
- MySQL em execução conforme as configurações de `application.yml`.

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

A aplicação agenda a tarefa `SuccessProductScheduler` para rodar a cada hora (`0 0 * * * *`). O método `analyzeNewProducts` busca registros com `novo=true`, chama `ChatGptClient` (no momento implementado por `DummyChatGptClient`) para preencher os campos e persiste o resultado.

Edite `src/main/resources/application.yml` caso precise alterar as credenciais ou a URL do banco de dados.
