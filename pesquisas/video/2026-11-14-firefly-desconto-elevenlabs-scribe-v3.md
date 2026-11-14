# Radar de IA para Video - 14/11/2026

**Janela de verificacao:** noticias e fontes primarias disponiveis ate 14/11/2026, em comparacao com o relatorio de 13/11. Dois achados relevantes ainda nao registrados foram recuperados: **desconto temporario no Adobe Firefly** e **lancamento dos modelos ElevenLabs Scribe v3/Live**. Ambos ocorreram em 10/11; nao foram anunciados hoje e nao se confirmou um novo modelo fundacional de geracao de video em 14/11.

## 1. Adobe Firefly - promocao forte para producao e testes audiovisuais

**Status: ATIVO no Firefly web.** A promocao iniciada em **10/11/2026**, valida ate **06/12/2026**, oferece desconto nos primeiros **seis meses** para novas assinaturas individuais elegiveis.

| Plano Adobe Brasil | Oferta mensal da vitrine | Preco regular mensal da vitrine | Limites basicos |
|---|---:|---:|---|
| Firefly Pro | R$ 52,25 (-50%) | R$ 104,50 | 4.000 creditos/mes; ate 40 videos de 5 s, dependendo de modelo/configuracao |
| Firefly Pro Plus | R$ 107,60 (-60%) | R$ 269,00 | 10.000 creditos/mes; ate 100 videos de 5 s em configuracao de referencia; Graph beta |

**Cuidado contratual:** os termos tambem mostram plano anual Pro cobrado mensalmente de R$44,50 durante a promocao e R$89,00 depois, mas com 12 meses de compromisso. Os precos do quadro sao os exibidos na modalidade mensal da pagina de planos; nao transferir as porcentagens para economia no ano inteiro sem calcular o contrato e o preco de renovacao. Verificar os valores exatos, impostos e condicoes no checkout.

O Pro Plus anuncia geracoes de video ilimitadas no primeiro ano **apenas em modelos e resolucoes selecionados**. A matriz oficial inclui Firefly Video, Kling 2.5 Turbo, Kling 3.0 Standard 720p, Luma Ray 3.14, Runway Gen 4.5 e Veo 3.1 Fast, mas a elegibilidade nao e igual para todos os planos. **Nao significa uso ilimitado de Kling 4.0, Omni 1.2 ou API de desenvolvedores**. Adobe possui servicos proprietarios e integracoes com terceiros, com licencas de uso diferentes.

E uma reducao significativa do custo de experimentar workflows de publicidade, clipes, redes sociais e video explicativo. **Nao e evidência de aumento de realismo ou melhoria do motor de video**.

Fontes: https://www.adobe.com/br/products/firefly/plans.html ; https://www.adobe.com/br/offer-terms/ff-special-offer.html ; https://helpx.adobe.com/br/firefly/web/get-started/learn-the-basics/current-firefly-promotions.html .

## 2. ElevenLabs Scribe v3 e Scribe v3 Live - melhor compreensao de fala para video

**Status: ATIVOS na ElevenAPI; pesos proprietarios.** Lancados em **10/11/2026** (artigo atualizado em 13/11). Sao modelos de **speech-to-text** para transcricao de audio e trilhas de video, nao geradores de imagens ou filmes.

A ElevenLabs reporta em testes internos: **27% menos erros de palavra** no v3 batch versus v2 sobre 102 idiomas FLEURS; **17% menos erros de atribuicao de locutor** em gravacoes com tres ou mais falantes; **43% menos erro semantico de palavra** no v3 Live versus v2 Realtime. Para WER tradicional, a melhora do Live divulgada e de 31%, portanto nao misturar metricas. Os dados nao sao comparacoes independentes em portugues brasileiro.

| Modelo | Status | API ID | Preco base |
|---|---|---|---|
| Scribe v3 | ATIVO | `scribe_v3` | US$ 0,20 por hora de audio |
| Scribe v3 Live | ATIVO | `scribe_v3_live` | US$ 0,22 por hora de audio |

