# 2026-07-05 — Sincronismo de status Facebook Ads com a Meta

- Problema: campanhas podiam aparecer ativas na Meta enquanto ad set e anúncios permaneciam `PAUSED` no painel do Marketing Hub.
- Causa-raiz: o backend, ao receber novamente uma campanha já persistida, atualizava só o status da campanha; além disso, o sync periódico de métricas não reconciliava `status/effective_status` dos filhos na Meta.
- Correção aplicada: o callback de publicação reconcilia campanha, ad set e anúncios existentes; o Facebook Ads Worker passa a consultar o retrato efetivo da Meta e enviar `POST /api/facebook-campaigns/{campaignId}/status-sync`.
- Prevenção de recorrência: testes cobrem o callback idempotente, o endpoint de status e o mapeamento do snapshot de status da Meta.

## 2026-07-09 — Reconciliação final de gasto Meta x Hub

- Problema: campanhas encerradas antigas podiam ficar fora da fila de métricas após a janela recente de sincronização, mantendo no Hub um gasto menor que o total vitalício exibido pela Meta.
- Causa-raiz: a fila distinguia campanhas recentes de antigas, mas não tinha uma marca persistida de que a campanha encerrada já havia recebido a sincronização final contra a Meta.
- Correção aplicada: o backend passa a expor campanhas encerradas sem `metrics_final_synced_at` na fila de métricas; ao receber métricas oficiais de campanha encerrada, grava `metrics_final_synced_at`.
- Prevenção de recorrência: a fila agora permite backfill de campanhas paradas uma vez e continua sincronizando campanhas encerradas recentes na janela de liquidação, sem manter campanha antiga em consulta infinita.
