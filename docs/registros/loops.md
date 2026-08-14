# Registros de loops operacionais — Experimentos

> Documento auxiliar de prevenção de recorrência.
>
> Objetivo: registrar pontos em que o Marketing Hub entrou ou pode entrar em ciclos repetidos de correção, retrabalho ou diagnóstico incompleto.
>
> Fonte inicial: análise do histórico `docs/registros/experimentos.md` em 2026-06-17.
>
> Uso obrigatório recomendado: antes de corrigir problema em GeraLanding, Facebook Ads, Lead Portal, OpenAI/schema, pipelines administrativos ou pipeline de hipótese, verificar se a solicitação reabre algum loop listado aqui.

## LOOP-GROWTH-OPERATOR-OBSERVABILIDADE-001 — backlog sem diagnóstico do executor

- **Sintoma:** o `growth-operator-worker` deixa de consumir a fila, mas o MCP não oferece health nem logs próprios do executor.
- **Causa-raiz:** o worker não publicava porta, logfile persistente ou endpoints operacionais; o deploy validava apenas processo e identidade Codex.
- **Correção efetiva:** exposição HTTP versionada de health/logfile, aliases no MCP e validação obrigatória dos endpoints durante o deploy.
- **Prevenção:** testes de contrato preservam host e rotas; o deploy falha se health ou logfile não responderem.

## LOOP-GROWTH-OPERATOR-AUXILIARY-QUEUE-STARVATION — ciclo automático bloqueia pendências

- **Sintoma:** execuções de Planos Comerciais permanecem `PENDING` embora o worker esteja saudável.
- **Causa-raiz confirmada em 2026-08-12:** antes de reservar a fila principal, o scheduler tentava criar um ciclo automático para um plano configurado. O HTTP 409 esperado quando esse plano não possuía experimento `RUNNING` encerrava o polling e impedia o consumo de pendências independentes de Agenda Cheia e MUSA.
- **Correção efetiva:** a avaliação do ciclo automático continua auditável, mas sua falha fica isolada; o worker sempre tenta reservar a fila principal no mesmo polling.
- **Prevenção:** teste unitário exige `claimPending()` mesmo quando `ensureAutomaticCycle()` falha, impedindo uma fila auxiliar de causar starvation global.

## LOOP-COMMERCIAL-PLAN-WITHOUT-NEXT-ACTION — plano aberto esquecido sem agente

- **Sintoma:** plano comercial permanece aberto e bloqueado, mas nenhum agente possui execução pendente ou em andamento.
- **Causa-raiz confirmada em 2026-08-12:** o worker garantia continuidade somente para um `commercialPlanId` configurado e o backend recusava criar novo diagnóstico quando não existia experimento `RUNNING`.
- **Correção efetiva:** a cada polling, o backend reconcilia todos os planos não encerrados e cria ciclos idempotentes mesmo sem experimento ativo; experimentos compatíveis continuam compondo um portfólio, sem vínculo exclusivo.
- **Prevenção:** teste do worker exige varredura global antes da reserva da fila e teste do backend deve excluir apenas planos concluídos ou cancelados.

## LOOP-COMMERCIAL-PLAN-EXPERIMENT-IDENTITY-RECURSION — portfólio derruba a tela de planos

- **Sintoma:** `GET /api/planning/commercial-plans` retorna HTTP 500 depois que o plano passa a expor o portfólio de experimentos.
- **Causa-raiz confirmada em 2026-08-12:** o `LinkedHashSet` do portfólio acionava o `hashCode` gerado pelo Lombok para `Experiment`; esse método percorria associações JPA e entrava no ciclo `Hypothesis` ↔ `DeliverablePackage`, terminando em `StackOverflowError`.
- **Correção efetiva:** igualdade e hash de `Experiment` usam somente identidade persistida e classe Hibernate, sem percorrer associações.
- **Prevenção:** teste unitário monta um grafo bidirecional e exige que inserir o experimento em conjunto não cause recursão; entidades JPA usadas em conjuntos não podem herdar igualdade/hash baseada no grafo completo.

## LOOP-EXPERIMENTO-FAKE-CONTABILIZADO-COMO-HUMANO — Métricas de homologação

- Sintoma: sessões de homologação do experimento fake apareciam como humanas para o Operador de Crescimento.
- Causa-raiz: a classificação dependia apenas de URL, viewport e user-agent, sem considerar o tipo canônico `FAKE_EXPERIMENT`.
- Prevenção: toda sessão vinculada a experimento fake é classificada como automação, preservando os eventos para auditoria técnica sem contaminá-los como evidência comercial.
- Contrato: teste garante zero sessões humanas no analytics de experimento fake.

## LOOP-OPERADOR-SEM-EVIDENCIA-DE-ENTREGA-DA-MICROAMOSTRA — Inteligência incompleta

- Sintoma: o pacote era gerado, entregue e aberto, mas o Operador afirmava que essas etapas não estavam comprovadas.
- Causa-raiz: a evidência congelada continha apenas eventos da landing e não incluía o estado persistido dos pacotes de imagem.
- Prevenção: a inteligência de sessão também expõe pacotes solicitados/concluídos, imagens planejadas/geradas, ZIP, envio e abertura, sempre marcados como auditoria técnica quando o experimento é fake.
- Proteção financeira: enquanto os campos legados de custo do pacote não forem canônicos, o Operador recebe `generationCostAvailable=false` em vez de interpretar o preço comercial de R$ 67 como custo em USD.

## LOOP-AGENT-RUNNING-WITHOUT-PROGRESS — Agentes Codex

- Sintoma: execução permanece `RUNNING`, mas não é possível comprovar se o processo Codex está vivo ou avançando.
- Causa-raiz: o job registrava apenas início, fim e timeout; a atividade intermediária existia somente no processo local do worker.
- Prevenção: telemetria canônica com heartbeat de 15 segundos, PID, processo vivo, linhas/eventos, bytes de saída, tokens quando informados e detecção de atraso após dois minutos.
- Contrato: `docs/canonical/codex-agent-execution-telemetry-canon.v1.md` e tool MCP `codex_agent_execution_telemetry`.
- Fechamento complementar no Operador de Crescimento (2026-08-07): o ciclo automatico recupera `RUNNING` sem processo vivo e heartbeat recente, registra falha auditavel e libera nova execucao; o plano tambem e reconciliado apenas com um experimento ativo compativel, evitando repetir analise de experimento encerrado.
- Fechamento complementar em planos legados (2026-08-08): quando hipotese e nicho nao estao gravados diretamente no plano, a reconciliacao usa o contexto do experimento ja vinculado; teste de contrato cobre o caso real do plano 2 com troca do experimento 84 para o 85.
- Fechamento complementar no Aprovador Meta (2026-08-09): o worker reservava três criativos de uma vez, marcava todos como `PROCESSING`, mas executava o Codex sequencialmente. Um item lento multiplicava o tempo do lote e ocultava quais revisões realmente estavam em execução. O lote agora usa tarefas virtuais concorrentes, mantendo timeout, telemetria, callback e falha isolados por criativo; teste de contrato exige início simultâneo dos três itens.
- Fechamento complementar no Aprovador Meta (2026-08-09): reinício ou timeout do worker deixava revisões indefinidamente em `PROCESSING`, pois a reserva não possuía lease. O backend agora persiste início e recuperações, reenfileira leases órfãos com limite e encerra em `FAILED` com causa auditável após reincidência.
- Fechamento complementar no MCP do Aprovador Meta (2026-08-14): a telemetria genérica existia, mas exigia conhecer que a execução era correlacionada pelo `creativeId` e não cruzava o heartbeat com o parecer canônico nem com a memória governada. A tool `meta_ad_approver_execution_telemetry` passa a consolidar parecer, processo, atividade, bloqueio e contagens separadas de memórias confirmadas e candidatas; testes preservam o contrato e impedem tratar memória candidata como consenso.
- Fechamento transversal de prontidão Codex (2026-08-14): Apolo e Argos concluíam o OAuth, mas permaneciam `UNKNOWN` porque confirmação de autenticação e heartbeat periódico não faziam parte do mesmo gate global usado pelos agentes anteriores. O padrão agora exige reporter em até 60 segundos, `codex login status`, acesso ao backend, versão e build para todo agente Codex atual ou futuro; cadastro e teste arquitetural impedem adicionar executor sem essa prova operacional.
- Fechamento complementar no Aprovador Meta (2026-08-14): a reconexão Codex por device code podia ocupar por até 16 minutos a única thread do agendador Spring e impedir Têmis de consumir pareceres pendentes. O executor agora mantém pool mínimo de duas threads, com teste contratual que preserva a independência entre autenticação e revisão.
- Fechamento complementar nas mesas e planos comerciais (2026-08-11): o limite fixo de 40 minutos encerrava Atena e Dédalo mesmo com atividade, leases antigas podiam permanecer `RUNNING` fora do item mais recente e uma falha na fila auxiliar de vídeo impedia Plutus de consumir sua fila financeira. O timeout passa a medir inatividade com teto absoluto, leases órfãs recebem uma única retomada com a entrada congelada e filas independentes falham isoladamente. Testes de contrato impedem transformar timeout recuperável em falha definitiva e impedem a fila de vídeo de causar starvation financeiro.
- Fechamento complementar em dossiês e monitor (2026-08-11): cadastrar oportunidade não abria execução consumível por Argos e o monitor não reconhecia `FALHA` como estado terminal de Dédalo. Cada dossiê agora cria ciclo de descoberta e tarefa correlacionada; conclusão sincroniza evidências reais, falha bloqueia a mesa e o monitor trata status terminal ou inatividade como bloqueio, impedindo trabalho fantasma.
- Fechamento complementar em pareceres de oportunidade (2026-08-13): recriar uma tarefa administrativa para Atena não alterava `opportunity_agent_review`, que é a fila realmente consumida pelo worker, deixando o dossiê sem nova execução. O painel agora reenfileira o parecer canônico vinculado ao dossiê e ao agente, impede duplicidade durante `RUNNING` ou após conclusão e expõe o identificador da execução; testes de backend e frontend preservam o vínculo ponta a ponta.
- Fechamento de observabilidade em pareceres de oportunidade (2026-08-13): o health `READY` de Atena era exibido isoladamente e mascarava a execução canônica `FAILED`. O monitor agora prioriza `opportunity_agent_review`, expõe dossiê, execução e último erro persistido, enquanto a tela apresenta `READY — parecer bloqueado` quando o executor está saudável mas o parecer não está.
- Fechamento transversal em pareceres de oportunidade (2026-08-14): a observabilidade anterior cobria apenas Atena e não alertava `PENDING` sem início. O monitor agora cruza a execução canônica de Atena, Psique, Plutus e Hermes, alerta após três minutos sem reserva e comprova por contrato que leases órfãs dos quatro agentes recebem uma única retomada automática após indisponibilidade transitória do backend.
- Fechamento complementar no worker de Atena (2026-08-14): o reenfileiramento canônico do dossiê #6 permanecia `PENDING` porque pesquisa estratégica, parecer de oportunidade e reconexão Codex compartilhavam o agendador padrão de uma única thread; uma execução Codex bloqueante impedia o polling independente do parecer. O worker passa a reservar três threads, uma por rotina concorrente, e um teste de contrato impede regressão para fila única.
- Fechamento complementar em Dédalo (2026-08-12): a execução do experimento #88 já havia sido gravada como `FALHA` por uma versão antiga do worker antes da política de preservar a lease em timeout. Como a recuperação consultava apenas `PROCESSANDO`, esse registro terminal nunca voltava à fila. O backend agora reconhece exclusivamente o erro legado de timeout, limpa o estado terminal e permite uma única retomada auditável; o marcador persistido impede repetição infinita.
- Fechamento complementar no AI Worker (2026-08-14): o experimento #88 permaneceu `PLANNED` com execuções de `landing-page-image-planning` em `INICIADO`, embora o endpoint `pending` respondesse três itens em aproximadamente 582 KB. O worker possuía 31 rotinas `@Scheduled` sobre o agendador padrão de uma única thread; uma integração bloqueante podia impedir todas as filas independentes de executar. O executor passa a usar pool concorrente configurável, com mínimo de duas threads e teste preventivo, preservando isolamento entre etapas.

## LOOP-CUSTOMER-AGENT-UNSTRUCTURED-EXECUTION — Avaliação sem parecer final

- Sintoma: processo permanece vivo até o timeout, com saída mínima de diagnóstico e sem parecer persistível.
- Causa-raiz: a avaliação passava o prompt como argumento e misturava stdout operacional com a resposta funcional, apesar de existir schema versionado não aplicado ao comando.
- Prevenção: entrada por stdin, `--output-schema`, resposta final em `--output-last-message`, validação JSON antes do callback e teste de contrato do comando.
- Contrato: `docs/canonical/customer-agent-personas-canon.v1.md`.

## LOOP-PDE-EVIDENCIA-VAZIA — Descoberta PDE

- Sintoma: pesquisa real sem resultados termina em HTTP 400 no callback `complete` e o ciclo aparece como falha.
- Causa-raiz: o worker corretamente não fabrica evidência, mas o contrato backend exigia ao menos uma oportunidade.
- Prevenção: aceitar lista vazia de oportunidades, concluir o ciclo auditavelmente como pesquisa insuficiente e manter teste de contrato que proíba a reintrodução de fallback artificial.

## LOOP-PRODUCT-AI-PAID-DELIVERY-CONTRACT-DRIFT — Entrega paga sem template ativo

- **Severidade**: CRÍTICO.
- **Status**: fechado localmente em 2026-08-09; aguarda publicação.
- **Sintoma**: o worker consulta `personalizedsample.v1/paid-delivery` e recebe HTTP 409, impedindo a entrega posterior à compra aprovada.
- **Causa-raiz confirmada**: o Liquibase registrava o changeset v1 como executado, mas o catálogo persistido não continha nenhuma versão do contrato; reaplicar o changeset antigo não repararia o drift.
- **Correção efetiva**: criar versão v2 idempotente em novo changeset, ativar explicitamente o contrato e proteger modelo, schema e inclusão relativa com teste preventivo.
- **Prevenção**: nunca editar changeset já executado para recuperar template ausente; criar nova versão idempotente e manter a ausência do contrato como bloqueio explícito, sem fallback genérico.

## LOOP-GERALANDING-PRESET-NAO-CONVERGE-HTML — Dédalo repete falha estrutural do preset

- **Sintoma:** Têmis reprova versões sucessivas porque classes flex posteriores sobrescrevem grids desktop, embora Dédalo recomende corrigir o HTML.
- **Causa-raiz confirmada em 2026-08-14:** `LANDING_PAGE_HTML` era encaminhado novamente ao preset; a autoridade declarada de reconstrução integral não possuía executor registrado nem contrato de resultado.
- **Correção efetiva:** `CODEX_CODE_IMPLEMENTATION` passa a aceitar HTML completo de Dédalo, validado e persistido pelo backend como rascunho, preservando CTA e checkout e retornando automaticamente a Têmis.
- **Prevenção:** schema e validações do worker exigem HTML integral somente nessa abordagem; o backend bloqueia scripts e mudança do contrato comercial.

## Regra operacional de uso

## LOOP-APOLO-LEGACY-LUMA-RESELECTION — plano legado reintroduz provider reprovado

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-13; aguarda publicação.
- **Sintoma:** após falha 402 e score comercial insuficiente, novos ciclos MUSA voltavam à Luma porque o seletor priorizava a menção antiga do projeto e usava Luma como padrão para vídeos longos.
- **Causa-raiz confirmada:** a preferência estava hardcoded no backend e contradizia a decisão comercial e o conhecimento operacional de Apolo.
- **Correção efetiva:** Luma deixa de ser selecionável pelo ciclo autônomo MUSA; Seedance 2.5 via Runway passa a ser o padrão, e a decisão é registrada no cânone e no prompt versionado de Apolo.
- **Prevenção:** teste de contrato usa um projeto legado que ainda cita Luma e exige que o job seja criado como `RUNWAY_SEEDANCE_2_5`.

## LOOP-AGENT-DEPLOY-GLOBAL-REVISION-MARKER — deploy saudável reportado como falha

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-13; aguarda publicação.
- **Sintoma:** o deploy de Atena ou Hermes conclui o rebuild e confirma saúde, mas o GitHub Action falha na etapa final de relatório.
- **Causa-raiz confirmada:** após a sincronização ser isolada por worker, os workflows ainda tentavam ler o marcador global `/opt/marketing-hub/repo/.deployed-revision`, que não é produzido pelo deploy modular.
- **Correção efetiva:** validar diretamente a imagem em execução contra o SHA do commit e reportar imagem, início e estado do container.
- **Prevenção:** o contrato dos agentes rejeita workflows que voltem a depender do marcador global incompatível com deploy modular.

## LOOP-CODEX-AUTH-ACCOUNT-READ-CONTRACT — OAuth concluído exibido como falha

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-13; aguarda publicação.
- **Sintoma:** o operador conclui o device code e a sessão é gravada no executor, mas o painel exibe `FAILED` com “Codex App Server encerrou com falha”.
- **Causa-raiz confirmada:** o cliente compartilhado exigia o campo legado `authMode` na raiz da resposta de `account/read`; o App Server atual devolve a identidade em `account.type`, fazendo uma autenticação concluída terminar localmente com código de erro.
- **Correção efetiva:** validar primeiro `account.type`, preservando compatibilidade com `authMode`, e somente confirmar o callback depois da prova retornada pelo próprio App Server.
- **Prevenção:** o teste ponta a ponta do device code simula o contrato atual de `account/read` e exige callback autenticado sem transportar token ou refresh token.

