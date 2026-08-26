# Wan 3.0: API entra em GA e Alibaba reduz preço em 30%

**Data da verificação:** 26/08/2026  
**Mudança detectada:** disponibilidade geral + redução relevante de preço

## Resumo

Na verificação de 26/08/2026, a página oficial do Alibaba Cloud Model Studio passou a apresentar o **Wan 3.0 Video API como “Now generally available”**, sem necessidade de candidatura ou aprovação: a empresa afirma que o modelo está disponível para todos os desenvolvedores e que as chamadas podem começar imediatamente.

Ao mesmo tempo, o **Wan 3.0 Video Standard recebeu desconto oficial de 30% até 24/09/2026 às 00:00 (UTC+8)**.

Isso muda a comparação feita na rodada anterior: o acesso direto pela Alibaba agora não só está em GA como, na faixa inicial, ficou mais barato que a promoção do Wan 3.0 pela Atlas Cloud.

## Status atual

### Wan 3.0 / Alibaba Cloud Model Studio

**Status: ATIVO / GA**

A página oficial atual informa:

- API em disponibilidade geral;
- nenhuma candidatura necessária;
- acesso para todos os desenvolvedores;
- modelo `wan3.0-video`;
- até 30 segundos em uma única geração;
- 480p, 720p e 1080p;
- áudio e vídeo nativos;
- texto, imagem, vídeo, áudio, documentos e páginas web como entrada;
- até 10 imagens, 5 vídeos e 5 áudios como referências.

Há documentação técnica mais antiga, atualizada no começo de agosto, que ainda descrevia o Wan 3.0 como preview. Para status operacional, a página oficial de lançamento atual é a evidência mais recente e explícita e, portanto, o modelo deve ser tratado agora como **GA**.

## Novo preço promocional oficial

O Wan 3.0 Video Standard está com **30% de desconto até 24/09/2026**:

| Resolução | Preço de tabela | Preço promocional | 30 segundos |
|---|---:|---:|---:|
| 480p | US$ 0,05/s | **US$ 0,035/s** | **~US$ 1,05** |
| 720p | US$ 0,10/s | **US$ 0,07/s** | **~US$ 2,10** |
| 1080p | US$ 0,20/s | **US$ 0,14/s** | **~US$ 4,20** |

A própria Alibaba ressalva que o preço final deve ser confirmado no console.

## Comparação com Atlas Cloud

A Atlas Cloud continua oferecendo Wan 3.0 Standard a partir de **US$ 0,04/s**, uma promoção de 20% sobre a faixa inicial de US$ 0,05/s.

Com o desconto oficial da Alibaba, a faixa inicial direta passa a custar **US$ 0,035/s**, menor que os US$ 0,04/s anunciados pela Atlas.

A Atlas ainda pode ser útil pela API unificada e pelo roteamento entre vários modelos, mas deixou de ser automaticamente a rota mais barata para Wan 3.0.

## Comparação com Veo 3.1 Fast

O Google mantém o **Veo 3.1 Fast em GA**. Na tabela atual, vídeo + áudio custa aproximadamente:

- 720p: US$ 0,10 por segundo de saída;
- 1080p: US$ 0,12 por segundo de saída.

O Veo 3.1 Fast continua ligeiramente mais barato que o Wan 3.0 promocional em 1080p (**US$ 0,12/s vs US$ 0,14/s**), porém trabalha com vídeos de **4, 6 ou 8 segundos**.

O Wan 3.0 ganha em duração contínua e amplitude de entrada: até **30 segundos** e referências multimodais que incluem documentos e páginas web.

## Por que isso importa

A mudança reduz duas barreiras ao mesmo tempo:

1. **acesso** — deixa de depender de preview/aprovação e entra em GA;
2. **custo** — cai 30% diretamente no provedor oficial.

Para produção automatizada, publicidade, UGC, vídeos explicativos e agentes que transformam briefings ou documentos em vídeo, o Wan 3.0 passa a ser ainda mais competitivo.

Também reforça uma regra importante para um roteador de modelos: não basta escolher o melhor modelo; vale comparar **o mesmo modelo entre provedores** antes de cada job, porque preço e disponibilidade mudam rapidamente.

## Pesos e licença

Esta atualização é de **disponibilidade e preço**, não de abertura de pesos. O Wan 3.0 continua sendo oferecido como modelo hospedado/proprietário; não houve nesta rodada anúncio de pesos abertos ou mudança relevante de licença.

## Fontes

- Alibaba Cloud Model Studio — Wan 3.0: https://modelstudio.console.alibabacloud.com/model-releases/wan3.0-video
- Alibaba Cloud — apresentação do Wan 3.0: https://modelstudio.alibabacloud.com/intl/blog/wan3-ai-video-generation-model/
- Atlas Cloud — Wan 3.0: https://www.atlascloud.ai/models/wan-3.0
- Google Cloud — preços de modelos generativos: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- Google Cloud — documentação do Veo 3.1: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