Os precos sao iguais aos antecessores correspondentes na tabela atual. Keyterm prompting e edicao podem cobrar adicional. Acesso, residencia de dados e configuracao de modelo padrao variam por ambiente; configurar ID explicitamente. Aplicacoes: legenda, indexacao de documentarios, preparacao de dublagem e corte de entrevistas por transcricao.

Fontes: https://elevenlabs.io/blog/scribe-v3 ; https://elevenlabs.io/pricing/api ; https://elevenlabs.io/docs/overview/capabilities/speech-to-text .

## 3. Outra mudanca de 13/11: ElevenLabs Forward Deployed Creatives

**Status: LIMITADO/Enterprise, por contato comercial; preco e API dedicados nao publicados.** A ElevenLabs anunciou uma equipe que usa ElevenCreative, agentes, sistemas externos e revisao humana para produzir anuncios, variantes e localizacoes para marcas, inclusive entregando material final. E um servico assistido, nao um novo gerador de video. Nao ganhou card porque faltam dados de preco/eficiencia e nao foi demonstrada nova capacidade tecnica de modelo.

Fonte oficial: https://elevenlabs.io/blog/introducing-forward-deployed-creatives .

## 4. Comparacao e checagem de ciclo de vida em 14/11

| Sistema / mercado | Status atual | Papel principal |
|---|---|---|
| Adobe Firefly - EUA | **ATIVO web; desconto temporario ate 06/12; Graph beta** | Montagem, geracao e edicao audiovisual com modelos diversos, acesso SaaS |
| ElevenLabs Scribe v3/Live - EUA | **ATIVOS por API e aplicativo** | Transcricao, tempo por palavra e falantes, sem gerar video |
| Google Gemini Omni 1.2 Flash - EUA | **ATIVO GA/API** desde 05/11 | Geracao/edicao de video com audio, ate 15 s por geracao, ~US$0,07/s 720p |
| Kling 4.0 (Kuaishou) - China | **ATIVO via Runway Dev API** | Geracao e edicao multimodal com audio, ate 30 s, preco por duracao/resolucao/entrada |
| Wan 3.0 (Alibaba) - China | **ATIVO via Alibaba Cloud API** desde 23/08 | Geracao de video com audio ate 30 s/1080p, modelo de servico nao assumido como open weights |
| OpenAI Sora 2 - EUA | **DESCONTINUADO**; app 26/04 e API 24/09/2026 | Nao disponivel para producao; nao recomendar como alternativa atual |

Fontes: https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available/ ; https://dev.runway.com/learn/kling-4-0-api ; https://www.aliyun.com/product/news/30378 ; https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation .

## 5. Marketing Hub: hipoteses a testar

Comparar 20 pecas curtas no Firefly Pro Plus e em uma API direta (Kling/Omni), contabilizando geracoes descartadas, edicoes, revisao humana, tempo e licencas. Em paralelo, medir erros de palavras, rotulos de falantes e tempo de revisao em 10 videos PT-BR transcritos com Scribe v2/v3. O indicador principal e custo por video final aprovado, nao preco promocional ou benchmark isolado.

## 6. Cards e integridade

- `pesquisas/video/cards/2026-11-14-adobe-firefly-desconto-video.json`
- Fonte: `pesquisas/video/cards/fontes/2026-11-14-adobe-firefly-desconto-video.md`
- SHA-256 da fonte UTF-8: `ece37ad4d6c1702db6b8831268d65e187003aa9097250d01ccb4d9bd4ca62dc6`
- `pesquisas/video/cards/2026-11-14-elevenlabs-scribe-v3-video.json`
- Fonte: `pesquisas/video/cards/fontes/2026-11-14-elevenlabs-scribe-v3-video.md`
- SHA-256 da fonte UTF-8: `870ef1bdc6e4b712b7a877b04b2139d9e968df3fd25f76e83ce8a45b286ab0da`

**Limites:** nao foram feitas chamadas a API, testes visuais ou contratacoes. A documentacao oficial suporta os status listados, mas modelos/valores podem mudar com atualizacoes futuras.
