# Publicação do botão Prompt para AIHUB — 12/09/2026

## Diagnóstico

A página do produto 9, processo 75, cadeia 14 foi aberta em Chromium novo. O card
mostrava apenas **Copiar contexto do processo**; nenhuma consulta retornou erro e
não houve erro JavaScript. O frontend publicava `vega-destination-7cfb9392ba65`,
base `4eebe408`, construída às 16:33 UTC. O MCP confirmou a mesma base do backend.

O botão entrou na main pelo PR #5175, commit `9f9671aa`, às 18:00 UTC. Os PRs
#5173 e #5174 já integraram as correções anteriormente protegidas do Vega.
O coordenador conservava a intervenção `f3221756d77344dd94a10c02816956fe` em
`ACTIVE`: cinco publicadores pausados, nenhuma execução em curso. O último deploy
automático era o run `34680679956`, da revisão `b3d30f74`, às 07:25 UTC.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Limpar cache | Baixo esforço | A imagem servida continuaria antiga | Evidência descarta essa causa |
| Substituir outra imagem manualmente | Disponibiliza o botão | Conserva a pausa e o risco de repetição | Não escolhida |
| Encerrar a intervenção integrada e publicar main pelo fluxo normal | Atualiza a interface e restaura as próximas entregas | Conferir integração, validação, filas e identidade | Escolhida |

## Falha secundária encontrada no Actions

O frontend foi aprovado no run `34709785360`. O run de Liquibase `34709785328`
falhou na limpeza dos dados entre os cenários 16 e 17 da homologação de ciclos.
O mesmo código passou em uma rodada
local e reproduziu a falha em outra, com MySQL 5.7 e Atena simulada. O stderr SQL
era capturado sem ser apresentado; o log histórico não permite afirmar seu código.

A ampliação local da janela entre as instruções de limpeza comprovou consultas
e callbacks para a proposta sintética #14 depois de sua exclusão e tentativas de
recriar a ocorrência `learning-cycle:11:decision:11`. O banco rejeitou a duplicação.
Os resets SQL e HTTP faziam exclusões separadas, enquanto o consumidor continuava
ativo. Cada método de repository possuía sua transação, sem uma transação para a
operação inteira. Essas evidências pertencem somente à fixture local.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir Actions até passar | Baixo esforço imediato | Mantém a causa e troca diagnóstico por tentativas no CI | Rejeitada |
| Desligar chaves estrangeiras | Evita parte das falhas de exclusão | Permite dados órfãos e reduz a fidelidade | Rejeitada |
| Centralizar a limpeza local em uma transação com locks dos ciclos | Preserva integridade e impede interleaving das exclusões com reservas | Mudança restrita aos testes e à captura de evidências | Escolhida |

A fixture agora bloqueia os ciclos em ordem estável e só confirma a limpeza
inteira no commit. Os dois clientes usam esse único contrato. A homologação
confere que nenhuma linha das quatro tabelas de execução sobrou após cada reset.
Erros SQL passam a expor o diagnóstico; o Actions preserva logs por run/tentativa,
inclusive em falhas. O encerramento local para o consumidor antes da API.

O teste novo usa JDBC real e banco H2 exclusivo por contexto. Ele comprova commit
e rollback após falha no meio da limpeza. O controle negativo, removendo somente
a transação, falhou com `expected: 0 but was: 1`; a implementação corrigida foi
restaurada antes das rodadas finais. Não há mudança em classes Java produtivas,
schemas ou dados de negócio. Esta correção de homologação fica local até PR.

## Matriz local definida antes das rodadas finais

| Critério | Validação |
| --- | --- |
| Limpeza e falhas | Commit e rollback JDBC; MySQL 5.7 com consumidor ativo; nenhum dado restante após reset |
| Ciclos e integrações | REST, concorrência, decisão, retorno, isolamento, migração, reaplicação e contexto do sucessor |
| Backend e executor | Suíte backend completa; testes do executor Atena simulado |
| Botão e regressões | Matriz `process-context-copy`: 101 testes, tipos, build, cópia, prévia e falhas |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; HTTP e Clipboard API em contexto seguro |
| Observabilidade | Diagnóstico SQL e arquivos de evidência por tentativa, inclusive na falha |
| Segregação e métricas | Projeto Docker exclusivo, produtos sintéticos, sem chamadas pagas ou eventos comerciais em produção |
| Contratos e diff | Formatação Java, sintaxe Bash/Python, actionlint e revisão do diff |
| Confirmação publicada | Identidade e saúde; botão e colagem real na página informada, sem comandos de execução de produto |