## LOOP-ARGOS-CODEX-AUTH-GENERIC-FAILURE — falha real apagada na reconexão

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-14; aguarda publicação.
- **Sintoma:** Argos reserva o pedido de reconexão, encerra antes de exibir o device code e o painel informa apenas que o App Server não confirmou a sessão.
- **Causa-raiz confirmada:** o coordenador herdava o `stderr` do processo sem capturá-lo e substituía qualquer falha do App Server ou callback por uma mensagem genérica; uma indisponibilidade transitória do backend também encerrava todo o OAuth na primeira tentativa.
- **Correção efetiva:** capturar e sanitizar a causa operacional, persistir o último diagnóstico no pedido e repetir somente o callback ao backend até três vezes, sem reiniciar nem duplicar o fluxo OAuth.
- **Prevenção:** teste contratual exige que uma falha anterior ao device code preserve a causa segura no backend, sem token, cookie ou credencial.

## LOOP-ARGOS-CODEX-AUTH-MISSING-ROOT-CERTIFICATES — device code falha antes de gerar link

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-14; aguarda publicação.
- **Sintoma:** Argos reserva a reconexão, mas o App Server falha ao acessar `auth.openai.com` e nenhum link ou código aparece no painel.
- **Causa-raiz confirmada:** a imagem `node:20-bookworm-slim` de Argos instalava o cliente Codex sem instalar o trust store `ca-certificates`; a pesquisa Brave continuava saudável porque o runtime Node usa sua própria cadeia, mascarando a deficiência exclusiva do binário Codex.
- **Correção efetiva:** instalar e atualizar os certificados raiz antes do cliente Codex na imagem versionada do Product Discovery Worker.
- **Prevenção:** teste de contrato do Dockerfile exige `ca-certificates` antes da instalação do cliente Codex, alinhando Argos às imagens dos demais agentes autenticados.

## LOOP-APOLO-CODEX-AUTH-DEPLOY-CONTRACT — reconexão permanece em REQUESTED

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-14; aguarda publicação.
- **Sintoma:** o painel cria a reconexão de Apolo, mas ela permanece em `REQUESTED` e não exibe o device code.
- **Causa-raiz confirmada:** o deploy informava somente `VIDEO_BACKEND_BASE_URL`, enquanto o consumidor de reconexão lia `BACKEND_URL` e tentava o host inexistente `backend:8000`; o mesmo descritor não montava o `CODEX_HOME` persistente de Apolo.
- **Correção efetiva:** o Compose publicado usa a mesma URL canônica para vídeo e reconexão, fixa a identidade `videomaker` e monta o diretório Codex individual e persistente.
- **Prevenção:** o contrato de deploy valida URL compartilhada, identidade, caminho do cliente e volume individual antes de permitir nova publicação do serviço de vídeo.
- **Correção complementar em 2026-08-14:** o runtime publicado ainda recebeu apenas `VIDEO_BACKEND_BASE_URL`; o resolver de configuração agora reutiliza essa URL canônica quando `BACKEND_URL` estiver ausente. O contrato local bloqueia regressão para o host interno inexistente `backend:8000` nesse cenário.

## LOOP-APOLO-HEALTH-BLOCKED-BY-SINGLE-SCHEDULER — Apolo autenticado permanece sem READY

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-14; aguarda publicação.
- **Sintoma:** o executor inicia e a sessão Codex está autenticada, mas nenhum health-check de Apolo é persistido e a Mesa não apresenta `READY`.
- **Causa-raiz confirmada:** polling de jobs, reconexão Codex e health-check compartilhavam a única thread padrão do scheduler; uma chamada lenta ao backend impedia indefinidamente a telemetria e as demais rotinas.
- **Correção efetiva:** o serviço de vídeo passa a usar pool dedicado com no mínimo três threads, isolando produção, autenticação e telemetria.
- **Prevenção:** teste contratual exige o mínimo de três threads e prefixo próprio; jobs legados e créditos permanecem bloqueados até existir health-check recente, storyboard Codex aprovado e teto financeiro auditável.

## LOOP-TEMIS-LANDING-WITHOUT-DEDALO-DELEGATION — diagnóstico sem responsável operacional

- **Severidade:** CRÍTICO.
- **Status:** fechado localmente em 2026-08-12; aguarda publicação.
- **Sintoma:** Têmis reprova um anúncio por falha da landing, mas a interface não mostra tarefa para Dédalo e o fluxo permanece preso em correções de criativo.
- **Causa-raiz confirmada:** o coordenador enviava o parecer `LANDING` diretamente ao wireframe, sem criar delegação entre agentes nem acionar a fila autônoma de Dédalo.
- **Correção efetiva:** o backend cria delegação idempotente Têmis → Dédalo, enfileira o mesmo briefing no executor autônomo, sincroniza o resultado da tarefa pelo callback e mantém publicação, oferta, checkout e tracking protegidos.
- **Prevenção:** teste de contrato deve exigir tarefa, execução autônoma e correlação única para cada causa de landing do ciclo de convergência.

## LOOP-TEMIS-META-COPY-WITHOUT-VISIBLE-COUNTS — reenfileiramento sem diagnóstico acionável

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-13; aguarda publicação.
- **Sintoma:** Têmis falha por limite textual da Meta, mas a mesa mostra apenas a tarefa bloqueada e permite retomá-la sem informar qual campo excedeu o contrato ou por quantos caracteres.
- **Causa-raiz confirmada:** o AI Worker encerrava a validação na primeira violação e persistia somente o limite, sem a contagem real; o endpoint de experimentos não expunha violações estruturadas para a tela.
- **Correção efetiva:** validar todos os campos publicáveis, contar caracteres Unicode, persistir `atual/limite`, estruturar o diagnóstico no backend e exibir motivo e contagens na tarefa bloqueada antes da retomada.
- **Prevenção:** testes de contrato no worker, backend e frontend exigem contagem auditável e apresentação do motivo antes de reenfileirar Têmis.

## LOOP-LANDING-AGENT-DELIVERABLES-NOT-DISPATCHED — parecer correto termina em HTTP 500

- **Severidade:** CRÍTICO.
- **Status:** fechado localmente em 2026-08-12; aguarda publicação.
- **Sintoma:** o Quality Review reprova uma landing por checkout/entrega, recomenda `LANDING_PAGE_DELIVERABLES`, mas o callback do Dédalo termina em HTTP 500 e novas revisões repetem o mesmo bloqueio.
- **Causa-raiz confirmada:** o schema e o prompt aceitavam a etapa de entregáveis, enquanto o coordenador do agente não possuía roteamento para o executor canônico já existente.
- **Correção efetiva:** registrar `GeraLandingDeliverablesStageExecutionService` no coordenador e iniciar `landing-page-deliverables` quando essa for a causa mais antiga recomendada.
- **Prevenção:** teste de contrato exige que uma reprovação por checkout/entrega seja encaminhada à etapa de entregáveis, sem cair no erro genérico de etapa não automatizável.

## LOOP-LANDING-AGENT-NULL-SNAPSHOT — fila do Dédalo bloqueada por campo opcional

- **Severidade:** CRÍTICO.
- **Status:** fechado localmente em 2026-08-12; aguarda publicação.
- **Sintoma:** a homologação fica em `INICIADO` e o endpoint `pending` retorna HTTP 500 antes de o worker reservar o trabalho.
- **Causa-raiz confirmada:** o snapshot adicionava campos opcionais nulos do experimento e depois chamava `Map.copyOf`, que rejeita valores nulos.
- **Correção efetiva:** omitir do snapshot os campos opcionais ausentes, preservando os dados disponíveis e o contrato imutável.
- **Prevenção:** teste de contrato reserva a fila com experimento parcialmente preenchido e comprova que a ausência de HTML não bloqueia o Dédalo.

## LOOP-IMAGE-PLANNING-PAYLOAD-ACIMA-DO-BUFFER — planejamento visual sem consumo

- **Sintoma:** execuções `landing-page-image-planning` permanecem em `INICIADO`, enquanto o AI Worker recebe HTTP 200 e falha a cada polling com `DataBufferLimitException` em 256 KB.
- **Causa-raiz confirmada em 2026-08-13:** o aumento de buffer aplicado à etapa de copy não alcançava o cliente HTTP específico de planejamento visual; o endpoint também devolvia até 20 contextos comerciais ricos sem respeitar o limite solicitado pelo worker.
- **Correção efetiva:** o cliente de planejamento visual usa o buffer versionado de 50 MB, envia o limite ao endpoint e o backend restringe a resposta a no máximo três pendências antes de serializar o lote.
- **Prevenção:** teste de contrato consome payload superior a 256 KB e confirma o parâmetro de limite; teste do backend confirma que a fila acumulada é truncada antes da resposta.

Antes de implementar uma correção em tema com histórico de loop:

1. Identificar se o problema pertence a algum `LOOP-*` deste documento.
2. Se pertencer, corrigir a causa-raiz sistêmica, não apenas o sintoma atual.
3. Verificar o bloco **O que resolveu efetivamente no histórico** para não voltar a uma solução já superada.
4. Atualizar ou criar teste de contrato que prove que o loop foi fechado.
5. Atualizar cânone, Swagger, tela ou Worker AI quando o contrato entre módulos mudar.
6. Registrar no documento de tema correspondente o que foi feito e, quando necessário, atualizar este arquivo.

## LOOP-DEDALO-TEMIS-REVISAO-SEM-MUDANCA — revisão repetida do mesmo HTML

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-14; aguarda publicação.
- **Sintoma:** Dédalo e Têmis acumulam execuções e custo de revisão embora o HTML consolidado permaneça idêntico.
- **Causa-raiz confirmada:** o teto de quatro revisões era validado somente depois do parecer, e o design preset criava a próxima revisão por um caminho direto que não aplicava idempotência por versão.
- **Correção efetiva:** o serviço canônico registra o hash do HTML antes da fila, reutiliza a execução da mesma versão e bloqueia o quinto HTML distinto no ciclo; design preset não cria mais a revisão diretamente.
- **Prevenção:** testes de contrato exigem zero nova persistência para HTML idêntico, aceitam revisão após mudança real e bloqueiam a quinta versão do ciclo.
- **Correção complementar em 2026-08-14:** quando Têmis classifica a causa como `LANDING_PAGE_HTML`, o preset deixa de tratar a estrutura anterior como totalmente imutável e permite edição estrutural governada, preservando checkout, tracking, oferta e demais contratos funcionais. Um teste de prompt impede a regressão para simples reestilização incapaz de corrigir layout e responsividade.

## LOOP-GERALANDING-COPY-PENDING-BUFFER — copy parada antes do processamento

- **Severidade:** ALTO.
- **Status:** fechado localmente em 2026-08-13; aguarda publicação.
- **Sintoma:** Dédalo conclui a correção da landing, mas a etapa `landing-page-copy` permanece em `INICIADO` e Têmis não recebe novo artefato.
- **Causa-raiz confirmada:** o endpoint devolvia até 20 pendências com artefatos auditáveis volumosos, enquanto o client de copy mantinha o buffer padrão de 256 KB e não enviava seu limite ao backend; o consumo falhava com `DataBufferLimitException` antes de reservar qualquer job.
- **Correção efetiva:** o endpoint passou a respeitar `limit` com teto seguro e o client passou a enviar esse parâmetro e usar o limite de memória já versionado para as etapas OpenAI.
- **Prevenção:** teste de contrato consome payload acima de 256 KB e valida que a consulta limita explicitamente a quantidade de pendências.

## LOOP-VIDEO-SCENE-PROMPT-PERSISTENCE — Estúdio substituindo decisão visual salva

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-05.
- **Causa-raiz confirmada**: geração de cenas podia depender de prompt fixo ou imagem inicial genérica, em vez do storyboard persistido e da saída aprovada do plano anterior.
- **Correção efetiva**: storyboard editável e salvo é a única fonte do prompt; cada plano persiste seu quadro final e o plano seguinte usa esse asset como ponte auditável; montagem e pós-produção permanecem etapas separadas e encadeadas pelo backend.
- **Prevenção**: testes devem impedir geração com prompt não salvo, plano intermediário sem predecessor aprovado, substituição do quadro-ponte por imagem genérica e montagem sem todas as funções narrativas.
- **Correção complementar em 2026-08-05**: a conclusão do provider passou a combinar o resultado técnico com os metadados comerciais originais, preservando projeto, versão, ordem e papel narrativo. A tela usa o snapshot de auditoria como recuperação somente-leitura para jobs históricos concluídos antes dessa correção.

## LOOP-APOLO-PAID-CLIPS-DISCARDED — clipes pagos perdidos após falha terminal

- **Severidade**: CRÍTICO.
- **Status**: fechado localmente em 2026-08-13; aguarda publicação.
- **Causa-raiz confirmada**: a montagem Runway podia produzir e anexar um MP4 completo, mas o gate de duração marcava o job como falho; a reconciliação tratava qualquer falha como motivo para nova geração e não encaminhava o arquivo preservado à pós-produção.
- **Correção efetiva**: render curto com ativo persistido passa a ser fonte válida de pós-produção local; o ciclo aponta para esse novo job sem chamar provider pago, e rejeição de créditos recebe código financeiro estável que bloqueia a reconciliação na primeira ocorrência.
- **Prevenção**: testes exigem reaproveitamento do ativo antes de qualquer `requestRender`, aceitação controlada do render curto na pós-produção e somente uma chamada à Runway quando ela responder saldo insuficiente.

## LOOP-MARKETPLACE-AUTH-ENDPOINT-DRIFT — coleta autenticada presa em login obsoleto

- **Severidade**: ALTO.
- **Status**: corrigido localmente em 2026-08-14; pendente de publicação.
- **Causa-raiz confirmada**: o coletor ClickBank navegava para o host removido `sso.clickbank.com`, enquanto o login oficial passou a `accounts.clickbank.com/login.htm`; os dois coletores também acumulavam esperas de oito segundos para overlays inexistentes, fazendo a homologação Hotmart exceder 120 segundos.
- **Correção efetiva**: ClickBank usa os endpoints oficiais atuais de login e marketplace; Hotmart e ClickBank só aguardam overlays que realmente existam e estejam visíveis.
- **Prevenção**: os testes dos dois módulos e a homologação mínima somente leitura devem validar DNS, progressão do login e término limitado antes de ativar qualquer scheduler.

## Como ler este documento

## LOOP-PRODUCT-DISCOVERY-FALSE-EMPTY-SUCCESS — falha externa tratada como pesquisa vazia

- **Severidade**: ALTO.
- **Status**: reaberto e corrigido localmente em 2026-08-11; pendente de publicação.
- **Causa-raiz confirmada**: o worker capturava erros HTTP de todas as consultas do provider, retornava lista vazia e concluía o ciclo como `RESEARCH_MORE`, ocultando a indisponibilidade externa. O MCP também tentava observar um container inexistente no próprio host.
- **Correção efetiva**: falhar o ciclo quando todas as consultas falharem, registrar provider/status/ciclo, publicar logfile operacional versionado no host real e disponibilizá-lo em `java_module_logs`.
- **Fechamento complementar em produção (2026-08-11)**: a primeira pesquisa apó o deploy comprovou que o worker alcançava backend e Brave, mas as 14 consultas excediam o contrato do provider e retornavam HTTP 422. O gerador agora limita cada consulta a 400 caracteres e 50 palavras, preservando o recorte inicial e a intenção de pesquisa final.
- **Reabertura complementar em produção (2026-08-11)**: consultas já curtas continuaram retornando HTTP 422, descartando tamanho como causa completa. O cliente agora registra o corpo sanitizado do erro e, somente para 422, repete uma vez pelo contrato mínimo oficial (`q`), removendo parâmetros opcionais incompatíveis sem ocultar uma segunda falha.
- **Prevenção**: testes de contrato impedem converter falha total do provider em zero evidências, validam a rota operacional sem expor segredo, bloqueiam consultas Brave acima de 400 caracteres ou 50 palavras e exigem fallback mínimo auditável para HTTP 422.

Cada loop possui dois tipos de informação:

- **Correção efetiva**: aquilo que, no histórico real do projeto, reduziu ou encerrou o ciclo de retrabalho.
- **Prevenção futura**: regra mínima para evitar que o mesmo tipo de loop volte com outro nome, outro endpoint ou outra etapa.

Quando houver divergência entre tentativa antiga e correção efetiva, a correção efetiva prevalece.

## Classificação

- **CRÍTICO**: envolve gasto real, publicação externa, campanha, Meta Ads, landing pública, submissão ou dados comerciais.
- **ALTO**: bloqueia geração de landing, pipeline, OpenAI, qualidade comercial ou publicação.
- **MÉDIO**: causa retrabalho arquitetural, ruído visual, testes quebrados ou divergência documental.
- **BAIXO**: melhoria de governança sem impacto operacional imediato.

