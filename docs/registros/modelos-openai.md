# Registros — Modelos OpenAI

## 2026-07-09 — Correção da fonte da rotina diária de preços

- Confirmado que a rotina diária falhava porque tentava obter preços financeiros em `GET /models`, mas essa API não entregava metadados de preço suficientes para sincronização.
- Corrigida a causa-raiz: a rotina de preços agora consulta a página oficial pública de pricing da OpenAI (`https://developers.openai.com/api/docs/pricing`), extrai as tabelas de preço por 1M tokens e persiste Standard/Batch no catálogo local.
- Mantida a API `/models` apenas como fonte técnica de existência/capacidade dos modelos; preço financeiro passa a registrar a URL de pricing como `pricing_source`.
- Adicionados testes para parsing de modelos de texto, modelos de imagem e ausência de Batch, impedindo voltar a inventar preço ou depender de `/models` para custo.

## 2026-06-06 — Correção da atualização diária de preços para variantes datadas

- Investigada a tela `/openai-models`, alimentada pelo endpoint backend `GET /api/modelos/openai/catalogo/v1/modelos` e pela tabela `openai_model`.
- Confirmado via MCP que a rotina das 04:00 America/Sao_Paulo executou em 2026-06-06 07:00 UTC e atualizou o modelo-base `gpt-5.5`, mas não atualizou a variante cadastrada `gpt-5.5-2026-04-23`, que permaneceu com preços zerados.
- Corrigida a causa-raiz na sincronização diária: além de criar/atualizar os modelos-base publicados na fonte oficial de preços, a rotina agora percorre os modelos já cadastrados e aplica o preço-base mais específico às variantes datadas sem sobrescrever nome ou código da variante.
- Adicionado teste unitário garantindo que `gpt-5.5-2026-04-23` herda os preços oficiais de `gpt-5.5` e preserva sua identidade operacional.

## 2026-06-07 — Ordenação da listagem por preço

- Ajustada a tela `/openai-models` para ordenar os modelos do mais caro para o mais barato usando o maior preço cadastrado entre as colunas standard e batch, preservando desempate determinístico por nome.
- Adicionado teste unitário para garantir a ordenação decrescente por preço e o desempate por nome.