O critério de aprovação após a última correção foi duas rodadas locais completas
e consecutivas sem falhas.
Os controles de publicação existentes também foram executados: 33 testes do
coordenador e contrato de detecção de revisões pendentes. A primeira matriz do
botão já passou com 101 testes e 18 combinações de cópia/dispositivo/contexto.
Os resultados finais e o recibo da publicação estão registrados abaixo.

Evidências locais: `artifacts/aihub-button-publication/` e
`artifacts/process-context-copy/publication-main/`. Celulares são emulações de
Chromium; não houve teste em Safari ou aparelho físico.

## Primeira rodada final

`aihub-release-1` terminou completa: 2.760 testes de backend contabilizados, zero falhas
e erros, oito testes opcionais ignorados; os 101 testes de interface passaram. O MySQL
aprovou REST, concorrência, decisão, contexto, migração, reaplicação e idempotência.
As três matrizes de cópia passaram nas seis combinações de dispositivo e contexto,
com zero comandos produtivos. Os logs da fixture e do consumidor não registraram
duplicação de ocorrência, ausência de registro nem falha de proposta nessa rodada.
Formatação, actionlint, sintaxe e diff aprovados. A segunda rodada usa as mesmas fontes.

## Segunda rodada e retomada

`aihub-release-2` terminou completa e consecutiva, com as mesmas contagens e zero
falhas. Ambas aprovaram seis cenários da proposta, 18 de ciclos e dez de contexto
com MySQL real. O manifesto dos seis arquivos de código/CI permaneceu igual entre
as rodadas. A matriz do botão aprovou 101 testes e as 18 combinações de cópia por rodada.

A main avançou para `18b98454af11602d914f7baa90dd0dc10ec074b6` com somente três
arquivos novos em `pesquisas/video/`. O conteúdo foi sincronizado sem descartar
as correções locais. Antes de disparar o deploy, o hash da fonte do card foi confirmado e os testes de
catálogo e curadoria passaram. Não houve mudança adicional em frontend, backend produtivo ou workers.

O coordenador confirmou a intervenção original sem novas operações desde 16:45 UTC
e a encerrou por `resume`, restaurando os cinco publicadores anteriormente ativos.
O registro persistido terminou `RELEASED`; nenhuma execução produtiva foi cancelada.
Como não existia publicação da revisão atual, foi disparado o workflow versionado
`deploy-containers.yml` em `main`: run `34712362128`. Essa publicação usa somente
código já integrado. As alterações locais desta tarefa em testes/CI aguardam PR.

Recibos: `coordinator-before-resume.json`, `coordinator-resumed.json` e
`deployment-runs.json` em `artifacts/aihub-button-publication/`.

## Publicação e confirmação na tela

O [deploy 34712362128](https://github.com/paulofor/marketing-hub/actions/runs/34712362128)
terminou com sucesso às 19:08 UTC. O workflow aprovou a saúde do backend e a
identidade do frontend. O endpoint `/healthz` do frontend e a ferramenta MCP
`runtime_build_info` do backend confirmaram a revisão `18b98454af11602d914f7baa90dd0dc10ec074b6`.
Os cinco publicadores foram consultados novamente e permaneceram `active`.

A página informada pelo usuário, produto 9, processo 75, cadeia 14, foi reaberta
em contextos novos de Chromium desktop, iPhone 15 Pro e Pixel 7 emulados. Nos três:

- O card apresentou **Prompt para AIHUB** e **Ver prompt para AIHUB**.
- O clique apresentou a confirmação de cópia e a colagem real recuperou 12.384
  caracteres, incluindo o pedido de ajuda, o produto 9 e a definição de processo 75.
- Não houve erro JavaScript, erro nas consultas de API ou requisição de alteração.

Evidências: `deployment-result.json`, `backend-after.json`, `frontend-after.json`,
`publishers-after.json`, `after-browser.json` e imagens `after-*-copied.png` em
`artifacts/aihub-button-publication/`. A topologia Docker de homologação foi
removida com volumes e órfãos; recibo em `final-docker-cleanup.log`.

A correção publicada já estava integrada na main. Nenhum commit ou PR novo foi
criado. As mudanças adicionais desta investigação em testes, CI e documentação
ficam na sandbox para revisão por PR; a execução histórica que falhou continua
registrada como falha e não foi usada para testar essas alterações locais.
