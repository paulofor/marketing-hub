# Fashion Chat Service

Modulo piloto para produtos `AI_SANDBOX_CONVERSATIONAL_PRODUCT`.

O servico expoe um chat HTTP independente do `backend/ads-service`. O Marketing Hub chama este modulo diretamente.

## Endpoints

- `GET /health`
- `GET /codex-app-server/account/read`
- `POST /codex-app-server/account/login/start`
- `POST /codex-app-server/account/login/cancel`
- `POST /codex-app-server/account/logout`
- `POST /api/fashion-chat/messages`

Body:

```json
{
  "message": "Qual look usar em uma reuniao casual?",
  "customerId": "cliente-demo"
}
```

## Execucao local

```bash
npm install
npm run build
PORT=8094 npm start
```

Para executar com Codex App Server local:

```bash
CODEX_APP_SERVER_ENABLED=true npm start
```

O piloto nao clona repositorio. A sandbox e criada como diretorio temporario local, recebe contexto de pesquisa de moda e executa o turno do App Codex Server.

O servico tenta usar o Codex App Server como resposta principal:

- nao usa `OPENAI_API_KEY`;
- nao usa cliente OpenAI direto;
- se o Codex App Server nao estiver pronto ou autenticado, `POST /api/fashion-chat/messages` responde em modo `local_fallback` para nao quebrar a conversa do cliente;
- a prontidao operacional em `GET /health/ready` so retorna `200` quando o Codex App Server estiver pronto e autenticado;
- `FASHION_CHAT_FORCE_FALLBACK=true` forca o modo local para validacao operacional.

Na imagem Docker, o CLI `codex` fica instalado e o `CODEX_APP_SERVER_ENABLED` vem habilitado por padrao. O volume `fashion-chat-codex-home` preserva o `CODEX_HOME` entre reinicios do container.

## Autenticacao da sandbox Codex

A sandbox precisa estar autenticada no `CODEX_HOME` persistente do container. Sem isso, o chat ainda pode responder em `local_fallback`, mas o healthcheck fica degradado e `GET /health/ready` retorna `503`.

Procedimento operacional no host:

```bash
cd /opt/marketinghub/containers/fashion-chat-service
docker compose exec fashion-chat-service codex login
docker compose exec fashion-chat-service codex app-server --help >/dev/null
curl -fsS http://localhost:8094/health/ready
```

O comando de login deve gravar a sessao no volume `fashion-chat-codex-home`, usando `CODEX_HOME=/var/lib/ai-hub/codex`. Nao use `OPENAI_API_KEY` para este servico.

Alternativa via API, alinhada ao fluxo do `/exemplos/aih6`:

```bash
curl -fsS -X POST http://localhost:8094/codex-app-server/account/login/start \
  -H 'Content-Type: application/json' \
  -d '{"type":"chatgptDeviceCode"}'
curl -fsS http://localhost:8094/codex-app-server/account/read
curl -fsS http://localhost:8094/health/ready
```

Abra a URL retornada pelo primeiro comando, informe o codigo de usuario e valide que `account/read` retorna `connected: true`.

## Deploy no host do MCP

O workflow `.github/workflows/fashion-chat-service-ci.yml` publica a imagem no GHCR e faz deploy no mesmo host do MCP server (`191.252.210.83`), em:

```bash
/opt/marketinghub/containers/fashion-chat-service
```

Container padrao:

- `marketinghub-fashion-chat`
- porta publica: `8094`
- health-check: `GET /health`

Validacao operacional apos deploy:

```bash
curl -fsS http://191.252.210.83:8094/health/ready
```

Os logs do container podem ser consultados pelo MCP via tool `chat_container_logs`, limitada por allowlist ao container `marketinghub-fashion-chat`.
