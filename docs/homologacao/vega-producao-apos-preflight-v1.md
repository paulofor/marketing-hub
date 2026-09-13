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
