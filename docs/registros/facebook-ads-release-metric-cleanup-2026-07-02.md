# 2026-07-02 — Hotfix de republicacao Facebook Ads

## Contexto

Ao tentar republicar o experimento 53 pelo endpoint `POST /api/experiments/{id}/facebook-release`, o backend falhou porque a campanha antiga possuia metrica agregada em `experiment_campaign_metric`.

## Causa-raiz

A campanha antiga em `facebook_ads_campaign` era referenciada por `experiment_campaign_metric.campaign_id`. A FK nao tinha `ON DELETE CASCADE`, entao a limpeza da campanha antiga travava antes de recolocar o experimento na fila do Facebook Ads Worker.

## Correcao aplicada

Foi adicionado hotfix Liquibase para trocar a FK de `experiment_campaign_metric.campaign_id` para `ON DELETE CASCADE`, somente quando a FK existente ainda nao estiver em cascade.

## Impacto esperado

A republicacao de experimento pode remover a campanha antiga sem falhar por metrica agregada dependente. Isso destrava a publicacao correta do experimento 53 como campanha low-ticket de venda.

## Prevencao

Fluxos de republicacao Facebook precisam limpar ou permitir cascade para dados dependentes de campanha antes de remover `facebook_ads_campaign`, especialmente metricas agregadas usadas no funil.
