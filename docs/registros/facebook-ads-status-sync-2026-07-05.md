# 2026-07-05 — Sincronismo de status Facebook Ads com a Meta

- Problema: campanhas podiam aparecer ativas na Meta enquanto ad set e anúncios permaneciam `PAUSED` no painel do Marketing Hub.
- Causa-raiz: o backend, ao receber novamente uma campanha já persistida, atualizava só o status da campanha; além disso, o sync periódico de métricas não reconciliava `status/effective_status` dos filhos na Meta.
- Correção aplicada: o callback de publicação reconcilia campanha, ad set e anúncios existentes; o Facebook Ads Worker passa a consultar o retrato efetivo da Meta e enviar `POST /api/facebook-campaigns/{campaignId}/status-sync`.
- Prevenção de recorrência: testes cobrem o callback idempotente, o endpoint de status e o mapeamento do snapshot de status da Meta.
