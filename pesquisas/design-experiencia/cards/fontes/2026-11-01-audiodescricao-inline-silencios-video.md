# Evidência revisada — Audiodescrição inline de vídeo e sincronismo da narração

**Fonte científica:** Venugopalan, S. et al., *Mind The Gap (MTG-AD): Dataset and Fine-grained Evaluation for End-to-End Inline Audio Descriptions*. arXiv:2610.39955v1, submetido em 29/10/2026; NeurIPS 2026 (Evaluations & Datasets).
**Link:** https://arxiv.org/html/2610.39955v1
**Revisão para card:** 01/11/2026.

## Evidência encontrada

- O estudo apresenta MTG-AD, com **433 vídeos em inglês** de conteúdo gerado por usuários (**35,9 horas**) e **8.126 segmentos de audiodescrição**, cocriados por profissionais videntes e cegos.
- A avaliação inclui saídas de **nove modelos multimodais**, métricas de conteúdo e temporais calculadas sobre **áudio efetivamente sintetizado**, incluindo sobreposições com a fala original, e avaliação humana que inclui **sete participantes cegos ou com baixa visão**.
- Os modelos conseguiram descrever eventos visuais e reduzir sobreposições de fala, mas permaneceram deficiências no **ritmo** e no **encadeamento narrativo**. No estudo humano, houve leve preferência por audiodescrições profissionais, embora as produzidas por modelos fossem consideradas úteis para compreensão.
- Os vídeos foram selecionados para permitir audiodescrição durante pausas; vídeos com narração contínua frequentemente não eram viáveis para a modalidade inline.

## Hipótese interpretativa (não resultado causal isolado)

Narrar elementos visuais essenciais que ainda não estejam comunicados pelo áudio, respeitando intervalos sem fala, **pode** facilitar a compreensão sem interromper diálogos e preservar a continuidade auditiva. Nem a seleção de conteúdo nem a duração da pausa, isoladamente, tiveram efeitos comerciais testados.

## Aplicação possível ao Marketing Hub

Testar em Apolo a geração **opcional** de uma faixa de audiodescrição, com extração de eventos visuais importantes, síntese de voz, cálculo de sobreposição, verificação de continuidade e revisão humana com pessoas cegas ou com baixa visão. Comparar uma versão convencional e uma versão com audiodescrição sobre **compreensão de informações visuais**, percepção de interrupções e acessibilidade. Não presumir que isso aumenta CTA ou vendas.

## Resultados reais observados

Os resultados observados são de **avaliação de audiodescrição e compreensão de vídeos**, com referências humanas e julgamentos de qualidade. **Não foram observadas conversões, receitas, retenção comercial ou melhora de campanhas** no artigo.

## Limites e riscos

Estudo focado em conteúdo de vídeo em inglês, com vídeos previamente filtrados para viabilidade temporal; grupo específico de avaliação de usuários cegos ou com baixa visão é pequeno (n=7). A audiodescrição automática pode omitir detalhes críticos, inventar informações visuais, colidir com diálogos e comprometer a compreensão. Revisão de acessibilidade por pessoas do público-alvo é necessária; legendas e outros recursos não são substituídos pela audiodescrição.