## Índice dos loops identificados

| Loop                                                | Severidade | Status inicial                   | Tema                                                  | Correção efetiva principal                                                                   |
| --------------------------------------------------- | ---------- | -------------------------------- | ----------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `LOOP-FB-PUBLICATION`                               | CRÍTICO    | Aberto/recorrente                | Publicação Facebook Ads                               | contrato enxuto + protocolo `jobid` + validação com payload final                            |
| `LOOP-GL-PUBLICATION-LEADPORTAL`                    | CRÍTICO    | Estabilizado com risco           | GeraLanding → Lead Portal                             | separação `html_geralanding` puro vs `landing_page_html` publicável                          |
| `LOOP-OPENAI-SCHEMA-CONTRACT`                       | ALTO       | Recorrente                       | Prompts, schemas e Structured Outputs                 | prompt + schema + parser + consumer test por etapa                                           |
| `LOOP-GL-ARCHITECTURE-STAGES`                       | ALTO       | Parcialmente estabilizado        | Arquitetura por etapas                                | padrão por etapa backend e `openai.core.<etapa>` no Worker AI                                |
| `LOOP-GL-AUTOMATION-CHAIN`                          | ALTO       | Recorrente                       | Encadeamento automático de etapas                     | orquestração no backend após callback de sucesso                                             |
| `LOOP-QUALITY-REVIEW-VISION`                        | ALTO       | Parcialmente estabilizado        | Quality Review visual                                 | screenshot renderizado mobile-first + auditoria por hash                                     |
| `LOOP-LANDING-ANALYTICS-FUNNEL`                     | CRÍTICO    | Recorrente                       | Analytics, funil e submissão                          | contrato Lead Portal → backend → evento bruto + evento normalizado + UI                      |
| `LOOP-LEAD-PORTAL-STALE-FRONTEND`                   | CRÍTICO    | Prevenido                        | Deploy do Lead Portal                                 | upstream canônico + asset do HTML validado após deploy                                       |
| `LOOP-PIPELINE-ADMIN-CONTRACT`                      | MÉDIO      | Estabilizado com risco           | Tela `/pipelines` e contrato persistente              | registry oficial + sincronizador + definição/configuração separadas                          |
| `LOOP-HYPOTHESIS-PIPELINE`                          | ALTO       | Em formação                      | Pipeline Dor → Resultado → Mecanismo → Prova → Oferta | etapas completas + pré-requisitos + finalização separada + lease                             |
| `LOOP-ARTIFACT-CONTAMINATION`                       | ALTO       | Estabilizado com risco           | Metadado técnico em artefato final                    | separação auditoria vs artefato publicável + whitelist de DTO final                          |
| `LOOP-COST-MODEL-AUDIT`                             | MÉDIO      | Em observação                    | Custos OpenAI e modelo por etapa                      | preço vindo do catálogo backend + modelo efetivo auditado por etapa                          |
| `LOOP-LOW-TICKET-SALES-PAGE-BYPASS`                 | CRÍTICO    | Fechado em 2026-07-01            | Low-ticket/GeraSalesPage                              | campanha bloqueada sem etapa final do pipeline concluída                                     |
| `LOOP-GERASALESPAGE-VISUAL-TRANSFORMATION`          | ALTO       | Fechado em 2026-07-07            | GeraSalesPage                                         | prompts v5 + quality review + auditoria bloqueiam pagina sem cenas visuais                   |
| `LOOP-GERASALESPAGE-PER-PAGE-CUSTOMIZATION`         | ALTO       | Fechado em 2026-08-09            | GeraSalesPage                                         | blueprint + componentes/tokens versionados + processor único com gates                       |
| `LOOP-CREATIVE-REVIEW-WITHOUT-DESTINATION-EVIDENCE` | CRÍTICO    | Fechado localmente em 2026-08-09 | Aprovador Meta                                        | copy + evidência visual do anúncio e da landing + continuidade auditável                     |
| `LOOP-VIDEO-SCENE-PROMPT-PERSISTENCE`               | ALTO       | Fechado em 2026-08-05            | Estudio de Audio e Video                              | storyboard editavel + persistencia unica + teste impede prompt fixo de substituir cena salva |
| `LOOP-DEPLOY-COMPOSE-CROSS-SERVICE-SECRETS`         | ALTO       | Fechado em 2026-08-04            | Deploy por serviço                                    | descritor Compose isolado por destino + teste sem secrets alheios                            |
| `LOOP-DEPLOY-STALE-IMAGE`                           | ALTO       | Fechado em 2026-08-04            | Detecção de mudanças do deploy                        | alteração de publicador/workflow força rebuild e teste do artefato                           |
| `LOOP-DEPLOY-GLOBAL-TIMEOUT`                        | ALTO       | Fechado em 2026-08-06            | Deploy backend/frontend                               | limites próprios por operação + saúde obrigatória do backend                                 |
| `LOOP-CUSTOMER-AGENT-OBSERVABILITY`                 | ALTO       | Fechado em 2026-08-06            | Agente Cliente                                        | logfile canônico do worker + alias MCP + teste ponta a ponta                                 |
| `LOOP-CUSTOMER-AGENT-EVALUATION-TIMEOUT`            | ALTO       | Fechado em 2026-08-06            | Agente Cliente                                        | timeout adequado + erro persistido e integralmente visível no frontend + retry controlado    |
| `LOOP-FINANCIAL-AGENT-OBSERVABILITY`                | ALTO       | Fechado em 2026-08-06            | Agente Financeiro                                     | logfile canônico do worker + alias MCP + teste ponta a ponta                                 |
| `LOOP-BACKEND-LOGS-DEPENDENT-ON-BACKEND`            | ALTO       | Fechado em 2026-08-06            | Backend / MCP                                         | leitor independente no volume persistente + erro de rede explícito                           |
| `LOOP-STUDIO-COST-ATTRIBUTION`                      | CRÍTICO    | Fechado em 2026-08-06            | Estúdio / Agente Financeiro                           | ledger em todo estado terminal + custos sem plano visíveis e bloqueantes                     |

---

## LOOP-CREATIVE-REVIEW-WITHOUT-DESTINATION-EVIDENCE — Parecer isolado do anúncio

- **Severidade**: CRÍTICO.
- **Status**: fechado localmente em 2026-08-09; aguarda publicação.
- **Sintoma**: o Aprovador Meta repetia correções sem comprovar a coerência entre o anúncio e a página de destino; vídeos também eram representados apenas pela URL do arquivo.
- **Causa-raiz confirmada**: o contrato entregava `destinationUrl` como texto, mas não fornecia screenshots renderizados da landing nem quadros do vídeo ao modelo multimodal.
- **Correção efetiva**: capturar e auditar a landing em mobile e desktop, extrair três quadros de vídeos e exigir pareceres separados de copy, estética comercial e continuidade anúncio → página.
- **Prevenção**: aprovação técnica exige URL pública válida, evidência visual da landing e todas as dimensões acima do piso; o agente não publica, não aprova humanamente e não altera orçamento.
- **Correção complementar em 2026-08-09**: criativos legados sem `destinationUrl` recebem no contrato de revisão a URL pública persistida do experimento; o MCP é materializado com acesso somente leitura ao runtime versionado do Playwright, evitando que a extração em `/tmp` rompa a resolução do navegador.
- **Teste de contrato**: a fila `pending` comprova o fallback para a landing do experimento e o worker comprova o vínculo do MCP ao runtime multimodal do container.
- **Recorrência encontrada em 2026-08-09**: o servidor MCP encerrava antes do handshake porque o Codex não repassa variáveis de ambiente arbitrárias aos subprocessos MCP; além disso, suas ferramentas reliam o DTO público do criativo e perdiam a `destinationUrl` resolvida apenas na fila.
- **Correção sistêmica**: transporte `stdio` migrado para o SDK oficial, variáveis não sensíveis registradas explicitamente na configuração MCP por job e contexto segregado exposto por endpoint canônico que preserva a mesma landing efetiva da reserva. O teste passa a exigir handshake real do Codex e igualdade entre contexto da fila e contexto consultado pelo MCP.
- **Recorrência operacional em 2026-08-09**: o MCP voltou a carregar e inspecionou as imagens, mas o subprocesso iniciado pelo Codex não recebeu `PLAYWRIGHT_BROWSERS_PATH`; por isso o `playwright-core` não encontrou o Chromium instalado em `/ms-playwright` e bloqueou a inspeção da landing em mobile e desktop.
- **Prevenção adicional**: a configuração MCP por job passa a declarar explicitamente o caminho versionado dos navegadores junto das demais variáveis permitidas, e o teste de contrato exige esse transporte antes do deploy.
- **Recorrência operacional em 2026-08-09 após o Chromium voltar a iniciar**: o MCP capturava a landing logo após `domcontentloaded` e auditava somente o estado transitório React `Preparando uma oferta especial para você...`, embora o conteúdo comercial carregasse em seguida.
- **Correção sistêmica complementar**: a inspeção mobile e desktop agora espera um critério objetivo de prontidão do conteúdo comercial e das fontes antes do screenshot; o teste de contrato impede o retorno da captura imediata do shell.
- **Prevenção de copy incompatível em 2026-08-09**: armazenamento e publicação foram separados. O histórico permanece íntegro em campo textual amplo, mas correções do Aprovador e o preflight de publicação bloqueiam texto principal acima de 125 caracteres, headline acima de 40 e descrição acima de 25, sem truncamento silencioso.
- **Recorrência operacional em 2026-08-12**: a tarefa da Têmis foi bloqueada porque a primeira geração desrespeitou o limite de `primaryText`; ao ser retomada pela tela, permaneceu `IN_PROGRESS` sem voltar à fila, pois a reconciliação enfileirava somente tarefas `PENDING`.
- **Correção sistêmica complementar**: tarefas retomadas após `FAILED` ou `TIMEOUT` voltam à fila sem duplicar geração ativa, e o AI Worker solicita uma única reescrita semântica completa antes de encerrar por violação persistente do contrato Meta. Testes de contrato cobrem retomada, idempotência, reescrita válida e bloqueio após duas respostas inválidas.
- **Recorrência confirmada em 2026-08-13**: os limites existiam apenas como instrução textual anexada ao prompt; a Responses API não recebia JSON Schema com `maxLength` e a segunda tentativa repetia o pedido sem informar qual campo havia excedido o contrato.
- **Prevenção sistêmica adicional**: prompt e schema da copy Meta passam a ser recursos versionados, a saída estruturada restringe `primaryText` a 125, `headline` a 40, `description` a 25 e `cta` a 32 caracteres, e uma eventual segunda tentativa recebe explicitamente a causa da rejeição para reescrita completa. A validação defensiva do worker permanece antes da geração de imagem e persistência.

## LOOP-META-AD-APPROVER-LANDING-PROMPT-TRUNCATION — aprendizado bloqueado ao persistir correção da landing

- **Severidade**: ALTO.
- **Status**: fechado localmente em 2026-08-10, pendente de publicação por PR.
- **Causa-raiz confirmada**: o schema real preservava `gera_landing_stage_execution.prompt_content` como `TINYTEXT`, embora o changelog de criação já declarasse `LONGTEXT`. O ciclo enriquecido com memória concluiu a análise, mas o callback falhou ao distribuir a tarefa de landing porque o briefing auditável excedeu 255 caracteres.
- **Correção efetiva**: changelog corretivo altera explicitamente `prompt_content` para `LONGTEXT`, e a entidade JPA fixa o mesmo tipo canônico sem truncar o prompt.
- **Prevenção**: teste de contrato valida entidade, changelog MySQL e include relativo no mestre; o cânone proíbe reduzir ou truncar prompts para contornar limites de persistência.

---

## LOOP-LEAD-PORTAL-STALE-FRONTEND — HTML carregado com bundle inexistente

- **Severidade**: CRÍTICO.
- **Status**: fechado com teste de contrato em 2026-08-07.
- **Causa-raiz confirmada**: o HTML podia referenciar um bundle ausente no frontend atendido pelo proxy; a rota de SPA devolvia HTML para o caminho JavaScript e o navegador recusava o arquivo pelo MIME incorreto. Na publicação, proxies legados do mesmo projeto também podiam continuar donos das portas 80/443 quando o nome do container não coincidia com uma lista fixa.
- **Correção efetiva**: usar o container canônico do frontend, impedir fallback de `/assets/` para `index.html`, desabilitar cache do HTML e validar após o deploy que o bundle referenciado existe e responde `200:application/javascript`. O deploy reconcilia o proprietário real das portas públicas por labels do Compose e remove somente containers pertencentes ao Lead Portal.
- **Prevenção**: o CI executa `lead-portal/scripts/test-frontend-cache-contract.sh` para impedir regressão da configuração e da reconciliação de portas, além da sonda pública pós-deploy. Se o proprietário das portas não pertencer ao projeto, o deploy interrompe com diagnóstico em vez de remover serviço alheio.

---

## LOOP-BACKEND-LOGS-DEPENDENT-ON-BACKEND — Falha de bootstrap elimina o próprio diagnóstico

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-06.
- **Causa-raiz confirmada**: a tool `java_module_logs` lia o logfile por uma rota do próprio backend; quando o bootstrap falhava, a rota também desaparecia e uma `ConnectException` sem mensagem era exposta como `Failed to read log stream URL: null`.
- **Correção efetiva**: o volume persistente do logfile passou a ser servido por um leitor Nginx independente do processo Java, e o MCP passou a descrever a classe da falha e todas as tentativas quando a exceção de rede não possui mensagem.
- **Prevenção**: teste de contrato fixa a origem independente, teste unitário impede mensagem nula e o deploy recria o leitor junto da pilha sem depender da saúde do backend.

---

## LOOP-STUDIO-COST-ATTRIBUTION — Consumo de vídeo invisível na conciliação

- **Severidade**: CRÍTICO.
- **Status**: fechado em 2026-08-06.
- **Causa-raiz confirmada**: o ledger só era sincronizado no sucesso e descartava jobs cujo projeto não possuía plano comercial; o MUSA v7 era legado sem plano, deixando inclusive renders concluídos fora da conciliação. A revisão ampliada também encontrou chamadas legadas de ElevenLabs, HeyGen e Synthesia sem ledger e estimativas desconhecidas convertidas em zero.
- **Correção efetiva**: abrir o ledger antes da chamada externa, atualizar a mesma tentativa em qualquer estado, registrar áudio e vídeo legados com produto obrigatório, aceitar atribuição nula de plano para histórico e manter tarifa desconhecida como nula. Custos sem plano ficam visíveis e bloqueantes, sem contaminar outro planejamento.
- **Prevenção**: testes devem provar que toda tentativa nasce no ledger antes do consumo, que sucesso, falha e expiração atualizam sem duplicar, que áudio é contado separadamente, que custo desconhecido nunca vira zero e que custos sem plano aparecem ao Agente Financeiro até atribuição correta.
- **Correção complementar em 2026-08-06**: o snapshot e o painel financeiro passaram a cruzar ledger e revisão comercial por provedor, expondo taxa de aprovação e custo conhecido por asset aprovado. A recomendação de recarga fica bloqueada quando custos ou revisões estiverem incompletos.
- **Diagnóstico operacional**: a tool MCP `studio_ledger_coverage` compara jobs e ativos do Estúdio com o ledger por origem, tipo e provedor, tornando ausências, custos desconhecidos e falta de atribuição comprováveis sem SQL manual.
- **Recorrência fechada em 2026-08-11**: o ciclo Apolo–Plutus exigia `commercial_plan_id NOT NULL`, embora projetos MUSA legados e o ledger canônico permitissem custo sem plano. O ciclo agora preserva plano nulo e entrega a Plutus o snapshot segregado de custos não atribuídos, com teste que impede voltar a vincular ou inventar planejamento.
- **Recorrência fechada em 2026-08-13**: Plutus aprovava os ciclos MUSA de 30/60 segundos, mas o backend enviava a duração final como uma única solicitação Runway de no máximo 10 segundos. A criação do job falhava dentro da mesma transação, apagava a decisão e fazia o worker repetir indefinidamente o ciclo mais antigo. O backend agora seleciona Luma para vídeos longos e o executor calcula a quantidade de cenas de dez segundos a partir da duração final, preservando montagem, ledger e teto do ciclo. Testes de contrato cobrem a aprovação de 60 segundos e o plano de seis cenas.
- **Decisão comercial de encerramento histórico em 2026-08-12**: as tentativas anteriores a 2026-08-13 que ainda não possuem custo recuperável são classificadas uma única vez como USD 0 com evidência `USER_ASSUMED_ZERO_LEGACY_20260812`. Valores conhecidos permanecem intactos e toda tentativa nova continua obrigada a registrar custo real ou estimado; a exceção elimina dívida impossível de apurar sem reabrir o risco sistêmico para novos consumos.

---

## LOOP-CUSTOMER-AGENT-OBSERVABILITY — Observação presa sem diagnóstico completo

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-06.
- **Causa-raiz confirmada**: o `customer-agent-worker` não gravava logs em arquivo nem publicava uma rota de leitura, enquanto o MCP não reconhecia o módulo; falhas de Codex, codec, navegador e callback ficavam reduzidas ao erro persistido no backend.
- **Correção efetiva**: o worker publica logfile operacional versionado e o MCP o consulta pelo alias `customer-agent-worker`, com destino fixado nos descritores de deploy.
- **Prevenção**: testes de contrato devem validar a rota, a porta, o destino produtivo e a leitura filtrada pela tool `java_module_logs`.

