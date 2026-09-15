# Radar IA para Vídeo — 2026-09-15

## Resumo executivo

Duas mudanças passaram o filtro de relevância desta rodada:

1. **Creatify Labs lançou o Boreal**, um novo modelo/API de vídeo focado em publicidade, UGC e produto, com áudio nativo e preço público de **US$ 0,01/s em 720p, US$ 0,03/s em 1080p e US$ 0,12/s em 2K** na fal. A Creatify posiciona o modelo para geração em volume e estratégia de múltiplas tentativas.
2. **Vidu S2 saiu do estado de anúncio e passou a ter rotas públicas de integração**, incluindo documentação de S2-Avatar e S2-Editing. A documentação atual mostra criação de streams, WebSocket/RTC, troca de referência durante a sessão e cobrança por segundo. A página pública principal ainda contém partes de FAQ herdadas do S1, então a documentação técnica deve ser considerada a fonte operacional.

Além disso, a mudança de preço já anunciada para H3 Max entrou em vigor: o preço de lista da fal para H3 Max é US$ 0,08/s em 768p e H3 Max Turbo US$ 0,04/s em 768p.

## 1. Boreal — lançamento relevante para vídeo publicitário em volume

**Status em 15/09/2026: 🟢 ATIVO / API pública / uso comercial.**

A fal expõe `creatify/boreal` como endpoint ativo para vídeos de produto, UGC e apresentadores. A página informa áudio nativo sincronizado, entrada por texto e referências opcionais de imagem e áudio. O resultado de exemplo usa 24 fps e o endpoint está marcado como `Commercial use`.

### Preço público atual

| Resolução | Boreal |
| --- | ---: |
| 720p | US$ 0,01/s |
| 1080p | US$ 0,03/s |
| 2K | US$ 0,12/s |

Um clipe de 10 s custa, respectivamente, US$ 0,10, US$ 0,30 ou US$ 1,20.

A Creatify afirma que um clipe de cinco segundos pode ser gerado em aproximadamente cinco segundos de GPU e que o Boreal foi pós-treinado sobre o LTX-2.5 com foco em publicidade. A empresa reporta melhorias em fidelidade de produto, identidade do criador e legibilidade de elementos na cena. No teste cego divulgado pela própria Creatify, Boreal foi preferido ao LTX-2.5 base em 25 de 31 comparações decisivas, com 9 empates em 40 casos.

Esses números de qualidade e velocidade são **evidência do fornecedor**, não benchmark independente. A própria Creatify reconhece que o modelo está mais forte em clipes curtos e cenas simples, e que sua vantagem é menor em tomadas de uma única pessoa falando.

### Comparação de custo atual no mesmo agregador

| Sistema | Status | Preço de referência | Observação |
| --- | --- | ---: | --- |
| Boreal | 🟢 ativo | US$ 0,01/s 720p | áudio nativo; foco em ads/UGC |
| H3 Max Turbo | 🟢 ativo | US$ 0,04/s 768p | muito rápido; preço pós-promoção |
| H3 Max | 🟢 ativo | US$ 0,08/s 768p | áudio nativo e maior conjunto de rotas que Turbo |
| Kling 3.0 Pro | 🟢 ativo | US$ 0,168/s com áudio | multi-shot e áudio nativo |
| Veo 3.1 Fast | 🟢 ativo | US$ 0,15/s 720p/1080p com áudio | ecossistema Google, até 4K em outros tiers |
| Seedance 2.5 | 🟢 ativo | ~US$ 0,473/s 720p | até 30 s contínuos e até 50 referências |

Preço baixo não implica liderança de qualidade. Veo, Kling e Seedance têm capacidades de controle, duração, referências e qualidade de fronteira que podem justificar custo maior dependendo da tomada.

### Implicação para o Marketing Hub

O ponto mais interessante não é simplesmente trocar o renderer atual por Boreal. É mudar a estratégia de geração para cenas em que a variabilidade é alta:

`briefing fixo → 4–8 candidatos baratos → filtros de invariantes → avaliação visual → 1–2 finalistas → revisão/acabamento`

Quando o custo marginal fica muito baixo, pode ser melhor comprar **diversidade de tentativas** do que apostar que uma única geração cara acerte tudo. Isso precisa ser testado com orçamento total fixo e métricas como custo por tomada aprovada, número de regenerações, tempo até aprovação, fidelidade do produto e nota humana.

## 2. Vidu S2 — de anúncio para integração pública

**Status em 15/09/2026: 🟢 ATIVO para S2-Avatar e S2-Editing, com documentação pública de API; algumas páginas institucionais ainda estão parcialmente desatualizadas.**

