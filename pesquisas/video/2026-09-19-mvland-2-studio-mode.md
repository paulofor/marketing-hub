# Radar IA para vídeo — 2026-09-19

## Resumo executivo

Uma mudança passou o filtro de relevância desta rodada:

1. **MVLAND 2.0 lançou o Studio Mode**, um workflow de produção de videoclipes orientado por agente e por análise musical. A mudança relevante não é um novo renderer: é tratar a própria música como estrutura de planejamento para história, storyboard, tomadas e edição, com controle por cena e timeline multifaixa.

Não encontrei hoje um novo lançamento de renderer comparável de Runway, Google Veo/Gemini Omni Video, Kling, Seedance, Wan, MiniMax/H3, Vidu ou Adobe que justificasse outra notificação. Republicações de Fotor Agent, Aunio e OpenCreator/KrillinAI foram filtradas porque os lançamentos relevantes já haviam ocorrido em dias anteriores.

## 1. MVLAND 2.0 Studio Mode

**Status: 🟢 ATIVO no produto web — proprietário; sem API pública documentada do Studio Mode; pesos não abertos.**

A Meitu anunciou o MVLAND 2.0 com o **Studio Mode** em 18/09/2026. O novo fluxo integra:

- compreensão da música;
- desenvolvimento criativo;
- storyboard;
- geração de tomadas;
- pós-edição;
- editor multifaixa.

O diferencial declarado é uma lógica **music-first**. O sistema analisa ritmo, clima, gênero e estrutura da faixa e usa esse entendimento para propor narrativa e tomadas. A página oficial do MVLAND também descreve detecção de batida, BPM, drops, acentos e mudanças emocionais para sincronizar imagens e transições com a estrutura da música.

### Controle depois da geração

O Studio Mode permite modificar ou regenerar separadamente:

- movimento de câmera;
- composição;
- ações dos personagens;
- cenas individuais.

O editor multifaixa permite combinar material gerado e assets originais, evitando tratar o primeiro render como artefato fechado. Isso aproxima o produto do padrão que vimos no Fotor Agent: projeto estruturado + edição localizada, em vez de regeneração integral.

### Plataforma multimodelo

O MVLAND Creative Canvas documenta suporte a vários modelos de vídeo, incluindo **Seedance 2.5, MiniMax H3 e Kling 3.0**, entre outros. O valor do MVLAND 2.0, portanto, está mais na **orquestração e direção** do que em possuir um renderer isolado superior.

O preço público começa em **US$24/mês**, mas o custo efetivo depende de créditos e modelos utilizados. A página de preços não expõe um preço separado para o Studio Mode.

### Comparação com sistemas recentes

| Sistema | Status atual | Papel principal |
|---|---|---|
| **MVLAND 2.0 Studio Mode** | 🟢 Ativo | música → planejamento → storyboard → cenas → edição |
| **Fotor Agent** | 🟢 Ativo | briefing/assets → projeto multifaixa editável e vídeo completo |
| **Aunio** | 🟡 Beta | vídeo/briefing → voz, música, SFX e pós de áudio |
| **Suno v6** | 🟢 Ativo | vídeo/ideia → geração de música |

A diferença mais útil é direcional: **Suno/Aunio usam o vídeo para orientar o áudio; MVLAND usa o áudio para orientar a estrutura visual**. Para um harness audiovisual, os dois sentidos podem coexistir.

## 2. Licença comercial: atenção

Há uma inconsistência que impede classificar o MVLAND como escolha comercial sem ressalvas.

A página `music-to-video` promete exportação 4K sem marca d'água com **“full commercial rights”**. Porém, os Termos de Serviço atualmente publicados dizem que, salvo autorização expressa em contrário, o uso do serviço e dos conteúdos gerados é pessoal e não comercial.

Por isso, para anúncios pagos ou conteúdo de cliente, o status correto é:

**produto ativo, mas direitos comerciais precisam ser confirmados para o plano/fluxo específico antes do uso.**

## 3. Por que isso importa para o Marketing Hub

O princípio novo é tratar a trilha como **estado estrutural do projeto**, não como acabamento.

Uma implementação possível para Apolo:

`trilha → audio_timeline → storyboard → requisitos de cena → renderer → revisão → patches localizados → render final`

O `audio_timeline` pode registrar intro, verso, refrão, drop, silêncio, aceleração, clímax, final e intensidade ao longo do tempo. O storyboard passa a mapear hook, problema, demonstração, prova e CTA para esses pontos, mantendo cada cena como unidade editável.

Isso não deve virar regra universal. Em anúncios de resposta direta, mensagem comercial e clareza continuam prioritárias; a música serve como uma restrição de ritmo e emoção, não como objetivo superior.

## Card desta rodada

Criei um card novo porque o princípio não é duplicado pelos anteriores:

`video-musica-como-estrutura-temporal`

Ele é diferente de `video-trilha-condicionada-por-video`: naquele caso, o rough cut orienta a trilha; neste, a trilha existente orienta roteiro, storyboard e timing visual.

Fonte revisada:
`pesquisas/video/cards/fontes/2026-09-19-mvland-musica-estrutura-temporal.md`

SHA-256:
`34d80a71d112e887ea944d42fde208f4e3fe71fcf10d3f06f858dab7ddd64491`

JSON:
`pesquisas/video/cards/2026-09-19-mvland-musica-estrutura-temporal.json`

## Fontes

- https://www.businesswire.com/news/home/20260918263125/en/
- https://mvland.com/
- https://mvland.com/pricing
- https://mvland.com/infinite-canvas
- https://mvland.com/music-to-video
- https://mvland.com/agreements/terms-of-service
- https://www.prnewswire.com/news-releases/introducing-fotor-agent-create-fully-editable-ae-quality-motion-graphics-and-long-form-videos-302876846.html
- https://finevoice.ai/aunio
