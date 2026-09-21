# Radar IA para Vídeo — 2026-09-21

Nesta rodada, duas mudanças passaram o filtro de relevância: a HelloGen lançou um tier realmente sem cobrança por clipe para MiniMax H3, e a Meshy apresentou publicamente Mora, uma arquitetura de mundos interativos que separa lógica, estrutura 3D e renderização por vídeo em tempo real.

## 1. HelloGen: MiniMax H3 Unlimited

**Status: ATIVO no produto web; integração do tier ilimitado: não documentada publicamente.**

A HelloGen passou a oferecer MiniMax H3 em modo ilimitado nos planos Pro e Max, sem créditos por clipe e sem limite diário declarado. O modo cobre 480p, 2–15 segundos, áudio estéreo gerado, texto, primeiro frame, primeiro+último frame e até nove imagens de referência. O Pro custa US$ 19,90/mês e garante prioridade para pelo menos 2 clipes por dia; o Max custa US$ 49,90/mês e garante pelo menos 5. Depois disso, os renders continuam sem cobrança por clipe, mas entram na fila de velocidade padrão.

O upscale para 1080p/4K é separado. Modelos premium como Veo, Kling e Seedance continuam consumindo créditos. A HelloGen declara licença comercial para os resultados, mas o usuário continua responsável por direitos de terceiros.

### Comparação com MiniMax H3 via API

O MiniMax H3 continua ativo na fal, com API serverless e uso comercial. A fal lista US$ 0,05/s em 480p e US$ 0,06/s em 768p para o H3 padrão. Portanto, a mudança da HelloGen não é um novo renderer: é uma mudança de economia de iteração. Para quem produz muitos candidatos, o custo marginal por tentativa deixa de ser a principal restrição; fila e revisão passam a ser os gargalos.

### Por que importa

Esse regime reforça a estratégia `best-of-N`: gerar vários candidatos baratos para hooks, demonstrações e CTA, filtrar automaticamente erros de produto, identidade, claims e continuidade, selecionar 1–2 finalistas e só então usar acabamento ou um renderer de maior resolução. Não há evidência de que mais variantes aumentem conversão; o ganho deve ser medido por custo e tempo até uma tomada aprovada.

## 2. Meshy Mora

**Status: LIMITADO / PESQUISA. Mora 1 é publicamente jogável, mas não é uma ferramenta geral de produção.**

Em 21/09, a Meshy detalhou Mora (Multimodal Open-world Real-time Architecture). A arquitetura não pede a um único modelo de vídeo que simule todo o ambiente. Agentes de código implementam lógica e mecânicas; a camada 3D mantém estrutura espacial e assets; e um modelo de vídeo em tempo real transforma esse estado em imagem e áudio.

A Meshy classifica Mora 1 como demonstração de pesquisa para validar a arquitetura. Uma ferramenta geral de criação de mundos é objetivo de longo prazo, e Mora 2 está em desenvolvimento. Não foram encontrados API pública, preço específico ou pesos abertos para Mora.

### Comparação

O Atlas, da World Labs, continua **LIMITADO / EARLY ACCESS**, disponível a parceiros selecionados. Ele é um world model multimodal treinado para texto, imagem, vídeo e 3D e busca manter consistência espacial diretamente no modelo. O Mora aposta em outra divisão de trabalho: persistência e regras ficam explicitamente em código/3D e o vídeo funciona como camada de apresentação.

### Por que importa para produção audiovisual

A ideia pode ser aplicada a publicidade multi-shot, séries, animação e experiências interativas mesmo sem usar Mora. Em vez de pedir ao renderer que “lembre” tudo, o harness mantém um `scene_state` canônico com cenário, personagem, produto, figurino, props, posições e âncoras de câmera. Cada tomada aplica apenas um evento/patch e o estado só é atualizado depois de revisão. Isso pode reduzir drift entre cenas, mas adiciona complexidade e ainda precisa ser validado.

## Cards

Foram atualizados dois princípios existentes, sem criar novos `cardKey`:

- `video-geracao-paralela-selecao-best-of-n`, agora reforçado pela economia do H3 Unlimited.
- `video-estado-persistente-eventos-temporais`, agora reforçado pela separação código + 3D + renderer do Mora.

## Fontes

- HelloGen — Unlimited AI Video Generator: https://hellogen.ai/unlimited-ai-video-generator/
- HelloGen — Acceptable Use Policy: https://hellogen.ai/acceptable-use/
- fal — MiniMax H3: https://fal.ai/minimax-h3
- Meshy / PR Newswire — Mora, 21/09/2026: https://www.prnewswire.com/news-releases/meshy-details-mora-a-research-architecture-for-ai-generated-interactive-worlds-and-launches-meshy-7-1--302884860.html
- World Labs — Atlas, 01/09/2026: https://www.worldlabs.ai/blog/atlas
