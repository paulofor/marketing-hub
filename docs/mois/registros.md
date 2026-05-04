# Registros — MOIS

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-03 18:45:13 UTC-3
- Ajustado o diagnóstico de coletas Hotmart no `MoisDomainService` para registrar explicitamente:
  - `method=GET` no início da tentativa;
  - `requestedUrl` e `finalUrl` nos logs de resposta rejeitada (`statusCode` fora de 2xx);
  - `requestedUrl`, `finalUrl`, `statusCode` e método no log de finalização.
- Objetivo: facilitar rastreabilidade de redirecionamentos HTTP 301 e identificar com precisão qual URL foi solicitada e qual URL final foi retornada pelo cliente HTTP.
- Commit relacionado: `356c904`.

## 2026-05-03 22:10:00 UTC-3
- Coleta Hotmart do MOIS aprimorada para reduzir falhas na extração de produtos em destaque.
- O parser da resposta do marketplace foi refatorado para:
  - manter a extração rica (título, link, descrição, produtor e imagem) quando os blocos completos estiverem presentes;
  - aplicar fallback de extração por URL de produto (`https://www.hotmart.com/product/...`) quando o HTML/JSON vier com estrutura parcial.
- O objetivo é manter geração de leads mesmo quando a Hotmart altera o shape do payload da vitrine.
