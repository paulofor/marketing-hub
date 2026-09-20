# Radar IA para vídeo — 2026-09-20

## Resumo executivo

Uma mudança passou o filtro de relevância desta rodada:

1. **fal H3 Max Lip Sync** ficou publicamente disponível como endpoint especializado de **imagem + áudio → vídeo com lip sync**, com 480p a 2K, sincronização guiada por transcrição opcional, API pay-per-use e licença comercial no serviço hospedado.

O endpoint apareceu publicamente na janela de 17–18/09 e não havia entrado nas duas rodadas anteriores. O considero relevante agora porque transforma uma tarefa até então muito ligada a suites de avatar em uma chamada simples de API e, para o Marketing Hub, permite separar **texto/voz aprovados** da **performance visual**.

Não encontrei em 20/09 um novo renderer de Runway, Google, Kling, Seedance, Wan, Vidu ou OpenAI com mudança comparável que justificasse outra notificação. A API Sora 2 continua em **encerramento anunciado para 24/09/2026**; isso já havia sido registrado e não é uma novidade desta rodada.

## 1. fal H3 Max Lip Sync

**Status: 🟢 ATIVO — API pública, pay-per-use e uso comercial no serviço da fal.**

O novo endpoint recebe:
- uma imagem com rosto visível;
- um arquivo de áudio;
- resolução;
- opcionalmente seed e modo de transcrição.

Ele retorna um vídeo cuja boca é animada para acompanhar o áudio. A trilha fornecida é preservada no resultado.

### Modos de sincronização

O detalhe mais interessante é a separação explícita entre dois modos:

- **transcription-guided** (padrão): o serviço transcreve a fala e usa as palavras para guiar os movimentos labiais;
- **waveform-only**: a transcrição pode ser desativada para canto ou áudio muito processado.

Isso é operacionalmente útil porque fala normal e canto deixam de depender exatamente do mesmo mecanismo.

### Limites e preço

Cada geração aceita **5 a 15 segundos** de áudio. Acima de 15 segundos, o áudio é truncado.

| Resolução | Preço atual |
|---|---:|
| 480p | US$0,05/s |
| 768p | US$0,08/s |
| 1080p | US$0,16/s |
| 2K | US$0,32/s |

Um clipe de 10 s custa, respectivamente, US$0,50 / 0,80 / 1,60 / 3,20.

A fal afirma que o output do endpoint pode ser usado comercialmente, sujeito aos termos da plataforma e aos direitos sobre o rosto e a voz utilizados.

## 2. Comparação com alternativas atuais

| Sistema | Status | Entrada principal | Duração/resolução | Custo de referência |
|---|---|---|---|---:|
| **H3 Max Lip Sync** | 🟢 Ativo | imagem + áudio | 5–15 s; 480p–2K | US$0,05–0,32/s |
| **Hedra Avatar** | 🟢 Ativo | imagem + áudio | até 10 min; 540p–1080p | US$0,025–0,0625/s |
| **Kling AI Avatar v2** | 🟢 Ativo via Hedra | imagem + áudio | 720p | ~US$0,0562/s Standard; 0,115/s Pro |
| **HeyGen Photo Avatar 4** | 🟢 Ativo via Hedra | imagem + áudio | 360p–1080p | US$0,10/s |
| **Sync Labs lipsync-2 / sync-3** | 🟢 Ativo | vídeo + áudio | até minutos; sync-3 até 4K | US$0,04–0,133/s |
| **VEED Lip Sync 2.0** | 🟢 Ativo | vídeo + áudio | até 10 min / 4K | US$0,07/s |

A comparação precisa separar duas categorias:

- **imagem → talking head**: H3 Max Lip Sync, Hedra Avatar, Kling Avatar, HeyGen Photo Avatar;
- **vídeo existente → fala corrigida/localizada**: Sync Labs e VEED.

O H3 Max Lip Sync não substitui VEED/Sync para redublar footage pronto. Sua vantagem é criar rapidamente um apresentador a partir de uma foto e uma locução já pronta.

## 3. Open weights e licença

O endpoint H3 Max Lip Sync é um **serviço hospedado da fal** e não localizei pesos separados do pós-treino especializado.

Ele é derivado do **MiniMax H3**, cuja base tem pesos disponíveis sob a **MiniMax H3 Community License**, e não sob uma licença OSI permissiva. Essa licença possui restrições territoriais e comerciais específicas; portanto, “open-weight” não deve ser traduzido como “open source sem restrições”.

Para integração comercial simples, o caminho mais claro hoje é a API hospedada da fal, que declara uso comercial para os outputs do endpoint.

## 4. Por que isso importa para o Marketing Hub

O princípio útil é **desacoplar voz e performance visual**.

Em vez de:

`roteiro → renderer inventa voz + pronúncia + boca + aparência`

o harness pode trabalhar como:

`roteiro aprovado → voz aprovada → imagem/persona → lipsync → revisão → montagem`

Isso permite que Têmis valide texto, números, preço, claims e CTA **antes** da geração visual. Depois, Apolo pode reutilizar exatamente o mesmo `voice_asset_id` em várias imagens/personas ou idiomas, alterando menos variáveis a cada experimento.

Para anúncios curtos, o limite de 15 s também combina com um fluxo modular: hook, demonstração/prova e CTA podem virar blocos separados, cada um revisado e montado no projeto estruturado.

## 5. Limites e riscos

- 15 s por chamada é curto para explicadores ou anúncios longos;
- imagem única não oferece a mesma liberdade corporal/câmera de um gerador audiovisual completo;
- não há benchmark independente específico que comprove superioridade de lip sync sobre Hedra/HeyGen/Kling;
- consentimento e direitos sobre rosto e voz continuam obrigatórios;
- para canto, áudio degradado ou forte sound design, a sincronização guiada por transcrição pode ser inadequada; o modo waveform-only deve ser testado.

## Card desta rodada

Criei um card novo:

`video-voz-aprovada-lipsync-desacoplado`

Princípio: **aprovar roteiro/voz como artefato canônico e gerar a camada visual depois**, reutilizando a mesma locução em diferentes personas, idiomas ou variantes.

Fonte revisada:
`pesquisas/video/cards/fontes/2026-09-20-h3-max-lip-sync-voz-desacoplada.md`

SHA-256:
`a426a8275581c7aebcb618ad5b231940781df8104c64e29f83490e4c562da228`

JSON:
`pesquisas/video/cards/2026-09-20-h3-max-lip-sync-voz-desacoplada.json`

## Fontes

- https://fal.ai/h3-max-lip-sync
- https://fal.ai/models/minimax/h3-max/lip-sync/image-to-video
- https://novoads.ai/en/blog/minimax-h3-max-lip-sync
- https://www.hedra.com/models/video/hedra/avatar
- https://www.hedra.com/models/video/kling/ai-avatar-v2
- https://www.hedra.com/models/video/heygen/photo-avatar-4
- https://sync.so/docs/product/billing
- https://sync.so/docs/models/lipsync
- https://www.veed.io/api
- https://developers.openai.com/api/docs/guides/video-generation
