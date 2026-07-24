# Registro operacional — Sales Video

## 2026-07-24 — Bloqueio visual não pode virar criativo

- Problema observado: o vídeo MUSA do experimento 71/job `20454` foi gerado pela Luma e ficou tecnicamente `VIDEO_READY`, mas a checagem visual comercial da tela marcou `Bloqueado: luz oscilando`.
- Causa-raiz tratada: o job nasceu como Luma text-to-video puro (`image_to_video_enabled=false`) e sem imagem-base OpenAI, apesar de o acervo já ter recurso preventivo para travar composição/luz antes da animação. Além disso, o bloqueio visual estava mais forte na UI do que no contrato operacional do render.
- Correção preparada: renders planejados comerciais Luma para `LANDING_HERO` ou `AD` passam a carregar `generation_strategy=OPENAI_IMAGE_TO_LUMA_VIDEO`, `image_to_video.enabled=true`, diretivas anti-flicker/haze/blur e `quality_gate.reject_if` com os problemas visuais que bloqueiam uso comercial. A tela de vídeo do produto também passa a deixar a imagem-base OpenAI ligada por padrão quando o provider suporta esse recurso.
- Regra operacional: asset bloqueado por QA visual não pode ser aceito como criativo, hero, retargeting, variação publicável ou referência final. Ele só serve como evidência de falha ou insumo de regeneração.

## 2026-07-24 — TTS OpenAI obrigatório para pós-produção comercial

- Problema observado: vídeos comerciais do MUSA podiam ficar tecnicamente renderizados, mas sem voz/legenda/trilha quando a pós-produção falhava por ausência de `OPENAI_API_KEY_FILE` no runtime do `video-management-service`.
- Causa-raiz tratada: o risco estava descrito apenas como limitação operacional, sem regra clara impedindo que MP4 bruto silencioso virasse peça comercial final por conveniência.
- Regra aplicada: `AGENTS.md` e o cânone de Sales Video agora exigem corrigir o secret `OPENAI_API_KEY_FILE` e reexecutar a pós-produção antes da aprovação comercial quando o roteiro depender de TTS/legenda/trilha.
- Impacto comercial esperado: preservar força persuasiva dos vídeos de PDE e evitar testes contaminados por asset silencioso incompleto, salvo aprovação humana explícita para teste limitado.

## 2026-07-24 — Estratégia de avatar no perfil de vídeo

- Decisão de produto: manter o vídeo HeyGen atual como teste de mercado e preparar o Marketing Hub para planejar avatares proprietários no futuro.
- Causa-raiz tratada: a escolha entre avatar pronto e criação de avatar proprietário ficava fora do perfil canônico de vídeo, dependendo de conversa ou documentação solta.
- Correção preparada: `sales_video_profile` passa a registrar `avatar_strategy`, com opção para testar avatar pronto, planejar avatar proprietário ou usar avatar proprietário aprovado; a criação de vídeo no frontend expõe essa escolha.
- Regra operacional: usar avatar pronto para medir dor, promessa e clique com baixo custo; só investir em avatar proprietário depois de sinal positivo de atenção/conversão.

## 2026-07-24 — Adapter direto HeyGen para avatar com voz sincronizada

- Problema observado: HeyGen aparecia na combo do Marketing Hub, mas ainda dependia de implementação futura no executor para criar asset real.
- Causa-raiz tratada: o catálogo visual do frontend e a configuração de segredo avançaram antes do `video-management-service` ter um provider direto para `providerName=HEYGEN`.
- Correção preparada: criado `HeyGenVideoProvider` com autenticação `X-Api-Key`, criação por `POST /v3/videos`, polling por `GET /v3/videos/{videoId}`, download do MP4 final e auditoria de request/status no metadata do job.
- Regra operacional: para geração real, o token HeyGen não basta; o ambiente ou o job deve informar `VIDEO_PROVIDERS_HEYGEN_AVATAR_ID`/`heygen_avatar_id` e `VIDEO_PROVIDERS_HEYGEN_VOICE_ID`/`heygen_voice_id`, porque a escolha da apresentadora e da voz é decisão criativa do produto.

## 2026-07-24 — Token HeyGen via arquivo no container de video

- Problema observado: HeyGen ja aparecia no catalogo de fornecedores do Marketing Hub, mas o container de video ainda nao recebia o token pelo mesmo padrao operacional de Luma/Kling/VEO.
- Causa-raiz tratada: a operacao criou o arquivo seguro no host, mas o compose e o entrypoint nao montavam nem carregavam automaticamente `HEYGEN_API_KEY`.
- Correção preparada: compose local e compose de deploy montam `/root/infra/heygen-token/heygen_api_key` como secret somente leitura; o entrypoint carrega HeyGen em `HEYGEN_API_KEY` e `VIDEO_PROVIDERS_HEYGEN_API_KEY`.
- Regra operacional: token HeyGen deve seguir o mesmo padrao de segredo por arquivo, sem valor real em compose, Markdown, `.env`, logs ou resposta.

## 2026-07-23 — Tokens Luma e Kling via arquivo no container de video

