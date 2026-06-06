# Registros — Modelos OpenAI

## 2026-06-06 — Correção da atualização diária de preços para variantes datadas

- Investigada a tela `/openai-models`, alimentada pelo endpoint backend `GET /api/modelos/openai/catalogo/v1/modelos` e pela tabela `openai_model`.
- Confirmado via MCP que a rotina das 04:00 America/Sao_Paulo executou em 2026-06-06 07:00 UTC e atualizou o modelo-base `gpt-5.5`, mas não atualizou a variante cadastrada `gpt-5.5-2026-04-23`, que permaneceu com preços zerados.
- Corrigida a causa-raiz na sincronização diária: além de criar/atualizar os modelos-base publicados na fonte oficial de preços, a rotina agora percorre os modelos já cadastrados e aplica o preço-base mais específico às variantes datadas sem sobrescrever nome ou código da variante.
- Adicionado teste unitário garantindo que `gpt-5.5-2026-04-23` herda os preços oficiais de `gpt-5.5` e preserva sua identidade operacional.
