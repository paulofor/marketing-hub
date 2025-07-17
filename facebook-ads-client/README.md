# Facebook Ads Client

Este projeto se comunica com a API do Facebook Ads. Ele utiliza Spring Boot e pode ser executado como um serviço independente ou incluído em outros módulos.

## Pré-requisitos
- Java 21
- Maven configurado com as variáveis `GITHUB_ACTOR` e `GITHUB_TOKEN` se precisar publicar artefatos.

## Como compilar
```bash
mvn -s settings.xml test
```

## Execução
```bash
mvn spring-boot:run
```
Configure as propriedades `facebook.token` e `facebook.version` para acessar a API oficial.