- Problema observado: os arquivos de token Luma e Kling foram criados no host de video, mas o container ainda so montava e carregava automaticamente o arquivo do Gemini.
- Causa-raiz tratada: a operacao real passou a seguir o padrao de secrets por arquivo para multiplos providers, mas o compose e o entrypoint ainda estavam limitados ao VEO/Gemini.
- Correção preparada: compose local e compose de deploy montam `/root/infra/luma-token/luma_api_key` e `/root/infra/kling-token/kling_api_key` como secrets somente leitura; o entrypoint carrega Luma em `LUMA_API_KEY`, `LUMA_AGENTS_API_KEY` e `VIDEO_PROVIDERS_LUMA_API_KEY`, e Kling em `KLING_API_KEY` e `VIDEO_PROVIDERS_KLING_API_KEY`.
- Regra operacional: provider de video com token em arquivo deve ser montado pelo compose versionado e carregado no entrypoint sem imprimir segredo em logs, `.env` ou documentacao.

## 2026-07-23 — Providers múltiplos e montagem de vídeo hero MUSA

- Problema observado: o VEO entregou clipe de 8s e o vídeo foi percebido como incompleto para venda, apesar de o job técnico estar pronto.
- Causa-raiz tratada: a operação tratava provider curto como suficiente para um perfil comercial de 30s e não carregava no job uma estratégia clara de provider, cenas e montagem final.
- Correção preparada: a tela `/videos` e o detalhe do perfil passaram a usar catálogo de providers com Luma Ray 3.2 como padrão para hero premium, Kling 3.0 como alternativa de teste e Veo como teaser/cena curta; o pedido de render agora envia `metadataJson` com plano de montagem Dor -> Resultado -> Mecanismo -> CTA, duração mínima comercial e exigência de streaming HLS.
- Regra operacional: vídeo hero de venda do PDE deve ser montado por cenas e entregue como stream adaptativo; Veo não deve ser aprovado como peça única de 30s.

## 2026-07-23 — Player de streaming adaptativo para vídeo comercial

- Problema observado: entregar MP4 bruto direto para a cliente aumenta tempo de início, consumo de banda e risco de abandono no celular.
- Causa-raiz tratada: o contrato de conclusão do render só expunha asset bruto, sem URL publicável de streaming adaptativo.
- Correção preparada: `sales_video_job` passou a persistir `stream_playback_url`; o callback de conclusão aceita `streamPlaybackUrl`; os DTOs expõem essa URL; Marketing Hub e PDE priorizam HLS adaptativo com MP4 como fallback.
- Regra operacional: vídeo bruto/renderizado é ativo de auditoria e contingência; a experiência principal da usuária deve usar stream adaptativo sempre que houver URL processada pelo pipeline de mídia.

## 2026-07-21 — Vídeos de entrada do PDE pelo Marketing Hub

- Problema observado: a nova área `Vídeos` existia como planejamento local no navegador, mas isso não criava artefato rastreável no Marketing Hub.
- Causa-raiz: a tela usava `localStorage` como fonte de verdade e não acionava o módulo canônico `sales-video`.
- Correção preparada: a tela `/videos` passou a selecionar produto, criar perfil de vídeo do PDE, salvar roteiro aprovado e solicitar job de criação pelo backend do Marketing Hub.
- Regra operacional: código de vídeo muda via GitHub; artefato comercial de vídeo para PDE deve nascer e evoluir pelo Marketing Hub, preservando perfil, roteiro, job, asset e versão comercial associada.

## 2026-07-12 — Token Gemini via arquivo no container de video

- Problema observado: jobs de video VEO podiam falhar por provider sem token configurado quando o container nao recebia `GEMINI_API_KEY`.
- Causa-raiz tratada: o modulo `video-management-service` dependia de variavel de ambiente direta, mas a operacao real mantem a chave em arquivo no host.
- Correção preparada: compose local e compose de deploy montam `/root/infra/gemini-token/gemini_api_key` como `/run/secrets/gemini_api_key:ro`; o entrypoint carrega o arquivo para `GEMINI_API_KEY` e `VIDEO_PROVIDERS_VEO_API_KEY` antes de iniciar o Spring Boot.
- Protecao adicional: a passagem direta de `GEMINI_API_KEY` pelo compose foi removida para reduzir risco de vazamento em `docker compose config`.

## 2026-07-02 — Correção de truncamento em evento de retry

- Problema observado: backend saudável, mas logs com `Data truncated for column 'event_type' at row 1` durante `SalesVideoAutoRetryScheduler`.
- Histórico consultado: `docs/diagnostics/ai-worker-jobs-log-check-2026-05-01.md` já apontava suspeita de incompatibilidade entre código e banco.
- Confirmação via MCP: `sales_video_job_event.event_type` no banco real estava como `ENUM` sem o valor `RETRIED`; o código atual grava `SalesVideoJobEventType.RETRIED`.
- Causa-raiz: schema real ficou preso em contrato antigo de enum, enquanto entidade JPA e changelog fundacional atuais esperam `VARCHAR(64)`.
- Correção preparada: Liquibase `sales-video-hardening-007-event-type-varchar` converte `sales_video_job_event.event_type` para `VARCHAR(64) NOT NULL`, mantendo compatibilidade com eventos futuros sem precisar alterar enum físico no banco.
