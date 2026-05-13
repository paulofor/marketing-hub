# Coletor Hotmart — Ciclo 1 e Ciclo 2

Este documento descreve a estratégia operacional de coleta em dois ciclos para o coletor Hotmart.

## Visão geral

- **Ciclo 1 (horas ímpares):** busca a listagem de produtos na Hotmart (`v2/market/search`) e persiste snapshots base no backend.
- **Ciclo 2 (horas pares):** lê os produtos já persistidos no backend (resultado do ciclo 1), consulta detalhes produto a produto em `v1/market/product/{id}/details` e atualiza os dados com foco em `salesPageUrl`.

## Regras de agendamento

O scheduler executa de hora em hora e decide o ciclo com base na hora atual:

- Hora ímpar (`hour % 2 != 0`) → executa `collectFirstCycle(...)`.
- Hora par (`hour % 2 == 0`) → executa `collectSecondCycleFromBackend(...)`.

## Ciclo 1 — Listagem

1. Buscar token JWT da Hotmart em configuração geral do backend.
2. Chamar `POST https://api-affiliation-market.hotmart.com/v2/market/search`.
3. Mapear snapshots base dos produtos.
4. Persistir no backend em `/api/v1/mois/persistence/collection-jobs/{jobId}`.

## Ciclo 2 — Detalhes por produto

1. Buscar token JWT da Hotmart em configuração geral do backend.
2. Buscar produtos persistidos no backend via `/api/v1/mois/hotmart/products`.
3. Para cada produto, chamar:
   - `GET https://api-affiliation-market.hotmart.com/v1/market/product/{id}/details?userSessionId={session}`
4. Enriquecer snapshot priorizando `salesPageUrl` e demais campos vindos do detalhe.
5. Persistir novamente no backend para atualização das referências.

## Tratamento de falhas

- Se falhar obtenção do token: ciclo é marcado como `COLLECTION_SKIPPED`.
- Se falhar detalhe de um produto específico no ciclo 2: mantém dados do ciclo 1 para aquele item e continua o processamento dos demais.
- Logs registram `status` HTTP e `productId` para diagnóstico de causa-raiz.
