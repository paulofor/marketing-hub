# Radar de IA para Vídeo — 21 de novembro de 2026

**Pesquisa:** anúncios e artefatos de 19–21/11/2026. **Três mudanças relevantes ainda não registradas no histórico**: publicação de avatares abertos em streaming pela Kyutai (20/11), abertura dos pesos Hy4 pela Tencent (20/11) e release de agentes WeVisForensics para detectar manipulações de vídeo (pesquisa 16/11, artefatos em 19/11). Não foi identificado outro novo gerador generalista comercial de impacto equivalente lançado especificamente em 21/11.

## 1. Kyutai AURA (França) — streaming de apresentadores virtuais

**Status: ATIVO para pesquisa e self-host; pesos AURA-1.3B CC BY-NC 4.0 e AURA-5B Snap Non-Commercial; não liberado para uso comercial; nenhuma API comercial oficial confirmada.** Lançamento de código, modelos, demo e dataset de 30 mil vídeos sintéticos em 20/11; preprint de 19/11.

AURA usa um retrato e fala recebida continuamente para gerar rosto falante em pequenos blocos de vídeo, sem esperar o término do áudio. Destilação de 50 para quatro passos de difusão e cache de atenção móvel. Números dos autores em 1×H100: AURA-1.3B produz vídeo 576p/16fps com velocidade de inferência de 20,7fps; AURA-5B entrega 720p/25fps, mas gera a 9,7fps, não em tempo real nessas condições. Código de referência compilado gera aproximadamente 16fps e 8fps, respectivamente. A demo com blocos menores informa ~1,2 s de atraso inicial contra ~2 s no ajuste padrão. Não testado independentemente aqui.

**Comparação comercial:** HeyGen Avatar 6 (EUA) está ATIVO em web/API e produz vídeos de apresentador prontos em 2K; não é necessariamente streaming interativo. LongCat-Video-Avatar 1.6 (Meituan, China) está ATIVO com pesos MIT para experimentos de vídeos longos; velocidade interativa equivalente não foi demonstrada. Meta Muse Realtime Avatar (EUA) continua LIMITADO ao app Muse em mercados elegíveis, sem API pública confirmada. Não confundir demos de AURA com serviço comercial ou produto de anúncio liberado.

Fontes: https://kyutai.org/aura/ ; https://github.com/kyutai-labs/aura ; https://arxiv.org/abs/2611.26540

## 2. Tencent Hy4 (China) — abertura de pesos de compreensão multimodal de vídeo

**Status: ATIVO para download/self-host; licença Tencent Hy License 1.0, NÃO Apache 2.0; acesso proprietário por API já existia; não gera vídeo.** Em 20/11, Tencent publicou Hy4 Instruct, Hy4-FP8 e Hy4 Base. Backbone com 770B de parâmetros totais e 49B ativos/token, visão de ~1,23B, contexto declarado 1M tokens, entrada de texto, imagens e quadros de vídeo. Pesos FP8 próximos de 0,79 TB e BF16 de 1,55 TB tornam implantação própria substancialmente cara e dependente de infraestrutura multi-GPU.

A licença exige autorização adicional em determinadas operações MaaS acima de US$50 milhões de receita bruta mundial em 12 meses ou produtos acima de 100 milhões de usuários mensais; texto integral e exceções devem ser revisados. A Tencent havia divulgado versões preview sob termos diferentes, portanto não herdar licenças antigas.

**Comparação:** WeMM-Video da Tencent (lançado no radar de 20/11) é ATIVO, menor e sob Apache 2.0, adequado a testes locais e capaz de compreender som + vídeo; Qwen4-Max da Alibaba é ATIVO por API proprietária. Hy4 é avanço importante de abertura e de disponibilidade para pesquisa, não o melhor primeiro teste econômico para Marketing Hub. Qualidade e aderência em pt-BR não foram avaliadas nesta rodada.

Fontes: https://huggingface.co/tencent/Hy4 ; https://github.com/Tencent-Hunyuan/Hy4 ; https://github.com/Tencent-Hunyuan/Hy4/blob/main/LICENSE

## 3. Tencent WeVisForensics (China) — investigação de vídeos sintéticos por agentes

