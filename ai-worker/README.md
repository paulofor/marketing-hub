# AI Worker

Este projeto executa em segundo plano para processar dados cadastrados no Marketing Hub com auxílio do ChatGPT. Ele reutiliza as entidades do **ads-service** publicadas no GitHub Packages, garantindo que o modelo de dados esteja sempre alinhado ao backend. Durante o processamento, campos como `name` podem ser gerados pela IA.

## Pré-requisitos
- Java 21
- Maven
- MySQL em execução conforme as configurações de `application.properties`.

> Antes de compilar o worker, publique o **ads-service** com `cd backend/ads-service && mvn -s ../settings.xml deploy` para disponibilizar o artefato no GitHub Packages.

## Como compilar

```bash
mvn package
```

## Como testar

```bash
mvn test
```

## Como executar

```bash
mvn spring-boot:run
```

## Executar com Docker

> Antes de gerar a imagem, publique o **ads-service** no GitHub Packages com `cd backend/ads-service && mvn -s ../settings.xml deploy`.

1. Gere a imagem localmente:
   ```bash
   docker build -t marketinghub/ai-worker:latest ./ai-worker
   ```
2. Rode o container informando as variáveis obrigatórias:
   ```bash
   docker run -d --name ai-worker \
     -e SPRING_DATASOURCE_URL="jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb" \
     -e SPRING_DATASOURCE_USERNAME="marketing_hub_user" \
     -e MYSQL_PASS="<senha-do-banco>" \
     -e OPENAI_API_KEY="<token-openai>" \
     marketinghub/ai-worker:latest
   ```

   Caso a chave da OpenAI esteja armazenada em um arquivo no servidor, defina o caminho via `OPENAI_API_KEY_FILE` e monte o arquivo como volume:

   ```bash
   docker run -d --name ai-worker \
     -e SPRING_DATASOURCE_URL="jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb" \
     -e SPRING_DATASOURCE_USERNAME="marketing_hub_user" \
     -e MYSQL_PASS="<senha-do-banco>" \
     -e OPENAI_API_KEY_FILE="/run/secrets/openai_api_key" \
     -v /etc/openai/chave:/run/secrets/openai_api_key:ro \
     marketinghub/ai-worker:latest
   ```

### Publicar a imagem

```bash
docker tag marketinghub/ai-worker:latest registry.seudominio.com/marketinghub/ai-worker:latest
docker push registry.seudominio.com/marketinghub/ai-worker:latest
```

Atualize o host, o namespace do registro e as credenciais conforme o ambiente.

A aplicação agenda a tarefa `SuccessProductScheduler` para rodar a cada cinco minutos (`0 */5 * * * *`). O método `analyzeNewProducts` busca registros com `novo=true`, chama `ChatGptClient` para preencher os campos (incluindo `name`) e persiste o resultado. Agora a implementação padrão utiliza a API da OpenAI (`OpenAiChatGptClient`). Caso queira utilizar a versão de testes sem chamadas externas, ative o perfil `dummy`.

Para que a integração funcione é necessário definir a variável de ambiente `OPENAI_API_KEY` ou a propriedade `openai.api-key` com o token de acesso. O modelo utilizado pode ser configurado pela propriedade `openai.model` (padrão `o3`).
Caso queira permitir buscas na Internet pelo modelo, defina também `GOOGLE_API_KEY` e `GOOGLE_SEARCH_ID` ou as propriedades `google.api-key` e `google.search-id` com as credenciais do Google Search.

Durante a execução, o worker registra logs informando o início e o término da tarefa, além de detalhes sobre cada produto processado. Verifique o console para acompanhar o andamento.

Edite `src/main/resources/application.properties` caso precise alterar as credenciais ou a URL do banco de dados.
