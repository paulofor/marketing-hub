# Homologação do namespace Docker nos contratos PDE

Data: 2026-09-18  
Produto preservado: Vega (4), experimento 92, experiência
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`.

## Causa confirmada

O workflow próprio da PDE fornecia um projeto Compose exclusivo à regressão transacional, mas o
workflow central `GitHub Actions Contracts` chamava o mesmo teste sem `PDE_LOCAL_COMPOSE_PROJECT`.
A execução `35366605273` falhou antes de validar promoção, continuidade e rollback.

## Decisão

Foram comparadas três alternativas: usar um projeto padrão sujeito a colisões, pular a regressão
Docker ou derivar a identidade do `run_id` e `run_attempt`. A terceira preserva isolamento e a prova
transacional, por isso foi adotada.

## Evidências locais

- `bash -n`, ShellCheck e Actionlint aprovados;
- contrato leve aprovado com e sem a regressão Docker;
- promoção exclusiva da v8 aprovada;
- fingerprint divergente bloqueado antes da troca;
- falhas pós-troca de frontend e backend restauraram a identidade anterior;
- v5, v6, v7 e workers permaneceram ativos durante a troca;
- topologia temporária removida ao final.

## Limite comercial

A homologação não realizou venda, cobrança, campanha, chamada paga de IA ou retomada de Psique. A
oferta de R$ 67, o checkout Pepper `owm6x`, o acesso de 90 dias e o escopo do experimento 92 não foram
alterados.