Na rodada de 12/09, o Vidu S2 ainda estava tratado como lançamento previsto para 15/09. Hoje a documentação pública já expõe rotas específicas de `s2-avatar` e `s2-editing`.

O relatório técnico descreve:

- S2-Avatar com geração em tempo real em **720p e 25–42 FPS**;
- referências dinâmicas que podem ser atualizadas durante a sessão;
- melhor seguimento de instruções e movimentos de corpo inteiro;
- S2-Editing para transformação de stream em tempo real, incluindo estilo, roupa, personagem e fundo;
- exploração de vídeo espacial/estéreo, que ainda deve ser tratado como capacidade de pesquisa e não como recurso geral disponível.

A documentação operacional do S2-Editing confirma criação de stream, autenticação por API key, conexão WebSocket, integração RTC e tipos como `style_transfer`, `virtual_tryon`, `subject_replacement` e `background_replacement`. Também permite trocar prompt/referência durante a sessão. A cobrança documentada é por duração real do stream, **1 crédito por segundo, equivalente a CNY 0,03125/s** nas páginas atuais do S2. O S2-Avatar também apresenta cobrança por segundo na documentação técnica.

A página pública principal do Vidu ainda mostra em seu FAQ características de 540p/25 FPS associadas ao Vidu S anterior. Portanto, para integração, a referência correta agora são as páginas técnicas específicas de S2, não o FAQ genérico.

### Por que isso importa

A mudança de hoje é principalmente de **disponibilidade**, não de conceito. O padrão de agente visual e feedback já havia sido registrado no card de 12/09. Agora o S2 passa a ser algo que pode efetivamente entrar em prova de conceito de stream interativo/edição em tempo real.

Não foi criado novo card para o S2 nesta rodada porque a ideia reutilizável mais importante — observar o resultado e corrigir o próximo estado/prompt — já está representada por `video-feedback-visual-agente-autocorrecao`. Criar outro card só pela abertura operacional duplicaria conhecimento.

## 3. H3 Max — preço de lista agora efetivo

**Status: 🟢 ATIVO.**

A promoção de lançamento terminou. A própria fal publica agora H3 Max a **US$ 0,08/s em 768p** e H3 Max Turbo a **US$ 0,04/s em 768p**. O H3 Max continua capaz de gerar cinco segundos em 768p em menos de três segundos de inferência segundo a fal. A mudança de preço havia sido antecipada na rodada de 14/09; hoje ela apenas se tornou efetiva, portanto não foi tratada como uma descoberta separada nem virou card.

## Card criado

Foi criado um card novo porque o Boreal torna acionável uma estratégia de produção diferente, e não apenas uma troca de fornecedor:

- `cardKey`: `video-geracao-paralela-selecao-best-of-n`
- JSON: `pesquisas/video/cards/2026-09-15-boreal-geracao-paralela-best-of-n.json`
- Fonte revisada: `pesquisas/video/cards/fontes/2026-09-15-boreal-best-of-n-geracao-paralela.md`
- SHA-256 da fonte: `d4b3a9811a9239a8be8da69053f3d41a1260946d3260875530ca28c982c8817e`

O card propõe testar best-of-N com orçamento fixo e medir custo/tempo por tomada aprovada, aderência ao briefing, fidelidade e avaliação humana. Não assume impacto em conversão ou vendas.

## Fontes principais

- Creatify Labs — Boreal: https://labs.creatify.ai/models/boreal
- fal — Boreal: https://fal.ai/models/creatify/boreal
- Creatify — anúncio de lançamento em 15/09/2026: https://www.prweb.com/releases/creatify-labs-launches-boreal-a-text-to-video-ai-model-that-matches-top-tier-quality-at-one-cent-per-second-and-up-to-40x-the-speed-302879156.html
- fal — H3 Max: https://fal.ai/minimax-h3-max
- fal — Veo 3.1: https://fal.ai/models/fal-ai/veo3.1
- fal — Kling 3.0 Pro: https://fal.ai/models/fal-ai/kling-video/v3/pro/text-to-video
- fal — Seedance 2.5: https://fal.ai/models/bytedance/seedance-2.5/text-to-video
- Vidu S2-Avatar: https://www.vidu.com/vidu-stream/avatar
- Vidu S2-Editing API parameters: https://platform.vidu.com/vidu-stream/doc/s2-editing/parameters
- Vidu S2-Avatar API parameters: https://platform.vidu.com/vidu-stream/doc/s2-avatar/component/parameters
- Vidu S2 technical report: https://arxiv.org/abs/2609.11638
