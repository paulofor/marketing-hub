# Radar de IA para vídeo — 2026-09-16

## Resumo executivo

Duas mudanças passaram o filtro de relevância desta rodada:

1. **HiDream-O1-Video-1.0 (HD-V1)** teve lançamento público/formal em 15/09/2026. É um novo modelo chinês nativamente multimodal para vídeo, com entradas de texto, imagem e vídeo, saída declarada de 5–20 s em 1080p e geração conjunta de áudio/vídeo. No leaderboard independente de image-to-video com áudio da Artificial Analysis, aparece hoje em 4º lugar, atrás de H3 Max, Seedance 2.0 e MiniMax H3 e à frente de Gemini Omni Flash nessa categoria específica.
2. **Fotor Agent / Video Agent** recebeu anúncio amplo em 15/09/2026, após um soft launch no Product Hunt em 31/08. O avanço relevante não é um novo renderer, mas uma arquitetura de produção em que o agente cria e mantém um **projeto audiovisual editável**, com timeline multifaixa e elementos paramétricos, em vez de tratar o MP4 achatado como único estado da produção.

## 1. HiDream-O1-Video-1.0 (HD-V1)

**Status: 🟡 ATIVO / LIMITADO.**

A HiDream/智象未来 anunciou publicamente em 15/09 o HiDream-O1-Video-1.0, abreviado HD-V1. As informações publicadas descrevem:

- entradas de texto, imagem e vídeo;
- geração de 5 a 20 segundos;
- saída 1080p;
- modelagem conjunta de texto, vídeo e áudio;
- planejamento estruturado de tomada, incluindo duração, localização, ação/expressão, câmera, diálogo e som ambiente;
- duração condicionada pelo conteúdo em vez de uma duração fixa única;
- foco declarado em coerência física e espacial.

As capacidades de física, planejamento e narrativa são alegações do fornecedor e da cobertura de lançamento; não devem ser tratadas como benchmark independente.

### Posição competitiva

No leaderboard **Image-to-Video com áudio** da Artificial Analysis, consultado em 16/09, a ordem principal é:

| Posição | Modelo | Elo aproximado | Preço de API exibido |
| --- | --- | ---: | ---: |
| 1 | Minimax H3 Max (post-trained by fal) | 1206–1207 | US$ 2,40/min |
| 2 | Dreamina Seedance 2.0 720p | 1196–1197 | US$ 9,07/min |
| 3 | MiniMax H3 | 1190 | US$ 7,80/min |
| 4 | **HiDream-O1-Video** | **1185–1186** | **US$ 5,80/min** |
| 5 | Gemini Omni Flash | 1181 | US$ 6,00/min |
| 6 | Wan 3.0 | 1178–1179 | US$ 12,00/min |

Esse ranking é específico de image-to-video com áudio e não prova liderança em text-to-video, edição, consistência longa ou custo total de produção.

Há uma nuance de cronologia: a Artificial Analysis marca o modelo como “Released Aug 2026”, enquanto a divulgação pública/formal ocorreu em 15/09. Isso sugere acesso anterior para benchmark ou disponibilidade restrita antes do anúncio amplo.

### Disponibilidade e licença

Não encontrei nesta rodada uma página oficial self-service da HiDream documentando API do HD-V1, pesos públicos ou licença aberta específica do modelo de vídeo. A Artificial Analysis acompanha um preço de API de US$ 5,80/min, mas a integração oficial ainda está menos documentada que fal, Google, ByteDance ou MiniMax.

A HiDream mantém pesos abertos e licença MIT para sua família **HiDream-O1-Image**, mas essa licença **não deve ser transferida por inferência ao HD-V1**. Até publicação explícita, HD-V1 deve ser tratado como fechado/sem licença de pesos confirmada.

### Por que importa

A relevância não é apenas ter mais um modelo chinês. Um novo fornecedor entrou imediatamente próximo ao topo de um benchmark independente de vídeo com áudio e oferece duração variável até 20 s em 1080p. Isso aumenta a pressão competitiva sobre MiniMax, ByteDance, Alibaba e Google, principalmente em geração audiovisual nativa.

Para o Marketing Hub, ainda não recomendo HD-V1 como integração padrão: primeiro é necessário confirmar endpoint oficial, SLA, termos comerciais e documentação de API. Vale mantê-lo no conjunto de candidatos para avaliação quando essa camada operacional estiver clara.

## 2. Fotor Agent / Video Agent

**Status: 🟢 ATIVO no produto web; 🟡 LIMITADO para integração externa do workflow completo.**

A Fotor fez em 15/09 um anúncio amplo do Fotor Agent, mas o Video Agent já havia aparecido no Product Hunt em 31/08, quando ficou em 1º lugar do dia. Portanto, esta rodada registra a formalização/expansão do produto e sua arquitetura documentada, não um lançamento binário que só passou a existir ontem.

O sistema recebe briefing ou materiais brutos, faz planejamento de longo horizonte, estrutura roteiro/storyboard, gera assets e monta automaticamente o projeto numa **timeline multifaixa**.

O aspecto mais importante é que o artefato de trabalho permanece editável:

