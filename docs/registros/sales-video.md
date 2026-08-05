# Registro operacional — Sales Video

## 2026-07-28 — Bloqueio de vídeos MUSA gerados por slides

- Problema observado: v5/v6 do PDE MUSA estavam usando MP4/HLS gerados a partir de slides do diagnóstico como se fossem vídeos comerciais.
- Decisão comercial: esse tipo de vídeo fica proibido para MUSA; vídeos comerciais devem nascer da estrutura de produção de vídeos do Marketing Hub, com roteiro, job, asset e URL auditáveis.
- Correção preparada: remoção dos assets MP4/HLS de slides, bloqueio no build, retirada dos defaults de runtime e atualização da validação de slot para não exigir HLS antigo.
- Impacto esperado: evitar teste contaminado por criativo fraco e preservar a percepção de valor do PDE antes de enviar tráfego pago.

## 2026-07-25 — Prompt de roteiro adaptável por produto

- Problema observado: a melhoria de qualidade do roteiro corrigia o MUSA, mas deixava exemplos específicos de moda no prompt global, criando risco de contaminar scripts de outros produtos.
- Causa-raiz tratada: o prompt base recebia poucos blocos comerciais estruturados e misturava regra universal de conversa natural com vocabulário específico de um produto.
- Correção preparada: o `SalesVideoPromptBuilder` passa a concatenar blocos de contexto por nicho/consumidor, hipótese/promessa, oferta/funil/conversão e prova/experiência de valor quando esses dados existirem no produto.
- Correção preparada: o template de roteiro remove exemplo fixo do MUSA e orienta o modelo a extrair palavras concretas do contexto do produto atual.
- Regra operacional: vocabulário específico como look, peça-sinal ou roupa nova só deve aparecer quando vier do contexto comercial do produto, não do prompt genérico de Sales Video.

## 2026-07-25 — Reforço de qualidade para scripts naturais de venda

- Problema observado: scripts aprováveis tecnicamente estavam soando frios e abstratos, com promessa pouco desejável para anúncio, como "imagem coerente".
- Causa-raiz: o prompt de roteiro pedia hook, dor e mecanismo, mas não obrigava conversa natural, situação cotidiana, microexperiência de valor e troca de abstrações por efeitos percebidos.
- Correção preparada: o prompt versionado de Sales Video passou a exigir a estrada situação reconhecível -> dor percebida -> mecanismo plausível -> microexperiência -> redução de risco/esforço -> CTA.
- Correção preparada: o AI Worker passou a ter modelo próprio para roteiro de vídeo em `SALES_VIDEO_SCRIPT_MODEL`, com default `gpt-5.5`, separado do `OPENAI_MODEL` genérico.
- Decisão comercial: para MUSA, evitar termos vagos como "imagem coerente" e priorizar linguagem concreta de conversa, como "parar de sentir que falta algo no look", "peça-sinal" e "sem comprar roupa nova".

## 2026-07-28 — Reputação de providers sem bloqueio temporário

- Decisão operacional: não deixar nenhum provider bloqueado enquanto a base comparativa ainda está sendo formada.
- Causa-raiz tratada: a tela de provedores traduzia reprovação visual ou score baixo como “Bloquear ou regenerar”, o que impedia aprendizado justamente quando precisamos acumular mais amostras por provider.
- Correção preparada: o backend passa a recomendar `usar_com_cautela` ou `testar_controlado` em vez de bloqueio; o frontend troca o rótulo legado por “Regenerar e testar”.
- Regra comercial: histórico ruim continua visível e penaliza score, mas não bloqueia novos testes controlados até haver massa de dados suficiente para decisão definitiva.

## 2026-07-25 — Bloqueio de VEO direto acima de 8 segundos

- Problema observado: pedidos de vídeo VEO podiam ser criados com alvo comercial de 30s, embora o VEO direto entregue clipes curtos de até 8s por render.
- Causa-raiz tratada: a tela e o backend aceitavam uma duração comercial incompatível com o limite nativo do provider, gerando job tecnicamente incoerente e risco de desperdício operacional.
- Correção preparada: o backend passa a rejeitar render direto com `providerName=VEO` acima de 8s tanto no fluxo de experimento quanto no fluxo central de Sales Video; o frontend passa a mostrar o limite e bloquear a solicitação incompatível.
- Regra operacional: VEO direto é teaser/cena curta. Vídeo de venda de 30s deve usar provider compatível ou montagem explícita de múltiplas cenas.

