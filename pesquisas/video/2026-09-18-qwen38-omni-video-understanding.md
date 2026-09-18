# Radar IA para vídeo — 2026-09-18

## Resumo executivo

Uma mudança passou o filtro de relevância desta rodada:

1. **Alibaba/Qwen lançou o Qwen3.8-Omni-Flash**, um modelo omnimodal voltado a compreensão de áudio/vídeo e workflows agênticos. Ele aceita texto, imagem, áudio e vídeo, trabalha com contexto de 1 milhão de tokens, suporta reasoning e function calling e está disponível por API no Alibaba Cloud Model Studio. O output nativo é texto, portanto ele não substitui um renderer de vídeo; sua função natural é atuar como analisador, crítico e orquestrador de ferramentas.

Não encontrei hoje um novo lançamento de renderer comparável de Runway, Google Veo/Gemini Omni Video, Kling, Seedance, Wan, MiniMax/H3, Vidu ou Adobe que justificasse outra notificação. Republicações do HiDream V1 e do Fotor Agent foram tratadas como cobertura de lançamentos já registrados nas rodadas anteriores, não como novidades novas.

## 1. Qwen3.8-Omni-Flash

**Status: 🟢 ATIVO — API hospedada disponível; sem pesos públicos anunciados para esta variante; output nativo somente em texto.**

A documentação oficial da Alibaba Cloud lista `qwen3.8-omni-flash` para compreensão de áudio/vídeo e análise de conteúdo. O modelo aceita texto, imagens, áudio e vídeo, produz texto, tem **janela de contexto de 1M tokens**, suporta **function calling**, web search, caching e thinking com esforço ajustável. Está disponível em regiões que incluem Beijing, Singapore, Hong Kong, Tokyo, Frankfurt e Virginia.

O catálogo de modelos da Alibaba registra disponibilidade do modelo em 17/09 em algumas regiões, enquanto o anúncio público da Qwen foi publicado em 18/09. Para esta rodada, o ponto relevante é que a API está efetivamente documentada e utilizável.

### Economia

A Qwen afirma que, em relação ao Qwen3.5-Omni-Plus, o novo modelo reduz aproximadamente:

- **89%** do custo de entrada de vídeo;
- **mais de 93%** do custo de entrada audiovisual por hora;
- **mais de 98%** do custo de entrada de áudio por hora.

Esses percentuais são números do fornecedor e precisam ser validados no workload real. O preço divulgado no QwenCloud é **US$0,15 por 1 milhão de tokens de entrada** e **US$0,47 por 1 milhão de tokens de saída**, com cache implícito ainda mais barato.

### Comparação com Google

O **Gemini 3.8 Flash continua 🟢 GA** e também aceita texto, imagem, vídeo e áudio, com contexto de **1.048.576 tokens**, function calling, caching e thinking. Portanto, o Qwen não inaugura a ideia de “LLM que entende vídeo longo e chama ferramentas”. A mudança relevante é a combinação de **custo declarado muito menor + percepção audiovisual + tool calling**, tornando mais plausível colocar um avaliador multimodal em cada iteração de produção.

A própria Qwen afirma desempenho audiovisual próximo ao Gemini 3.8 Flash em seus benchmarks, mas isso é avaliação do fornecedor, não prova independente de superioridade.

### Ecossistema agêntico

O repositório oficial **Qwen-MM-Plugins** é público sob Apache-2.0 e já expõe Skills e ferramentas MCP para compreensão de mídia e edição de vídeo. Há ferramentas para percepção áudio+vídeo, ASR com timestamps, diarização, localização/contagem de eventos e uma skill específica de `video-edit`, projetada para decidir seleção de tomadas, pacing, beat-sync, sound design e montagem antes de entregar a composição ao pipeline de render.

Isso reforça o desenho arquitetural:

`renderer → rough cut → avaliador audiovisual → decisão → ferramentas de correção → nova versão → gate humano`

O modelo não precisa gerar o vídeo para melhorar o vídeo. Ele pode atuar como **critic/orchestrator** sobre renderers especializados.

## Por que importa para o Marketing Hub

A principal aplicação é ampliar o card já existente `video-feedback-visual-agente-autocorrecao`.

Na versão anterior, o foco era revisar frames-chave depois de cada tomada. Com um modelo omnimodal barato e de contexto longo, o revisor pode analisar o **vídeo completo**, incluindo:

- fidelidade do produto e do cenário;
- continuidade entre cenas;
- fala, pronúncia e sincronização;
- legenda versus áudio;
- timing de hook, demonstração e CTA;
- música/SFX versus ritmo visual;
- claims e dados comerciais versus briefing;
- duração, silêncio, cortes ruins e trechos redundantes.

Quando encontrar um problema, o agente pode chamar uma ferramenta específica em vez de regenerar tudo: corrigir legenda, trocar voice-over, ajustar volume, regenerar só uma tomada, antecipar CTA ou abrir um gate de revisão humana.

O benefício deve ser medido por **defeitos detectados, falsos positivos, custo de revisão, regenerações e tempo até aprovação**, não por uma suposição de aumento de conversão.

## Card desta rodada

Não criei um `cardKey` novo. Atualizei o conceito existente:

`video-feedback-visual-agente-autocorrecao`

A nova versão amplia o princípio de feedback visual para **revisão audiovisual de longa duração + tool calling**.

Fonte revisada:
`pesquisas/video/cards/fontes/2026-09-18-qwen38-omni-avaliacao-audiovisual-agente.md`

SHA-256:
`99b551754ae1653db72f73d60bb096655a67176bf121bb922f67697f3737016d`

JSON:
`pesquisas/video/cards/2026-09-18-qwen38-omni-feedback-audiovisual.json`

## Fontes

- https://www.alibabacloud.com/help/en/model-studio/qwen3-8-omni-flash
- https://docs.modelstudio.console.alibabacloud.com/en/model-studio/omni
- https://www.alibabacloud.com/help/en/model-studio/newly-released-models
- https://qwen.ai/blog?id=qwen3.8-omni-flash
- https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash
- https://github.com/QwenLM/Qwen-MM-Plugins
- https://github.com/QwenLM/Qwen-MM-Plugins/blob/main/src/capabilities/api/skill/SKILL.md
- https://github.com/QwenLM/Qwen-MM-Plugins/blob/main/src/capabilities/video-edit/skill/SKILL.md