**Status: ATIVO para execução própria; código e adapters LoRA Apache 2.0, sujeito também aos termos da base e demais componentes; sem API comercial oficial ou preço público confirmado.** Paper divulgado em 16/11; repositório, demo, guias e LoRA publicados em novembro, com checkpoints confirmados em 19/11. Diferentemente de um classificador único, um investigador escolhe inspeções de trechos específicos por artefatos visuais, física, plausibilidade, OCR e áudio.

Tencent relata média de 95,2% em avaliações de detecção, 9 pontos percentuais acima do comparador mais forte; resultado interno, sem replicação independente. O guia descreve teste com GPU de 24 GB e Qwen3.5-4B como base; os arquivos liberados são adapters LoRA, NÃO checkpoint completo. Detectar sinais de conteúdo sintético é uma tarefa distinta de medir qualidade ou aprovar direitos de uso e não implica ausência de falsos positivos.

Para Marketing Hub, recomenda-se ensaio com vídeos reais e sintéticos de referências autorizadas, métricas de falso positivo, recall e acerto na localização temporal dos defeitos, revisão humana e teste em português.

Fontes: https://github.com/Tencent/WeVisForensics ; https://arxiv.org/abs/2611.21373

## Situação comparativa em 21/11/2026

| Sistema | Origem | Status efetivo | Função / acesso |
|---|---|---|---|
| Kyutai AURA 1.3B/5B | França | ATIVO pesquisa, pesos não comerciais | Avatar por voz em streaming, self-host |
| HeyGen Avatar 6 | EUA | ATIVO comercial web/API | Apresentadores e gestos contextuais |
| LongCat-Video-Avatar 1.6 | China | ATIVO pesos abertos MIT | Avatar por áudio, self-host |
| Meta Muse Realtime Avatar | EUA | LIMITADO app Muse, sem API pública confirmada | Avatar conversacional ao vivo |
| Tencent Hy4 | China | ATIVO pesos abertos sob licença própria | Compreensão multimodal de vídeo |
| Tencent WeMM-Video | China | ATIVO pesos Apache 2.0 | Compreensão conjunta de áudio e vídeo |
| Tencent WeVisForensics | China | ATIVO código/LoRA Apache 2.0 | Inspeção e detecção forense |
| Kling 4.0 e Seedance 2.5 | China | ATIVOS via APIs | Geração e edição de clipes |
| Gemini Omni 1.2 | EUA | ATIVO API | Geração audiovisual e edição |
| OpenAI Sora 2 | EUA | DESCONTINUADO — app abril/2026 e API setembro/2026 | Referência histórica, NÃO disponível |

**Prioridade:** avaliar AURA apenas como tecnologia de pesquisa por restrição comercial; realizar piloto de inspeção de vídeos com WeVisForensics em material controlado; não gastar com GPU para Hy4 sem benchmark que mostre vantagem sobre WeMM-Video ou APIs hospedadas. Revisões humanas e checagem de direitos de imagem são necessárias.

## Registro de integridade e cards

- Fonte AURA: `pesquisas/video/cards/fontes/2026-11-21-kyutai-aura-avatar-streaming.md`; SHA-256 `4c02ff37e54e3066c5e58b6d577710e9a8ea0141b36328d448cbb75bfcc6e095`
- Fonte Hy4: `pesquisas/video/cards/fontes/2026-11-21-tencent-hy4-openweights-video.md`; SHA-256 `01f5762a4cf221fd3831be632600f75f6f3f9f7b5be32b35c8551e9fab48fcae`
- Fonte WeVisForensics: `pesquisas/video/cards/fontes/2026-11-21-tencent-wevisforensics-video.md`; SHA-256 `241f8bd4dc4be0fdcdfd9dc738f200d4f6cb5695989269a24c486787bbb65116`
- Cards JSON correspondentes em `pesquisas/video/cards/` com `sourceUri` e `sourceSha256` vinculados às fontes.

**Limitações:** pesquisa documental, conferência de artefatos e termos publicados, sem rodar inferência nem medições independentes. Preços, limites e disponibilidades podem variar por canal e região.
