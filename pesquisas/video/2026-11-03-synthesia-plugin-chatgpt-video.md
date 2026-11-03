# Radar de IA para Vídeo — 03/11/2026

**Decisão editorial: um alerta relevante.** Hoje a **Synthesia (Reino Unido)** anunciou a abertura de um plugin no ChatGPT para produzir vídeos de explicação, comunicação, treinamento e marketing a partir de conversas, briefings e materiais conectados. **É um avanço de integração de produção, não um modelo fundacional novo.** Verificação realizada em 03/11/2026; não fiz chamadas autenticadas de geração.

## 1. Synthesia no ChatGPT — lançamento de 03/11

A fabricante informa que o plugin está disponível para conexão em todos os planos Synthesia, inclusive o gratuito. É possível fornecer documentos, apresentações, planilhas, textos do chat ou materiais de aplicativos conectados mediante permissão e pedir um vídeo sem copiar manualmente o roteiro para outra ferramenta. O plugin também pode pesquisar vídeos já existentes na biblioteca autorizada.

- **Syren:** o agente cria vídeo com motion graphics, avatares, locução, música e b-roll e permite ajustes por conversa. **Vídeos Syren não abrem no editor Studio.**
- **Studio:** produz um rascunho com script literal, estilo, template ou marca, para concluir e editar cena por cena no ambiente Synthesia. Rascunhos não consomem créditos; a renderização final consome.
- **Status operacional:** plugin **ATIVO segundo o lançamento oficial**; a conexão **Synthesia MCP está em BETA PÚBLICO**, conforme documentação da própria empresa. A disponibilidade efetiva por conta e região não foi testada. Os modelos são proprietários, sem pesos abertos anunciados.

O Syren existia desde **09/10/2026**. A mudança real em 03/11 é a capacidade de inseri-lo em um fluxo conversacional com outras ferramentas, inclusive tarefas recorrentes quando autorizadas. Isso facilita explicativos e apresentações, mas não demonstra qualidade cinematográfica superior aos modelos especializados.

Fontes primárias: [lançamento Synthesia de 03/11](https://www.synthesia.io/post/synthesia-launches-in-chatgpt); [MCP beta, escopo e limitações](https://help.synthesia.io/en/articles/17504685-how-do-i-use-the-synthesia-mcp); [Syren lançado em 09/10](https://www.synthesia.io/post/syren-studio).

## 2. Preço, licença e limitações

A [tabela oficial](https://www.synthesia.io/pricing) consultada hoje informa **Basic US$ 0**, com 10 minutos de criação Studio ou dublagem por mês e 500 créditos mensais, além de **um vídeo Syren em promoção limitada**. No gratuito, há marca d'água e restrições a download. O **Starter é US$ 19/mês** depois do primeiro mês promocional de US$ 9 e o **Pro é US$ 89/mês** depois do primeiro mês de US$ 49 na modalidade mensal exibida. Estes não são descontos novos de 03/11. A geração usa créditos, conforme formato e elementos utilizados; o conector MCP não adiciona cobrança própria durante o beta.

Para uso comercial, conferir permissões de avatares, vozes, marcas, imagens, músicas e arquivos do cliente. Não houve auditoria contratual, teste de cobrança nem benchmark independente.

## 3. Comparação operacional em 03/11

| Sistema / mercado | Status | Onde se diferencia |
| --- | --- | --- |
| **Synthesia Plugin + Syren/Studio / Europa** | **ATIVO** segundo anúncio; **MCP em beta público** | Documento, roteiro e conversas convertidos em vídeos com avatar, locução e animação |
| **Runway Ads e App Studio / EUA** | **ATIVOS**, planos pagos | Otimização de criativos com dados de campanhas e construção de workflows |
| **Genspark Media Studio / internacional** | **ATIVO** como SaaS | Agentes, canvas e produção multimídia |
| **Kling 4.0 / China** | API **ATIVA** na Runway Dev | Cenas gerativas e edição com referências, até 30 segundos |
| **Seedance 2.5 / China** | API **ATIVA** na Runway Dev | Vídeo/áudio e edição baseada em referências |
| **Veo 3.1 GA / EUA** | **ATIVO** no Google Cloud | Geração audiovisual; canal distinto do Preview |
| **Veo 3.1 Preview / EUA** | **DESCONTINUADO** na Gemini API em 22/10 | Não utilizar os endpoints antigos |
| **OpenAI Sora 2 / EUA** | **DESCONTINUADO** (app 26/04; API 24/09) | Qualidade histórica não significa disponibilidade |
| **ElevenLabs SFX v3 / EUA** | Serviço **ATIVO**, habilitação pode variar | Texto para efeitos sonoros; o guia da API de 02/11 só detalha um lançamento já anunciado em 29/10 |

Fontes de status: [Runway API changelog](https://docs.dev.runwayml.com/api-details/api_changelog/), [ElevenLabs SFX v3 — orientação de 02/11](https://elevenlabs.io/blog/sfx-v3-for-developers), [Google deprecações](https://ai.google.dev/gemini-api/docs/deprecations), [Sora encerramento](https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation).

## 4. Implicação para o Marketing Hub

**Teste sugerido:** transformar um briefing de produto aprovado em vídeo explicativo de **30–60 segundos em português brasileiro** usando Syren e Studio. Comparar com o fluxo multimodelo atual: fidelidade à oferta, marca, naturalidade, tempo de revisão, créditos consumidos e custo por vídeo aprovado. A integração é interessante para apresentações e anúncios didáticos, mas não substitui Kling, Veo e Seedance para tomadas cinematográficas especializadas.

**Card JSON:** criado 1 card com fonte revisada e SHA-256 exato. Nenhum card sobre notícias repetidas.