## 2026-07-24 — Pontuação comercial de providers de vídeo

- Decisão de produto: o Marketing Hub deve pontuar providers de vídeo por evidência comercial e qualidade real, não apenas por catálogo ou preferência manual.
- Causa-raiz tratada: vídeos ruins ou bloqueados podiam ficar apenas como eventos isolados de QA, sem reduzir a chance de o mesmo provider/configuração ser escolhido novamente.
- Correção preparada: o resumo comercial de Sales Video passa a expor `providerScores`, combinando jobs prontos/falhos, assets aprovados/rejeitados e eventos de funil como lead, lead qualificado, checkout e compra.
- Regra operacional: vídeo de sucesso aumenta reputação do provider; vídeo bloqueado, falha técnica ou rejeição visual reduz reputação e deve orientar regeneração, troca de provider ou uso controlado.

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

## 2026-07-29 — Contrato HeyGen exige background tipado

- Problema observado: o job MUSA PDE v7 com avatar e voz corretos chegou na API HeyGen, mas foi rejeitado com `invalid_parameter` em `background.type`.
- Causa-raiz tratada: o `video-management-service` enviava `background.value`, mas a API HeyGen v3 espera o objeto de fundo tipado.
- Correção preparada: o payload HeyGen agora envia `background: { type: "color", value: "#F8F0EA" }` e o teste do provider valida esse contrato.
- Regra operacional: erro 400 de provider deve ser investigado pelo contrato real enviado, não tratado como instabilidade temporária.

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

## 2026-07-24 — Token Runway via arquivo no container de video

- Problema observado: o token Runway foi disponibilizado no host de video, mas o container ainda nao o recebia pelo mesmo padrao operacional de Luma/Kling/VEO/HeyGen.
- Causa-raiz tratada: a configuracao versionada do modulo de video ainda nao montava nem carregava automaticamente `RUNWAY_API_KEY`.
- Correção preparada: compose local e compose de deploy montam `/root/infra/runaway-token/runaway_api_key` como secret somente leitura; o entrypoint carrega Runway em `RUNWAY_API_KEY` e `VIDEO_PROVIDERS_RUNWAY_API_KEY`.
- Regra operacional: token Runway deve seguir o mesmo padrao de segredo por arquivo, sem valor real em compose, Markdown, `.env`, logs ou resposta.

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

## 2026-07-28 — HLS gerenciado para vídeos de PDE

- Decisão comercial: PDE público deve consumir sempre HLS (`.m3u8`); MP4 fica como master, fallback interno ou evidência, mas não como URL canônica de experiência.
- Causa-raiz tratada: a v6 podia ser padronizada para um vídeo aprovado com MP4 rastreável, mas ainda sem campo explícito de HLS no ativo comercial do Marketing Hub.
- Correção preparada: `experiment_video_asset` passa a ter `hls_playback_url`, exposto como `hlsPlaybackUrl`; o sync de jobs copia a playlist HLS quando o worker informa `streamPlaybackUrl`; `LANDING_HERO` aprovado exige áudio e HLS.
- Prevenção: a biblioteca de vídeos e a aba de vídeo do experimento priorizam HLS no player e sinalizam se o ativo está pronto para PDE.
- Impacto comercial esperado: evitar publicação de PDE com vídeo fora do fluxo gerenciável, reduzir abandono por falha de reprodução no celular e manter custo/revisão/asset sob governança do Marketing Hub.

## 2026-07-28 — Cadastro operacional de playlist HLS no Hub

