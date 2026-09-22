# Radar IA para Vídeo — 2026-09-22

Nesta rodada, duas mudanças passaram o filtro de relevância: a Xiaomi abriu MiMo-V2.6 Pro e Flash sob MIT com compreensão nativa de vídeo/áudio, tool calling e harnesses públicos; e a Runway ampliou Workflows com componentes de pós-produção que tornam o pipeline operável por agente mais completo.

## 1. Xiaomi MiMo-V2.6 — revisor audiovisual open-weight

**Status: ATIVO; pesos públicos sob MIT; API oficial disponível.**

A Xiaomi lançou MiMo-V2.6-Pro-RL e MiMo-V2.6-Flash-RL. Ambos recebem texto, imagem, vídeo e áudio, têm contexto de 1 milhão de tokens, suportam tool calling e retornam texto. Portanto, não são renderers de vídeo: são candidatos a camada de compreensão, planejamento e revisão audiovisual.

O Pro usa arquitetura Sparse MoE de 1,02T parâmetros totais com 42B ativados; o Flash tem 309B totais e 15B ativados. A Xiaomi também publicou ambientes/código de reinforcement learning e descreve mini-harnesses composáveis que desacoplam system prompts, ferramentas e gerenciamento de contexto.

### Preços oficiais

| Modelo | Cache hit / 1M | Input / 1M | Output / 1M |
| --- | ---: | ---: | ---: |
| MiMo-V2.6-Flash | US$ 0,0028 | US$ 0,14 | US$ 0,28 |
| MiMo-V2.6-Pro | US$ 0,0036 | US$ 0,435 | US$ 0,87 |

### Comparação

O Qwen3.8-Omni-Flash continua **ATIVO via API, mas sem pesos públicos dessa variante**, com preço de aproximadamente US$ 0,15/1M de entrada e US$ 0,47/1M de saída. Ele continua muito competitivo para ingestão audiovisual barata. O diferencial do MiMo-V2.6 não é somente preço: é oferecer pesos MIT e artefatos de harness/RL para quem quer maior controle do revisor e menor dependência de fornecedor.

O Gemini 3.8 Flash continua **ATIVO/PROPRIETÁRIO**, com entrada multimodal e contexto de 1M tokens, mas não oferece a mesma abertura de pesos.

### Por que importa para produção de vídeo

O padrão mais importante é separar o renderer do avaliador:

`renderer -> rough cut -> revisor audiovisual -> decisão -> ferramenta de correção -> nova versão`

Isso permite trocar o modelo que gera vídeo sem reescrever toda a lógica de QA. Com MiMo-V2.6, essa camada de revisão passa a ter uma opção open-weight permissiva. No Marketing Hub, ainda seria mais realista usar API no curto prazo, porque os checkpoints são grandes demais para a VPS atual.

## 2. Runway Workflows — pós-produção entra mais fundo no grafo do agente

**Status: ATIVO.**

A Runway ampliou Workflows com componentes de **Compositing, Alpha, HDR, Depth Map e RGB Depth**. A documentação atual confirma também que o Runway Agent consegue construir, editar e executar Workflows diretamente de uma conversa.

Os Workflows já oferecem nós para extrair áudio, combinar vídeos, extrair primeiro/último frame, gerar mapas de profundidade e manipular mídia. O nó Depth Anything Video aceita até 1 minuto e custa 1 crédito por segundo. O Ruby HDR está ativo em Max ou superior no app e em todas as contas Runway Dev, com custo documentado de 20 créditos por segundo. Workflows vinculados ao Developer podem ser publicados como endpoint de API.

### Comparação

DaVinci Resolve + MCP e Reuters/CuttingRoom já mostraram agentes operando ferramentas reais de edição. A mudança da Runway empurra o mesmo princípio para um ambiente no-code/multimodelo em que geração e pós-produção ficam no mesmo grafo. Fotor Agent segue forte na ideia de projeto multifaixa editável, enquanto Runway ganha em composição de pipeline reutilizável e integração com sua plataforma de modelos/APIs.

### Por que importa para produção de vídeo

A arquitetura fica mais próxima de:

`briefing -> storyboard -> renderer -> revisão -> depth/alpha/compositing/HDR -> export`

O valor é poder refazer apenas a etapa defeituosa. Uma falha de continuidade não exige repetir HDR; uma correção de fundo não exige refazer a trilha; um erro de cor não precisa reabrir a geração. Isso reduz retrabalho e torna logs/gates mais claros.

## Cards

Nenhum `cardKey` novo foi criado para evitar duplicação. Foram atualizados dois princípios existentes:

- `video-feedback-visual-agente-autocorrecao`: agora reforçado por uma alternativa audiovisual open-weight/MIT com tool calling e harness público.
- `video-agente-edicao-regras-explicitas-mcp`: agora reforçado pelos Workflows da Runway com compositing, alpha, HDR e depth dentro do pipeline do agente.

## Fontes principais

- Xiaomi MiMo — MiMo-V2.6: https://mimo.mi.com/docs/en-US/news/latest/v2-6
- Hugging Face — MiMo-V2.6-Pro-RL: https://huggingface.co/XiaomiMiMo/MiMo-V2.6-Pro-RL
- Hugging Face — MiMo-V2.6-Flash-RL: https://huggingface.co/XiaomiMiMo/MiMo-V2.6-Flash-RL
- Xiaomi MiMo — Pro: https://mimo.mi.com/models/en-US/mimo-v2.6-pro
- Xiaomi MiMo — Flash: https://mimo.mi.com/models/en-US/mimo-v2.6-flash
- Runway — Workflows: https://runway.com/workflows
- Runway Help — Utility Nodes: https://help.runwayml.com/hc/en-us/articles/47184761711379-Using-Utility-Nodes-in-Workflows
- Runway Help — Agent + Workflows: https://help.runwayml.com/hc/en-us/articles/53645211363475-Building-and-running-Workflows-with-Agent
- Runway — Ruby: https://runway.com/product/ruby
