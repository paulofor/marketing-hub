# Fashion Chat Service

Modulo piloto para produtos `AI_SANDBOX_CONVERSATIONAL_PRODUCT`.

O servico expoe um chat HTTP independente do `backend/ads-service`. O Marketing Hub chama este modulo diretamente.

## Endpoints

- `GET /health`
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

Para habilitar o Codex App Server:

```bash
CODEX_APP_SERVER_ENABLED=true npm start
```

O piloto nao clona repositorio. A sandbox e criada como diretorio temporario local, recebe contexto de pesquisa de moda e executa o turno do App Codex Server quando ele estiver disponivel e autenticado.

O servico nao usa `OPENAI_API_KEY` nem cliente OpenAI direto como fallback. Se o Codex App Server nao estiver disponivel, a resposta cai no fallback deterministico local.

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
curl -fsS http://191.252.210.83:8094/health
```

Os logs do container podem ser consultados pelo MCP via tool `chat_container_logs`, limitada por allowlist ao container `marketinghub-fashion-chat`.