- Problema observado: o HLS da v6 podia estar fisicamente no build do PDE e, por isso, nao ficava facil de encontrar nem corrigir pela tela de videos do Marketing Hub.
- Causa-raiz tratada: a governanca estava parcialmente no contrato e no banco, mas faltava uma acao administrativa simples para salvar a playlist HLS no ativo comercial.
- Correção preparada: a biblioteca global de videos passa a exibir e editar a `Playlist HLS do PDE` em cada ativo, gravando `hlsPlaybackUrl` pelo endpoint oficial do experimento.
- Prevenção: video `LANDING_HERO` continua bloqueado para aprovacao quando nao possui `.m3u8`, e playlists HLS externas ao fluxo completo devem ser cadastradas no Hub antes de uso comercial em PDE.

## 2026-07-30 — Painel de vídeos por versão PDE

- Problema observado: o painel `/products/{productId}/pde-videos` podia mostrar vídeo da v6 em outra versão quando o agrupamento usava apenas o experimento de origem do slot.
- Causa-raiz tratada: a tela cruzava localmente todos os assets por `sourceExperimentId`, mas a verdade comercial do vídeo de PDE é a versão publicada/playlist HLS, não apenas o experimento histórico em que o asset foi gravado.
- Correção preparada: o backend passa a expor `/api/products/{id}/pde-videos`, retornando cada slot produtivo com seus vídeos HLS já resolvidos por versão; o frontend deixa de inferir vínculo no navegador e apenas apresenta o contrato do backend.
- Prevenção: quando o HLS aponta para uma versão, mas o asset pertence a outro experimento, o painel exibe alerta operacional para revisão sem esconder o vídeo da versão correta.
- Impacto comercial esperado: reduzir risco de aprovar, revisar ou pausar vídeo na versão errada antes de campanha paga do MUSA.

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

# 2026-08-02 — Audio Video Studio: análise manual estruturada de vídeo

- foi feito: a tela de resultado de vídeos de referência passou a ter um formulário de análise comercial com evidências, diagnóstico, sequência, aprendizados, melhorias de venda e decisão operacional.
- contrato oficial: `PATCH /api/sales-videos/reference-videos/{referenceId}/analysis`.
- impacto comercial: vídeos vencedores de TikTok/Reels/YouTube deixam de ficar apenas “na fila” e viram aprendizado persistido para novos roteiros, criativos, provas e CTAs.
- prevenção: o relatório continua vindo do backend em `analysis_notes`, preservando a tela como verdade do sistema e evitando análise solta apenas no navegador.

## 2026-08-05 — Ponte de continuidade e montagem premium

- Problema observado: planos curtos eram gerados com a mesma imagem-base e apenas concatenados, produzindo repetição, saltos de identidade e vídeo final sem acabamento contínuo.
- Causa-raiz tratada: a continuidade dependia de texto no prompt; o fluxo não materializava o último quadro aprovado como entrada obrigatória do plano seguinte e não encadeava a pós-produção após a montagem.
- Correção preparada: cada render passa a persistir seu quadro final; a geração sequencial usa esse asset como imagem inicial do próximo plano; a montagem aplica microtransições, mede ritmo e bloqueia média acima de quatro segundos em sequências com seis ou mais planos; o backend enfileira voz e legenda após a montagem cinematográfica.
- Prevenção: a tela bloqueia geração intermediária sem plano anterior aprovado, o contrato registra origem da ponte e testes protegem a montagem e a continuidade sem substituir o storyboard persistido.
## 2026-08-05 — Gate comercial determinístico para a MUSA v7

- Problema observado: o job `20519` estava tecnicamente pronto, mas era um único clipe silencioso e podia ser interpretado como vídeo publicável.
- Causa-raiz tratada: `VIDEO_READY` representava conclusão técnica e a tela não recebia uma decisão canônica do backend sobre montagem, áudio pt-BR, legendas, CTA, HLS e revisão humana.
- Correção preparada: o backend passa a expor gate `READY/BLOCKED` com causas objetivas; a montagem narrativa exige exatamente `DOR`, `RESULTADO`, `MECANISMO` e `CTA`; a tela bloqueia visualmente a publicação enquanto faltar qualquer evidência.
- Critério comercial: só vincular a MUSA v7 ao PDE após gate aprovado e reprodução humana validada em desktop e mobile.
