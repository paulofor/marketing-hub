# Vega — continuidade após o preflight isolado

Data: 13/09/2026. Contexto preservado: produto 4, cadeia 14/v14, processo
75/v6, execução 4, ciclo de aprendizado 2, experimento 92, versão
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`, projetos 4/5 e perfis 59/60.

## Diagnóstico confirmado antes da correção

UI, endpoints oficiais e MCP (`marketinghubdb`) confirmam os ciclos de produção
15/16 em `PROVIDER_PREFLIGHT_ONLY_COMPLETED`, sem job, tarefa financeira ou
reserva. Os preflights 8/9 passaram e venceram; não são produção autorizada.
O evento 11 registra o teto humano de USD 20 para produção e revisão das duas
peças. O briefing do evento 12 preserva essa referência e as restrições comerciais.

O comando de consulta chama `/api/sales-videos/autonomy/v1/provider-preflights`.
`VideoProductionCycleService.completeProviderPreflight` encerra corretamente
essa consulta antes da reserva ou da tarefa de Plutus. A produção usa outro
comando, `/api/sales-videos/autonomy/v1/cycles`. `ProcessRunVideoGuidance` só
projetava falhas, deixando a consulta encerrada como espera automática no pai.
O teste anterior incluía explicitamente esse estado entre os casos sem orientação.
O ciclo histórico 11/#91 comprova que produção, Plutus e entrega para revisão
possuem estados próprios; o seu sucesso não autoriza reutilizar provas na v12.

Recorrência de `LOOP-BPM-DECISAO-HUMANA-COMO-EXECUCAO`: a ausência de trabalho
enfileirado deve orientar o comando necessário, sem simular execução ou aprovação.

## Alternativas comparadas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Iniciar geração automaticamente ao concluir a consulta | Remove um comando | Contraria o contrato sem cobrança; risco financeiro alto | Recusada |
| Explicar somente nesta conversa como abrir o Estúdio | Esforço mínimo | Mantém a causa da espera enganosa para outros ciclos | Insuficiente |
| Projetar a solicitação de produção no backend e preservar o comando governado | Explica a pendência e mantém preflight atualizado, Plutus e auditoria | Mudança localizada; exige regressões de identidade e estados | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Critérios |
| --- | --- |
| Reprodução | Consulta encerrada sem job deve expor solicitação de produção; teste antigo reproduz ausência de orientação |
| Caminho feliz | Processo → projeto correto → solicitação de produção → preflight novo → Plutus → Apolo → artefato e revisões independentes; aprovações humanas preservadas |
| Validações/falhas | READY e EXPIRED da consulta não autorizam gasto; bloqueios anteriores preservados; preflight/Plutus/Apolo ativos não oferecem disparo duplicado; inconsistência de tarefa/job não vira autorização |
| Integração | Backend real na fixture REST/MySQL 5.7, callbacks canônicos, suites de vídeo e financeiro com dependências locais; nenhuma API paga na homologação |
| Observabilidade | Motivo, ação, referência do preflight e histórico persistidos; leitura não escreve; custos estimados separados dos confirmados |
| Segregação | Produto, experimento, versão, papel da peça e marco temporal exatos; fixtures 91001+ sem campanha, cobrança ou métrica humana |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; contexto copiado, ausência de spinner enganoso, ida ao projeto e retorno ao processo |
| Encerramento | Duas rodadas locais completas consecutivas após a última correção, revisão do diff, empacotamento íntegro antes da aplicação excepcional autorizada |

Evidências brutas da investigação: `.sandbox/vega-recuperacao/`. Resultados e
situação operacional serão registrados após a execução da matriz.


## Impedimento adicional reproduzido localmente

A reprodução do contrato de produção de quinze segundos falhou nos dois aliases
Gen-4.5: o backend fornecia quatro cortes; o schema e o planejador do executor
exigem cinco funções. Comparadas três opções: reduzir a exigência do planejador
(esforço baixo, perda de prova comercial); aumentar a duração (esforço médio,
mudança de material e custo); distribuir cinco funções nos mesmos quinze segundos
(esforço baixo, custo de clipes preservado). Escolhida a terceira. O teste agora
consome o metadata exportado no planejador real, com somente a IA simulada.
O plano técnico do Estúdio deve refletir cinco cortes antes da solicitação real.

## Finalização e prova privada — ampliação antes da homologação final

A revisão do caminho completo confirmou que a rota genérica só entrega clipes brutos.
O histórico Product UGC possui pós-produção automática e prova determinística; essa
integração não existia na rota genérica escolhida para a v12. Não houve geração paga
para descobrir essa diferença. As duas rodadas anteriores passaram, mas a homologação
final deve ser reiniciada depois desta ampliação causal.

Alternativas: (1) editar e carregar arquivos manualmente — pouco código, mas quebra
a automação e a auditoria de produção; (2) trocar para Product UGC — usa caminho já
existente, porém exige referências HTTPS públicas e aumenta a estimativa de render;
(3) inserir a prova privada pelo executor e acionar a finalização existente — esforço
local moderado, mantém rota, orçamento e captura privada. Escolhida a terceira.

Matriz adicional definida antes dos novos testes: captura técnica aprovada da versão
exata; recusa de produto, experimento, versão, hash, tarefa ou enquadramento divergentes;
download apenas pelo controller do módulo de vídeo; nenhuma chamada externa paga em
caso de prova inválida; voz e legenda derivadas do texto aprovado; composição com FFmpeg
real, inspeção por ffprobe e reprodução desktop/mobile; linhagem e custos no job filho;
nenhuma alegação de prova humana ou comercial por usar captura de AGENT_VALIDATION.

A revisão do filtro HTTP confirmou que a rota administrativa de prova exige `X-Tenant-ID`.
A primeira fixture HTTP da composição não exigia esse cabeçalho e teria permitido um falso
positivo. O contrato agora propaga o tenant e a fixture recusa sua ausência; testes recusam
tenant divergente. A rodada `final1` fica diagnóstica, pois as duas rodadas finais devem
seguir esta correção de integração. Nenhum container publicado ou provider pago foi usado.

A suíte integral também recusou dependência direta de `salesvideo.service` em entidades e
serviços de agentes. A regra foi preservada: `VideoProductProofSource` é a porta neutra e
`AgentTaskVideoProductProofSource` traduz a evidência dentro do módulo proprietário.
Não foi adicionada exceção ao ArchUnit para fazer a integração passar.

## Homologação local concluída

As rodadas consecutivas `final2` e `final3` passaram na matriz completa, com os
mesmos arquivos de implementação e testes (hashes conferidos entre as rodadas).
Por rodada: **2.922 testes executados no backend**, **170 no executor de vídeo**,
**38 no financeiro** e **128 no frontend**: **3.258 testes executados sem falhas**.
O backend registrou ainda oito testes opcionais desabilitados pelas condições
próprias da suíte; eles não estão incluídos no total executado.

Também passaram os contratos de CI e pacote, correspondência de classes/recursos
do JAR, typecheck, build, sete testes do aplicador, REST financeiro/preflight com
MySQL 5.7 real, planejamento de Apolo com o contrato exportado pelo backend,
download privado com tenant obrigatório, FFmpeg/ffprobe, voz HTTP simulada,
legendas e reprodução desktop/iPhone 15 Pro/Pixel 7. Nenhuma API paga ou métrica
comercial foi usada pelas fixtures. A composição usa um tom sintético: naturalidade
da voz real e mérito comercial dependem da inspeção do candidato real.

Evidências: `artifacts/video-production-continuity/final2/` e `final3/`, com
`counts.json`, relatórios Surefire, `media/worker-result.json`,
`media/player-results.json`, `browser/results.json` e screenshots por dispositivo.
O diff foi revisado, incluindo comentários de responsabilidade dos métodos Java,
isolamento entre módulos, identidade da prova, aprovação humana e ausência de
novos changelogs. Os Compose temporários foram removidos com volumes e órfãos.

Antes da aplicação, a consulta MCP continuava mostrando os ciclos 15/16 sem tarefa,
job ou custo. Os dois projetos pertenciam a produto 4/experimento 92/v12; nenhuma
produção desta recuperação havia sido solicitada. As pré-checagens somente de
leitura confirmaram que os três serviços publicados usam os Compose versionados
esperados e que a configuração operacional pode ser preservada na aplicação.

## Imagens da revisão validada

Revisão local: `b8283442948497a181641b81ea0bbd09591415d6`, sobre a `main`
`9a5d4361ff2a0fabde650201352dcf9145e138a7`, novamente conferida antes da aplicação.
Sem push ou PR. Imagens construídas pelos Dockerfiles versionados de backend,
frontend e `video-management-service`, identificadas por `vega-cycle6-b82834429484`
e pelo rótulo OCI da revisão completa.

- Backend: JAR SHA-256 `3cac62cf8b27b5c19d4804d0982a9c678a13361c5a9a2d90f12b5dbc547b6fce`,
  idêntico ao pacote testado, conferido dentro da imagem sem iniciar a aplicação produtiva.
- Executor: 135 classes e prompt conferidos byte a byte contra a compilação local;
  JAR SHA-256 `15784cf97d4d5604229820a2f727acdf39660287d33892f2832f31916cda39c1`.
  A imagem executou `pending → dry runs 10s/5s → callback READY` na rede de mocks.
- Frontend: bundle produtivo com revisão explícita; Chromium desktop/iPhone/Pixel
  confirmaram a nova referência e a API na porta 80, com todas as APIs simuladas.
  A fixture da imagem precisou fornecer o DNS `backend` exigido pelo Nginx e uma
  rede acessível ao navegador; isso não exigiu alteração no produto ou na imagem.

Os testes das imagens também encerraram o projeto Compose exclusivo com volumes
e órfãos. O MCP confirmou zero jobs de vídeo/roteiro pendentes ou em processamento
antes de iniciar a intervenção `405517f219104aac8e0257aeada71410`, em `ACTIVE`,
com os quatro publicadores APP pausados e nenhuma execução em drenagem.

Decisão operacional: comparadas produção simultânea (mais rápida, duas reservas
concorrentes), recarga prévia (gasto externo desnecessário e dependente de nova
autorização) e produção sequencial (permite conciliar saldo e qualidade por peça).
Escolhida a sequência anúncio → inspeção → demonstração, preservando USD 8 por peça
e USD 4 para revisão dentro do teto humano total de USD 20. Teto não é meta de gasto.

## Preparação editorial sem nova geração

A prévia local reutilizou a captura homologada 118, com o hash já validado, e
as legendas exatas dos dois projetos divididas pelo separador canônico `|`.
A normalização das cinco legendas conserva cada palavra aprovada; os separadores
não alteram o roteiro nem a hipótese. O enquadramento explícito preserva o ajuste,
a autoavaliação e o estado salvo, reservando uma área independente para a legenda.

As duas prévias passaram pelo acabamento real com HTTP/TTS simulados e foram
reproduzidas em desktop, iPhone 15 Pro e Pixel 7, a 1080×1920/15 s, sem estouro
do layout. A inspeção visual confirmou a captura e a legenda separadas. São
prévias técnicas com tom sintético, não vídeos finais ou evidência comercial.
Arquivos locais: `.sandbox/vega-recuperacao/layout-4/` e `layout-5/`.

O backend aplicado apresentou saúde `UP`, revisão `b82834429484` e a ação
`REQUEST_VIDEO_PRODUCTION` no processo correto. O processo permaneceu com
zero de quatro objetivos comprovados e os links dos três subprocessos preservados.

## Aplicação coordenada confirmada

Os três serviços foram aplicados por `execute` na intervenção citada, mantendo
configurações e mounts e preservando tags de rollback. Os hashes portáveis das
imagens locais e carregadas nos hosts coincidiram:

- Backend: `sha256:9b7bd035ac07e2983002ffd0956fcfacc2ed89f07fb7beea0e9076fcc8e510e8`.
- Executor: `sha256:09552779a2a240351679059d732243cf6c27578782c206b0e813117941deb47e`.
- Frontend: `sha256:db96524262435de533dd2225770107200c74c5ec19722a028831fd02010e8bec`.

Backend e banco confirmaram `UP` no endpoint canônico de observabilidade. O
executor iniciou normalmente em 28,738 s, com health `UP`. A primeira consulta
MCP de logs durante o bootstrap retornou `ConnectException`; a nova consulta
funcionou e confirmou o início sem erro. A tela publicou `healthz` com revisão
completa `b8283442948497a181641b81ea0bbd09591415d6`. Nenhum publicador foi retomado.

## Lacuna encontrada na preparação do briefing histórico

Antes de qualquer solicitação de produção, a navegação publicada confirmou que
o editor não permitia adicionar a quinta cena ao projeto original de quatro
cenas. Quebras de linha são normalizadas dentro de uma cena. A matriz anterior
validava planejamento de cinco cortes, referência privada e navegação, mas não
a transição de quatro para cinco cenas no formulário histórico. Nenhum save ou
job foi criado nessas tentativas de preparação.

Alternativas comparadas: trocar o preset e reconstruir o briefing (alto risco de
perder contexto, esforço alto); expor somente um bloco de texto integral (esforço
baixo, edição pouco segura); adicionar uma cena sem tocar nas existentes (esforço
baixo, identidade preservada). Escolhida a terceira, usando o PATCH já existente.
A revisão adiciona o controle ao editor e testes de persistência e limite.

A matriz foi ampliada **antes dos testes** para cobrir o projeto legado com quatro
cenas, quinta cena editável, PATCH, reabertura, preservação de copy/CTA/identidade/
prova, limite de cenas e ausência de render/gasto ao editar. O mesmo percurso deve
passar em desktop/iPhone/Pixel. Duas novas rodadas completas consecutivas são
obrigatórias; `final2`/`final3` permanecem como evidência da revisão anterior.

A rodada `final4` passou com 2.922 testes executados no backend, 170 no executor,
38 no financeiro e 130 no frontend, total **3.260**, sem falhas. Também passaram
os contratos de pacote, MySQL, mídia e navegação nos três dispositivos. O roteiro
visual acrescenta a confirmação de `POST /api/sales-videos/autonomy/v1/cycles`
somente após o PATCH e a reabertura; o POST usa mock e não inicia um provedor.
Os 32 arquivos de implementação/testes foram conferidos por hash antes da rodada
consecutiva `final5`. O limite de cenas usou fixture com os campos obrigatórios
do projeto; sua primeira versão incompleta foi corrigida antes dessas rodadas.

Nova consulta MCP após a primeira rodada confirmou somente os ciclos 12–16 nos
projetos 4/5, todos de preflight isolado, sem `agent_task_id` ou `sales_video_job_id`.
Nenhuma produção paga foi usada para descobrir a limitação de edição.

## Homologação final da edição de cenas

As rodadas completas consecutivas **`final4` e `final5` passaram**, com **3.260
testes executados por rodada** (2.922 backend, 170 vídeo, 38 financeiro, 130
frontend), oito testes opcionais do backend fora da contagem, e os mesmos 32
arquivos de implementação/testes conferidos por SHA-256. MySQL 5.7, contrato de
planejamento, prova privada, FFmpeg/ffprobe, TTS simulado, legendas, empacotamento
e navegação desktop/iPhone/Pixel passaram novamente. O percurso visual inclui
adicionar a quinta cena a quatro cenas persistidas, salvar por PATCH, reabrir
e solicitar o ciclo governado por POST simulado, preservando copy e identidade.

O diff adicional contém somente o controle de cenas, seus testes e registros.
Não houve alteração adicional em Java, configuração publicada ou imagem do
backend/executor. Os Compose temporários foram encerrados com volumes e órfãos.

## Produção solicitada pelo processo

Frontend final `69d86c98e693a1d7e9a9416a2cf31517a4a0c03f` aplicado por `execute`,
com rollback para a imagem anterior; hash portátil local/remoto idêntico
`sha256:adf8089e1c3d86f0685d9a463453d3171d3700441aebe91e26ab3c0cf365c923`.
A imagem também passou na navegação local dos três dispositivos antes da aplicação.

Pelo processo 75/ciclo 2, o projeto 4 recebeu cinco cenas, a captura homologada 118
e legendas segmentadas, preservando as palavras aprovadas, IDs e duração. O PATCH
e o GET privado da prova retornaram 200. O primeiro pedido criou o ciclo audiovisual
17/preflight 10; a validação recusou a direção visual com 1.035 caracteres, acima
do limite 1.000, sem tarefa de agente, reserva ou geração. O montador real reproduziu
o bloqueio localmente. Nenhum limite foi ampliado nem texto truncado.

Alternativas editoriais: retirar restrições (reduz tamanho com risco de falsidade),
alterar roteiro/oferta (muda a variável do teste) ou condensar somente a direção
visual (preserva intenção e copy). Escolhida a última. Os dois projetos passaram
duas vezes pelo montador real na sandbox, com clipes de **966/977 caracteres**
e durações **10/5 s**. O roteiro, a captura e a finalização permaneceram iguais.

O segundo pedido pela tela criou o ciclo **18**, preflight **11**, projeto **4**,
produto **4**, plano **3**, experimento **92**, teto individual **USD 8**. O preflight
ficou **READY**, cotou **USD 1,80** e confirmou saldo de **1.372 créditos**. O backend
criou automaticamente a tarefa financeira **417** e passou a `PENDING_FINANCIAL_REVIEW`.
Os ciclos 17 e anteriores permanecem no histórico, sem nova geração paga até esse ponto.

Plutus concluiu a tarefa **417** com `APPROVED`, `NO_PURCHASE`, estimativa de
USD 1,80 e justificativa de teste privado com ledger incremental segregado.
O backend reservou 800 créditos sob o teto individual e criou o job **21237**
(`RUNWAY_ROUTER`, perfil **59**, modo **TEST**). Apolo validou a captura 118 pelo
controller de vídeo com tenant `default`, antes do planejamento e da geração.
O planejamento usou `gpt-5.6-sol`, `service_tier: flex`, prompt/schema versionados
e cinco cortes. O fornecedor iniciou a tarefa
`45a8f0e1-0530-4375-9e6f-b32cb0c24e57` para o primeiro clipe.

A conferência publicada do processo em desktop/iPhone/Pixel preservou ciclo 2,
experimento 92 e links dos subprocessos 66/62/76, com 0/4 objetivos comerciais
comprovados. Logs do financeiro não retornaram linhas na janela consultada;
a decisão, a tarefa e o encaminhamento foram confirmados pelo banco via MCP.

## Verificação do material real e correção da entrega

O render 21237 concluiu as duas tarefas Runway, com custo conhecido de USD 1,80.
O backend enfileirou o acabamento 21238, gerando MP4 2796, pôster 2797 e VTT 2798.
A captura 118 foi aplicada com hash idêntico, cinco legendas sincronizadas e áudio
pt-BR. FFprobe confirmou 15 s, 1080×1920, H.264/AAC. A reprodução humana/escuta
final continua pendente; a ferramenta desta sessão não recebe áudio como entrada.

A inspeção real revelou duas lacunas que a matriz anterior não cobria: o uploader
não produzia HLS (o teste de mídia terminava no MP4/VTT), e o canal canônico
`SOCIAL_REELS_STORIES` caía no fallback `LANDING_HERO`. O ativo 41 foi cadastrado
nesse papel apesar de o projeto 4 ser o anúncio. O canal/objetivo persistidos,
o callback, o código e o histórico confirmam as causas; não se trata de falta
de credencial. O MP4 responde 200 com user-agent de navegador; o primeiro 403
isolado da consulta Python não comprova indisponibilidade do storage.

Alternativas para entrega: cadastrar uma playlist externa manualmente (esforço
baixo, rastreabilidade fraca); contratar outra transcodificação (integração/gasto
adicionais); gerar e armazenar HLS pelo executor e API de assets existentes
(esforço moderado, sem nova geração IA, mesma governança). Escolhida a terceira.
Para o papel, editar apenas o ativo corrigiria o efeito; inferir pelo título é
frágil; mapear os canais canônicos e rejeitar os desconhecidos elimina o fallback
indevido. A recuperação deve reaproveitar os bytes do MP4/voz/VTT, preservando
IDs comerciais, tentativas, custos e decisões humanas.

Matriz ampliada antes dos novos testes: acabamento real → upload de segmentos e
playlist → callback com URL HLS → ativo correto → reprodução com manifesto e
segmentos HTTP; recuperação de MP4 final sem chamada de IA/TTS/render; hash
obrigatório; idempotência, tenant, falha de upload/FFmpeg, canal social e PDE,
canal desconhecido bloqueado, preservação de custo/aprovação. Tela desktop,
iPhone e Pixel deve solicitar somente preparação da reprodução, mantendo MP4
como fallback. Duas novas rodadas locais completas serão executadas após a
última correção; nenhuma nova geração paga é necessária para validar a entrega.

A rodada `final6` passou com 3.275 testes executados e entrega HLS local, mas a
conferência preventiva no navegador publicado identificou ausência de CORS no
bucket. A API S3 confirmou `NoSuchCORSConfiguration`; o navegador da origem
`http://191.252.181.168:5173` recusou leitura mesmo com MP4 íntegro. A documentação
[oficial do R2](https://developers.cloudflare.com/r2/buckets/cors/) confirma que a
política deve permitir a origem do aplicativo; disponibilizar o objeto por URL
não basta para leitura por JavaScript.

Alternativas: proxy de todos os segmentos pelo backend (custo operacional e
acoplamento maiores); liberar qualquer origem (escopo desnecessário); acrescentar
somente leitura para as duas origens operacionais conhecidas do Hub (menor
exposição e esforço). Escolhida a terceira, preservando quaisquer regras
existentes e uma cópia de retorno. Não foi criada origem para uma versão PDE
ainda não publicada. A integração futura deve cadastrar seu domínio aprovado.

O script versionado `scripts/configure-video-read-cors.py` confere o hash do estado
anterior antes de aplicar; a preparação local não altera o bucket. A origem do
storage no teste HLS agora usa outra porta, exigindo CORS real no navegador. As
rodadas finais serão `final7` e `final8`, incluindo esse contrato. Snapshot anterior
sem regras: SHA-256 `9edcb11e5d84ed338440e19f54db62a34ac92640f9b691079f59ed93625d85be`.
Candidata somente GET/HEAD nas origens `http://191.252.181.168:5173` e
`http://191.252.181.168`: SHA-256
`ddd939571f79f4c79a0c9dab3faca62974a6139d9bfc4465fd59561346d84cf1`.

As rodadas `final7` e `final8` passaram com 3.275 testes cada, incluindo CORS entre
origens diferentes. Na revisão arquitetural do diff, a disponibilidade do novo
botão ainda era deduzida na tela a partir de provider/status/filho. Alternativas:
sempre oferecer o botão e recusar no POST (baixo esforço, experiência confusa),
manter inferência local (baixo esforço, divergência de responsabilidade) ou expor
comando elegível/pendente/indisponível no backend (esforço pequeno, contrato único).
Escolhida a terceira, conforme regra de verdade da tela. O job agora informa
`deliveryPreparation`, incluindo texto congelado e a execução já em curso.
A matriz foi ampliada para negar recuperação de arquivo sem hash e para garantir
que o frontend não oferece o comando quando o backend o declara indisponível.
As novas rodadas finais são `final9` e `final10`; nenhuma alteração adicional foi
aplicada em runtime durante essas verificações.

A rodada `final9` identificou uma violação ArchUnit: o DTO legado do job não pode
depender de classes em `service`. A decisão de entrega foi mantida como `record`
no próprio contrato existente `SalesVideoJobDto`, sem afrouxar a regra nem criar
outro controller. O backend continua sendo responsável por calcular a decisão.
A falha ocorreu somente na sandbox. A contagem reinicia nas rodadas `final11` e
`final12`, após teste focado incluindo as regras de arquitetura completas.

## Homologação final da recuperação de entrega

As rodadas completas consecutivas **`final11` e `final12` passaram** com **3.279
testes executados por rodada**: 2.934 backend, 173 executor de vídeo, 38 financeiro
e 134 frontend. Oito testes opcionais do backend não entram nessa contagem.
Também passaram MySQL 5.7, contratos de CI e pacote, CORS, integrações HTTP de
preflight/financeiro, FFmpeg, recuperação de bytes e reprodução em desktop,
iPhone 15 Pro e Pixel 7 simulados, incluindo origens diferentes para HLS.
Os mesmos 31 arquivos de implementação/testes foram conferidos por SHA-256;
manifesto de fontes `894e2c5c3e543c14e3bea285fac0c7f0c2594ea48f038f135343c0c3236a7cdb`.

Antes do commit, a imagem candidata do Dockerfile do executor também passou por
`pending → claim → download de MP4/VTT por hash → HLS → seis uploads → callback`,
com um único claim, um callback, nenhum erro e nenhuma geração externa.
O contrato de preflight da imagem passou nos dry runs de 10/5 segundos.
As **138 classes** empacotadas conferem byte a byte com as classes testadas;
JAR SHA-256 `67b5c5baf4ca218bf0811c55d6f3da38e4607198c3595745abf841492597b243`.
A imagem da tela, construída com a configuração de API da publicação, passou
nos três dispositivos: preservação da prova, quinta cena e comando HLS informado
pelo backend, com uma requisição simulada por dispositivo e zero escritas externas.

Relatórios por rodada ficam em `artifacts/video-production-continuity/final11`
e `final12`, com contagens, logs, capturas e mídia sintética. Os XMLs foram
compactados com conferência dos hashes individuais, sem descarte de evidência.
A aprovação técnica da recuperação não é aprovação dos vídeos para uso comercial.

## Aplicação e recuperação confirmada do anúncio em 14/09/2026

A revisão local `a4acf26515f1121b1e012ef4594efbee962ebd7c` foi construída pelos
Dockerfiles versionados e aplicada, após a homologação completa, pelo coordenador
`405517f219104aac8e0257aeada71410`, escopo `app`, usando `execute`. Backend,
frontend e executor ficaram saudáveis. Nenhum PR, push ou Actions foi usado para
testar a alteração. Cada aplicação preservou uma tag de rollback da imagem anterior.

Os conteúdos portáteis local/remoto conferem, apesar da conversão de formato do
Docker alterar o identificador externo da imagem:

| Serviço | Tag | SHA-256 do conteúdo portátil |
| --- | --- | --- |
| Backend | `marketing-hub/backend:vega-cycle6-a4acf26515f1` | `0c5adc6c38f002e1b4c395b66b7beefd621c880dfdf95d4bf1e4f7eda08442cf` |
| Frontend | `marketing-hub/frontend:vega-cycle6-a4acf26515f1` | `29d6664026143ac6472e160e29bb716307a6b5ab69933deac513b4c6ccc4fe68` |
| Vídeo | `marketing-hub/video-management:vega-cycle6-a4acf26515f1` | `a95776ab2db414d00844f0846c13191c3dcc2dbf2a4efab97553ad0e6a09aa64` |

O CORS foi aplicado pelo script versionado sob o mesmo coordenador, conferindo
os hashes anterior e candidato já registrados. O MP4 respondeu com a origem
administrativa autorizada. A sandbox chegou ao limite de espaço após os builds;
somente cópias temporárias desta tarefa foram removidas após comparar seus hashes
com imagens ou relatórios compactados preservados. Nenhum recurso alheio foi limpo.

Pelo processo 75 → ciclo 2 → projeto 4, o comando **Preparar reprodução HLS** fez
um único POST oficial e criou o job **21239**, filho do acabamento **21238**, em
modo `TEST`. O executor registrou reaproveitamento por hash, dois segmentos e
playlist **2809**. O callback concluiu o job com MP4 **2804** e manteve o ativo
comercial **41**, agora no papel correto **AD**, com revisão **PENDING**.

O novo MP4 tem exatamente o hash do anterior:
`29fbb7ce12675bb323f32219e613f2f56beb07f11d825ae216ebe663ca6dfcb2`.
O metadado registra `delivery_only=true`, `generation_repeated=false` e custo
incremental de geração zero. O ciclo audiovisual **18** mantém custo conhecido
de renderização **USD 1,80**. Voz registra `PENDING_PROVIDER_RECONCILIATION`;
portanto, esse valor não deve ser apresentado como custo total conciliado de IA.

A navegação publicada passou em desktop, iPhone 15 Pro e Pixel 7 simulados:
MP4 de 15 s, HLS de 15,010666 s, H.264/AAC, 1080×1920, reprodução e avanço para 6 s,
sem erro de mídia ou escrita de aprovação. O teste HLS usa um player de diagnóstico
na origem real administrativa, lendo manifesto/segmentos no storage com CORS;
isso não comprova integração do vídeo numa página PDE publicada. A captura
homologada 118 continua visível e legível no trecho demonstrado.

Evidências operacionais desta sessão: `ad-hls-request.json`, `ad-recovery-runtime.json`,
`mcp-job21239-logs.json` e `real-delivery-4/results.json`, em
`.sandbox/vega-recuperacao`. O MCP confirmou o percurso, incluindo a mensagem
“Acabamento reaproveitado sem geração” e a conclusão com a playlist persistida.

O comando **COMPLETE** de `CAMPAIGN_VIDEO` foi registrado pela tela com ativo 41,
evidência técnica e identificação de Codex como executor da recuperação autorizada.
O backend avançou o mesmo ciclo **2** para **PDE_ENTRY_VIDEO**. Essa conclusão
prova produção técnica; não constitui aprovação humana, campanha, cobrança ou venda.

## Solicitação governada da demonstração

Pela entrada do mesmo processo, o projeto **5**, perfil **60**, preservou produto 4,
plano 3, experimento 92, canal `PDE_HERO_DIAGNOSTIC`, versão v12, roteiro, CTA e
identidade. Foram salvas as cinco cenas e a prova homologada 118 pelo PATCH da tela,
com conferência posterior. O pedido de produção criou ciclo audiovisual **19**,
preflight **12**, teto individual **USD 8** e tarefa financeira **418**.

O preflight ficou `READY`: dois clipes de 10/5 s, 180 créditos, estimativa
**USD 1,80**, saldo oficial de 1.192 créditos e reserva **4** de 800 créditos.
Plutus aprovou produção privada, sem compra de créditos nem mídia. O backend
enfileirou o job **21240** de Apolo. Os logs via MCP confirmam a captura íntegra,
com hash `58320a0f1f7205d78a4e5f9d771d6eea6ca9ef8b6edaf9584a17cd74a770b339`,
e planejamento com prompt/schema versionados em modo Flex. Nenhum job pago anterior
foi repetido para iniciar a demonstração.

## Falso bloqueio de Apolo e ampliação da matriz

O job **21240** parou em `APOLLO_STORYBOARD_BLOCKED`, antes de chamar Runway,
porque o detector interpretou “sem apontar, celebrar, depor ou mostrar tela,
logo, letras ou CTA gerados” como solicitação positiva. Request, response e plano
foram preservados no evento **79184**. A comparação com o sucesso **21237**,
evento **79086**, e o precedente **21217** em `loops.md` confirma a recorrência:
o detector removia pontuação e reconhecia negação apenas numa janela curta.

Alternativas: reescrever o briefing e repetir a IA (baixo esforço, recorrência
provável), adicionar outra IA para revisar a interpretação (custo/latência e
nova incerteza) ou corrigir o detector e reproduzir as respostas históricas
(esforço moderado, causa fechada sem alterar o storyboard). Escolhida a terceira.
O contrato permanece proibindo texto gerado; cada ordem positiva é avaliada
separadamente, preservando a pontuação e listas de verbos explicitamente negadas.

Matriz ampliada **antes dos testes**: replay completo dos storyboards 21237 e
21240 com cliente de IA simulado, auditoria idêntica, negação simples/listada,
conjunções, ordem positiva após ponto/contraste/outra proibição, custo e duração
inalterados. A suíte integral anterior, MySQL, HLS, mídia e três dispositivos
continua obrigatória. A contagem das rodadas reinicia em **final13/final14** após
esta correção; nenhuma repetição paga foi solicitada para testar o detector.

Os 32 testes focados passaram, incluindo os dois replays completos. A rodada
`final13` não iniciou testes: o MySQL local acusou `OS error 28 / No space left
on device` na criação de `ibdata1`. A reprodução preservou o log e encerrou o
Compose. A topologia efêmera passou a usar um override local de **tmpfs de 256 MB
somente para `/var/lib/mysql`**, mantendo MySQL 5.7, SQL, credenciais sintéticas,
endpoints, verificações e projeto Compose exclusivos. Isso não altera o banco
publicado nem substitui MySQL por um test double. As rodadas finais passaram a
ser **final14/final15**; durabilidade de disco não é critério desta correção de
interpretação textual, e nenhum changelog foi alterado.

A `final14` passou com **3.301 testes executados** (2.934 backend, 195 vídeo,
38 financeiro, 134 frontend), além dos contratos e da matriz de mídia/navegação.
Os 56 arquivos de implementação/testes permanecem iguais ao manifesto
`9de85f501a5b0b40bd1086dce1cc5c4d4df1c2bf1e211ae70a5f072855f488e9`.
A imagem candidata contém as mesmas **139 classes** testadas; JAR SHA-256
`c97699d61c8c7a2548c6ff7cd56fd740ca9c1c70851696bcbb9fc5147e574cec`.

Custos da tentativa bloqueada não foram apagados: Plutus 418 tem estimativa
persistida de **USD 0,112836**; o planejamento 21240 registra 12.345 tokens de
entrada e 11.699 de saída. Pelo catálogo persistido, a tarifa standard conservadora
representa **USD 0,28336**, embora a chamada tenha usado Flex; não é valor faturado.
A reserva de Runway foi liquidada com zero créditos e nenhuma tarefa do provider.
A próxima solicitação terá teto reduzido de **USD 6**, dentro dos USD 8 originais
da peça e dos USD 20 conjuntos, preservando margem para os custos auxiliares.

As rodadas completas consecutivas **final14 e final15 passaram**, ambas com
**3.301 testes executados**, zero falhas e os mesmos 56 arquivos conferidos por
hash. MySQL 5.7 com dados efêmeros, integração financeira, preflight, entrega HLS,
FFmpeg, reprodução/avanço e navegação nos três dispositivos passaram nas duas.
Os Compose foram encerrados com volumes e órfãos. O replay também foi executado
na JVM da **imagem candidata**, carregando as classes do seu JAR, sem rede:
os planos 21237/21240 passaram e ordens positivas continuaram bloqueadas.
Esse teste não chamou IA, provider de vídeo ou banco publicado.

O diff foi revisado: alteração produtiva adicional restrita ao classificador
e à preservação de pontuação na chamada do planejador; fixtures, regressões e
documentação correspondentes. Nenhuma mudança nova no backend, frontend,
prompt, schema, orçamento aprovado ou gate humano. A revisão anterior da
entrega HLS permanece válida; a nova imagem altera somente o executor.

## Falha de produção invisível no processo pai — matriz antes dos testes

Depois da recuperação do detector, o MCP confirmou ciclo audiovisual 19 em
`APOLLO_BLOCKED`, job 21240 e falha `APOLLO_STORYBOARD_BLOCKED`. O anúncio/ciclo
18 continuava `VIDEO_READY_FOR_REVIEW`. A API e a tela do processo 75/run 4,
porém, retornavam `WAITING_ACTIVITY` sem ação: `ProcessRunVideoGuidance` tratava
somente os estados de preflight. Esse é um novo caso do loop de espera enganosa.

Alternativas: orientar a navegação manual até a lista de projetos (esforço baixo,
não evita recorrência), repetir automaticamente a produção (reduz intervenção,
mas aumenta custo sem resolver a causa) ou projetar a interrupção persistida no
processo pai (esforço pequeno, ação clara sem alterar gates). Escolhida a terceira.

A matriz adicional cobre, antes da execução: bloqueio do planejamento com
preflight vencido ou ausente; bloqueio sem job e bloqueio financeiro; projeto,
papel, versão e experimento corretos; ausência de payload bruto na orientação;
nova tentativa ativa ou concluída substituindo a falha histórica; persistência
do estado/motivo, pausa/retomada, contexto copiado e navegação nos três dispositivos.
Nenhuma leitura pode criar tarefas, aprovar ou consumir. Os dados continuam
sintéticos no MySQL 5.7 local. A suíte integral, mídia, HLS, custos e integrações
anteriores será repetida nas rodadas finais 16/17 após a última correção.

O ciclo audiovisual 20 foi bloqueado corretamente por Plutus 419: o teto
incremental reduzido de USD 6 era inferior à soma dos dois tetos configurados
(USD 8). Não houve job nem reserva. A autorização humana vigente continua em
USD 20 conjuntos. O ciclo 21 restabeleceu os USD 8 originalmente previstos para
a demonstração, preservando custo anterior e margem para revisão. Plutus 420
aprovou; o job 21241 recebeu HTTP 429 da Responses API antes da Runway.

O MCP confirma `WebClientResponseException$TooManyRequests`; o conector perdia
o corpo do erro e convertia qualquer falha em `APOLLO_STORYBOARD_BLOCKED`.
Não é possível afirmar se esse 429 foi capacidade Flex, limite de taxa ou quota
financeira, pois o corpo não foi preservado. Os sucessos 21237/21240 comprovam
que o mesmo modelo/contrato já funcionou; não há evidência para trocá-los.

Alternativas: repetir todo o ciclo (novos pareceres e diagnóstico perdido),
trocar para processamento standard (possível custo maior sem tratar quota) ou
auditar cada resposta e tratar somente rejeições temporárias explícitas no
executor (preserva custo, modelo, Flex e causalidade). Escolhida a terceira.
[Flex](https://developers.openai.com/api/docs/guides/flex-processing) documenta
backoff para indisponibilidade; [erros da API](https://developers.openai.com/api/docs/guides/error-codes)
distinguem taxa de quota/saldo e exigem respeito a Retry-After.

Matriz adicional definida antes de testar: 429 temporário seguido de sucesso,
três rejeições consecutivas, Retry-After numérico/data/maior que a janela local,
429 de quota ou código desconhecido, 400/401/403/500, transporte ambíguo,
resposta inválida, persistência de request/response/status/request-id por tentativa,
mesmo modelo/Flex e nenhum render após falha. O pai também deve expor
`FINANCIAL_BLOCKED`. Depois dessas correções as duas rodadas integrais recomeçam;
a rodada 16 em andamento não vale como rodada final desse escopo ampliado.

A rodada 16 terminou sem falhas (3.307 testes), antes da ampliação HTTP.
Na revisão do caminho completo, o contrato canônico também revelou que
`VIDEO_APPROVAL` exige intervenção explícita e era excluído da orientação do pai.
A matriz passa a conferir a chegada nessa etapa sem fingir trabalho automático,
aprovar vídeo, integrar contrato ou publicar por simples mudança de status.

Os testes focados da integração HTTP e do planejador passaram (31 testes).
A imagem candidata foi exercitada em JVM real, sem rede externa, com servidor
HTTP local: rejeição temporária, sucesso e quota permanente; auditoria, Flex
e limite de tentativas comprovados. As **140 classes** empacotadas são idênticas
às compiladas para os testes, assim como prompt e schema; JAR SHA-256
`a2a7b747a2d8123eee875d3e4305c5c1108eefaf72a87e10f67259dd6a81aa32`.
O manifesto final de 62 arquivos de código/configuração/testes tem SHA-256
`3c555f2c8bfdf979169bfe0ebaa9debfd9a25d123df7bdb4603795c71e0979f6`.
As rodadas completas deste escopo são `final17` e `final18`.

A rodada `final17` revelou uma incompatibilidade na fixture de integração:
`VerifyWorker` ainda sobrescrevia a assinatura anterior do cliente, sem o
callback de auditoria. A fixture foi atualizada e seu contrato focado passou.
Nenhuma publicação foi feita com esse problema pendente. A contagem recomeçou;
as rodadas finais passam a ser `final18` e `final19`. O manifesto dos mesmos
62 arquivos, incluindo a fixture corrigida, tem SHA-256
`5c8b1958998b27f7963e2e6f3c2fca6eed1ccfad82064fb8cedfeb8db3a5733a`.
Os bytes produtivos da imagem candidata permaneceram inalterados.

Consulta MCP em 2026-09-14 03:43 UTC confirmou que nenhuma tentativa posterior
ao ciclo 21 foi criada. Plutus 417/418/419/420 soma USD 0,436976 estimados;
o anúncio consumiu USD 1,80 de Runway e o planejamento histórico tem estimativa
conservadora de USD 0,646416. Esses valores não são uma fatura consolidada:
há custo de voz ainda não conciliado. As falhas 21240/21241 não criaram tarefas
Runway e não autorizam omitir os custos anteriores.

`final18` concluiu a matriz sem falhas: 2.942 testes executados no backend,
214 no executor de vídeo, 38 no financeiro e 135 na interface, totalizando
3.329. Oito testes opcionais do backend ficaram fora do total executado.
Também passaram os 24 cenários REST de preflight/orientação, MySQL 5.7,
empacotamento, contratos de imagens, prova privada, voz simulada, FFmpeg/ffprobe,
MP4/HLS e navegação em desktop, iPhone 15 Pro e Pixel 7. O manifesto foi
conferido novamente antes de iniciar `final19`, sem mudanças nos arquivos.

A releitura dos subprocessos 66/v5, 62/v4 e 76/v4 confirmou a referência
`experiment:92` e a preservação dos vínculos. Operação e entrega continuam
aguardando seus gates; o subprocesso de aprendizado registra cinco atividades
concluídas e aguarda `PDE_ENTRY_VIDEO`. Essa contagem do subprocesso não equivale
a comprovar os quatro objetivos do processo pai de venda, entrega e aprendizado.

`final19` concluiu a mesma matriz, também com **3.329 testes executados sem
falhas**, consecutivamente a `final18`. O manifesto de 62 arquivos permaneceu
idêntico. As duas execuções passaram pelos contratos REST, mídia e pelos três
dispositivos. O diff final foi revisado, incluindo comentários Java, isolamento
dos dados, ausência de novas migrações e manutenção de todos os gates.
O Compose exclusivo encerrou containers, volumes e órfãos após cada rodada.
O MCP confirmou zero jobs de vídeo em execução antes da aplicação; o coordenador
permaneceu `ACTIVE`, sem publicador habilitado ou execução em drenagem.
