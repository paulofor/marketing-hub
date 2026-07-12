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