## LOOP-CUSTOMER-AGENT-EVALUATION-TIMEOUT — Avaliação falha sem reprocessamento

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-06.
- **Causa-raiz confirmada**: a avaliação #1 atingiu o limite fixo de dez minutos do processo Codex; o backend não possuía comando de retry e o callback preservava somente a mensagem superficial da exceção.
- **Correção efetiva**: limite padrão elevado para vinte minutos, causa completa persistida e retry explícito restrito a `FAILED`, preservando o mesmo ID e contando as novas tentativas.
- **Prevenção**: testes devem bloquear retry fora de `FAILED`, comprovar incremento da tentativa e garantir que o worker envie stack trace e cadeia de causas com tamanho limitado.

---

## LOOP-FINANCIAL-AGENT-OBSERVABILITY — Conciliação sem diagnóstico do executor

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-06.
- **Causa-raiz confirmada**: o `financial-agent-worker` não gravava logs em arquivo nem publicava rota de leitura, enquanto o MCP não reconhecia o módulo; a saúde do executor e falhas de reserva, Codex, conciliação e callback não podiam ser confirmadas pelos logs operacionais.
- **Correção efetiva**: o worker publica logfile operacional versionado e o MCP o consulta pelo alias `financial-agent-worker`, com destino fixado nos descritores de deploy.
- **Prevenção**: testes de contrato validam a rota, a porta, o destino produtivo e a leitura filtrada pela tool `java_module_logs`.

---

## LOOP-DEPLOY-COMPOSE-CROSS-SERVICE-SECRETS — Deploy isolado bloqueado por segredo alheio

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-04.
- **Sintoma recorrente ou risco observado**:
  - deploy de vídeo ou MCP falha antes de atualizar o serviço porque o Compose exige segredo privado do backend/pagamentos;
  - `--no-deps` não evita a falha, pois a interpolação ocorre antes da seleção dos serviços.
- **Causa-raiz sistêmica confirmada**:
  - stacks de hosts e responsabilidades diferentes compartilhavam o mesmo descritor Compose monolítico.
- **Correção efetiva**:
  - manter descritores independentes para vídeo e MCP;
  - fazer cada script de publicação apontar explicitamente para seu descritor;
  - preservar secrets obrigatórios apenas no stack que efetivamente os consome.
- **Regra preventiva**:
  - todo deploy isolado deve validar e subir somente o descritor do seu destino;
  - um teste de contrato deve renderizar o Compose isolado sem secrets de outros módulos.

---

## LOOP-DEPLOY-STALE-IMAGE — Workflow verde com imagem antiga

- **Severidade**: ALTO.
- **Status**: fechado em 2026-08-04.
- **Sintoma recorrente ou risco observado**:
  - workflow termina com sucesso, mas pula o build e mantém no container uma imagem anterior;
  - correções de integração parecem publicadas, embora o serviço continue executando código antigo.
- **Causa-raiz sistêmica confirmada**:
  - o detector classificava mudanças no script de publicação ou no próprio workflow sem exigir reconstrução de todos os artefatos afetados.
- **Correção efetiva**:
  - mudança no publicador isolado de vídeo força rebuild da imagem de vídeo;
  - mudança no próprio workflow força reconstrução e deploy dos artefatos governados por ele;
  - teste de contrato protege as duas classificações.
- **Regra preventiva**:
  - sucesso do workflow só comprova publicação quando o job de build e o job de deploy do destino foram executados, não apenas quando o workflow agregado ficou verde.

---

## LOOP-GERASALESPAGE-PER-PAGE-CUSTOMIZATION — Correções exclusivas a cada página

- **Severidade**: ALTO.
- **Status**: fechado arquiteturalmente em 2026-08-09.
- **Sintoma**: cada experimento novo exige alterações particulares de HTML, prompt, responsividade, analytics ou checkout antes de atingir qualidade publicável.
- **Causa-raiz**: conteúdo, identidade visual, componentes e validações não estavam formalizados como camadas distintas de uma fábrica de páginas configurável.
- **Correção efetiva**: `Sales Page Blueprint` persistido, biblioteca de componentes e tokens versionados, processor único e matriz de homologação com pareceres obrigatórios dos agentes.
- **Prevenção**: páginas novas configuram o pipeline canônico; exceções viram componente/contrato reutilizável e versionado. ArchUnit impede processor paralelo e acesso direto ao modelo fora do processor auditável.

## LOOP-GERASALESPAGE-VISUAL-TRANSFORMATION — Pagina de venda sem transformacao visual

- **Severidade**: ALTO.
- **Status**: fechado em 2026-07-07.
- **Sintomas recorrentes ou risco observado**:
  - pagina publicada com promessa textual, mas pobre de imagens;
  - usuario nao consegue sentir o estado depois da transformacao;
  - pagina baseada quase so em texto, cards, icones ou gradientes;
  - preview ou demonstracao do produto nao materializa a transformacao percebida.
- **Causa-raiz sistemica confirmada**:
  - os prompts pediam antes/depois de forma generica, mas o HTML e o quality review nao exigiam quantidade minima de cenas visuais nem bloqueavam pagina pobre de prova visual.
- **Correcao efetiva**:
  - criar templates v5 do GeraSalesPage para visual-plan, HTML, quality review e publication package exigindo hero com cena do depois e 3 a 5 blocos visuais de transformacao;
  - marcar blocos visuais com `data-transform-visual`;
  - bloquear deterministamente a publicacao quando o HTML final tiver menos de 3 cenas visuais.
- **Regra preventiva**:
  - pagina de venda para trafego pago nao pode ser aprovada apenas por texto e clareza de oferta; deve provar visualmente dor, depois, preview/prova e contexto real de uso antes de liberar publicacao.

## LOOP-LOW-TICKET-SALES-PAGE-BYPASS — Low-ticket sem página criada pelo pipeline

- **Severidade**: CRÍTICO.
- **Status**: fechado em 2026-07-01.
- **Sintomas recorrentes ou risco observado**:
  - experimento low-ticket com página publicada por ponte operacional, fora da execução completa do GeraSalesPage;
  - liberação de campanha baseada apenas em `followUpActionUrl` preenchido;
  - aprendizado de qualidade da página ficando fora do pipeline;
  - risco de tráfego pago apontar para artefato não auditado pela etapa de quality review/publication package.
- **Causa-raiz sistêmica confirmada**:
  - a prontidão de campanha validava destino de venda como URL, não como artefato concluído pelo pipeline responsável;
  - o GeraSalesPage usava Flex, que reduzia custo, mas aumentava indisponibilidade em artefato comercial crítico.
- **Correção efetiva**:
  - bloquear prontidão e liberação de campanha para `LOW_TICKET_PRODUCT` sem `sales-page-publication-package=CONCLUIDO`;
  - adicionar rebuild canônico para substituir execuções antigas por `SUBSTITUIDO` e recriar a página pelo pipeline;
  - usar `service_tier=default` no GeraSalesPage v1 por padrão;
  - calcular custo OpenAI conforme o service tier usado.
- **Regra preventiva**:
  - nunca liberar campanha low-ticket apenas porque existe URL pública; a URL deve ser resultado auditável do GeraSalesPage v1 concluído.
  - templates globais do GeraSalesPage nunca podem fixar marca, URL, arquivo ou entrega de um experimento específico; conteúdo específico deve vir apenas do contrato persistido do experimento atual.
  - a revisão de qualidade deve bloquear contaminação cruzada entre produtos antes do pacote de publicação.

## LOOP-FB-PUBLICATION — Publicação Facebook Ads

- **Severidade**: CRÍTICO.
- **Status**: aberto/recorrente.
- **Sintomas recorrentes**:
  - experimento volta várias vezes para a fila de publicação;
  - campanha duplicada na Meta;
  - publicação marcada como `FAILED` antes de criar campanha/ad set/anúncio;
  - erro da Meta muda a cada tentativa;
  - público aprovado existe na UI, mas o Worker não consegue materializar targeting;
  - payload enviado à Meta diverge do contrato canônico.
- **Causa-raiz sistêmica provável**:
  - publicação dependia de contratos grandes ou genéricos demais;
  - o mesmo endpoint era usado para listar fila e resolver targeting de experimento específico;
  - validações prévias não usavam exatamente o mesmo payload final que seria enviado para a Meta;
  - fluxo de retry não limpava completamente estados anteriores;
  - rastreabilidade por job só foi introduzida depois de vários erros.
- **O que resolveu efetivamente no histórico**:
  - tornar o `POST /api/facebook-campaigns` idempotente para o mesmo `campaignId` e bloquear nova campanha para o mesmo `experimentId`, evitando duplicidade de campanhas;
  - retirar da fila experimentos que já possuem campanha persistida, para o worker não recolocar o mesmo experimento em publicação;
  - criar endpoint enxuto `GET /api/facebook-adsets/experiments/{experimentId}/targeting-package`, evitando carregar `ExperimentDto` completo com HTML/copy/landing e removendo falso erro por buffer/payload grande;
  - quando `experimentId` é informado, resolver targeting mesmo se o experimento estiver `FAILED`, permitindo retry operacional sem depender do status de fila;
  - exigir pelo menos um público aprovado com `metaId` oficial e bloquear publicação ampla acidental;
  - fazer upload de imagem por bytes para `/adimages` e usar `image_hash`, removendo dependência da Meta baixar URL externa;
  - usar no `reachestimate` o mesmo targeting final do ad set, incluindo `geo_locations.countries=["BR"]`;
  - tratar ausência de limites em `reachestimate` como alerta operacional, não falha automática, e bloquear somente erro explícito ou alcance fora da faixa canônica;
  - registrar passos da publicação com `publicationJobId`/protocolo `jobid`, incluindo payload enviado, resposta recebida, endpoint, status e erro;
  - usar destino standalone vindo de `followUpActionUrl` no worker;
  - alinhar orçamento real por Ad Set, reportando `budgetMode=ADSET`;
  - enviar `is_adset_budget_sharing_enabled=false` em campanhas sem orçamento no nível da campanha.
  - enviar `experimentType` no contrato `/api/facebook-campaigns/experiments-ready` para o worker não degradar `LOW_TICKET_PRODUCT + SALES` para campanha de leads quando existir `freeReward` secundário.
  - exigir `facebookPixelId` para `LOW_TICKET_PRODUCT + SALES` antes de entrar na fila e publicar o ad set com `optimization_goal=OFFSITE_CONVERSIONS`, `promoted_object.pixel_id` e `custom_event_type=PURCHASE`.
  - adicionar `facebook_ads_campaign.publication_key` com unicidade para novas publicações, impedindo que retry/concorrência grave duas campanhas novas para o mesmo experimento no backend.
  - validar antes de `/campaigns` e `/adcreatives` que a Page selecionada no experimento está conectada ao `instagram_user_id` usado pelo criativo, bloqueando pares incompatíveis com `CAMPAIGN_PAGE_INSTAGRAM_CONNECTION_BLOCKED`.
  - em 2026-08-08, a leitura operacional de pausas passou a consultar diretamente os campos canônicos `stop_requested_at`/`stop_completed_at`, evitando que uma solicitação persistida ficasse invisível ao Facebook Ads Worker; o diagnóstico da tela também passou a reconhecer o snapshot profissional de targeting enviado à Meta quando a publicação usa o pacote manual aprovado, sem exigir vínculo opcional com um playbook de ad set.
  - em 2026-08-08, criativos novos passaram a exigir revisão multimodal auditável antes da aprovação humana, impedindo que peças incompletas ou sem CTA cheguem à fila de publicação apenas por mudança manual de status.
- **Módulos envolvidos**:
  - `backend/ads-service`;
  - `facebook-ads-worker`;
  - Swagger Facebook Ads;
  - cânone de publicação Facebook Ads;
  - tela de experimento e tela `/facebook-campaigns`.
- **Contratos sensíveis**:
  - fila de experimentos prontos;
  - pacote enxuto de targeting;
  - criativos prontos;
  - upload de imagem por bytes;
  - reach estimate;
  - registro de campanha;
  - protocolo `jobid`.
- **Fechamento mínimo do loop**:
  - etapa de dry-run antes de criar qualquer objeto na Meta;
  - payload final de campaign/adset/creative/ad validado e registrado antes do envio;
  - `publicationJobId` obrigatório em todos os passos;
  - teste cobrindo retry de experimento `FAILED` com targeting aprovado;
  - teste garantindo que ausência de `users_lower_bound`/`users_upper_bound` em `reachestimate` é alerta, não falha automática;
  - teste garantindo orçamento por Ad Set e `is_adset_budget_sharing_enabled=false` em campanha sem orçamento.
  - teste garantindo que o contrato do backend expõe `experimentType=LOW_TICKET_PRODUCT` e que o worker publica `campaignObjective=SALES` como `OUTCOME_SALES`.
  - teste garantindo que campanha low-ticket de venda só é considerada pronta com pixel e que o worker usa `OFFSITE_CONVERSIONS` + evento `PURCHASE`.
  - teste garantindo que Page sem Instagram conectado bloqueia a publicação antes de criar qualquer objeto parcial na Meta.
- **Regra preventiva**:
  - nunca corrigir publicação Facebook apenas pelo erro atual da Meta; comparar payload esperado, payload enviado, resposta, estado do experimento, campanha persistida e job steps.

## LOOP-GL-PUBLICATION-LEADPORTAL — GeraLanding → Lead Portal

- **Severidade**: CRÍTICO.
- **Status**: estabilizado com risco de regressão.
- **Sintomas recorrentes**:
  - aprovação/publicação da landing quebra por rota legada;
  - `customFormHtml` rejeitado;
  - HTML final contém metadado técnico;
  - backend e Lead Portal discordam sobre contrato do payload;
  - frontend habilita botão sem usar a mesma fonte de verdade do backend;
  - landing publicada sem submissão, tracking ou URL standalone correta.
- **Causa-raiz sistêmica provável**:
  - confusão entre HTML fonte, HTML provisório, HTML puro, HTML publicável e HTML salvo no Lead Portal;
  - coexistência de endpoints legados e novos;
  - injeção de pixel, tracking, analytics e submissão em momentos diferentes do fluxo.
- **O que resolveu efetivamente no histórico**:
  - marcar endpoints legados de aprovação/publicação como obsoletos e forçar uso do endpoint canônico do GeraLanding;
  - alterar o frontend para chamar `POST /api/experiments/{id}/geralanding/landing/approve-and-publish`;
  - simplificar o payload para o Lead Portal usando somente `slug`, `name`, `description` e `customFormHtml`, removendo `legacyPreviewHtml` e `renderMode`;
  - retirar a validação/normalização restritiva de `CustomFormHtmlResolver` no Lead Portal quando ela bloqueava HTML publicável válido;
  - separar definitivamente `html_geralanding` como HTML/CSS puro e `landing_page_html` como HTML publicável enriquecido com scripts, pixel, analytics e submissão;
  - habilitar a aprovação pela fonte de verdade do backend, sem depender exclusivamente da prévia local do frontend;
  - injetar submissão canônica idempotente quando o HTML tem controles mínimos de captura, evitando landing publicada sem envio de formulário;
  - criar endpoint de compatibilidade no Lead Portal para receber submissão pública e encaminhar ao backend principal.
- **Campos sensíveis**:
  - `html_geralanding`: HTML/CSS puro gerado pelo GeraLanding;
  - `landing_page_html`: HTML publicável, enriquecido com scripts/pixel/tracking/submissão;
  - `follow_up_action_url`: destino oficial de campanha;
  - `customFormHtml`: contrato enviado ao Lead Portal.
- **Fechamento mínimo do loop**:
  - teste ponta a ponta de aprovação: `html_geralanding` puro → injeções idempotentes → publicação Lead Portal → `follow_up_action_url` salvo;
  - bloqueio explícito de `legacyPreviewHtml`, `renderMode` e comentários técnicos no contrato final;
  - Swagger do Lead Portal e Swagger GeraLanding sincronizados.
- **Regra preventiva**:
  - não alterar publicação de landing sem declarar qual artefato está sendo lido, qual está sendo enriquecido e qual está sendo publicado.

## LOOP-OPENAI-SCHEMA-CONTRACT — Prompts, schemas e Structured Outputs

- **Severidade**: ALTO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - OpenAI retorna JSON com formato diferente do parser;
  - payload vem em Markdown code fence;
  - JSON duplicado/concatenado;
  - JSON escapado dentro de string;
  - schema aceito localmente, mas rejeitado pela Responses API;
  - frontend considera variações disponíveis e backend/worker não encontra candidatos;
  - totalizadores zerados apesar de artefato salvo.
- **Causa-raiz sistêmica provável**:
  - prompt, schema, parser backend, Worker AI e frontend não evoluíam como um único contrato versionado.
