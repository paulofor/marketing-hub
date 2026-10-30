# Fonte revisada: TBC mischief 1 (30/10/2026)

## Marco e status
- A The Biological Computing Co. (TBC), EUA, anunciou em **30/10/2026** o lançamento público de mischief 1, derivado do gerador de vídeo Wan 2.2 (Alibaba, China).
- **Status: ATIVO por API na fal e listado na OpenRouter**, com cobrança por uso. Acesso autenticado e render real não foram executados nesta verificação.
- Modelo de imagem para vídeo, com entrada de imagem e prompt textual, 480p (832x480) e 720p (1280x720), 16 fps, clipes de 3 ou 5 segundos. A documentação do endpoint não confirma geração nativa de áudio, dublagem, lipsync, 1080p ou extensão longa.
- O modelo e camada de eficiência da TBC são **proprietários**, sem publicação de pesos/tecnologia de otimização confirmada; não confundir com os pesos abertos do Wan 2.2 base.

## Preço e evidência
- Tarifa oficial fal: **US$0,005/segundo 480p** e **US$0,01/segundo 720p**. Cinco segundos custam US$0,025 ou US$0,05, respectivamente.
- TBC anuncia geração 5x mais rápida que a base Wan 2.2, com qualidade comparável, obtida de descobertas experimentais sobre neurônios vivos transferidas para software. Não há estudo independente nem resultados de qualidade e latência ponta-a-ponta publicados que comprovem equivalência em todas as categorias.
- A página do OpenRouter exibe uma métrica dinâmica de disponibilidade, que varia e não equivale a SLA contratual. Acessibilidade da API não garante geração concluída a cada tentativa.
- A TBC informa disponibilidade para uso comercial em marketplaces, sujeita aos respectivos termos; revisar propriedade intelectual de inputs e outputs, dados e contratos antes de produção.

## Aplicação e limitações
- Muito competitivo para **rascunhos animados baratos a partir de uma imagem**, sobretudo anúncios curtos, moodboards e pré-visualização. Comparação com Pika H3 Flex deve usar o mesmo conjunto de imagens e medir qualidade; Pika acrescenta áudio sincronizado e 15 s, porém custa mais nas resoluções testadas.
- Não é substituto universal de Veo, Kling, Seedance ou Wan 3.0: limites de 3–5 s, 720p e 16 fps tornam inadequados alguns resultados finais e diálogos.
- A Pika anuncia pesos abertos condicionados à licença MiniMax; TBC oferece API proprietária; evitar confundir acessibilidade por API com abertura de pesos.

## Fontes primárias consultadas
- https://www.tbc.co/press-releases/neuron-derived-ai-video-model
- https://fal.ai/models/tbc/mischief-1/image-to-video
- https://fal.ai/models/tbc/mischief-1/image-to-video/api
- https://openrouter.ai/tbc/mischief-1
