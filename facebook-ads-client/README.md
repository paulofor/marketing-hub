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
Defina as variáveis de ambiente:

```bash
export FB_ACCESS_TOKEN=<seu_token>
export FB_AD_ACCOUNT_ID=<ad_account>
export FB_ENV=SANDBOX # ou PROD
export HUB_BASE_URL=http://localhost:8000/api
```

Quando `FB_ENV` for `SANDBOX`, os nomes das campanhas criadas recebem o prefixo `[SANDBOX]` e a conta de anúncios de teste é utilizada.

Gere a documentação pública com:

```bash
mvn javadoc:javadoc
```