- **O que resolveu efetivamente no histórico**:
  - normalizar respostas com Markdown code fence antes do parse;
  - extrair o primeiro objeto JSON balanceado em vez de usar substring ingênua do primeiro `{` ao último `}`;
  - tratar JSON escapado e payload duplicado/concatenado nos consumidores;
  - alinhar o caminho esperado pelo backend, por exemplo `landingPageImagePlanning.images[]` quando o resumo de imagens contava `images` e não `imagePlan`;
  - remover palavras-chave incompatíveis com Structured Outputs estrito, como `allOf`, condicionais e `uniqueItems`, quando a Responses API rejeitou o schema;
  - exigir `additionalProperties: false` nos objetos usados em schemas estritos;
  - mover prompts relevantes para o local versionado correto no `ai-worker/src/main/resources/prompts/...`;
  - criar validações pós-resposta para impedir estilos/classes inexistentes em `definicoes`;
  - atualizar extratores backend e Worker AI para reconhecer JSON direto, aninhado, serializado em texto e encapsulado em Markdown quando a UI já conseguia detectar o conteúdo.
- **Etapas mais afetadas**:
  - `landing-page-wireframe`;
  - `landing-page-copy`;
  - `landing-page-image-planning`;
  - `landing-page-design-preset`;
  - `campaign-angle`;
  - `ad-copy`;
  - `ad-image-briefing`.
- **Fechamento mínimo do loop**:
  - teste de compatibilidade do schema com Structured Outputs estrito;
  - golden JSON por etapa;
  - teste do consumer backend processando o golden JSON;
  - teste do Worker AI montando request final com `service_tier=flex`;
  - teste de frontend somente para aquilo que o backend também consegue extrair.
- **Regra preventiva**:
  - todo ajuste em prompt deve responder: o schema aceita, a OpenAI aceita, o backend consome, a UI interpreta e o relatório consegue auditar?
  - propriedades que usam `const` também devem declarar `type`; o contrato do Estrategista possui teste específico para impedir nova rejeição `invalid_json_schema`.
  - imagens de agentes com Playwright devem fixar uma distribuição Linux suportada pela versão do navegador; o Estrategista usa `eclipse-temurin:21-jre-noble`, protegido por teste de contrato e build do container no CI, evitando que a tag móvel avance para Ubuntu 26.04 incompatível.
  - a prontidão do deploy de agentes deve registrar separadamente estado do container, autenticação Codex e corpo do health check em cada tentativa; o Estrategista aceita JSON com espaços e aguarda até dois minutos, evitando falso negativo de um comando composto sem evidência do requisito que falhou;
  - em 2026-08-06, o workflow ainda rejeitou 23 respostas saudáveis `{"status":"UP"}` porque as aspas do regex foram consumidas pela camada de quoting do comando SSH. A verificação passou a buscar os marcadores estáveis `status` e `UP`, sem depender das aspas literais do JSON no shell remoto.
  - em 2026-08-09, o Aprovador Meta iniciou saudável e autenticado, mas o deploy falhou porque a observabilidade dedicada moveu o health check para `/ops-meta-ad-approver-observability-v1/health` enquanto o workflow continuou consultando `/actuator/health`. Um teste de contrato agora exige que a rota de prontidão do workflow acompanhe o `base-path` versionado do agente.
  - no mesmo ciclo, a correção comercial do criativo 280 falhou no callback porque `creative.primary_text` ainda era `VARCHAR(255)`, menor que a copy válida produzida pelo fluxo. O contrato canônico passou a preservar o texto integral em `LONGTEXT`, alinhado explicitamente na entidade JPA e no changelog MySQL 5.7.

## LOOP-GL-ARCHITECTURE-STAGES — Arquitetura por etapas do GeraLanding

- **Severidade**: ALTO.
- **Status**: parcialmente estabilizado.
- **Sintomas recorrentes**:
  - mover controller para pacote de etapa quebra teste MVC;
  - service da etapa depende de service transversal;
  - DTO fica em pacote genérico e viola ArchUnit;
  - Worker AI consome endpoint genérico quando o contrato exige endpoint por etapa;
  - classe adaptadora é criada e depois removida por pouca responsabilidade;
  - regra ArchUnit precisa ser ajustada repetidamente.
- **Causa-raiz sistêmica provável**:
  - o padrão por etapa foi descoberto durante a implementação, não aplicado como template fechado desde o início.
- **O que resolveu efetivamente no histórico**:
  - padronizar backend por etapa com `Backend<Etapa>Controller`, `Backend<Etapa>Service` e records em subpacotes por operação;
  - expor endpoints internos específicos por etapa: `pending`, `recebe-prompt` e `recebe-resposta`;
  - remover controllers genéricos/transversais quando eles mantinham acoplamento entre etapas;
  - mover provisórios e assemblers para o pacote da própria etapa, como `presetdesign.provisorio`;
  - ajustar o frontend para consumir endpoints segmentados por etapa, inclusive detalhe com `stageCode`/segmento correto;
  - migrar etapas do Worker AI para `com.marketinghub.worker.openai.core.<etapa>`, reduzindo dependência do pacote legado `worker.geralanding`;
  - desativar/remover implementações legadas quando a etapa passou a operar pelo core OpenAI;
  - usar ArchUnit para proteger dependências por etapa/camada, mas ajustar regras somente quando a arquitetura efetiva já estava clara.
- **Template mínimo por etapa backend**:
  - `Backend<Etapa>Controller`;
  - `Backend<Etapa>Service`;
  - subpacotes `pending`, `recebePrompt`, `recebeResposta`, `listStageExecutions`, `detailStageExecution`;
  - endpoints públicos de start/list/detail quando aplicável;
  - endpoints internos `pending`, `recebe-prompt`, `recebe-resposta`;
  - Swagger atualizado;
  - testes de controller e service.
- **Template mínimo por etapa Worker AI**:
  - `openai.core.<etapa>`;
  - scheduler;
  - backend client;
  - prompt builder;
  - validator;
  - handler;
  - properties;
  - configuration;
  - testes de request e callback.
- **Regra preventiva**:
  - não criar etapa nova apenas copiando a etapa anterior; preencher o checklist de arquitetura antes de codificar.

## LOOP-GL-AUTOMATION-CHAIN — Encadeamento automático de etapas

- **Severidade**: ALTO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - etapa conclui e não dispara a próxima;
  - etapa dispara algo de outro fluxo sem intenção do usuário;
  - automação de anúncio conflita com automação do GeraLanding;
  - botão manual continua aparecendo em experimento publicado;
  - reexecução mantém artefatos antigos incompatíveis.
- **Causa-raiz sistêmica provável**:
  - automação era tratada como comportamento local da etapa, não como contrato de estado do pipeline.
- **O que resolveu efetivamente no histórico**:
  - colocar o encadeamento automático no backend, no callback de conclusão bem-sucedida da etapa anterior;
  - registrar `promptTemplateId` com origem automática, como `auto/wireframe`, `auto/copy`, `auto/image-planning` e `auto/image-generation`;
  - criar testes explícitos garantindo que sucesso cria a próxima execução e falha não cria;
  - separar a automação do GeraLanding da automação de criativos de anúncio;
  - bloquear geração automática de imagem de anúncio ao concluir `AD_IMAGE_BRIEFING` quando o usuário está em outro fluxo;
  - limpar imagens/jobs/manifesto ao reexecutar `Gera Prompt Imagem`, evitando que imagens antigas contaminem a próxima execução;
  - ocultar ou desabilitar ações de geração em experimento já enviado/publicado, preservando apenas histórico e consulta.
- **Encadeamentos sensíveis**:
  - Wireframe → Copy;
  - Copy → Prompt Imagem;
  - Prompt Imagem → Gera Imagem;
  - Gera Imagem → Preset Design;
  - Preset Design → Quality Review;
  - AD_IMAGE_BRIEFING → geração de criativos de anúncio, quando explicitamente solicitado.
- **Fechamento mínimo do loop**:
  - cada etapa declarar `manual`, `auto`, `retry`, `disabled-after-publication`;
  - teste de sucesso cria próxima etapa;
  - teste de falha não cria próxima etapa;
  - teste de reexecução limpa artefatos dependentes quando necessário.
- **Regra preventiva**:
  - antes de ativar automação, declarar qual etapa anterior autoriza, qual próxima etapa nasce e quais campos serão limpos ou preservados.

## LOOP-QUALITY-REVIEW-VISION — Quality Review visual

- **Severidade**: ALTO.
- **Status**: parcialmente estabilizado.
- **Sintomas recorrentes**:
  - revisão avalia imagem solta em vez da landing renderizada;
  - pixel/script é enviado como imagem;
  - screenshot falha por timeout;
  - desktop e mobile têm prioridades confusas;
  - execuções diferentes avaliam evidência igual, mas decisões divergem;
  - prompt textual fica longo demais e compete com a evidência visual.
- **Causa-raiz sistêmica provável**:
  - a evidência visual canônica não estava fechada desde o começo.
- **O que resolveu efetivamente no histórico**:
  - abandonar avaliação por imagens soltas extraídas do HTML e passar a renderizar o HTML em browser/headless;
  - enviar screenshots renderizados para o modelo de visão, com mobile como evidência prioritária;
  - aceitar desktop como complementar, sem impedir a revisão quando o mobile obrigatório já foi capturado;
  - aumentar timeout de screenshot e voltar ao full-page quando recortes prejudicavam a evidência;
  - usar modelo de visão dedicado e configuração própria de `imageDetail`;
  - reduzir o prompt textual quando os screenshots já representam a evidência principal;
  - calcular e persistir hashes de HTML, prompt/request e screenshots para detectar reuso de evidência e contradição entre avaliações;
  - exibir na tela de detalhe os screenshots e dados de auditoria enviados ao modelo.
- **Contrato recomendado**:
  - fonte única: `html_geralanding`;
  - renderização em browser/headless;
  - screenshot mobile obrigatório;
  - screenshot desktop complementar;
  - hash de HTML, request e screenshots;
  - modelo de visão dedicado;
  - prompt curto e visual;
  - resposta com score, bloqueios, recomendação de publicação e etapa sugerida para regeneração.
- **Fechamento mínimo do loop**:
  - teste garantindo que o request usa screenshots renderizados, não imagens extraídas do HTML;
  - teste de auditoria com hashes;
  - UI exibindo evidências visuais usadas na decisão.
- **Regra preventiva**:
  - nenhuma decisão de Quality Review deve ser analisada sem conferir qual screenshot/hash foi avaliado.
- **Recalibração em 2026-08-11:** 44 revisões produtivas mostraram teto de score 88 e apenas uma aprovação histórica, com score 86, enquanto avaliações recentes entre 84 e 88 ainda misturavam bloqueios reais com refinamentos opcionais. O gate deixou de depender do corte isolado de 90 e passou a exigir score mínimo 85, piso 8/10 por dimensão, prontidão comercial, especificidade e ausência de bloqueios; melhorias opcionais foram separadas em `improvementOpportunities`. O validator determinístico impede aprovação inconsistente.

## LOOP-LANDING-ANALYTICS-FUNNEL — Analytics, funil e submissão

- Em 2026-08-03, o GeraSalesPage foi protegido contra publicar o coletor público usando a rota administrativa `/mh-api/internal/...`. Páginas servidas pelo Lead Portal devem enviar eventos para `/api/flows/{slug}/page-analytics`; o endpoint interno continua sendo responsabilidade exclusiva do backend do Lead Portal ao encaminhar o evento.
- Em 2026-08-03, a resolução do experimento pelo destino da campanha passou a aceitar também a rota pública amigável `/flows/{slug}`. O gate do GeraSalesPage já exigia essa URL, mas o analytics reconhecia apenas `/api/flows/{slug}/page`, fazendo a página aprovada retornar 502 ao registrar eventos.
- Em 2026-08-07, a leitura de fluxos deixou de buscar `questions` e `questions.options` no mesmo `EntityGraph`: o produto cartesiano repetia uma pergunta uma vez por opção e publicava três blocos idênticos no formulário. As opções agora são carregadas em subconsulta separada. A tela do experimento também deixou de ocultar por condição fixa o comando oficial de criação e auditoria do GeraSalesPage.
- Em 2026-08-07, a republicação de fluxos aprovados no bootstrap passou a usar a leitura canônica do service, que inicializa perguntas e opções dentro da transação. Acesso direto do republicador ao repository deixava `options` lazy após o encerramento da sessão, derrubava o backend no `ApplicationReadyEvent` e preservava no portal a versão antiga com perguntas duplicadas. Testes do service e do republicador impedem o retorno desse atalho.
- Em 2026-08-07, a subconsulta de opções passou a ser executada explicitamente no service durante a transação de leitura. Apenas remover `questions.options` do `EntityGraph` evitava a duplicação, mas deixava a coleção lazy indisponível quando o controller montava o DTO. O teste de contrato agora exige simultaneamente uma pergunta e suas três opções.
- Em 2026-08-07, o comando de criação do funil passou a gravar sua URL pública como destino inicial do experimento comercial. Antes, o fluxo era publicado e aprovado, mas `follow_up_action_url` permanecia vazio e o GeraSalesPage recusava iniciar; homologações `FAKE_EXPERIMENT` continuam sem URL comercial para preservar o isolamento.
- Em 2026-08-07, o Lead Portal passou a desserializar o wrapper JSON do GeraSalesPage antes de procurar marcas de HTML. Antes, o `<!doctype>` contido em `htmlDocument` fazia o JSON inteiro ser tratado como HTML, exibindo metadados técnicos, mantendo SVGs escapados e degradando a experiência mobile. O contrato preventivo exige que payload iniciado por `{` ou `[` seja normalizado como JSON antes da detecção de HTML puro.
- Em 2026-08-07, a prontidão passou a tratar `STORY` e `LINK` com `image_url` como imagens publicáveis, preservando `VIDEO` como mídia separada. O resumo de Produto IA `AI_PERSONALIZED_SAMPLE` também deixou de exigir o GeraLanding paralelo e passou a validar a publicação auditada do GeraSalesPage v1 dentro do funil aprovado.
- Em 2026-08-07, a imagem final do backend do Lead Portal passou a instalar explicitamente o `curl` usado pelo healthcheck do Compose. Antes, o runtime `eclipse-temurin:21-jre` não garantia esse binário, o backend era marcado como `unhealthy` mesmo após iniciar e o proxy público não subia. O CI agora valida a dependência do healthcheck e o deploy imprime estado e logs dos containers quando a publicação falha.
- Em 2026-08-07, o MCP passou a expor pelo alvo restrito `lead-portal-stack` o estado e os logs dos containers canônicos `lead-portal-backend`, `lead-portal-frontend` e `lead-portal-proxy`. Antes, a análise dependia apenas do logfile HTTP do backend e não conseguia distinguir indisponibilidade do backend, frontend ou proxy; a consulta remota continua limitada a host e alvo em allowlist, sem shell ou nome de container informado pelo cliente.
- Em 2026-08-07, templates standalone com formulário gerenciado passaram a substituir a página pela confirmação persistente após HTTP 201. Antes, o feedback era escrito em um nó recriado pelo React e desaparecia imediatamente, fazendo a cliente acreditar que o envio havia travado. O runtime também passou a limitar conteúdo e mídias à viewport. A proteção usa precedência explícita porque o CSS do HTML do GeraSalesPage é anexado depois do CSS do portal; sem `!important`, uma grade gerada podia restaurar `min-width: auto` e causar rolagem horizontal mesmo com a regra preventiva presente.
- Em 2026-08-08, os templates v11 do GeraSalesPage separaram a linguagem interna do formulário da copy pública. O prompt v10 instruía o HTML a usar o “formulário gerenciado” e simultaneamente exigia que o quality review bloqueasse termos internos, criando reprovação determinística. Copy e HTML agora apresentam apenas “formulário” ou “responder algumas informações”, enquanto a auditoria continua bloqueando nomes de implementação.
- Em 2026-08-08, o comando da tela passou a usar sempre o `rebuild` auditável. Antes, uma rodada reprovada antes da primeira publicação mantinha a contagem de publicações em zero; a tela chamava `start`, que reutilizava a primeira etapa concluída por idempotência e nunca exercitava o template corrigido. O teste de contrato impede que a interface volte a escolher o comando pela existência de publicação.
- Em 2026-08-07, o marco de reset do funil passou a ser convertido explicitamente de `Instant` para `DATETIME` UTC antes das consultas JDBC. Antes, o driver aplicava o fuso local ao parâmetro e submissões técnicas anteriores ao reset continuavam contabilizadas; o painel podia iniciar um experimento comercial com conversões falsas mesmo após zerar as contagens.

- **Severidade**: CRÍTICO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - navegador indica envio, mas funil aparece zerado;
  - evento existe no banco, mas resumo não conta;
  - submissão cai em endpoint inexistente no domínio público;
  - reset do funil quebra por FK ou não limpa analytics normalizado;
  - landing antiga carrega script antigo sem debug;
  - `visitorId`, `sessionId`, device e OS entram em momentos diferentes do contrato.
- **Causa-raiz sistêmica provável**:
  - produção, persistência, normalização e consumo de eventos evoluíram separadamente.
