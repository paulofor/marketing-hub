# MOIS Hotmart Collector — Submódulo separado

Data: 2026-05-06

## Decisão

Foi criado o submódulo `mois-hotmart-collector` como aplicação Spring Boot independente, com container e imagem Docker separados.

## Motivação

- Isolar riscos operacionais da automação web (sessão, captcha, mudanças de layout).
- Permitir ciclo de deploy independente do `mois` principal.
- Facilitar evolução para Playwright + sessão persistida sem acoplamento ao núcleo de domínio.

## Contrato inicial

- `GET /api/v1/mois-hotmart/health`
- `POST /api/v1/mois-hotmart/collections`

## Próximo passo técnico

Implementar adaptador Playwright no serviço de coleta, mantendo o MOIS principal como orquestrador consumidor da API deste submódulo.
