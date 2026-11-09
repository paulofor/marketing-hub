# Radar IA para Vídeo — 09/11/2026

**Janela pesquisada:** 07–09/11/2026, checando também anúncios anteriores que alteraram acesso ou licença recentemente. **Notícia central datada de hoje:** mudança efetiva da política de créditos do AI Studios 5.0. A integração Kling Flash e a mudança de unidade de cobrança na Luma foram identificadas na comparação de preços 02→09/11 (já observáveis em 07/11), não são novo lançamento de Kling. Também foi identificada evolução do Phosphene v5.0 (06/11), ainda não documentada neste radar. Nenhum desempenho visual foi testado diretamente nesta rodada.

## 1. AI Studios 5.0 — mudança de política comercial em vigor hoje (Coreia do Sul)

**Status ATIVO SaaS; API específica da nova cobrança não verificada.** A partir de **09/11/2026 às 15h KST**, o AI Studios/DeepBrain AI passa a utilizar uma carteira única de créditos para exportação de vídeo com avatar, dublagem, áudio e geração de imagens/vídeos. Antes havia controle separado de minutos e créditos generativos. A documentação oficial apresenta Personal **US$29/mês (US$24 anual) / 700 créditos/mês**, Team **US$69 por assento/mês (US$55 anual) / 1.000 créditos por assento** (mín. 2). Gratuito: 12 créditos no cadastro e 3 vídeos de até 1 minuto com marca d'água no mês.

Tabela oficial de custos em créditos por minuto: exportação de avatar **9**, exportação somente voz **6**, exportação áudio **3**, dublagem **3**, dublagem com lipsync **6**. Geração de imagens e clipes custa valores distintos por modelo, que devem ser conferidos antes de cada render. **Não confundir custo de exportação com custo completo do vídeo.**

**Promoção 2× créditos no primeiro mês**, não desconto permanente: novos assinantes **09–16/11/2026** e assinantes antigos que migrem voluntariamente **09/11–09/12/2026**, respeitando elegibilidade e horário 15h KST. Contas antigas não mudam automaticamente. Minutos e créditos-base antigos não são transferidos; adicionais pagos viram bônus 4× com validade de um mês. Créditos mensais não acumulam.

**Por que importa:** agentes de produção e dublagem podem compartilhar orçamento mais flexível, mas reedições caras e migração de plano podem causar perda de saldo. Para o Marketing Hub, testar um vídeo PT-BR com avatar e três traduções autorizadas, medindo créditos efetivos.

Fontes: https://help.aistudios.com/en/articles/18242035-ai-studios-pricing-update ; https://help.aistudios.com/en/articles/17836891-generate-ai-images-and-video-clips-in-video-agent

## 2. Luma Marketplace — Kling Flash e mudança na unidade tarifária

**Status ATIVO SaaS**, com integração de modelos chineses Kling e Seedance; API dedicada das variantes nesta plataforma não verificada. A página atual da Luma mostra **Kling 4.0 Flash 720p por 32 créditos/s gerado** e **Kling 4.0 completo 720p por 53 créditos/s** (aprox. 40% menos créditos Flash por clipe para text/image-to-video). A Luma agora lista **Kling 4.0 Flash Omni V2V 720p a 21 créditos/s de entrada + saída**.

A comparação externa de capturas da página 02→09/11 relata mudança tarifária de **Seedance 2.0 V2V 480p de ~30 créditos/s para 15 créditos/s da soma de referência + saída**. Isso **não** significa 50% menos custo por job: se referência e saída têm ambos 10s, o custo permanece 300 créditos (10×30 ou 20×15). Com referência de 5s e saída 10s, cai a 225; com referência de 15s e saída 10s, sobe a 375 créditos. Exemplos apenas demonstrativos; a base antiga vem do monitor de preços de terceiros.

Luma Plus: US$30/mês (US$25 anual) com 10 mil créditos; Pro: US$90/mês (US$75 anual), 40 mil. Não comparar créditos Luma diretamente com dólares por segundo da Runway; a Runway disponibilizava Kling 4.0 Flash anteriormente.

**Por que importa:** escolher modelos por tarifa nominal pode induzir erro. Normalizar custo por clipe final incluindo segundos da entrada e saída, áudio, resolução e retries.

Fontes: https://lumalabs.ai/pricing ; https://pricingsaas.com/companies/lumaai/diffs/2026W46

## 3. Phosphene 5.0 — produção local aberta, exclusivamente Apple Silicon

