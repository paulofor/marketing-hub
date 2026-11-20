# Radar de IA para Vídeo — 20 de novembro de 2026

**Janela de pesquisa:** 19–20/11/2026. **Uma mudança importante confirmada:** Tencent publicou pesos e exemplos utilizáveis para a nova família WeMM-Video (modelos de compreensão simultânea de imagem em movimento e áudio). Artigo divulgado em 19/11; disponibilização dos pesos confirmada em 20/11. Outras notícias de baixo impacto, retrospectivas e anúncios já registrados anteriormente não geraram cards.

## 1. Tencent WeMM-Video — compreensão audiovisual conjunta em infraestrutura própria

**Status em 20/11: ATIVO para download e execução própria; pesos e código abertos com referência Apache 2.0; API comercial oficial hospedada não encontrada; não é gerador de vídeo.**

WeChat Vision, equipe da Tencent na China, abriu quatro variantes baseadas em backbones 2B, 4B, 9B (modelos densos) e 35B-A3B (MoE com cerca de 3B ativos). Os nomes indicam escala do backbone linguístico, não contagem total incluindo os encoders. Os arquivos completos BF16, formatos FP8, AWQ, GGUF e NVFP4 estão listados no repositório e publicados no Hugging Face.

A novidade não é produzir clipes a partir de texto: o sistema recebe vídeo com áudio original, áudios, imagens ou texto e responde em chinês ou inglês. Consegue descrever acontecimentos e relacioná-los ao diálogo ou à trilha; processa tokens visuais e sonoros em sequência temporal; oferece modos Instruct e Thinking. Isso é relevante para revisão de anúncios, documentários, séries, dublagem e vídeos explicativos, porque erros de fala, cortes e movimentos podem depender das duas modalidades simultaneamente.

**Código e implantação:** guia oficial inclui Transformers e servidores locais vLLM/SGLang com patches, via Docker, além de GGUF para CPU/CUDA. Exige dependências específicas (Python 3.12, FFmpeg, CUDA 12.9 nos exemplos). O principal modelo possui 67,39 GiB de arquivos BF16, fora cache/overhead de inferência; há quantizações e versões menores. Não há preço por minuto ou endpoint pago oficial confirmados, sendo o custo dependente de GPUs próprias/alugadas. O modelo não gera vídeo, nem áudio sintetizado.

**Desempenho e ressalvas:** no artigo submetido em 19/11 (arXiv 2611.26409), a Tencent relata avaliação em mais de 120 benchmarks e superioridade em compreensão audiovisual sobre determinadas versões Qwen/Seed, aproximando-se do Gemini 3.0 Flash; a equipe também relata que seu 9B supera um Qwen3-Omni maior em cenários selecionados. Não testado de forma independente nesta rodada. Como a documentação anuncia apenas chinês e inglês, o comportamento em português brasileiro é desconhecido e necessita avaliação própria.

**Limites e licença:** repositório e modelo principal identificam Apache 2.0, mas avisos de terceiros e mídias usadas nos exemplos têm regras próprias. O exemplo de contêiner usa 8192 tokens, extração de até 16 quadros por vídeo e no máximo 2 vídeos por requisição; são limites de configuração demonstrada, não limites máximos comprovados da arquitetura. Executar código remoto exige auditoria e versionamento. Não houve execução nem compra de infraestrutura nesta rodada.

Fontes oficiais: https://github.com/Tencent/WeMM-Video ; https://huggingface.co/tencent/WeMM-Video-35BA3B ; https://arxiv.org/abs/2611.26409

## 2. Comparação de sistemas de compreensão de vídeo (20/11)

| Sistema | País | Status verificado | Diferença |
| --- | --- | --- | --- |
| WeMM-Video 2B / 4B / 9B / 35B-A3B | China | ATIVO, pesos e código abertos; self-host; sem API comercial oficial confirmada | Analisa conjuntamente os quadros e a trilha, retornando texto |
| Qwen4-Max | China | ATIVO via API paga QwenCloud / Alibaba Cloud, pesos proprietários | Analisa vídeo e texto, janela 1M e suporte a vídeos de até 2 h no canal documentado; documentação de Qwen-VL diferencia compreensão visual de áudio |
| Gemini 3.8 Flash | EUA | ATIVO, API estável, pesos proprietários | Aceita texto, imagens, áudio e vídeo; recurso comercial hospedado, não self-host |
| Sora 2 (referência histórica) | EUA | DESCONTINUADO (app em 26/04/2026, API em 24/09/2026) | Era gerador, não alternativa atual para análise |

Qwen4-Max e Gemini 3.8 Flash são comparadores de **função** e acesso, não vencedores de avaliação direta aqui. Ambos têm endpoints ativos por documentação; não há comparação independente padronizada entre estes três na presente rodada. Na comparação de benchmarks da Tencent, versões específicas e antigas dos concorrentes não devem ser confundidas com as versões comerciais atuais.

Fontes de disponibilidade: https://docs.qwencloud.com/developer-guides/getting-started/text-generation-models ; https://www.alibabacloud.com/help/pt-br/model-studio/qwen-api-via-openai-chat-completions ; https://aistudio.google.com/docs/models/gemini-3.8-flash

## 3. Aplicações e próximos testes para Marketing Hub

Priorizar um teste mínimo com 30 clipes já revisados por humano em pt-BR, com cenas de produto, voz, música e cortes. Enviar o arquivo com trilha original para WeMM-Video-2B e 4B (ou 9B se infraestrutura disponível), solicitar JSON padronizado com presença do produto, conformidade visual, coerência de fala, eventos e timestamps. Avaliar precisão, falsos alarmes, custo e latência versus modelo comercial que tenha API ativa. Começar por prototipagem em hardware já disponível, sem contratar GPUs antes da autorização; não executar aprovação sem humano.

O resultado desejado é reduzir custo de triagem mantendo consistência entre roteiro, imagem e áudio. A família é potencialmente útil para o módulo de controle de qualidade do Marketing Hub, não para substituir Kling, Seedance ou sistemas de geração.

## Registro de integridade

- Relatório: pesquisas/video/2026-11-20-tencent-wemm-video-audio-aberto.md
- Card: pesquisas/video/cards/2026-11-20-tencent-wemm-video-audio-aberto.json
- Fonte revisada: pesquisas/video/cards/fontes/2026-11-20-tencent-wemm-video-omnimodal-aberto.md
- SHA-256 exato da fonte revisada: 1ed4df4a525dcbf6f8b4bf1a2309c4639f74b0ea8f241798d3cbe49fc5ee0436

**Limitações de verificação:** leitura de fontes oficiais datadas, sem rodar inferência, checar qualidade em pt-BR, validar segurança de código ou preço de GPUs nesta rodada. Avaliações do fornecedor não equivalem a benchmark independente.
