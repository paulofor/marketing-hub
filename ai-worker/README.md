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
     -v ${OPENAI_API_KEY_HOST_FILE:-/root/infra/openai-token/openai_api_key}:/run/secrets/openai_api_key:ro \
     marketinghub/ai-worker:latest
   ```

### Publicar a imagem

```bash
docker tag marketinghub/ai-worker:latest registry.seudominio.com/marketinghub/ai-worker:latest
docker push registry.seudominio.com/marketinghub/ai-worker:latest
```

Atualize o host, o namespace do registro e as credenciais conforme o ambiente.

A aplicação agenda a tarefa `SuccessProductScheduler` para rodar a cada cinco minutos (`0 */5 * * * *`). O método `analyzeNewProducts` busca registros com `novo=true`, chama `ChatGptClient` para preencher os campos (incluindo `name`) e persiste o resultado. Agora a implementação padrão utiliza a API da OpenAI (`OpenAiChatGptClient`). Caso queira utilizar a versão de testes sem chamadas externas, ative o perfil `dummy`.

Para que a integração funcione é necessário definir a variável de ambiente `OPENAI_API_KEY` ou a propriedade `openai.api-key` com o token de acesso. O modelo utilizado pode ser configurado pela propriedade `openai.model` (padrão `gpt-5.2`).
Caso queira permitir buscas na Internet pelo modelo, defina também `GOOGLE_API_KEY` e `GOOGLE_SEARCH_ID` ou as propriedades `google.api-key` e `google.search-id` com as credenciais do Google Search.

### Ajustes do processamento em lote da OpenAI

- `OPENAI_BATCH_TIMEOUT` / `openai.batch-timeout`: controla quanto tempo o worker aguardará a conclusão de um batch da OpenAI antes de desistir. O padrão é `PT30M` (30 minutos).
- `OPENAI_BATCH_POLL_INTERVAL` / `openai.batch-poll-interval`: define o intervalo entre cada verificação de status do batch. O padrão é `PT0.5S` (500 ms).

Durante a execução, o worker registra logs informando o início e o término da tarefa, além de detalhes sobre cada produto processado. Verifique o console para acompanhar o andamento.

Edite `src/main/resources/application.properties` caso precise alterar as credenciais ou a URL do banco de dados.


### Logs em arquivo no container

O `docker-compose.yml` do AI Worker agora define `LOGGING_FILE_NAME` para persistir logs em arquivo dentro do container.

- Caminho padrão do arquivo: `/var/log/ai-worker/application.log`
- Diretório no host (bind mount): `./logs` (configurável com `AI_WORKER_LOG_DIR`)
- Arquivo de log (configurável com `AI_WORKER_LOG_FILE`)

Exemplo de override:

```bash
AI_WORKER_LOG_DIR=/var/log/marketinghub/ai-worker \
AI_WORKER_LOG_FILE=/var/log/ai-worker/worker.log \
docker compose up -d
```

## Avatar Sales Video

O worker agora consome jobs do módulo de Avatar Sales Video exclusivamente via APIs internas do backend. O ciclo é:

1. `SalesVideoScriptJobScheduler` executa a cada ~45 segundos (pode ser alterado por `SALES_VIDEO_SCRIPT_FIXED_DELAY`).
2. `SalesVideoScriptJobService` lista jobs `SCRIPT_PENDING`, faz o *claim*, monta o prompt com `SalesVideoPromptBuilder` e chama a OpenAI.
3. O resultado estruturado (hook, script, CTA, legenda e storyboard em JSON) é enviado ao backend através do endpoint `/internal/ai/openai-jobs/{id}/complete`.
4. Falhas técnicas ou indisponibilidade do provider são reportadas usando `/fail` para manter o backend como fonte de verdade.

Variáveis relevantes:

- `SALES_VIDEO_SCRIPT_ENABLED`: desliga o polling quando necessário.
- `SALES_VIDEO_WORKER_ID`: identifica a instância do worker ao fazer *claim* dos jobs.
- `SALES_VIDEO_SCRIPT_MAX_JOBS`: controla quantos jobs são buscados por batelada.
- `SALES_VIDEO_SCRIPT_MAX_OUTPUT_TOKENS`: define o limite de tokens da resposta JSON da OpenAI.

O cliente de OpenAI utiliza o endpoint oficial de Responses e o formato `json_schema` para garantir que o retorno seja sempre serializável. Isso reduz retrabalho manual no backend e mantém o processo totalmente automatizado.
