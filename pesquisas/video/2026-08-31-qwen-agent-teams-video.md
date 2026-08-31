# Radar IA para Vídeo — 31/08/2026

## Mudança relevante: Alibaba/Qwen lança Agent Teams para produção audiovisual

Em 31 de agosto de 2026, a Alibaba/Qwen lançou o **Agent Teams** dentro do Qwen Creative (千问创作). A novidade desloca o fluxo de criação de vídeo de uma interação centrada em prompts para uma arquitetura multiagente: o usuário descreve a ideia e agentes especializados assumem papéis como roteirista, diretor, direção de arte e geração de vídeo, cooperando para executar o fluxo completo.

### Status atual

- **Qwen Creative Agent Teams:** ATIVO nas superfícies Qwen Creative para PC e no Qwen App.
- **API pública específica do Agent Teams:** não localizada até esta verificação; portanto, para integração programática, o recurso deve ser tratado como **limitado às interfaces do produto**.
- **Wan 3.0 / Wan 3.0 Prime:** ATIVOS como motores de vídeo usados pelo ecossistema Qwen/Alibaba.
- **Pesos abertos do Agent Teams ou Wan 3.0:** não anunciados; trata-se de serviço hospedado.

### O que mudou

O Agent Teams organiza automaticamente etapas como:

1. planejamento criativo;
2. roteiro;
3. definição de personagens;
4. storyboard e enquadramento;
5. geração de imagens/cenas;
6. geração de vídeo;
7. voz, música e áudio;
8. montagem do resultado final.

O sistema combina agentes especializados com o canvas do Qwen Creative e integra o **Wan 3.0 / Wan 3.0 Prime** para vídeo, além de modelos de imagem como o **Qwen-Image-3.0**.

A cobertura de lançamento informa geração de vídeo coerente de até **30 segundos**, em **720p**, com preço anunciado na superfície chinesa a partir de aproximadamente **¥0,26 por segundo**. Esse preço pertence ao produto Qwen Creative/Agent Teams e não deve ser confundido com a tabela internacional da API do Alibaba Cloud Model Studio.

### Comparação com outros sistemas agênticos

**Qwen Agent Teams**
- ponto forte: multiagentes com papéis de produção explícitos (roteiro, direção, arte, vídeo) e integração nativa com Wan 3.0;
- status: ativo no PC/App;
- limitação atual: não encontrei API pública específica do Agent Teams.

**Runway Agent 2.0**
- status: ativo;
- mais maduro como ambiente criativo e infraestrutura de vídeo, com workflows e API consolidados;
- continua mais apropriado quando o objetivo é integração programática e pipeline de produção já estruturado.

**Pika Agent / Pika API**
- status: ativo;
- abordagem mais API-first e agent-native, com uma única API para vídeo, imagem, áudio e fala e mais de 100 modelos;
- melhor opção hoje quando um agente externo precisa chamar diretamente os motores de mídia.

**SparkStation**
- status: beta desde 29/08/2026;
- oferece um pipeline mais próximo de um estúdio completo, incluindo casting, continuidade, QC, edição e exportação para NLEs;
- GA anunciada para 01/10/2026 e API enterprise para 01/11/2026.

### Por que isso importa

A novidade reforça uma mudança importante no mercado: a vantagem competitiva começa a migrar do **modelo isolado** para o **harness de produção** que coordena modelos e agentes.

O fluxo tende a evoluir de:

`prompt -> modelo -> clipe`

para:

`objetivo -> agente produtor -> equipe de agentes -> roteiro/storyboard/personagens -> modelos de imagem/vídeo/áudio -> montagem -> revisão`

O Qwen Agent Teams é especialmente relevante porque a Alibaba passa a juntar seu modelo de linguagem/planejamento com sua própria linha de vídeo Wan dentro de um fluxo multiagente. Para publicidade, curtas, animação e conteúdo social, isso pode reduzir significativamente a necessidade de o usuário decompor manualmente cada etapa em prompts separados.

### Avaliação

A mudança é **significativa**, mas ainda não torna o Qwen Agent Teams a melhor escolha para automação via API. Para uso interativo, ele passa a ser um dos experimentos mais interessantes de produção audiovisual multiagente. Para integração programática, Runway e especialmente Pika API continuam mais maduros enquanto a Alibaba não expuser o Agent Teams como API ou protocolo de automação.

## Fontes

- IT之家 — anúncio de 31/08/2026 sobre o Agent Teams: https://www.ithome.com/0/996/484.htm
- CNMO — detalhes de duração, resolução e preço: https://ai.cnmo.com/news/817306.html
- Alibaba Cloud Model Studio — tabela atual de preços do Wan 3.0: https://www.alibabacloud.com/help/en/model-studio/model-pricing
- Runway Academy — Agent 2.0: https://academy.runwayml.com/tutorial/agent-2
- Pika API / Agents: https://dev.pika.art/agent
- SparkStation: https://www.sparkstation.ai/