**Status ATIVO, código aberto MIT, execução local no Mac (não Windows/Linux).** A versão 5.0 lançada **06/11/2026** introduziu aplicativo Mac nativo, lipsync, ferramentas de fala, novas opções de imagem e recursos de agente. A **5.0.9, de 09/11**, facilita download de pesos MiniMax/Hailuo H3 diretamente no aplicativo, recuperação de downloads e ajustes de áudio. A **5.0.10**, no mesmo dia, corrige persistência da licença Pro; não é novo modelo.

É um orquestrador para modelos de terceiros (LTX-2.5, MiniMax H3, imagem, música), com API HTTP local, CLI e MCP, sem custo de GPU em nuvem. Limites: hardware Apple Silicon e tempos de geração significativamente mais lentos do que provedores GPU; pesos H3 requerem grande volume de download. A licença MIT **do aplicativo não torna todos os modelos livres para uso comercial**: licença de H3 possui restrições geográficas e outros pesos podem ter restrições comerciais. Não corresponde a abertura de pesos de um novo modelo.

Fontes: https://github.com/mrbizarro/Phosphene/releases ; https://github.com/mrbizarro/Phosphene ; https://phosphene.video/changelog/

## 4. Synthesia Syren — alteração de condições e término dos créditos de lançamento

**Status ATIVO, Syren em beta, plugin ChatGPT ativo.** O serviço tem hoje FAQ indicando **3 minutos/vídeo no plano Basic e até 60 minutos/vídeo em planos pagos**. A página comercial indica consumo compartilhado de créditos, com volumes ilustrativos (~2 vídeos/mês Starter e ~7 Pro), **não quotas garantidas** pois cada geração e cada edição consume créditos. Um monitor externo observou a troca na apresentação de limites entre 02 e 09/11, mas a página e FAQ não fundamentam interpretar as estimativas como quota rígida. Créditos gratuitos Syren-only da campanha **expiram 09/11/2026 às 23h59 BST**, conforme documentação, após o que o consumo usa saldo regular da assinatura. Novo preço por render não confirmado.

Fontes: https://www.synthesia.io/pricing ; https://help.synthesia.io/en/articles/17697950-how-do-syren-credits-work ; https://help.synthesia.io/en/articles/17441336-how-do-i-create-a-video-with-syren ; https://pricingsaas.com/companies/synthesia/diffs/2026W46

## Situação dos sistemas — checagem operacional

| Sistema / origem | Status em 09/11 | Observação |
|---|---|---|
| AI Studios 5.0 / Coreia do Sul | **ATIVO SaaS**, novo crédito unificado | Não confirmado que API replicará todos recursos |
| Luma + Kling Flash / EUA + China | **ATIVO SaaS** | Modelo também ativo via API Runway; Luma cobra créditos |
| Seedance 2.0 / China | **ATIVO por provedores** | Luma alterou base da tarifa V2V, não modelo |
| Phosphene 5.0 / open source | **ATIVO local Mac** | Código MIT, licenças dos pesos distintas |
| Synthesia Syren / Reino Unido | **ATIVO beta** | Créditos gratuitos de lançamento expiram 09/11 |
| Gemini Omni 1.2 Flash / EUA | **ATIVO GA/API** | Lançado 05/11, sem nova mudança de preço hoje |
| Veo 3.1 GA / EUA | **ATIVO**, aposentadoria datada não antes de 04/05/2027 no Google Cloud | Não confundir com Preview descontinuado |
| Seedance 1.5 Pro / ElevenLabs | **ENCERRAMENTO ANUNCIADO** em 11/11/2026 | Não iniciar integração nova |
| OpenAI Sora 2 | **DESCONTINUADO** desde 24/09/2026 na API | Não indicar como opção contratável |

Fontes: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate ; https://elevenlabs.io/docs/changelog ; https://dev.runwayml.com/learn/gemini-omni-flash-1-2-api

## Decisões para o Marketing Hub
- Criar card de política de créditos AI Studios 5.0: valor de processo de avatares/dublagem versus risco de créditos não usados.
- Criar card de análise de custo real V2V na Luma: fórmula (segundos de entrada + saída) × créditos; comparação justa entre provedores.
- Phosphene e Synthesia registrados no relatório sem novos cards: Phosphene limitado a Apple Silicon; Synthesia com política em atualização e sem custo determinístico por render.
- **Sem chamadas pagas, autenticação em plataformas audiovisuais ou benchmark independente.**

Fontes revisadas dos cards:
- `pesquisas/video/cards/fontes/2026-11-09-ai-studios-5-creditos-unificados.md` — SHA-256: `882e75152cfccced4e3828fa405d7df7b52352511f66be829b07e5ecdba9a8a5`
- `pesquisas/video/cards/fontes/2026-11-09-luma-kling-flash-v2v-metrica.md` — SHA-256: `b19fbd47bf6880b342bf9df281be8255cdfa6048b2230050f6e6fcb185dfd4d0`
