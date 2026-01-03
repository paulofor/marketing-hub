# market-research-service

Microserviço Spring Boot responsável por orquestrar pesquisas de mercado. Ele coleta conteúdo de fontes públicas na internet, consolida um snapshot e utiliza o ChatGPT para produzir um sumário acionável.

## Funcionalidades iniciais

- API REST (`/api/v1/market-research`) para disparar pesquisas e acompanhar o status.
- Conexão com o mesmo banco MySQL do backend principal (propriedades `SPRING_DATASOURCE_*`).
- Integração com a API da OpenAI (modelo configurável via `OPENAI_MODEL`, `OPENAI_BASE_URL` e `OPENAI_API_KEY`).
- Coleta de fontes HTTP com timeouts configuráveis e saneamento básico de HTML.
- Documentação automática via Springdoc/OpenAPI em `/swagger-ui.html`.

## Executando localmente

```bash
mvn -s settings.xml spring-boot:run
```

Por padrão, a aplicação usa H2 em memória em testes e MySQL em runtime. Ajuste as variáveis de ambiente conforme necessário:

- `SPRING_DATASOURCE_URL` (padrão `jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb?useSSL=false&serverTimezone=UTC`)
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `OPENAI_API_KEY` (ou configure `OPENAI_API_KEY_FILE` no container)
- `OPENAI_MODEL` (padrão `o3`)
- `OPENAI_BASE_URL` (padrão `https://api.openai.com/v1`)
- `MARKET_RESEARCH_HTTP_TIMEOUT` (padrão `PT12S`)
- `MARKET_RESEARCH_MAX_CONTEXT_LENGTH` (padrão `8000` caracteres)
- `MARKET_RESEARCH_PER_SOURCE_MAX_LENGTH` (padrão `2000` caracteres)

## Docker

```bash
docker build -t marketinghub/market-research-service .
docker run --rm -p 8093:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<pass> \
  -e OPENAI_API_KEY=<token> \
  marketinghub/market-research-service
```

Um `docker-compose.yml` de referência está incluído para rodar o serviço isoladamente.
