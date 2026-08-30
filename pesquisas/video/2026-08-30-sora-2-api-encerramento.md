# Radar de IA para Vídeo — 2026-08-30

## Mudança crítica de status: OpenAI encerrará Sora 2 e a Videos API em 24/09/2026

Esta rodada encontrou uma correção importante de status que o radar ainda não havia registrado. **Não é um anúncio feito hoje**: a OpenAI notificou desenvolvedores em **24 de março de 2026**. Porém, a página oficial de suporte sobre a descontinuação foi novamente atualizada em **30 de agosto de 2026**, e a proximidade do desligamento torna a mudança relevante para qualquer arquitetura audiovisual.

### Status atual

| Sistema | Status em 2026-08-30 | Observação |
|---|---|---|
| Sora web/app | 🔴 Descontinuado | Encerrado em 26/04/2026 |
| Sora 2 API | 🟠 Em encerramento | Desliga em 24/09/2026 |
| Sora 2 Pro API | 🟠 Em encerramento | Desliga em 24/09/2026 |
| OpenAI Videos API | 🟠 Em encerramento | Endpoint removido em 24/09/2026 |
| Veo 3.1 Fast / Google | 🟢 Ativo | Alternativa hospedada com áudio nativo |
| Veo 3.1 Lite / Google | 🟢 Ativo | Opção de menor custo entre os Veo atuais |
| Wan 3.0 / Alibaba Cloud | 🟢 Ativo | Até 30 s, multimodal, áudio nativo |
| Seedance 2.5 / Runway | 🟢 Ativo | Até 30 s, muitas referências multimodais |

A documentação oficial de depreciações lista explicitamente, todos para **24/09/2026**, o endpoint **Videos API**, `sora-2`, `sora-2-pro` e snapshots datados. O campo de substituto recomendado está vazio; ou seja, a OpenAI **não indica atualmente um sucessor direto de vídeo na própria API**.

### O que ainda funciona até o desligamento

O `sora-2` continua classificado no catálogo como **Legacy**, mas ainda pode ser chamado pela API até a data de encerramento. Ele gera vídeo com áudio sincronizado em 720p por **US$ 0,10/s**.

O `sora-2-pro` também está **Legacy** e custa atualmente:

- 720p: **US$ 0,30/s**
- 1024×1792 / 1792×1024: **US$ 0,50/s**
- 1080p: **US$ 0,70/s**

A API atual ainda documenta geração de até **20 s por job**, extensões de até 20 s e até seis extensões, chegando a 120 s acumulados, além de edição por prompt. Tudo isso deve ser tratado como capacidade de transição, não como base segura para um novo produto.

### Por que isso importa

A consequência estratégica é maior do que uma simples descontinuação de modelo. A OpenAI está, neste momento, **saindo da oferta pública de geração de vídeo por API sem anunciar substituto**. Portanto, qualquer produto novo que dependa de Sora 2 deve migrar antes de 24/09/2026.

Em custo, o desligamento torna a migração relativamente simples:

- **Veo 3.1 Lite**: US$ 0,05/s em 720p com áudio e US$ 0,08/s em 1080p.
- **Veo 3.1 Fast**: US$ 0,10/s em 720p com áudio e US$ 0,12/s em 1080p.
- **Wan 3.0**: preço oficial promocional atual varia por região; no endpoint global de Hong Kong, o Standard está em aproximadamente US$ 0,0825/s em 720p e US$ 0,165/s em 1080p durante a promoção de 30%.
- **Seedance 2.5 via Runway**: permanece ativo em 480p, 720p e 1080p, com até 30 s e até 50 referências por geração.

Isso significa que **Sora 2 Pro em 1080p, a US$ 0,70/s, já não é competitivo em custo** contra Veo 3.1 Fast/Lite, além do risco operacional do desligamento próximo.

### Recomendação para arquitetura

Para novos pipelines, Sora 2 deve ser removido da lista de opções primárias e marcado como `sunset`. Um roteador de modelos deveria evitar novas dependências e usar alternativas ativas, por exemplo:

```text
job de vídeo
   ↓
model router
   ├─ Veo 3.1 Lite/Fast → custo e rapidez
   ├─ Wan 3.0 → cenas mais longas / multimodal
   └─ Seedance 2.5 → controle e muitas referências
```

Se houver alguma integração existente com `/v1/videos`, a prioridade é migrar e validar qualidade, custos e regras de conteúdo antes de **24 de setembro de 2026**.

## Fontes

- OpenAI — What to know about the Sora discontinuation: https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation
- OpenAI API — Deprecations: https://developers.openai.com/api/docs/deprecations
- OpenAI API — Sora 2 model: https://developers.openai.com/api/docs/models/sora-2
- OpenAI API — Sora 2 Pro model: https://developers.openai.com/api/docs/models/sora-2-pro
- OpenAI API — Video generation guide: https://developers.openai.com/api/docs/guides/video-generation
- Google Cloud — Gemini Enterprise Agent Platform pricing: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- Alibaba Cloud Model Studio — Model pricing: https://www.alibabacloud.com/help/en/model-studio/model-pricing
- Runway — Creating with Seedance 2.5: https://help.runwayml.com/hc/en-us/articles/53542207042323-Creating-with-Seedance-2-5
