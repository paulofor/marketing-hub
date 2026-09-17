# Radar IA para vídeo — 2026-09-17

## Resumo executivo

Três mudanças passaram o filtro de relevância desta rodada:

1. **Pruna P-Video-2-Pro** foi lançado com geração de vídeo + áudio, modos Speed/Quality, condicionamento por primeiro/último frame e distribuição imediata em múltiplos provedores de inferência.
2. **OpenClips** lançou o fluxo que transforma uma URL de produto em roteiro, storyboard e anúncio final, roteando tomadas entre dezenas de modelos; o serviço também expõe um servidor MCP ativo para agentes.
3. **FineVoice Aunio** foi anunciado como agente de produção de áudio ponta a ponta; o produto está em beta e usa vídeo como contexto para coordenar voz, música e efeitos, além de oferecer skills reutilizáveis.

Não houve nesta rodada um novo lançamento comparável de Runway, Google Veo, Adobe, Kling, Seedance ou Wan que justificasse notificação adicional. O anúncio público formal do HiDream-O1-Video-1.0 apareceu em 17/09; como o modelo já havia sido identificado na rodada anterior, tratei isso apenas como correção de data/status, sem duplicar o achado.

## 1. Pruna P-Video-2-Pro

**Status: 🟢 ATIVO — API e playground disponíveis; integração já documentada em provedores terceiros.**

A Pruna lançou o P-Video-2-Pro para texto→vídeo e imagem→vídeo. O modelo aceita uma imagem inicial e opcionalmente uma imagem final, gera áudio junto do vídeo, trabalha com 5–15 segundos, 480p ou 768p e oferece dois modos: Speed para iteração e Quality para resultado final. A página oficial anuncia preços a partir de **US$0,02/s no Speed** e **US$0,035/s no Quality**.

A Runware publicou o modelo em 17/09 com ID `prunaai:p-video@2-pro`, duração de 5–15 s, 480p/768p, seed, first/last-frame e prompt upsampling. A Replicate o lista como modelo oficial ativo. A Pruna informa aproximadamente 1,99 s de inferência para seu benchmark de texto→vídeo em 480p; números de latência são do fornecedor e não representam necessariamente latência ponta a ponta.

O P-Video-2-Pro é baseado no **MiniMax H3**. Não localizei pesos públicos específicos do P-Video-2-Pro nem uma licença open-weight própria para essa variante; portanto, o status correto é serviço/API proprietário sobre uma linhagem de base aberta, não um novo modelo de pesos abertos.

### Comparação

O **H3 Max Turbo da fal continua 🟢 ativo** e custa atualmente **US$0,04/s em 768p**, enquanto o H3 Max custa US$0,08/s em 768p. A vantagem prática do P-Video-2-Pro não é apenas velocidade: ele chega distribuído em vários provedores e oferece uma superfície explícita de Speed/Quality. A comparação direta de qualidade ainda precisa de benchmark independente; a Pruna está publicando seus próprios números de eficiência.

### Por que importa

A família H3 está virando uma base sobre a qual diferentes fornecedores fazem pós-treino e otimização de inferência. Para um harness, isso reforça a ideia de tratar o renderer como backend substituível e medir qualidade, latência e custo por cena, sem acoplar a produção a um único endpoint.

**Não criei card novo para esse item**: os princípios de geração barata para iteração, best-of-N e separação entre preview e final já estão representados nos cards existentes.

Fontes:
- https://www.pruna.ai/p-video-2-pro
- https://runware.ai/docs/models/prunaai-p-video-2-pro
- https://replicate.com/prunaai/p-video-2-pro
- https://fal.ai/learn/tools/fastest-ai-video-generation-models

## 2. OpenClips — URL de produto → anúncio final + MCP

**Status: 🟢 ATIVO — web e MCP ativos; serviço proprietário; sem pesos próprios abertos.**

Em anúncio de 16/09, a OpenClips apresentou um fluxo em que o usuário fornece uma **URL de produto** e o sistema extrai assets, recursos e posicionamento, cria roteiro e storyboard e entrega um anúncio final. A plataforma afirma rotear cada tomada para o modelo mais adequado e hoje lista **54 modelos** no mesmo composer.

O servidor MCP oficial (`https://mcp.openclips.ai`) está ativo e expõe ferramentas como `get product`, `get brand`, `list skills`, `load skill`, `list endpoints` e `call api`. Isso permite que um agente use contexto estruturado de produto/marca e acione modelos sem depender de um prompt isolado.