- **O que resolveu efetivamente no histórico**:
  - injetar script de analytics no Lead Portal para a landing standalone realmente chamar o backend;
  - substituir instrumentação legada quando a landing já publicada tinha script antigo sem debug;
  - criar rota local de compatibilidade no Lead Portal para submissão pública e encaminhamento ao backend principal;
  - contar `landing-page-analytics` no resumo do funil, em vez de considerar apenas fontes legadas;
  - somar submissões públicas vindas de `experiment_funnel_event` na etapa `ENVIO_FORM`, com deduplicação por `submissionId`;
  - criar tabela normalizada `experiment_landing_analytics_event` vinculada ao evento bruto, preservando auditoria e permitindo recorrência por `visitorId`;
  - deduplicar `page_view` por `visitorId`, `sessionId`, `eventType` e `pageUrl` em janela curta;
  - usar `page_view` normalizado como fonte canônica da etapa de visualização no funil, mantendo `render-complete` apenas como fallback legado para não somar duas fontes da mesma visita;
  - no reset, apagar primeiro eventos normalizados e depois eventos brutos, evitando violação de FK;
  - invalidar também a query da aba Analytics no frontend após zerar contagens;
  - enviar `deviceType`, sistema operacional e tamanho de tela pelo script público para apoiar decisão de layout/mobile;
  - serializar horários de analytics vindos de `DATETIME` com offset operacional explícito, evitando que a UI interprete horário de Brasília como UTC.
  - classificar verificações internas e navegadores automatizados por sessão, preservando-os para auditoria e excluindo-os de audiência, abandono e desempenho comerciais;
  - deduplicar marcos de página, vídeo, CTA e formulário por sessão na consolidação, sem apagar os eventos brutos;
  - usar `Instant` como contrato temporal único entre eventos detalhados, sessões e visitantes.
- **Contratos sensíveis**:
  - `experiment_funnel_event`;
  - `experiment_landing_analytics_event`;
  - `source=landing-page-analytics`;
  - `visitorId` provável;
  - `sessionId`;
  - `eventType`;
  - `page_view`, `section_view_time`, `ENVIO_FORM`;
  - deduplicação de `page_view` em 3 segundos.
- **Fechamento mínimo do loop**:
  - registry de eventos com fonte, payload, tabela bruta, tabela normalizada e query de resumo;
  - teste: evento enviado pelo endpoint público aparece no funil e na aba Analytics;
  - teste: resumo do funil não soma `render-complete` com `page_view` normalizado na mesma etapa;
  - teste: reset apaga normalizados antes dos brutos;
  - teste: submissão pública soma `ENVIO_FORM` sem duplicar;
  - teste: jornada recente serializa `DATETIME` operacional com offset de Brasília.
  - teste: monitor interno permanece auditável, mas não altera sessões humanas nem tempo de carregamento;
  - teste: marco repetido na mesma sessão conta uma única vez no resumo comercial.
- **Regra preventiva**:
  - todo novo evento de landing só está pronto quando aparecer na UI que o usuário usa para decisão.

## LOOP-PIPELINE-ADMIN-CONTRACT — Tela `/pipelines` e contrato persistente

- **Severidade**: MÉDIO.
- **Status**: estabilizado com risco.
- **Sintomas recorrentes**:
  - tela permite criar/editar estrutura que deveria ser canônica;
  - etapa oficial ausente no banco;
  - etapa extra quebra diagnóstico;
  - Liquibase falha por posição duplicada;
  - definição e configuração operacional ficam misturadas;
  - modelo OpenAI configurado não aparece na etapa operacional.
- **Causa-raiz sistêmica provável**:
  - CRUD livre foi usado para dados que são contrato oficial de execução.
- **O que resolveu efetivamente no histórico**:
  - criar registry oficial de pipelines/etapas no backend;
  - expor diagnóstico de contrato na tela, mostrando divergências entre banco e cânone;
  - bloquear exclusão e alteração estrutural de pipeline oficial;
  - criar sincronizador seguro para etapas oficiais ausentes e correções estruturais não destrutivas;
  - criar rebuild controlado com confirmação para remover etapas operacionais divergentes e recriar somente as canônicas;
  - separar definição persistente (`pipeline_definition`, `pipeline_stage_definition`) de configuração operacional (`pipeline_stage_config`);
  - preservar modelo OpenAI, descrição e status operacional durante sincronização quando houver mapeamento seguro;
  - remover criação manual de pipeline/etapa no frontend;
  - ajustar changelogs de posição usando faixa temporária para evitar conflito de unique key no MySQL 5.7;
  - expor metadados de implementação e modelos por etapa para a tela de experimento/GeraLanding.
- **Fechamento mínimo do loop**:
  - tela só edita configuração operacional;
  - definição oficial vem do registry/cânone/sincronizador;
  - rebuild destrutivo exige confirmação explícita;
  - changelogs de posição usam faixa temporária para evitar unique conflict;
  - endpoint de metadados mostra implementação real por etapa.
- **Regra preventiva**:
  - pipeline oficial não é cadastro livre; é contrato sincronizado com campos operacionais editáveis.

## LOOP-HYPOTHESIS-PIPELINE — Pipeline de hipótese

- **Severidade**: ALTO.
- **Status**: em formação.
- **Sintomas recorrentes**:
  - etapa aparece fora de ordem;
  - Oferta executa sem Prova;
  - Worker AI não possui etapa correspondente;
  - job fica preso em `INICIADO`, `PROCESSANDO` ou `AGUARDANDO_RETORNO_OPENAI`;
  - fechamento da hipótese fica dentro da etapa Dor;
  - campo de banco não comporta resposta completa;
  - custo e relatório auditável entram depois da execução.
- **Causa-raiz sistêmica provável**:
  - o pipeline foi crescendo etapa por etapa, sem matriz inicial completa do fluxo Dor → Resultado → Mecanismo → Prova → Oferta → Fechamento.
- **O que resolveu efetivamente no histórico**:
  - completar a sequência com a etapa Prova entre Mecanismo e Oferta;
  - bloquear Oferta sem Prova concluída tanto na criação manual quanto na fila de pendentes;
  - criar Worker AI específico para cada etapa que existia no backend;
  - revalidar pré-requisitos no pending e na marcação de processamento, não apenas na tela;
  - extrair o fechamento da hipótese para `HypothesisPipelineFinalizationService`, fora da etapa Dor;
  - converter coluna insuficiente para armazenar resposta completa, como `success_rule` para `LONGTEXT`;
  - criar lease operacional para jobs presos em `PROCESSANDO` ou `AGUARDANDO_RETORNO_OPENAI`;
  - persistir `raw_response`, prompt, request cru e custo por etapa para relatório auditável;
  - adicionar fluxo completo automático com retry controlado, mantendo a orquestração no backend;
  - passar contexto enriquecido do nicho-cnae para todas as etapas sem criar oferta prematura fora da etapa Oferta.
- **Fechamento mínimo do loop**:
  - cada etapa declarar pré-requisito, próximo passo, campo final, prompt, schema, worker, endpoint e relatório;
  - Oferta exige Prova concluída tanto no start quanto no pending;
  - finalização da hipótese fica em service próprio;
  - lease operacional para jobs presos;
  - `raw_response`, request, prompt e custo persistidos por etapa.
- **Regra preventiva**:
  - não adicionar etapa na tela sem backend, worker, Swagger, lease, custo e pré-requisito equivalente.

## LOOP-ARTIFACT-CONTAMINATION — Metadado técnico em artefato final

- **Severidade**: ALTO.
- **Status**: estabilizado com risco.
- **Sintomas recorrentes**:
  - HTML final recebe comentário `AUTO`;
  - título técnico aparece na landing;
  - payload final inclui campo legado ou de debug;
  - JSON técnico fica serializado dentro de campo textual;
  - Quality Review aponta metadado visível ou aparência provisória.
- **Causa-raiz sistêmica provável**:
  - metadados de execução foram misturados com artefatos publicáveis.
- **O que resolveu efetivamente no histórico**:
  - remover comentários técnicos `<!-- AUTO: ... -->` dos HTMLs provisórios/finais;
  - impedir título técnico como `Wireframe provisório` no HTML final;
  - separar `html_geralanding` como artefato puro de geração e `landing_page_html` como versão publicável instrumentada;
  - formalizar no AGENTS a proibição de contaminar artefato final com metadado técnico;
  - usar whitelist de campos do DTO final antes de enviar payload publicável;
  - tratar auditoria, jobId, prompt, schema, request, resposta e hashes como dados de execução, não como conteúdo do cliente;
  - fazer o Quality Review apontar metadado técnico visível como problema bloqueante.
- **Campos/artefatos sensíveis**:
  - HTML final;
  - `html_geralanding`;
  - `landing_page_html`;
  - `customFormHtml`;
  - JSON final de etapa;
  - criativo aprovado;
  - relatório público.
- **Fechamento mínimo do loop**:
  - whitelist de DTO final;
  - teste de ausência de comentários técnicos;
  - separação explícita entre auditoria e artefato final;
  - Quality Review deve apontar contaminação como bloqueio.
- **Regra preventiva**:
  - todo metadado técnico deve ir para tabela/campo de auditoria, nunca para conteúdo publicável.

## LOOP-COST-MODEL-AUDIT — Custos OpenAI e modelo por etapa

- **Severidade**: MÉDIO.
- **Status**: em observação.
- **Sintomas recorrentes**:
  - custo aparece `$0.00` apesar de tokens retornados;
  - modelo da etapa não aparece na tela;
  - request auditado não mostra o `service_tier` efetivo da etapa;
  - Worker AI usa preço hardcoded ou propriedade zerada;
  - modelo configurado em `/pipelines` não chega à execução.
- **Causa-raiz sistêmica provável**:
  - seleção de modelo, modo de preço, catálogo de preço e cálculo de custo ficavam em fontes diferentes.
- **O que resolveu efetivamente no histórico**:
  - remover tabela hardcoded de preços do Worker AI;
  - fazer o Worker AI consultar o catálogo do backend em vez de acessar banco diretamente;
  - calcular custo pelo modelo efetivo do request e pelos preços cadastrados em `openai_model`;
  - persistir e exibir `inputTokens`, `outputTokens` e `costUsd` por execução;
  - expor `GET /api/pipelines/geralanding/stage-models` com modelo, preço flex, tipo de artefato e fallback aplicado;
  - mostrar na aba GeraLanding o modelo configurado, custos flex por 1M tokens e custo acumulado;
  - montar request auditável com `service_tier=flex` desde a origem nas etapas em que isso era necessário;
  - no pipeline de hipótese, recalcular custo no backend com base nos tokens/modelo/preços persistidos, sem confiar cegamente no `costUsd` enviado pelo worker.
- **Fechamento mínimo do loop**:
  - modelo por etapa vindo do pipeline/catálogo;
  - fallback default explícito por tipo de artefato;
  - custo calculado via backend/catalogo `openai_model`;
  - request auditável sempre com o `service_tier` efetivo da etapa;
  - exceções ao Flex, como Quality Review em processamento default/standard por indisponibilidade operacional do Flex em requisições multimodais grandes, devem registrar justificativa funcional no fluxo;
  - UI mostra modelo, modo, preço e custo acumulado.
- **Regra preventiva**:
  - nenhuma etapa OpenAI deve persistir execução sem modelo efetivo, tokens e regra de preço identificável.

## LOOP-EXPERIMENT-COST-RECONCILIATION — Total de custo sem origem auditável

- **Severidade**: ALTO.
- **Status**: fechado em 2026-07-07.
- **Sintomas recorrentes**:
  - `experiment.total_cost` maior que a soma de origem, mídia e despesa;
  - custo técnico em USD aparecendo como se fechasse total em BRL;
  - diferença legada sendo interpretada como custo real de IA;
  - reprocessamento ou sincronização de mídia inflando custo acumulado.
- **Causa-raiz sistêmica provável**:
  - custo total tratado como acumulador persistido e fonte principal de verdade, sem razão idempotente por origem;
  - atualização do custo combinando entidade JPA gerenciada com `increment` SQL direto.
- **O que resolveu efetivamente no histórico**:
  - usar custo rastreável em BRL como total principal da tela;
  - manter `total_cost` como legado e mostrar diferença positiva como custo não reconciliado;
  - separar auditoria técnica em USD de parcelas financeiras em BRL;
  - impedir `incrementTotalCost` SQL quando a entidade já está gerenciada pelo JPA.
- **Fechamento mínimo do loop**:
  - toda tela ou relatório de experimento deve diferenciar custo rastreável, total legado e diferença não reconciliada;
  - custos OpenAI/GeraLanding/GeraSalesPage em USD entram como auditoria técnica, não como parcela BRL sem conversão rastreável;
  - sincronização de mídia deve aplicar apenas delta e ter teste de regressão;
  - atribuição de custo não pode persistir o mesmo delta por dois caminhos na mesma transação.
- **Regra preventiva**:
  - nunca usar `experiment.total_cost` isolado como explicação financeira principal; sempre reconciliar por origem auditável ou marcar como legado não reconciliado.

---

## Checklist rápido antes de corrigir problema recorrente

Use este checklist quando o problema estiver em algum loop acima:

```md
- O problema reabre qual LOOP-\*?
- Qual contrato está divergindo?
- Qual correção efetiva já resolveu esse tipo de loop antes?
- Estou repetindo uma solução antiga que já foi superada?
- Qual módulo é dono da correção?
- O frontend, backend, worker, Swagger e cânone estão alinhados?
- Existe teste que reproduz a falha atual?
- Existe teste que impede o mesmo loop de voltar?
- O registro operacional foi atualizado no documento do tema?
```

## Registros deste documento

## 2026-06-17 00:01:07 UTC-3

- solicitação: criar um arquivo de registro de loops operacionais a partir da análise de `docs/registros/experimentos.md` e revisar o `AGENTS.md` para melhoria preventiva.
- causa-raiz observada: o histórico mostra recorrência de problemas por contratos instáveis entre frontend, backend, workers, OpenAI, Lead Portal e Meta Ads.
- registro do que foi feito: criado este documento com os principais loops, causa-raiz sistêmica, fechamento mínimo e regra preventiva por tema; incluída sugestão objetiva de melhoria para o `AGENTS.md`.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-06-17 00:12:41 UTC-3

- solicitação: melhorar este arquivo identificando, para cada loop, o que resolveu efetivamente o problema no histórico real do projeto.
- causa-raiz observada: a primeira versão registrava sintomas, causas e prevenção, mas ainda não destacava claramente quais correções concretas estabilizaram cada ciclo.
- registro do que foi feito: adicionado o bloco **O que resolveu efetivamente no histórico** em cada `LOOP-*`, diferenciando correção efetiva de prevenção futura.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/registros/experimentos.md

# LOOP-LANDING-DYNAMIC-CRITICAL-PATH — Landing comercial dependente de shell React e API lenta

- **Sintoma:** clique abre um shell vazio e o conteúdo comercial aparece vários segundos depois.
- **Causa-raiz:** a rota pública `/flows/exp-*-gerasalespage-v1` carregava o SPA antes de buscar novamente o HTML da landing no backend.
- **Prevenção:** o proxy serve a landing GeraSalesPage diretamente, usa cache curto com stale seguro e o deploy valida o HTML instrumentado na rota pública. Web Vitals reais continuam vinculados às sessões humanas.
- Em 2026-08-07, o funil administrativo foi protegido para que experimentos `FAKE_EXPERIMENT` sem URL comercial não sejam interpretados como PDE. A detecção de versão agora trata URL nula ou vazia como ausência de versão, e um teste de contrato impede novo HTTP 500 ao homologar ofertas isoladas.

# LOOP-LANDING-DUPLICATE-ANALYTICS — Coletores concorrentes na landing pública

- **Sintoma:** a mesma landing envia eventos tanto para o slug correto quanto para `/api/flows/page/page-analytics`, duplicando sessões e atrasando requisições de telemetria.
- **Causa-raiz:** o HTML persistido pelo GeraSalesPage continha um coletor próprio que inferia o slug pelo último segmento da URL, enquanto o Lead Portal injetava um segundo coletor canônico com o slug explícito.
- **Prevenção:** ao servir a landing standalone, o Lead Portal remove qualquer coletor legado `data-mh-sales-page-analytics` e mantém somente `data-mh-landing-analytics`, com slug recebido pelo controller e teste de contrato contra regressão.
- Em 2026-08-08, a homologação do experimento #88 encontrou nova duplicidade: o coletor canônico da landing e a ponte React do formulário gerenciado registravam `form_start`/`form_submit` para a mesma interação. A ponte agora detecta `data-mh-landing-analytics` e delega esses eventos exclusivamente ao coletor canônico.

# LOOP-GERALANDING-AGENT-AFTER-COMMIT — Parecer não chega ao Agente Gerador de Landing

- **Sintoma:** o Quality Review conclui com `REGENERATE_BEFORE_PUBLICATION`, mas nenhuma execução `landing-generation-agent-v1` é persistida e Dédalo não inicia a correção.
- **Causa-raiz:** o listener `AFTER_COMMIT` chamava a persistência da fila dentro da transação já concluída; a autoinvocação de `enqueue` não abria outra fronteira transacional e a gravação era descartada sem erro.
- **Prevenção:** o listener abre transação `REQUIRES_NEW` para memória, decisão e fila do agente; teste de contrato protege explicitamente essa propagação.
- **Fechamento complementar em 2026-08-13:** homologações comerciais interrompidas por uma versão antiga do executor passam a registrar o build que reservou a execução e podem ser retomadas uma única vez por um build novo. A política é explícita no briefing, preserva carência e idempotência e não reabre decisões funcionais concluídas.

