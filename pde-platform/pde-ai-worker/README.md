# PDE AI Worker

Executor direcionado para orientações por IA dentro do Produto Digital Experiencial.

## Responsabilidade

- Buscar pendências no backend PDE pelo endpoint `pending`.
- Chamar OpenAI com prompt e schema versionados.
- Retornar resultado estruturado e auditoria ao backend.

O worker não entrega chat aberto. A primeira etapa implementada é a orientação `MUSA_DAY_2_SIGNATURE`, usada para transformar os 3 sinais do Dia 2 em uma assinatura MUSA curta, prática e vendável.

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

