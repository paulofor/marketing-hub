# Fonte revisada — H3 Max Lip Sync: voz aprovada como camada reutilizável

Data da revisão: 2026-09-20

## Evidência observada

A fal colocou publicamente no ar o **H3 Max Lip Sync**, endpoint especializado da família H3 Max. A página oficial estava atualizada em 17/09/2026 e rastreadores de lançamento registram a disponibilização pública em 18/09/2026.

O endpoint recebe uma imagem com rosto visível e um arquivo de áudio e devolve um vídeo em que a boca acompanha a fala. A entrada de áudio precisa ter pelo menos 5 segundos e cada chamada usa no máximo 15 segundos; áudio maior é truncado. O resultado preserva a trilha original fornecida.

O recurso pode operar em dois modos:
- `enable_transcription=true` (padrão): transcreve a fala e usa as palavras para orientar a sincronização;
- `enable_transcription=false`: sincroniza pela forma de onda, opção indicada pelo fornecedor para canto ou áudio processado em que a transcrição pode falhar.

As saídas disponíveis são 480p, 768p, 1080p e 2K. O preço publicado pela fal é:
- 480p: US$0,05/s;
- 768p: US$0,08/s;
- 1080p: US$0,16/s;
- 2K: US$0,32/s.

Não há assinatura mínima para o endpoint. O uso comercial é permitido pelos termos da fal, desde que o usuário tenha os direitos sobre rosto e voz utilizados.

O endpoint é **imagem + áudio → vídeo**. Ele não serve para redublar diretamente um vídeo existente. A própria fal recomenda um modelo video-to-video, como VEED Lip Sync, para esse caso.

## Comparação atual

**H3 Max Lip Sync — 🟢 ATIVO / API pública / uso comercial**
- ponto forte: começar de uma única imagem, usar áudio pronto, 480p–2K e permitir sincronização guiada por transcrição;
- limite principal: 15 segundos por chamada;
- preço: US$0,05–0,32/s conforme a resolução.

**Hedra Avatar — 🟢 ATIVO / API pública**
- imagem + áudio → avatar;
- 540p/720p/1080p;
- até 10 minutos;
- US$0,025/s em 540p, US$0,05/s em 720p, US$0,0625/s em 1080p.
- É muito mais adequado para falas longas, mas não publica o mesmo modo explícito de transcrição-on/waveform-only do novo endpoint da fal.

**Kling AI Avatar v2 — 🟢 ATIVO via Hedra**
- imagem + áudio → avatar;
- 720p;
- cerca de US$0,0562/s Standard e US$0,115/s Pro.
- Competidor direto na faixa de avatar curto, com foco em performance facial/corporal.

**HeyGen Photo Avatar 4 — 🟢 ATIVO via Hedra**
- imagem + áudio → avatar;
- 360p–1080p;
- US$0,10/s.
- Ecossistema de avatar mais maduro, mas custo superior ao H3 Max Lip Sync em 768p e inferior ao H3 apenas quando H3 sobe para 1080p/2K.

**Sync Labs lipsync-2 / sync-3 — 🟢 ATIVO**
- video + áudio → video;
- lipsync-2 custa aproximadamente US$0,04–0,05/s e sync-3 US$0,107–0,133/s;
- suporta vídeos longos conforme plano e sync-3 chega a 4K nativo.
- É uma categoria diferente: corrige fala em footage existente, em vez de criar um talking head a partir de uma foto.

**VEED Lip Sync 2.0 — 🟢 ATIVO / API pública**
- video + áudio → video;
- até 4K e até 10 minutos;
- US$0,07/s na API atual.
- Também é mais apropriado para localização/redublagem de footage já produzido.

## Interpretação

O achado torna prática uma arquitetura em que **voz aprovada e performance visual ficam desacopladas**. Em vez de pedir ao renderer para inventar ao mesmo tempo texto, voz e boca, o pipeline pode gerar ou gravar a locução primeiro, aprová-la como artefato canônico e só depois produzir diferentes apresentadores ou idiomas sobre essa mesma trilha.

Isso reduz o número de variáveis que mudam a cada render. Também permite reusar a mesma locução em múltiplas imagens/personas, comparar apresentadores sem alterar prosódia ou texto e validar voz/claims antes de gastar com geração visual.

## Aplicação possível no Marketing Hub

Fluxo sugerido:

`roteiro aprovado → voz aprovada → imagem/persona → lipsync → revisão audiovisual → montagem`

Para uma campanha, Apolo pode manter `voice_asset_id` e `script_version` fixos e variar apenas a `presenter_image` ou o idioma. Têmis pode aprovar o texto e a locução antes da geração visual, diminuindo o risco de um renderer mudar palavras, números, preço ou CTA.

O experimento deve comparar esse fluxo desacoplado com geração audiovisual integrada, medindo:
- taxa de erro textual/fonético;
- regenerações;
- tempo até aprovação;
- custo por variante aprovada;
- consistência entre idiomas/personas;
- avaliação humana de naturalidade.

## Limites

A qualidade e a velocidade do H3 Max Lip Sync ainda precisam de benchmark independente específico para o endpoint. O limite de 15 segundos exige montagem de vários trechos para anúncios mais longos. Como o sistema parte de uma única imagem, gestos corporais, movimentos de câmera e interação física são mais limitados do que em um gerador audiovisual completo. Consentimento e direitos sobre rosto e voz continuam obrigatórios.

## Fontes consultadas

- https://fal.ai/h3-max-lip-sync
- https://fal.ai/models/minimax/h3-max/lip-sync/image-to-video
- https://novoads.ai/en/blog/minimax-h3-max-lip-sync
- https://www.hedra.com/models/video/hedra/avatar
- https://www.hedra.com/models/video/kling/ai-avatar-v2
- https://www.hedra.com/models/video/heygen/photo-avatar-4
- https://sync.so/docs/product/billing
- https://sync.so/docs/models/lipsync
- https://www.veed.io/api
