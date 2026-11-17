# Radar IA para Vídeo — 17/11/2026

**Janela verificada:** 16–17 de novembro de 2026; busca em notícias, anúncios, documentação de APIs e atualizações de ciclo de vida. **Achado novo que justifica alerta: HeyGen Avatar 6**, lançado publicamente hoje. Um aviso de marca Adobe e encerramentos de vozes HeyGen são registrados como alertas operacionais, sem confundir com desativação de produtos.

## 1. HeyGen Avatar 6 — lançamento público confirmado em 17/11

**Status: ATIVO na web e por API, com pesos proprietários.** O anúncio oficial foi emitido em 17/11/2026 às 12h07 ET. A versão já estava em early access Enterprise, portanto o avanço desta data é a disponibilidade pública do produto. Melhora prometida em naturalidade, sincronização labial, gestos associados a palavras/emoções, menos movimentos repetidos, controle textual de gestos e identidade. A janela de contexto temporal para o movimento cresceu de aproximadamente 3 para 30 segundos (dado do fornecedor).

**Resolução e duração:** 1080p ou 2K, com a pesquisa demonstrando gerações contínuas de 25–28 minutos; isso não é uma promessa de minutos gratuitos/sem quotas. A dimensão concreta de 2K depende do endpoint: 2880×1620 no Model API com referência 16:9 e 2560×1440 no Application API 16:9. O endpoint de aplicação não permite 4K.

**API comprovada em documentação:** `heygen-avatar-6` e `heygen-avatar-6-turbo`, endpoints `POST /v3/models/avatars` e `POST /v3/videos`. Há `motion_prompt` e `motion_expressiveness`; vídeo de referência real para obter gestos pessoais é restrito a Enterprise com consentimento, enquanto fotos podem ser usadas nos termos do serviço. `POST /v3/videos` suporta um avatar por requisição: demos de dois apresentadores não demonstram suporte universal a dois no mesmo endpoint. Não foi feita execução autenticada.

**Realismo:** no experimento da HeyGen com 270 julgamentos lado a lado, o avatar foi escolhido como real em **42,2%** das respostas. O anúncio chama esse número de “84,4% indistinguível” porque o multiplica por 2 em relação ao nível de 50% de confusão; não equivale a 84,4% dos avaliadores enganados. O mesmo trabalho reporta preferência frente a Seedance 2.5, Wan 3.0, Kling 4.0 e fornecedores de avatar. Avaliação interna, não reprodução independente.

**Preços:** primeira geração gratuita sem cartão no site, plano Creator US$29/mês (600 créditos). A tabela de preços consultada da API ainda não especificava tarifa por minuto do Avatar 6; API usa créditos próprios, separados dos créditos do plano web. Não usar preços de Avatar V nem de HeyGen Video genérico como estimativa definitiva de Avatar 6.

Fontes primárias: https://www.heygen.com/models/heygen-avatar-6 ; https://www.heygen.com/research/avatar-6-model ; https://developers.heygen.com/docs/models/avatar-6 ; https://www.prnewswire.com/news-releases/heygen-introduces-avatar-6-an-audio-driven-context-aware-ai-model-that-alleviates-repetitive-or-unnatural-motion-in-ai-generated-video-302942527.html

## 2. Alerta operacional: versões antigas da HeyGen em transição

Na atualização Enterprise de novembro, algumas vozes disponíveis **dentro da HeyGen** já foram encerradas, incluindo `heygen-voice-0` e Fish S1 em **15/11**. Vozes Google nessa plataforma estão **EM TRANSIÇÃO até 20/11**, Gemini até **30/11**; não se refere ao encerramento dos modelos públicos externos do Google. Migrar vozes de rascunhos para engines ativas; vídeos já gerados não são apagados. Avatar IV e V **continuam ATIVOS**, mas perderam a opção de exportação 4K em drafts com essas engines porque o render original era em 1080p. Esses avisos não significam descontinuação do produto HeyGen.

Fonte: https://heygen.noticeable.news/publications/november-2026-enterprise-web-releases

## 3. Adobe — restrição judicial de marca, não encerramento de serviço

Decisão judicial conhecida em **16/11/2026** proibiu temporariamente Adobe de usar a marca “Firefly Foundry” durante litígio de marca com a empresa de VFX Foundry. **Status do produto gerador:** não foi confirmado qualquer encerramento de aplicativo, modelos ou API pelo fato da ordem; registrar como alteração legal/branding, não como descontinuação técnica.

Fonte: https://www.reuters.com/legal/legalindustry/adobe-blocked-using-foundry-name-ai-tools-during-trademark-dispute-2026-11-16/

## 4. Comparação de situação dos sistemas — checagem em 17/11

| Sistema | Origem | Status | Relevância |
|---|---|---|---|
| HeyGen Avatar 6 | EUA | ATIVO web/API/MCP, pesos fechados | Avatar de fala contextual, 2K; API documentada |
| HeyGen Avatar IV/V | EUA | ATIVOS, com retirada da exportação 4K nos drafts indicados | Comparativo legado; saída antiga não era 4K nativo |
| HeyGen Google/Gemini voices | EUA | EM TRANSIÇÃO na integração HeyGen até 20/11 e 30/11 | Exige atualizar rascunhos; não são APIs Google desligadas |
| LongCat-Video-Avatar 1.6 | China | ATIVO, pesos e código abertos MIT; sem API hospedada oficial comprovada | Alternativa para infraestrutura própria, lançada 15/11 |
| Kling 4.0 / Flash | China | ATIVO via API Runway; proprietário | Geração/edição geral, não concorrente idêntico de avatar |
| Seedance 2.5 | China | ATIVO via API; proprietário | Geração geral com áudio e referências |
| Wan 3.0 | China | ATIVO via plataformas comerciais, fechado | Geração geral multimodal |
| Gemini Omni 1.2 Flash | EUA | ATIVO em API; proprietário | Geração e edição audiovisual, não avatar especializado |
| OpenAI Sora 2 | EUA | DESCONTINUADO app 26/04 e API 24/09 | Não adequado a novas integrações |

Fontes para ciclo de vida: https://docs.dev.runwayml.com/api/ ; https://help.openai.com/pt-br/articles/20001152-what-to-know-about-the-sora-discontinuation ; https://github.com/meituan-longcat/LongCat-Video ; https://heygen.noticeable.news/publications/november-2026-enterprise-web-releases

## 5. Implicação para Marketing Hub

Testar o HeyGen Avatar 6 em um conjunto de 12–20 anúncios e vídeos explicativos em PT-BR (20–60 s) com gestos expressivos, fala espontânea e edição local, comparando Avatar V e, se viável, LongCat. Registrar resolução real, lipsync, identidade, aderência ao prompt, naturalidade, tempo e **custo por vídeo final aprovado**; não adotar antes de obter tarifa específica de API e direitos de uso comercial de voz/imagem. Considerar o apoio de um modelo de compreensão de vídeo para QA das gerações.

**Card desta rodada:** `pesquisas/video/cards/2026-11-17-heygen-avatar6-contextual.json`
**Fonte revisada:** `pesquisas/video/cards/fontes/2026-11-17-heygen-avatar6-contextual.md`
**SHA-256 exato da fonte:** `83f940ed8afbcdeb2d03747363b09f293afc42f98bf9076339e70f7380520d7a`

A pesquisa não encontrou outro modelo fundacional novo de vídeo com impacto comparável publicado especificamente em 17/11; os desenvolvimentos de 16/11 já haviam sido abordados no radar anterior.
