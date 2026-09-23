# Radar IA para Vídeo — 2026-09-23

Nesta rodada, três mudanças passaram o filtro de relevância: o Tesseract 0.2.0 tornou uma suíte de edição agent-native realmente disponível em Linux/Windows/macOS e elevou o export para 4K/60 fps; a PixVerse lançou o R2 como segunda geração de seu real-time world model com memória persistente de sessão; e o YouTube anunciou edição conversacional com Gemini e dublagem automática ao vivo, ambas com disponibilidade prevista para 2027.

## 1. Mirage Tesseract 0.2.0 — edição local desenhada para agentes

**Status: ATIVO. Runtime/CLI distribuído; software proprietário; não é um renderer generativo de footage.**

A Mirage lançou o Tesseract como uma suíte de criação audiovisual operável diretamente por agentes. Em 23/09/2026, a release 0.2.0 adicionou **Linux x86_64**, além de macOS e Windows, e ampliou as opções de exportação para incluir **4K e 60 fps**. O repositório oficial documenta instalação por skills, plugin para ChatGPT/Codex e um projeto editável `.tsrct`.

O sistema trabalha com conceitos de edição e motion graphics — camadas, composições, keyframes, máscaras, texto, timing e áudio — e renderiza localmente. Isso o diferencia de um modelo de geração: ele não cria footage ou avatar do zero; organiza, edita e finaliza mídia existente, inclusive material produzido por modelos generativos.

### Licença e uso comercial

O engine é gratuito, mas **não é open source**. Os termos permitem uso profissional/comercial para empresas abaixo do limiar de **US$ 1 milhão de receita anual**; negócios cobertos a partir desse valor precisam de acordo separado com a Mirage. Os termos também atribuem ao usuário, entre usuário e Mirage, os direitos sobre Input e Output, sujeitos a direitos de terceiros.

### Comparação

- **Runway Workflows — ATIVO:** mais forte em grafo cloud multimodelo que combina geração e pós-produção.
- **DaVinci Resolve + MCP — ATIVO:** NLE profissional mais abrangente, com finishing tradicional e controle fino.
- **Fotor Agent — ATIVO:** timeline multifaixa editável dentro de um produto web.
- **Tesseract — ATIVO:** diferencia-se por projeto local, skills para agentes e edição/motion/som expostos diretamente ao agente.

### Por que importa

A mudança reforça a transição de `prompt -> MP4` para:

`briefing -> projeto editável -> preview -> revisão localizada -> render`

No Marketing Hub, geração e edição podem ficar desacopladas. Um renderer cria a tomada, um modelo de compreensão revisa e uma ferramenta como Tesseract altera apenas texto, timing, áudio, composição ou keyframes necessários, sem destruir partes já aprovadas.

## 2. PixVerse R2 — world model com memória persistente de sessão

**Status: ATIVO/LIMITADO. Mundos públicos e Game Engine disponíveis; sem API R2 dedicada, preço específico ou pesos abertos verificados.**

A PixVerse lançou o **R2 em 22/09/2026** como segunda geração de seu real-time world model. A diferença central em relação a clipes independentes é manter uma sessão contínua: texto, referências, áudio e comandos de ação entram no mesmo mundo e continuam influenciando o que acontece depois.

A empresa afirma maior coerência em sessões longas e memória do que já aconteceu na sessão. O **PixVerse Game Engine já usa R2**, e mundos R2 estão abertos para exploração pública. Isso é mais produtizado do que uma demo fechada, mas ainda não o coloca no mesmo nível de uma API de produção convencional.

### Comparação com Runway GWM Worlds 2

O **Runway GWM Worlds 2 continua em Research Preview**. Ele gera vídeo contínuo em 720p/24 fps com áudio 48 kHz, combina contexto persistente com eventos temporais e oferece controle contínuo de câmera, mas a própria Runway reconhece limitações de memória de longo prazo e perda de fidelidade em alguns movimentos difíceis.

R2 parece mais acessível como experiência de produto porque já alimenta o Game Engine e possui mundos públicos. Porém, sem documentação de API dedicada, preço e garantias operacionais, deve permanecer como tecnologia de exploração, não como backend recomendado para produção automatizada.

### Por que importa

A memória interna do world model pode reduzir a necessidade de redescrever cenário e ação a cada tomada. Para publicidade, entretanto, **produto, preço, CTA, claims, figurino e identidade visual devem continuar num estado canônico externo**, e não apenas na memória probabilística do modelo.

Fluxo candidato:

`scene_state canônico -> evento -> world model -> revisor audiovisual -> atualização somente do estado confirmado`

## 3. YouTube — edição conversacional e dublagem automática ao vivo

### Edição conversacional com Gemini

**Status: ANUNCIADO / disponibilidade ampla prevista para início de 2027.**

O YouTube anunciou integração de Gemini a um assistente de edição conversacional para **Shorts e YouTube Create**. O usuário poderá pedir em linguagem natural que o sistema gere um primeiro corte, reorganize frames, reduza trechos de fala, sincronize música com batidas e adicione texto, mantendo a timeline disponível para edição manual.

A mudança é importante como validação de mercado de edição por agente dentro de uma plataforma de massa, mas **não deve ser apresentada como opção atual de produção** enquanto a liberação ampla não ocorrer.

### Live auto dubbing

**Status: ANUNCIADO / piloto previsto para início de 2027.**

O YouTube também anunciou um piloto de **auto dubbing para transmissões ao vivo**, traduzindo a fala em tempo real para que espectadores ouçam no idioma preferido. Não foram anunciadas API pública nem tabela de preço específica para esse recurso.

Para produção audiovisual, isso indica que tradução/dublagem sincronizada está migrando de pós-produção para a própria camada de distribuição, mas hoje o recurso ainda não está disponível para uso geral.

## Cards

Nenhum `cardKey` novo foi criado. Foram versionados dois princípios já existentes:

- `video-projeto-estruturado-editavel-agente`: agora reforçado pelo Tesseract 0.2.0, com projeto local agent-native, Linux e export 4K/60 fps.
- `video-estado-persistente-eventos-temporais`: agora reforçado pelo PixVerse R2, distinguindo memória interna de sessão de um estado canônico externo para fatos comerciais.

Não foi criado card para os anúncios do YouTube porque eles reforçam princípios já cobertos e ainda têm disponibilidade futura.

## Fontes principais

- Mirage — Tesseract: https://mirage.app/tesseract
- GitHub — Tesseract: https://github.com/mirage-hq/Tesseract
- GitHub — Tesseract Releases: https://github.com/mirage-hq/Tesseract/releases
- Mirage — Tesseract Terms: https://mirage.app/legal/tesseract-terms
- PixVerse — R2: https://pixverse.ai/en/blog/pixverse-introduces-r2-real-time-world-model
- Runway — GWM Worlds 2: https://runway.com/research/introducing-gwm-worlds-2
- YouTube — Made on YouTube: https://blog.youtube/news-and-events/made-on-youtube-new-tools-power-creation-journey/
- YouTube — Live tools and auto dubbing: https://blog.youtube/news-and-events/made-on-youtube-live-tools-fan-funding-dubbing/