Planos pagos informam licença comercial para os renders. O plano Creator custa US$49/mês ou US$39/mês no equivalente anual; Scale US$149/119; Pro US$299/249. A plataforma também vende créditos avulsos. Como agrega modelos de terceiros, restrições de assets, marcas e fornecedores continuam relevantes mesmo com a licença comercial do serviço.

### Por que importa para o Marketing Hub

Esse fluxo é diretamente aplicável ao Apolo: a fonte comercial aprovada pode virar a entrada canônica do vídeo. O harness pode extrair oferta, produto, claims autorizados, imagens, CTA e restrições, montar o plano de cenas e então escolher o modelo por tomada. Isso reduz a chance de o modelo inventar detalhes comerciais e aproxima geração do contexto real do produto.

### Card criado

`video-produto-como-fonte-roteiro-multimodelo`

O card propõe testar se uma produção ancorada na fonte canônica e roteada por cena reduz erros factuais, regenerações e tempo até aprovação, sem assumir aumento de conversão.

Fontes:
- https://www.prnewswire.com/news-releases/finally-openclips-turns-a-single-product-link-into-a-production-ready-video-ad-302880391.html
- https://openclips.ai/mcp/
- https://openclips.ai/pricing/
- https://openclips.ai/models/

## 3. FineVoice Aunio — agente de áudio condicionado por vídeo

**Status: 🟡 ATIVO EM BETA — produto web disponível; não localizei API pública específica.**

A FineVoice anunciou em 17/09 o **Aunio**, uma camada agente que coordena voice-over, música, efeitos, sound design e processamento de áudio. A página oficial marca o produto como **BETA** e afirma que o `Smart Rhythm & Scene Matching` analisa cenas, movimentos, pacing e mudanças emocionais do vídeo para gerar música e efeitos coerentes com o conteúdo visual.

Além do pipeline integrado, Aunio permite criar, importar e compartilhar **AI Skills** para fluxos de voz, música e sound design, inclusive via GitHub ou arquivos locais. Isso aproxima áudio do mesmo padrão que já estamos acompanhando em vídeo: ferramentas especializadas organizadas por um harness com skills reutilizáveis.

Os planos anuais publicados equivalem a US$8,33/mês (20 mil créditos), US$14,16 (50 mil) e US$33,33 (120 mil). A página informa uso comercial de outputs elegíveis em planos pagos, sujeito aos termos e licenças dos modelos usados.

### Comparação

O **Suno v6** já havia mostrado o vídeo como entrada para geração musical. O Aunio amplia essa ideia: não fica só na música, mas tenta coordenar voz, música, efeitos, edição e preferências reutilizáveis em um mesmo fluxo. Em contrapartida, está em beta e não encontrei API pública específica, então ainda é menos direto para integração server-side do que sistemas com endpoints maduros.

### Card atualizado

Reutilizei o `cardKey` **`video-trilha-condicionada-por-video`**, em vez de criar uma ideia duplicada. A nova versão amplia o princípio de “trilha a partir do rough cut” para “orquestração sonora completa condicionada pelo vídeo”, incluindo voz, música, efeitos e skills.

Fontes:
- https://finevoice.ai/aunio
- https://finevoice.ai/aunio/pricing
- https://citizenwire.com/2026/09/17/news-finevoice-launches-aunio-an-audio-production-agent-for-end-to-end-ai-audio-creation-citizenwire/

## 4. Correção de status — HiDream-O1-Video-1.0

O anúncio público formal do **HiDream-O1-Video-1.0** está datado de **17/09/2026**, não 15/09. Como o modelo já tinha aparecido em rankings e sinais pré-lançamento na rodada anterior, a classificação operacional continua **🟡 ATIVO/LIMITADO**: o lançamento foi confirmado, mas nesta revisão não localizei documentação oficial self-service de API do modelo de vídeo nem pesos públicos específicos. O fato de o **HiDream-O1-Image** ser MIT e open-weight não deve ser extrapolado para o modelo de vídeo.

Fonte:
- https://vietnamnews.vn/media-outreach/1799906/hidream-unveils-hidream-o1-video-1-0-a-native-omnimodal-video-model-built-for-physical-consistency.html

## Cards desta rodada

Foram gerados dois candidatos a DRAFT:

1. `video-produto-como-fonte-roteiro-multimodelo` — ideia nova, útil para ancorar o videomaker em produto/oferta real e rotear cenas por requisitos.
2. `video-trilha-condicionada-por-video` — nova versão do card existente, agora sustentada por uma arquitetura de produção sonora mais completa do que somente música.

Não foi criado card apenas para registrar o P-Video-2-Pro ou a correção de data do HiDream, porque isso aumentaria o catálogo sem acrescentar um princípio novo de produto/harness.