- vídeo, voice-over, música, efeitos sonoros, legendas e motion graphics ficam em faixas independentes;
- texto, cores, gráficos, posições, espessuras e timing de motion graphics permanecem paramétricos;
- um clipe ou elemento pode ser selecionado e modificado localmente por edição manual ou conversa com o agente;
- o editor atual documenta exportação MP4 em 1080p;
- o anúncio também promove motion graphics 4K, mas essa afirmação não deve ser generalizada para todo tipo de exportação de vídeo.

A Fotor afirma que os elementos gerados pelo editor — incluindo animações, áudio e vozes sintetizadas — são royalty-free para campanhas comerciais, permanecendo a responsabilidade do usuário sobre direitos dos assets próprios enviados.

### O que muda arquiteturalmente

A diferença para um pipeline simples de geração é esta:

```text
briefing
  ↓
planejamento do agente
  ↓
projeto estruturado
  ├─ cenas
  ├─ clips
  ├─ voz
  ├─ música
  ├─ SFX
  ├─ legendas
  └─ motion graphics paramétricos
  ↓
patch localizado
  ↓
render final
```

Em vez de usar o MP4 como único estado, o sistema preserva uma representação intermediária editável. Isso é particularmente importante para agentes porque uma revisão deixa de significar necessariamente “gere tudo outra vez”.

### Comparação conceitual

- **FLUX Video Edit**, acompanhado em 13/09, edita seletivamente um master já renderizado e preserva grande parte do vídeo original.
- **DaVinci/CuttingRoom + MCP**, também acompanhados anteriormente, permitem a um LLM operar ferramentas e um projeto real de edição.
- **Fotor Agent** mostra a outra peça: o próprio agente cria desde o início um **estado estruturado e paramétrico** como artefato primário.

Essas abordagens são complementares, não substitutas.

### API, Skills e preços

A Fotor possui Developer APIs para recursos de vídeo, incluindo AI Video Generator, Image2Video e Video Enhancer, e mantém Fotor Skills para agentes como OpenClaw e Claude Code, inclusive com geração de clips e refinamento contínuo. Porém, não encontrei documentação pública que exponha via API o workflow completo do Video Agent com seu projeto multifaixa editável. Por isso a integração externa do agente completo permanece classificada como limitada.

A página de preços atual mostra:

- **Basic:** gratuito, créditos limitados, 1 geração simultânea, 50 chats de AI Agent e 1 tarefa de Agent;
- **Pro:** 10 gerações simultâneas, 2.000 chats e 2 tarefas de Agent;
- **Pro+:** 30 gerações simultâneas, 4.000 chats e 5 tarefas de Agent.

Os valores monetários de Pro/Pro+ não estavam renderizados corretamente na página consultada, então não foram inferidos.

As alegações de “vídeo de 2 minutos em menos de 1 hora”, redução de custo de motion graphics para 1/200 e ganho de 50× são benchmarks da própria Fotor, não validação independente.

## Implicação para o Marketing Hub

A principal ideia reutilizável desta rodada é manter o vídeo como um **projeto estruturado persistente**. Apolo poderia manter uma representação de cenas, tracks, assets e parâmetros; revisões de Íris, Psique e Têmis seriam traduzidas em patches localizados antes de decidir por regenerar uma cena ou o projeto completo.

Exemplos:

```text
“Têmis rejeitou o claim”
→ troca somente texto/voice-over/legenda afetados

“Psique achou o CTA tardio”
→ altera timing da cena e trilhas relacionadas

“Íris mudou a oferta”
→ atualiza copy, preço e elementos gráficos autorizados

“produto perdeu fidelidade”
→ regenera apenas o clip problemático
```

Essa arquitetura precisa ser testada por custo, número de regenerações, tempo até aprovação, erros de continuidade e violações de marca. Não há evidência de que, por si só, aumente conversão ou vendas.

## Card criado

Foi criado apenas um card novo nesta rodada:

- `video-projeto-estruturado-editavel-agente`

Não foi criado card para HD-V1: o lançamento é importante para o radar competitivo, mas nesta rodada ele não acrescenta um princípio de produto suficientemente distinto dos cards existentes para justificar novo conhecimento no harness.

Fonte revisada:

`pesquisas/video/cards/fontes/2026-09-16-projeto-estruturado-editavel-agente.md`

SHA-256:

`b0581fdc18d059cffa60e058fe1976285f72b6cc52a0a74021c00feb1baa77ee`

JSON:

`pesquisas/video/cards/2026-09-16-projeto-estruturado-editavel-agente.json`

O card segue a coleção `video` e permanece candidato a DRAFT no fluxo editorial da Harness Library.

## Fontes consultadas

- HiDream/HD-V1 — cobertura do lançamento: https://tech.ifeng.com/c/8nM0fVKgw6M
- Artificial Analysis — Image-to-Video Leaderboard: https://artificialanalysis.ai/video/leaderboard/image-to-video
- Fotor — anúncio de 15/09: https://www.prnewswire.com/news-releases/introducing-fotor-agent-create-fully-editable-ae-quality-motion-graphics-and-long-form-videos-302876846.html
- Fotor Video Agent: https://www.fotor.com/agent/
- Fotor AI Video Editor: https://www.fotor.com/ai-video-editor/
- Fotor Pricing: https://www.fotor.com/pricing/
- Fotor Developer APIs: https://developers.fotor.com/
- Fotor Skills: https://developers.fotor.com/fotor-skills/
- Product Hunt — Fotor: https://www.producthunt.com/products/fotor