# LOOP-GROWTH-OPERATOR-REPEATED-DIAGNOSIS — Ciclos sem evidência nova

- **Sintoma:** o Operador consome IA a cada 30 minutos e repete a mesma conclusão sem mudança de sessão, custo, venda, vídeo, falha ou prazo.
- **Causa-raiz:** o backend usava apenas cadência temporal; a memória do próprio ciclo também crescia e poderia aparentar mudança.
- **Prevenção:** criar ciclo automático somente quando mudar o fingerprint das evidências operacionais, excluindo a memória acumulada, e persistir as ferramentas MCP realmente usadas na conclusão.

# LOOP-AGENT-IMAGE-OPERATOR-GROUP — Imagem de agente recria grupo da imagem-base

- **Sintoma:** testes do worker passam, mas o build Docker falha com `groupadd: group 'operator' already exists` e impede o deploy do agente.
- **Causa-raiz:** o Dockerfile assumia que usuário e grupo `operator` não existiam, embora a instalação do Codex ou a imagem-base já pudesse fornecer o grupo.
- **Prevenção:** reutilizar usuário/grupo existentes e criá-los somente quando ausentes; todo workflow de agente deve executar teste de contrato e construir a imagem antes do deploy.

# LOOP-CUSTOMER-AGENT-OBSERVATION-ORPHAN — Observação mobile presa em RUNNING

- **Sintoma:** observações permanecem indefinidamente em `RUNNING` após interrupção do worker; outras terminam por timeout sem gerar parecer ou memória.
- **Causa-raiz:** a reserva não tinha expiração; o schema Structured Outputs era inválido em objetos internos; e o Codex herdava configuração/sessão, recebia instrução para navegar novamente e misturava a saída final com logs do processo.
- **Prevenção:** lease backend de quinze minutos com encerramento auditável, schema estrito coberto por execução real, Codex efêmero sem ferramentas ou configuração herdada, fatos do Chromium como única entrada, JSON final em arquivo dedicado e timeout de quatro minutos coberto por teste.

# LOOP-AGENT-CODEX-HOME-UNAUTHENTICATED — Worker inicia sem sessão Codex

- **Sintoma:** build e inicialização Spring passam, mas o deploy expira ao executar `codex login status`.
- **Causa-raiz:** o Compose monta um diretório novo e vazio como `CODEX_HOME`, confundindo volume gravável com volume autenticado.
- **Prevenção:** agentes com o mesmo UID usam o volume persistente autenticado `/opt/growth-operator/codex-home`; o workflow valida escrita e identidade Codex e informa explicitamente qual contrato de prontidão falhou.
- **Correção complementar em 2026-08-12:** os workflows deixaram de copiar `auth.json` para diretórios isolados. As cópias faziam vários agentes renovarem o mesmo refresh token independentemente e geravam `refresh_token_reused`. O deploy reconcilia atomicamente a sessão mais recente, todos os agentes montam o mesmo `CODEX_HOME` persistente e um teste de contrato impede a clonagem de retornar.
- **Correção complementar OAuth por device code em 2026-08-12:** a sessão canônica deixa de ser substituída por cópias legadas, todas as execuções Codex são serializadas por um lock no volume compartilhado e uma rotina interativa renova a conta operacional uma única vez com `codex login --device-auth`. Isso preserva a cobrança ChatGPT e impede duas renovações concorrentes do mesmo refresh token.
- **Correção complementar App Server em 2026-08-12:** a reconexão deixou de executar `logout` e de depender do fluxo interativo direto do CLI. Ela agora usa o contrato do Codex App Server adotado em `/exemplos/aih6` (`account/login/start` com `chatgptDeviceCode`, `account/login/completed` e `account/read`), preservando a sessão anterior até a nova autenticação ser confirmada e sem montar ou trocar tokens OAuth manualmente.
- **Correção complementar de runtime em 2026-08-13:** o script de reconexão não pressupõe mais Node.js e Codex instalados no host. Quando esses runtimes não existem, ele localiza o container ativo do Dédalo, confirma que o volume montado corresponde à sessão OAuth canônica e executa o App Server dentro da imagem versionada do worker.
- **Correção complementar de operação em 2026-08-13:** a reconexão passa a ser solicitada e acompanhada no painel administrativo. O backend persiste o comando e a auditoria, Dédalo coleta pelo endpoint `pending`, o Codex App Server reporta apenas link, código temporário e resultado, e falhas OAuth do Dédalo podem ser reabertas uma única vez sem SSH ou exposição de tokens.
- **Correção complementar de isolamento em 2026-08-13:** frontend e backend deixaram de restringir a reconexão ao Dédalo; cada um dos seis executores Codex consome apenas sua própria fila de device code e os workflows montam um `CODEX_HOME` persistente exclusivo por agente. Testes de contrato impedem que o painel volte a esconder a ação dos demais executores Codex.
- **Correção complementar de sincronização em 2026-08-13:** os seis workflows passam a observar e sincronizar explicitamente o cliente versionado `scripts/codex-app-server-device-login.mjs`. Antes, cada deploy atualizava somente a pasta do worker, mas o runtime executava o cliente compartilhado na raiz do host; isso permitia publicar executor novo com protocolo de autenticação antigo e encerramento antes de devolver o device code. O teste de isolamento exige agora gatilho e `rsync` do cliente em todos os agentes.
- **Correção complementar de Argos em 2026-08-14:** `market-radar` entrou no contrato canônico de reconexão, o Product Discovery Worker passou a consumir exclusivamente sua fila e sua imagem inclui o cliente App Server versionado. O `CODEX_HOME` individual deixou de ser somente leitura, pois a autenticação confirmada precisa persistir a sessão; credenciais de Hotmart e ClickBank permanecem fora desse volume e do agente.

# LOOP-DEPLOY-GLOBAL-TIMEOUT — Deploy interrompido durante carga das imagens

- **Sintoma:** imagens são transferidas corretamente, mas o workflow termina com código `124` enquanto o Compose começa a recriar backend e frontend, deixando o backend indisponível.
- **Causa-raiz:** um limite externo único de 18 minutos concorria com os limites internos da carga de imagens, recriação e saúde; o tempo consumido por uma fase retirava a janela necessária das seguintes.
- **Prevenção:** carga de cada imagem e recriação mantêm limites próprios e diagnósticos; o limite externo cobre o ciclo completo sem interromper prematuramente; uma etapa final independente exige container backend em execução e endpoint canônico saudável, exibindo estado, reinícios e logs em caso de falha.
- **Correção complementar em 2026-08-06:** após um deploy consumir quase toda a janela operacional e o Spring iniciar tarde sem alcançar o health check, o publicador passou a reservar até dez minutos exclusivamente para a saúde do backend, preservar as imagens `latest` anteriores antes da troca e restaurá-las automaticamente se a nova versão ou sua saúde falhar. Um teste de contrato impede remover a janela independente, a preservação ou a validação do rollback.

# LOOP-LEAD-PORTAL-PROCESSING-TIMEZONE — Pacote falha antes do timeout real

- **Sintoma:** pacote de amostra entra em `PROCESSING` e é marcado como falho poucos minutos depois com a mensagem de que permaneceu 30 minutos sem retorno.
- **Causa-raiz:** `updated_at` é `DATETIME` preenchido com `CURRENT_TIMESTAMP` na sessão MySQL UTC-3, mas o watchdog comparava esse valor sem fuso com `UTC_TIMESTAMP()`, tornando todo pacote três horas mais antigo no instante da comparação.
- **Prevenção:** cálculos de expiração sobre `DATETIME` gravado pelo banco devem usar `CURRENT_TIMESTAMP()` da mesma sessão. Teste de contrato do watchdog exige esse relógio e impede o retorno de `UTC_TIMESTAMP()` nessa consulta.

### LOOP-LEAD-PORTAL-MICROAMOSTRA-SEM-IMAGENS-LIVRES

- **Sintoma:** o funil promete uma microamostra gratuita, mas o pacote é concluído com `free_images = 0`, impedindo que as saídas geradas sejam tratadas como a entrega gratuita contratada.
- **Causa-raiz:** o montador de prompt atribuía zero imagens gratuitas a todo formulário simples, sem considerar o modelo canônico `AI_PERSONALIZED_SAMPLE_FUNNEL`.
- **Prevenção:** fluxos desse modelo liberam como gratuitas todas as saídas planejadas; teste de contrato exige que a quantidade gratuita seja igual ao lote configurado, sem ampliar a regra para formulários pagos.

### LOOP-LEAD-PORTAL-MODELO-PERSISTIDO-IGNORADO

- **Sintoma:** a microamostra registra `gpt-image-1`, mas o worker chama outro modelo e recebe `Unknown parameter: response_format` em todas as tentativas.
- **Causa-raiz:** pacotes legados sem `image_model_id` ignoravam o campo textual `model` já persistido e escolhiam o primeiro modelo do catálogo, cuja ordem não representa preferência operacional.
- **Correção sistêmica:** o planejador agora resolve primeiro IDs explícitos, depois o modelo persistido por `apiModel` e somente então usa o fallback do catálogo.
- **Prevenção:** teste de contrato mantém `gpt-image-1` mesmo quando `dall-e-2` aparece primeiro no catálogo e o pacote não possui IDs novos.

### LOOP-LEAD-PORTAL-AMOSTRA-USA-SLUG-COMO-SERVICO

- **Sintoma:** a amostra do experimento #88 escreve `product ai exp 88 personalized sample` nas imagens, embora o formulário informe `Alongamento em gel delicado`, e persiste `free_images = 0`.
- **Causa-raiz:** o Lead Portal reconhecia como amostra gratuita apenas o modelo legado `AI_PERSONALIZED_SAMPLE_FUNNEL`; o contrato atual do GeraSalesPage usa `AI_PERSONALIZED_SAMPLE_GERA_SALES_PAGE`. Sem reconhecer esse modelo, o prompt genérico derivava atividade e serviços do slug técnico e não liberava o lote prometido.
- **Correção sistêmica:** ambos os contratos passam pela política de amostra personalizada, que usa diretamente nome, serviço e estilo respondidos, proíbe termos técnicos na imagem e libera todo o lote como prévia gratuita.
- **Prevenção:** teste de contrato reproduz o modelo e os campos publicados pelo experimento #88, exigindo seis imagens livres, conteúdo comercial real e ausência do slug como profissão ou serviço.
- **Correção complementar em 2026-08-09:** a homologação com GPT Image 2 mostrou que o worker já executava seis chamadas, mas o prompt de cada chamada também pedia seis variações; cada arquivo virou uma colagem com seis miniartes. O prompt versionado agora exige exatamente uma arte ocupando todo o arquivo por chamada e proíbe grade, mosaico, colagem, carrossel ou múltiplos quadros. O teste de contrato mantém o lote de seis no envelope operacional, mas impede que essa quantidade seja repetida como pedido de múltiplas artes dentro de cada imagem.

### LOOP-LEAD-PORTAL-FORMULARIO-INVALIDO-ENVIADO

- **Sintoma:** ao tocar no CTA com campos obrigatórios vazios, a página exibe erro genérico de envio e registra uma exceção técnica, em vez de orientar o preenchimento do primeiro campo inválido.
- **Causa-raiz:** o runtime gerenciado interceptava o evento `submit` e montava o contrato antes de executar a validação HTML do formulário; templates ou scripts que disparassem o evento diretamente contornavam a barreira nativa do navegador.
- **Prevenção:** o runtime executa `checkValidity()` e `reportValidity()` antes de marcar o formulário como em submissão ou chamar a API; o contrato do frontend exige que essa validação permaneça antes do início da submissão.
- **Correção complementar em 2026-08-07:** a homologação produtiva revelou que bridges antigas do mesmo documento podiam permanecer concorrendo pelo evento, consumir a resposta `201` com callback desmontado e impedir a confirmação visual. O runtime agora mantém uma única bridge ativa por documento, entrega o sucesso ao React antes de eventos auxiliares e aplica a contenção horizontal no `body` durante páginas standalone. O contrato do frontend protege a exclusividade da bridge e a ativação da contenção.

# LOOP-2026-08-07 — Homologação `mh_test=1` classificada como tráfego humano

- **Sintoma:** sessões mobile e envios técnicos do experimento comercial apareciam nos eventos do funil após testes com `mh_test=1`.
- **Causa-raiz:** o HTML legado ainda emitia analytics e o classificador dependia do user-agent de automação; a emulação mobile do Playwright usa user-agent normal. A submissão também não propagava um marcador técnico.
- **Correção sistêmica:** classificar `mh_test=1` como automação, marcar submissões com `__mh_internal_test__`, excluí-las das métricas comerciais e impedir que acionem standby.
- **Prevenção:** testes de contrato para URL de homologação e submissão técnica, mantendo os eventos auditáveis sem tratá-los como comportamento humano.

# LOOP-MCP-LOG-BACKEND-CONTRACT-DRIFT — Contrato de deploy diverge da porta canônica

- **Sintoma:** o workflow do MCP falha antes do build na validação do deploy isolado, embora a configuração e os testes do módulo estejam corretos.
- **Causa-raiz:** a URL canônica do leitor independente de logs passou a usar a porta `8099`, mas o teste shell do deploy continuou exigindo implicitamente a porta `80`.
- **Prevenção:** manter o contrato do deploy alinhado ao mesmo endpoint explícito usado pelo Compose, pela aplicação, pelo README e pelos testes Java; toda mudança futura da URL deve atualizar e executar essas validações em conjunto.

# LOOP-2026-08-07 — Prontidão aprovada, mas pacote de público retornava 404

- **Sintoma:** o experimento comercial aparecia sem bloqueios na prontidão, porém o Facebook Ads Worker interrompia a publicação por ausência de pacote de segmentação aprovado.
- **Causa-raiz:** a consulta do pacote ainda exigia o sinalizador legado `experiment.creative_approved`, enquanto a prontidão e a publicação de mídia usam os criativos `READY` como fonte canônica.
- **Correção sistêmica:** remover a dependência do sinalizador legado da consulta de público e preservar os gates canônicos independentes de criativo e segmentação.
- **Prevenção:** teste de repositório comprova que um público Meta aprovado continua disponível quando o sinalizador legado está falso.

# LOOP-CREATIVE-APPROVER-FREE-TEXT-CORRECTION — Parecer não materializado na nova arte

- **Sintoma:** o Aprovador registra problemas concretos e gera sucessivas versões, mas os mesmos defeitos visuais reaparecem até o limite de tentativas; no experimento #88, 12 versões terminaram com 0/3 criativos aprovados.
- **Causa-raiz:** o contrato de correção entregava ao gerador apenas `revisedImagePrompt` em texto livre. Problemas, recomendações e critérios verificáveis permaneciam no parecer, sem obrigação estrutural no prompt executado nem bloqueio de contratos vagos.
- **Correção sistêmica:** o Aprovador passa a devolver requisitos visuais obrigatórios, elementos proibidos e critérios objetivos de aceitação; o backend persiste e publica essas listas e o worker monta deterministicamente o prompt final com todos os itens.
- **Prevenção:** testes de contrato bloqueiam geração sem requisitos/critérios e comprovam que cada lista chega ao prompt enviado ao GPT Image 2. A versão continua voltando ao gate multimodal e não herda aprovação técnica ou humana.
- **Prevenção complementar em 2026-08-10:** a baixa qualidade das imagens do anúncio ou da landing tornou-se gate visual prioritário e eliminatório no Aprovador. O parecer deve começar pela evidência visual, atribuir o responsável correto e impedir otimizações secundárias enquanto a imagem não tiver nitidez, foco, autenticidade, coerência com o público e acabamento comercial; teste de contrato preserva essa precedência.

# LOOP-CREATIVE-CONVERGENCE-UNCOORDINATED — Gate identifica falha fora do próprio executor

- **Sintoma:** novas versões do anúncio repetem a mesma divergência com a landing até consumir o limite, embora o parecer descreva corretamente o problema.
- **Causa-raiz:** o parecer possuía recomendações em texto livre, mas o backend encaminhava somente a correção visual; não existiam responsável, aceite, progresso ou tarefa de landing persistidos.
- **Correção sistêmica:** ciclo versionado no backend transforma cada bloqueio em tarefa de `CREATIVE_COPY`, `CREATIVE_MEDIA` ou `LANDING`, encaminha a landing pelo pipeline GeraLanding e mede score, custo e repetição entre versões.
- **Prevenção:** contrato estruturado obrigatório, impressão digital estável e gates de repetição, custo e iteração impedem ciclos infinitos ou falsos sucessos.

# LOOP-AGENT-MCP-BACKEND-ROUTE-DRIFT — MCP registrado aponta para endpoint inexistente

- **Sintoma:** o agente inicia o Codex e registra seu MCP, mas a primeira ferramenta de contexto falha com HTTP 404, impedindo a análise baseada nos dados congelados.
- **Causa-raiz:** Cliente, Financeiro e Estrategista implementaram no MCP rotas internas de detalhe por execução sem criar os endpoints equivalentes nos controllers do backend; o gate premium verificava apenas a presença textual de `mcp_servers`, não o contrato ponta a ponta.
- **Correção sistêmica:** cada módulo passou a expor a leitura interna pelo identificador reservado, sem mutação nem recomputação do snapshot.
- **Prevenção:** o gate premium compara a rota usada por cada MCP com o `@GetMapping` canônico do controller e bloqueia divergências antes do consumo do modelo.

