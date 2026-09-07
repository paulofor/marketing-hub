# Fonte revisada — Estado persistente + eventos temporais em vídeo agêntico

## Achado revisado

O Runway GWM Worlds 2, apresentado como Research Preview em 3 de setembro de 2026, separa a definição persistente do mundo dos eventos que acontecem ao longo do tempo. A camada persistente inclui cenário, personagens, materiais, iluminação, regras físicas, perspectiva de câmera e estado inicial. A camada temporal inclui movimentos, gestos, fala, interação com objetos, eventos ambientais, sons e movimentos de câmera, inclusive com timestamps e sobreposição.

A Runway chama esse formato de **WorldPrompt**. A própria documentação indica três modos de operação: planejamento antecipado de eventos, interação por turnos e controle em tempo real. Também reconhece que o rastreamento de estado pode exigir um harness externo em tempo real.

## Interpretação para o Marketing Hub

A evidência sustenta que existe uma arquitetura emergente de produção audiovisual na qual o estado global da cena é mantido separado das instruções momentâneas. Isso é relevante como padrão de harness para agentes de vídeo, mas ainda não demonstra melhoria comercial, aumento de conversão ou superioridade de um criativo.

Uma aplicação possível é estruturar o videomaker do Marketing Hub com dois contextos distintos:

1. **Estado persistente**: identidade visual, produto, personagem, cenário, restrições de marca, câmera, estilo e regras que não devem variar.
2. **Eventos temporais**: ações, falas, movimentos de câmera, entradas e saídas, efeitos e mudanças de ritmo em timestamps específicos.

A hipótese a testar é se essa separação reduz inconsistências entre cenas e retrabalho em comparação com prompts de tomada independentes.

## Status e limitações

- GWM Worlds 2: **LIMITADO / Research Preview**.
- Sem API pública self-service, preço público ou pesos abertos na data da revisão.
- Não deve ser tratado como opção operacional atual.
- A evidência é arquitetural e de produto; não é evidência de impacto comercial.
- A consistência de longo prazo ainda é imperfeita e não há benchmark independente suficiente para afirmar superioridade.

## Fontes primárias e de comparação

- Runway GWM Worlds 2: https://runway.com/research/introducing-gwm-worlds-2
- H3 Max Director API: https://fal.ai/models/minimax/h3-max/director/api
- Relatório da rodada: repo:pesquisas/video/2026-09-07-runway-gwm-worlds-2.md
