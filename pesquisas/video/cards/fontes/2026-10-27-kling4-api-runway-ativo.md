# Fonte revisada — Kling 4.0 via API Runway Dev

**Verificação:** 27/10/2026. **Evento:** 23/10/2026.

## Fato operacional verificado

A Runway adicionou o modelo `kling_4` ao Runway Dev em 23/10/2026, com endpoint público autenticado de texto para vídeo, imagem para vídeo e vídeo para vídeo. A documentação técnica da Runway de 26/10 contém exemplo em curl e SDK. **Status: ativo no agregador Runway Dev; API direta da Kling com acesso antecipado/limitado, não confundir canais.**

## Capacidades e limites

- Geração de vídeo com áudio incluído, entre 3 e 30 segundos, em 720p ou 1080p. Não há comprovação de 4K via `kling_4` no Runway Dev.
- Pode receber até dez imagens/quadros de referência no conjunto permitido pelo endpoint e até cinco vídeos de referência com duração total máxima de 30 segundos; as combinações específicas obedecem ao schema de cada rota.
- Edição de vídeo de entrada preserva duração e enquadramento da fonte.
- `model: "kling_4"` nos endpoints Runway `/v1/text_to_video`, `/v1/image_to_video`, `/v1/video_to_video`.
- Qualidade relativa, consistência de produto, latência e taxa de rejeição precisam de testes; anúncio não demonstra superioridade frente a concorrentes.

## Custo confirmado

Runway: 1 crédito = US$0,01. **Promoção de 20% até 30/11/2026 UTC.**
- Sem vídeo como entrada: 720p 12,56 créditos por segundo = US$0,1256/s; 1080p 18,8 créditos = US$0,188/s. Preços-padrão: 15,7 e 23,5 créditos/s, respectivamente.
- Com vídeo de entrada: 720p 7,52 créditos multiplicados pelos segundos de saída **mais** 7,52 multiplicados pelo maior entre a duração de entrada e de saída; 1080p aplica 11,28 créditos a cada parcela. Não reduzir esse caso a um preço fixo por segundo de saída. O arredondamento de créditos é feito pelo fornecedor.
- Exemplo 10s sem vídeo como entrada, no desconto: US$1,256 (720p) ou US$1,88 (1080p), antes de outras despesas, taxas e custos de rejeição.

## Utilidade e cautelas

Comercialmente testável agora para anúncios, clipes, explicativos, curtas e pós-produção. Comparar custo por tomada aprovada, retenção de identidade visual, coerência de áudio e motion/câmera com Runway Gen-4.5 (API ativa), Wan 3.0 (API ativa) e Google Veo 3.1 GA (API Cloud ativa). Comparação deve manter mesmo roteiro e referências. Rever termos comerciais e direitos de mídia antes de subir materiais de clientes; modelo chinês disponibilizado por plataforma americana não implica licença aberta de pesos.

## Fontes

1. Runway Dev, changelog oficial, evento 23/10/2026: https://docs.dev.runwayml.com/api-details/api_changelog/
2. Runway Dev, tutorial `kling_4`, publicado 26/10/2026: https://dev.runwayml.com/learn/kling-4-0-api
3. Runway Dev, preços e créditos vigentes: https://docs.dev.runwayml.com/guides/pricing/
