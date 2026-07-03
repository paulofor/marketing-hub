# Cânone v1 — Retry por tier em integrações OpenAI

## Objetivo

Este cânone define a regra operacional de retry por `service_tier` para chamadas OpenAI usadas pelos workers do Marketing Hub.

## Regra canônica

Quando uma chamada OpenAI usar Flex como padrão operacional e encontrar falha transitória (`408`, `429`, `5xx`, timeout, `rate_limit` ou indisponibilidade temporária), o executor deve manter:

1. primeira tentativa em Flex;
2. segunda tentativa em Flex;
3. terceira tentativa em Standard/default, quando a API/chamada suportar esse fallback sem quebrar o contrato.

Essa regra vale para geração textual e para geração de imagens de criativos.

## Imagens de criativos

Em geração de imagem de criativo:

- a primeira e a segunda tentativa podem usar a Responses API com `service_tier=flex`;
- a terceira tentativa pode usar a Image API direta sem enviar `service_tier`, representando o modo Standard/default operacional;
- a falha transitória não deve encerrar a geração antes da terceira tentativa quando houver fallback suportado.

## Auditoria

O tier efetivo de cada tentativa deve aparecer em log ou auditoria operacional suficiente para explicar custo, latência e motivo do fallback.

Não é permitido transformar toda a etapa em Standard/default por padrão sem registro explícito de decisão funcional, porque isso aumenta custo e remove o benefício financeiro do Flex.
