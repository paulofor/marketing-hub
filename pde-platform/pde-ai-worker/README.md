# PDE AI Worker

Executor direcionado para orientações por IA dentro do Produto Digital Experiencial.

## Responsabilidade

- Buscar pendências no backend PDE pelo endpoint `pending`.
- Chamar OpenAI com prompt e schema versionados.
- Retornar resultado estruturado e auditoria ao backend.

O worker não entrega chat aberto. A Consultora MUSA usa contratos fechados por
missão para transformar as respostas da cliente em um cartão curto, prático e
vendável nos 7 dias do método.

## Execução

```bash
OPENAI_API_KEY=... \
PDE_BACKEND_URL=http://localhost:8096 \
npm start
```

Variáveis principais:

- `OPENAI_API_KEY`: chave da OpenAI para execução real.
- `OPENAI_MODEL`: modelo textual, padrão `gpt-5.4-mini`.
- `PDE_BACKEND_URL`: backend PDE, padrão `http://pde-platform-backend:8096`.
- `POLL_INTERVAL_MS`: intervalo de polling, padrão `4000`.
