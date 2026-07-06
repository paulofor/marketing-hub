# Experimentos — Estratégia de Campanha para Auto-stop

## 2026-07-06

- solicitação: criar o conceito de Estratégia de Campanha para o próprio sistema desligar campanhas quando deixarem de ser úteis.
- causa-raiz: o backend já possuía regras automáticas fragmentadas de parada, mas a campanha não nascia com uma estratégia explícita, visível e auditável.
- foi feito: campanha publicada passa a criar `campaign_strategy`, cada sync de métricas registra `campaign_strategy_evaluation` e, quando a estratégia reprova utilidade, o backend cria stop request com `CAMPAIGN_STRATEGY_STOPPED`.
- integração: o Facebook Ads Worker continua responsável por pausar a campanha na Meta consumindo `/api/facebook-campaigns/stop-requests`; o backend decide e audita.
- prevenção de recorrência: a tela de campanhas exibe objetivo, preset, limite financeiro e taxa mínima de checkout para reduzir decisões invisíveis.
