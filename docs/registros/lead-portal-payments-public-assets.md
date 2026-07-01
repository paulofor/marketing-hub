# Proteção de ativos comerciais do Lead Portal Payments

## 2026-07-01 — Deploy inteligente para campanhas do Marketing Hub

- Problema: páginas de venda, páginas de obrigado e ZIPs de entrega gerados pelo Marketing Hub podiam ser publicados no VPS antes de entrarem no `main`; o deploy seguinte usava `rsync --delete` e apagava esses ativos comerciais.
- Causa-raiz: o deploy tratava `lead-portal-payments-service` como código estático versionado, mas o módulo também serve ativos comerciais vivos de experimentos em validação.
- Correção aplicada: o workflow cria backup remoto de `docker/proxy/html` antes do sync e mantém `--delete` com proteção para `docker/proxy/html/downloads/**`, `sales-page-exp*.html` e `obrigado-exp*.html`.
- Prevenção de recorrência: campanhas novas geradas pelo Marketing Hub deixam de ser apagadas por deploy antes do PR de versionamento, preservando checkout, pós-compra premium e entrega digital durante validações controladas.