# LOOP-META-AD-APPROVER-MCP-APPROVAL-CANCELLED — Ferramentas canceladas sem operador

- **Sintoma:** o Aprovador devolve `ADJUST` informando `user cancelled MCP tool call` para contexto, mídia e landing, mesmo com handshake e catálogo MCP válidos.
- **Causa-raiz:** o `codex exec` não interativo herdava política de aprovação da identidade e as ferramentas MCP não declaravam anotações de risco; não existia usuário para responder à elicitação.
- **Correção sistêmica:** declarar `approval_policy=never` por configuração explícita com sandbox `read-only` e anotar todas as ferramentas com `readOnlyHint`, `openWorldHint` e `destructiveHint` coerentes.
- **Prevenção:** teste de contrato valida simultaneamente a política não interativa e as anotações MCP.

# LOOP-META-AD-APPROVER-LOG-ENDPOINT-DRIFT — MCP aponta para rota de log inexistente

- **Sintoma:** `java_module_logs` retorna HTTP 404 para `meta-ad-approver-worker`, embora o health do agente esteja `UP` e o logfile real esteja disponível.
- **Causa-raiz:** o MCP registrou uma rota nominal `meta-ad-approver-worker-log`, mas o Actuator expõe o endpoint configurado como `logfile` sob o base path versionado.
- **Correção sistêmica:** aplicação, Composes, documentação operacional e teste de contrato usam `/ops-meta-ad-approver-observability-v1/logfile`, validado contra o runtime publicado.

# LOOP-EXPERIMENT-STRATEGIST-LOG-ENDPOINT-DRIFT — Atena executa sem logs acessíveis pelo MCP

- **Sintoma:** `java_module_logs` retorna HTTP 404 para `experiment-strategist-worker`, embora o worker esteja em execução.
- **Causa-raiz:** o MCP apontava para a rota nominal `experiment-strategist-worker-log`, mas o Actuator publica o endpoint como `logfile` sob o base path versionado.
- **Correção sistêmica:** defaults do MCP, Composes e documentação usam `/ops-experiment-strategist-observability-v1/logfile`; o deploy valida health e logfile antes de declarar o worker pronto.
- **Prevenção:** testes de contrato no worker e no MCP fixam a rota, a porta, os descritores publicados e a sonda do workflow.

### LOOP-META-AD-APPROVER-CORRECTION-TARGETS-EMPTY

- **Sintoma:** na primeira execução assistida do ciclo de convergência do experimento #88, o Aprovador devolveu `ADJUST` sem `correctionTargets`; o backend encerrou o ciclo como `FAILED` e o callback respondeu HTTP 500.
- **Causa-raiz:** o prompt descrevia a obrigação, mas o schema aceitava uma lista vazia e o gate local do executor validava somente o contrato visual legado.
- **Correção sistêmica:** o schema exige ao menos uma tarefa para `ADJUST`/`REJECTED` e nenhuma para `APPROVED`; o executor repete a validação deterministicamente antes do callback, com testes de contrato contra recorrência.
- **Prevenção:** o teste de defaults exige o mesmo endpoint nos descritores local e de deploy; a homologação operacional deve confirmar HTTP 200 tanto no health quanto no logfile.

### LOOP-META-AD-APPROVER-STRICT-SCHEMA-CONDITIONAL

- **Sintoma:** o Aprovador encerra três revisões com `invalid_json_schema` antes de inspecionar mídia e landing, embora o JSON Schema seja válido pelo padrão 2020-12.
- **Causa-raiz:** o contrato adicionou `anyOf` para condicionar `correctionTargets`, mas cada ramo era interpretado como objeto pelo Structured Outputs e não declarava `additionalProperties: false`; tornar os ramos estritos também proibiria os demais campos da resposta.
- **Correção sistêmica:** remover a condição incompatível do schema enviado à OpenAI e manter a regra no gate determinístico do executor, que já bloqueia `ADJUST`/`REJECTED` sem tarefas e `APPROVED` com tarefas.
- **Prevenção:** teste de contrato proíbe `anyOf`, `oneOf` e `allOf` no schema estrito do Aprovador e preserva testes executáveis para as condições de negócio antes do callback.

### LOOP-LANDING-GENERATOR-STRICT-SCHEMA-UNIQUE-ITEMS

- **Sintoma:** Dédalo recebe e reserva a reprovação do Quality Review, mas encerra antes de analisar a landing com `invalid_json_schema` em `recommendedRegeneration`.
- **Causa-raiz:** o schema JSON versionado usava `uniqueItems`, válido no padrão 2020-12, porém não aceito pelo Structured Outputs usado pelo Codex.
- **Correção sistêmica:** remover `uniqueItems` do contrato enviado ao modelo; duplicidades continuam inofensivas porque o backend converte a recomendação em uma única etapa causal.
- **Prevenção:** teste do worker proíbe `uniqueItems`, `anyOf`, `oneOf` e `allOf` no schema estrito antes do deploy.

### LOOP-LANDING-GENERATOR-HISTORICAL-REVIEW-LIMIT

- **Sintoma:** Dédalo conclui a análise de uma nova tarefa, mas o backend bloqueia a primeira correção válida porque quatro Quality Reviews antigos já existem no experimento.
- **Causa-raiz:** os gates de revisão e custo eram acumulados por experimento, sem identidade persistida para o ciclo autônomo que originou a regeneração.
- **Correção sistêmica:** cada início manual abre um `autonomous_cycle_id`; tarefas, revisões automáticas e custos herdam a correlação, e os gates consultam somente o ciclo corrente.
- **Prevenção:** teste de contrato mantém revisões históricas fora do contador atual sem apagar auditoria nem ampliar o limite seguro de quatro revisões.

### LOOP-LANDING-GENERATOR-MEMORY-TEXT-OVERFLOW

- **Sintoma:** Dédalo conclui a análise autônoma, mas o callback responde HTTP 500 e nenhuma regeneração é iniciada.
- **Causa-raiz:** `premium_agent_memory.content_text` usava `TEXT`; o parecer enriquecido de 21.406 caracteres excedeu a capacidade efetiva em `utf8mb4` e o MySQL 5.7 retornou erro 1406.
- **Correção sistêmica:** conteúdo e evidências da memória premium e de seus feedbacks passam a `LONGTEXT`, com mapeamento JPA explícito e sem truncamento silencioso.
- **Prevenção:** teste de contrato valida conjuntamente entidade, changelog e include relativo do Liquibase.

## LOOP-GERALANDING-WIREFRAME-PENDING-PAYLOAD — lote rico bloqueia o scheduler

- **Data:** 2026-08-12.
- **Sintoma:** Dédalo enfileira `landing-page-wireframe`, mas a execução permanece `INICIADO` e o AI Worker registra erro não tratado a cada polling.
- **Causa-raiz:** o backend retornava até 20 snapshots ricos sem respeitar o limite do worker; o client desserializava o lote inteiro com o buffer padrão e ainda aceitava campos opcionais nulos em um mapa imutável.
- **Correção:** o endpoint passou a limitar o lote solicitado, o client envia esse limite, reserva memória compatível com o teto controlado e normaliza campos opcionais antes de criar o input.
- **Prevenção:** testes de contrato exigem o parâmetro `limit`, payload acima do buffer padrão e snapshot com campos opcionais ausentes.

## LOOP-GERALANDING-WIREFRAME-COPY-DUPLICADA — backlog reabre a próxima etapa

- **Data:** 2026-08-12.
- **Sintoma:** ao drenar wireframes antigos do mesmo experimento, cada callback concluído criava uma nova `landing-page-copy`, acumulando execuções `INICIADO` e custo repetido.
- **Causa-raiz:** o avanço automático wireframe → copy não verificava se uma copy igual ou mais recente já havia sido enfileirada para o experimento.
- **Correção:** o backend só cria a próxima copy quando a última copy é anterior ao wireframe que acabou de concluir.
- **Prevenção:** teste de contrato simula callback tardio de wireframe e impede duplicação da próxima etapa.

## LOOP-GERALANDING-QUALITY-REVIEW-AGENT-BACKLOG — revisões concorrentes duplicam correções

- **Data:** 2026-08-14.
- **Sintoma:** o experimento #88 permaneceu `PLANNED` enquanto revisões de versões antigas concluíam em paralelo e criavam dezenas de execuções `landing-generation-agent-v1` para o mesmo ciclo.
- **Causa-raiz:** cada callback reprovado do Quality Review enfileirava Dédalo sem verificar se já existia correção `INICIADO` ou `PROCESSANDO` para o mesmo experimento e ciclo autônomo.
- **Correção:** a fila do agente passou a ser idempotente por experimento, etapa, ciclo e status ativo; uma reprovação posterior permanece auditada, mas não abre trabalho concorrente.
- **Prevenção:** teste de contrato simula callbacks reprovados repetidos e exige uma única correção ativa antes de nova revisão.

## LOOP-MUSA-PROJETO-LEGADO-PLANO-OBRIGATORIO — perfil de Apolo não é salvo

- **Data:** 2026-08-12.
- **Sintoma:** ao vincular um perfil de vídeo a um projeto MUSA legado, a edição retorna HTTP 400 e o ciclo de Apolo retorna HTTP 409 por ausência do perfil.
- **Causa-raiz:** os DTOs REST ainda exigiam `commercialPlanId`, embora a entidade, o ledger e o ciclo autônomo já suportassem projetos legados sem plano comercial.
- **Correção:** criação e edição de projetos aceitam plano comercial ausente, preservando o vínculo opcional e a segregação financeira já aplicada pelo serviço.
- **Prevenção:** teste atualiza um projeto sem plano, persiste o perfil e comprova que o contrato continua aceitando o cenário legado.

# LOOP-COMMERCIAL-PLAN-OPERATIONAL-CONTEXT-TRUNCATED — plano não aceita contrato operacional completo

- **Sintoma:** a tela de planejamento retorna HTTP 500 ao salvar consenso, autonomia e critérios observáveis para os agentes.
- **Causa-raiz confirmada em 2026-08-13:** `commercial_plan.next_action`, `current_blocker` e `root_cause` permaneciam em `VARCHAR(512)`, embora o plano versionado precise transportar contexto operacional completo.
- **Correção efetiva:** os três campos passam a `LONGTEXT` no Liquibase e no mapeamento JPA, preservando o contrato integral entre Têmis, Dédalo e o backend.
- **Prevenção:** a validação Liquibase e o mapeamento explícito impedem o retorno do limite de 512 caracteres; decisões de gasto, preço e publicação continuam em gates separados.

# LOOP-LIQUIBASE-NESTED-PRECONDITIONS — deploy bloqueado por chave YAML inválida

- **Sintoma:** o workflow `Build & Deploy containers` falha na validação do changelog antes de construir o backend.
- **Causa-raiz confirmada em 2026-08-13:** o changelog do contexto operacional usava `nestedPreconditions`, chave que o Liquibase 4.26 não reconhece, enquanto o validador estático local verificava apenas contratos de SQL e não a estrutura dessa precondição.
- **Correção sistêmica:** a condição `dbms:mysql` passa a usar a lista direta suportada em `preConditions`, e o validador local rejeita qualquer retorno de `nestedPreconditions` antes do workflow.
- **Prevenção:** executar o validador estático em escopo completo e a validação real do Liquibase antes de consolidar changelogs.

## LOOP-APOLO-JOB-FALHO-DESSINCRONIZADO — ciclo aprovado preso em job terminal

- **Sintoma:** ciclo MUSA permanece `QUEUED_FOR_APOLLO` enquanto o job vinculado está `VIDEO_FAILED`, impedindo retomada após troca de provider.
- **Causa-raiz:** o polling do executor consultava somente jobs novos e não reconciliava ciclos aprovados por Plutus com jobs terminais antigos.
- **Correção sistêmica:** antes de cada polling, Apolo solicita reconciliação idempotente ao backend; jobs falhos são substituídos por Seedance 2.5 via Runway, com vínculo ao job anterior e plano explícito de 3 cenas para 30s ou 6 cenas para 60s. O ciclo preserva o identificador, código, detalhe e horário da falha anterior, e o painel diferencia essa falha do novo job enfileirado. O adapter Runway gera e monta todas as cenas localmente.
- **Prevenção:** testes do ciclo e do painel comprovam provider, quantidade de cenas, diagnóstico da falha, rastreabilidade do job substituído e atualização do vínculo persistido.

## LOOP-APOLO-RECONCILIACAO-CONSOME-CREDITOS — substituição infinita e montagem rejeitada

- **Data:** 2026-08-13.
- **Sintoma:** Apolo recriava jobs MUSA a cada polling, esgotava créditos e rejeitava uma montagem de três cenas como se tivesse apenas dez segundos.
- **Causa-raiz confirmada:** a reconciliação substituía toda falha terminal sem distinguir erro não recuperável ou substituição anterior; o adapter contabilizava duração e custo de uma cena, embora já tivesse gerado e montado todas.
- **Correção sistêmica:** saldo insuficiente, erro não recuperável, segunda substituição ou asset existente bloqueiam o ciclo; duração e custo abrangem todas as cenas, que recebem funções comerciais distintas.
- **Recorrência fechada em 2026-08-13:** o ciclo ainda confundia a duração máxima do clipe cobrado com a duração de cada corte editorial: fixava dez segundos no backend e repetia contexto amplo em cada geração. Apolo agora resolve a capacidade por modelo, persiste 8 cortes para 30s ou 12 para 60s, envia a cada clipe somente seu grupo de tomadas e reserva texto, legenda e CTA para pós-produção determinística. O Estúdio expõe clipes solicitados, duração por clipe e cortes planejados antes de novo consumo.
- **Prevenção:** testes impedem novo job após bloqueio financeiro ou asset aproveitável; o contrato canônico exige avaliação do material existente e novo gate antes de qualquer gasto adicional.
- **Fechamento financeiro complementar em 2026-08-13:** cada task/cena aceita passa a registrar imediatamente modelo, duração, créditos e custo estimado por identidade idempotente do provedor. A soma permanece no ledger do job mesmo se uma cena posterior ou a montagem falhar; a primeira recusa por saldo mantém o ciclo `APOLLO_BLOCKED`, impedindo que reconciliações automáticas criem novo consumo.
- **Fechamento editorial complementar em 2026-08-14:** o Estúdio persiste parecer, percentual aproveitável, evidência, autor e horário por task/cena; render curto com arquivo preservado é encaminhado idempotentemente a um único job de pós-produção, reutilizando o encaminhamento existente e sem abrir nova geração paga.
- **Fechamento de monitoramento financeiro em 2026-08-14:** cada callback idempotente de task aceita ou liquidada recalcula tasks, créditos e custo do ciclo; o backend persiste alerta visível no Estúdio e muda imediatamente o ciclo para `APOLLO_BLOCKED` ao exceder o teto ou receber saldo insuficiente. A tela consulta essa verdade canônica periodicamente, sem inferir orçamento no frontend.

## LOOP-FRONTEND-CI-ARTIFACT-CLEANUP-TRANSIENT — build aprovado termina vermelho na limpeza

- **Data:** 2026-08-13.
- **Sintoma:** o Frontend CI conclui instalação, type-check, build e upload, mas termina com falha ao remover artefatos antigos.
- **Causa-raiz:** a manutenção auxiliar tratava respostas transitórias HTTP 500/502 da API de artefatos do GitHub como falha do produto; execuções próximas também podiam tentar excluir o mesmo conjunto. A primeira correção alterou apenas o workflow e seu teste, mas os filtros observavam somente `frontend/**`, então o próprio Frontend CI corrigido não executou no `push` que o incorporou.
- **Correção sistêmica:** a chamada passa a repetir falhas transitórias e, após esgotar as tentativas, registra aviso e deixa a limpeza idempotente para o próximo run. O workflow também dispara quando sua definição ou seu contrato preventivo mudam e aceita execução manual de diagnóstico. Erros permanentes continuam falhando o workflow.
- **Prevenção:** teste de contrato exige retries, tolerância restrita a 404 e erros transitórios 5xx, proíbe tolerância ampla que esconda falhas de permissão ou contrato e confirma que mudanças no próprio mecanismo disparam sua validação.

## LOOP-LANDING-GENERATOR-CI-SSH-IDLE — deploy saudável perde a sessão durante o readiness

- **Data:** 2026-08-13.
- **Sintoma:** o Landing Generator Agent Worker conclui build, recria o container e termina com `Broken pipe` após cinco minutos aguardando o readiness.
- **Causa-raiz:** a sessão SSH do deploy não enviava keepalive e permanecia sem saída durante as tentativas de saúde, sendo encerrada por inatividade antes de o workflow obter o diagnóstico final do executor.
- **Correção sistêmica:** o SSH passa a enviar keepalive com tolerância superior à janela de readiness, e cada tentativa produz progresso observável; indisponibilidade real continua exibindo os logs e falhando o deploy.
- **Prevenção:** o contrato de isolamento dos agentes exige keepalive e progresso explícito no workflow de Dédalo.
