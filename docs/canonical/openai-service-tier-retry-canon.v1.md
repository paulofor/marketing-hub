# Cânone v1 — Retry por tier em integrações OpenAI

## Objetivo

Este cânone define a regra operacional de retry por `service_tier` para chamadas OpenAI usadas pelos workers do Marketing Hub.

## Regra canônica

Quando uma chamada OpenAI usar Flex como padrão operacional, o executor deve manter a sequência canônica de até três tentativas:

1. primeira tentativa em Flex;
2. segunda tentativa em Flex;
3. terceira tentativa em Standard/default, quando a API/chamada suportar esse fallback sem quebrar o contrato.

Essa regra vale para todo o sistema em chamadas OpenAI síncronas ou assíncronas em que o executor controle a tentativa: geração textual, análise, classificação, extração, síntese e geração de imagens de criativos. Falhas transitórias (`408`, `429`, `5xx`, timeout, `rate_limit` ou indisponibilidade temporária`) não devem encerrar a chamada antes da terceira tentativa quando o fallback for suportado.

## Imagens de criativos

Em geração de imagem de criativo:

- a primeira e a segunda tentativa podem usar a Responses API com `service_tier=flex`;
- a terceira tentativa pode usar a Image API direta sem enviar `service_tier`, representando o modo Standard/default operacional;
- a falha transitória não deve encerrar a geração antes da terceira tentativa quando houver fallback suportado.

## Auditoria

O tier efetivo de cada tentativa deve aparecer em log ou auditoria operacional suficiente para explicar custo, latência e motivo do fallback.

Não é permitido transformar toda a etapa em Standard/default por padrão sem registro explícito de decisão funcional, porque isso aumenta custo e remove o benefício financeiro do Flex.

## Custos

Toda chamada OpenAI deve coletar tokens/custo quando o provedor retornar esses dados ou quando o backend conseguir calcular pelo catálogo canônico de modelos. O custo deve ser persistido no registro individual da execução e somado ao agregado de negócio correspondente, quando existir.
