# Radar de IA para Vídeo — 01/11/2026

**Decisão editorial: sem alerta.** A verificação de lançamentos, preços, APIs, modelos de pesos abertos e encerramentos desde o relatório de 31/10 não encontrou mudança nova, relevante e suficientemente confirmada para justificar notificação. Fontes consultadas em **01/11/2026**, horário de Brasília. Não foram feitas chamadas autenticadas de inferência nem testes de cobrança.

## 1. Notícias recentes analisadas e não promovidas a alerta

- **Pika H3 Flex**: o registro de 01/11 no agregador AI Headline Hub é uma notícia *sobre o modelo já lançado em 29/10 e coberto no radar de 30/10*, não uma segunda abertura de pesos. Código e documentação continuam publicados. Nenhuma revisão comprovada de preço, licença ou nova variante após aquela rodada. [Repositório Pika H3 Flex](https://github.com/Pika-Labs/Pika-H3-Flex).
- **Runway Dev**: o registro mais recente do [changelog público](https://docs.dev.runwayml.com/api-details/api_changelog/) é **28/10**, inclusão do Ideogram Vector (produto de SVG, não novo gerador de vídeo). Em 27/10, Seedance 2.0/2.0 Fast/2.0 Mini ganharam a opção `bitrate=high` sem acréscimo de créditos. É ajuste útil de codificação e pós-produção, mas insuficiente como avanço central de vídeo para novo alerta.
- **ElevenLabs**: o [changelog público](https://elevenlabs.io/docs/changelog) consultado continua com entrada mais recente em **26/10**. O SFX v3 de **29/10** já foi incluído no radar de 31/10; não foi comprovado novo anúncio técnico relevante hoje.
- **fal**: a publicação de [31/10 sobre fine-tuning de FLUX](https://fal.ai/learn/devs/how-to-fine-tune-flux-with-the-api) é guia/tutorial de modelos de imagem para consistência de personagens e produtos, **não novo modelo de vídeo**, nova abertura de pesos de vídeo ou nova redução de preço de vídeo. A publicação de 01/11 é [guia de migração de GPU serverless](https://fal.ai/learn/serverless/how-to-switch-serverless-gpu-providers), sem alteração comprovada de modelo audiovisual.

## 2. Status operacional conferido em 01/11/2026

| Sistema ou canal | Status atual | Evidência e observações |
|---|---|---|
| **Kling 4.0 via Runway Dev** (China/EUA) | **ATIVO via API** | Runway documenta endpoint `kling_4`, saída até 30 s e preço promocional até **30/11**. A documentação direta do fabricante não foi revalidada com chamada autenticada. |
| **Seedance 2.0 (Fast/Mini) via Runway Dev** (China/EUA) | **ATIVO via API** | Suporte `bitrate=high` desde 27/10, mesmo preço documentado para o bitrate padrão. |
| **Wan 3.0 via Runway Dev** (China/EUA) | **ATIVO via API** | Modelo e preço publicados na documentação Runway; nenhum lançamento novo confirmado após a rodada anterior. |
| **Pika H3 Flex** (EUA/base MiniMax chinesa) | **CÓDIGO/PESOS PUBLICADOS; API anunciada ativa** | Disponibilidade pública de repositório verificada; pesos sujeitos à licença MiniMax H3 Community e demais componentes. Não se fez teste operacional da API. |
| **TBC mischief 1 via fal** (EUA/base Wan chinesa) | **ATIVO, endpoint comercial documentado** | A página da fal indica interface de inferência, API e uso comercial; geração paga não foi executada. |
| **ElevenLabs SFX v3** (EUA) | **LIMITADO por habilitação de conta** | Novidade de 29/10 registrada em 31/10; não há evidência de expansão geral de acesso hoje. |
| **Google Gemini Omni 1.1 Flash** (EUA) | **ATIVO/GA na Gemini API** | Google não anuncia data de desligamento. |
| **Veo 3.1 Preview na Gemini API** (EUA) | **ENCERRADO** | Endpoints Preview têm data de desligamento **22/10** na tabela do Google. Modelos GA do Google Cloud constituem oferta diferente e não devem ser confundidos com os endpoints descontinuados. |
| **Sora app e Sora API** (EUA) | **DESCONTINUADOS** | Web/app em 26/04 e API em 24/09, segundo documentação oficial OpenAI. |

Fontes de verificação:
- Runway API changelog: https://docs.dev.runwayml.com/api-details/api_changelog/
- Runway preços e custos: https://docs.dev.runwayml.com/guides/pricing/
- Google Gemini API deprecations: https://ai.google.dev/gemini-api/docs/deprecations
- OpenAI Sora discontinuation: https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation
- Pika H3 Flex: https://github.com/Pika-Labs/Pika-H3-Flex
- fal mischief 1: https://fal.ai/models/tbc/mischief-1/image-to-video
- ElevenLabs changelog: https://elevenlabs.io/docs/changelog

## 3. Encerramento já anunciado para acompanhar

A ElevenLabs registrou em **23/09** que o **Seedance 1.5 Pro** deixou de ser oferecido para novas gerações na sua plataforma e tem **encerramento anunciado para 11/11/2026**, com orientação de migração para **Seedance 2.0**. **Status: em descontinuação**, não recomendável para novos pipelines. Trata-se de aviso antigo, não de mudança ocorrida hoje; a data não deve ser interpretada como desligamento já realizado. Fonte: https://elevenlabs.io/docs/changelog (23/09/2026, Image & Video).

## 4. Consequência para o Marketing Hub

Sem necessidade de trocar modelos, preços de referência ou arquitetura nesta rodada. Recomenda-se manter o roteamento por provedor/modelo e a validação de disponibilidade/licença por endpoint antes de cada integração. A possibilidade de `bitrate=high` no Seedance 2.0 pode ser acrescentada ao backlog de controle de qualidade, mas não exige card de pesquisa novo.

**Cards JSON criados nesta rodada: nenhum**, para evitar duplicação e respeitar a regra de gerar cards somente para achados realmente úteis. **Nenhum acesso pago, compra ou inferência foi realizado.**
