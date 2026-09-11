# Retomada do deploy da aplicação — v1

## Contrato operacional

### Intervenções autorizadas e versão em homologação

Antes de uma intervenção autorizada em um serviço publicado, usar o coordenador
`scripts/coordinate-deploy-intervention.py`. Ele pausa somente os workflows dos componentes
selecionados e suas continuações dependentes, preserva os estados anteriores e espera todas
as execuções já iniciadas terminarem. Uma intervenção só pode começar no estado `ACTIVE`,
com os publicadores desativados e a fila vazia. Pausar um workflow sozinho não prova esse estado.

O registro e a exclusão entre operadores ficam no host administrativo, fora de diretórios de
rsync/deploy. Motivo, autorização, responsável, versão protegida, escopo, SHA inicial, execuções
observadas e decisões de retomada permanecem auditáveis. A perda da conexão, erro da API ou
fim da sessão não libera automaticamente a pausa. Não cancelar uma transação remota em curso.
Não usar PR, push, dispatch ou deploy para testar este controle.

Quando a fila global do APP contiver revisões antigas que sequer iniciaram jobs, o comando
explícito `discard-unstarted` pode removê-las: exige publicadores pausados, status `pending` ou `queued`,
zero jobs e nova leitura do estado antes do pedido de cancelamento. Runs iniciados e filas de
outros publicadores continuam em drenagem. `--keep-run` preserva uma execução existente da
`main` atual, permitindo terminar a publicação já enfileirada da correção integrada antes
de iniciar a intervenção. O aceite do cancelamento não libera a proteção; a fila é reconsultada.

Comandos operacionais passam por `execute`, que mantém o lock e registra `OPERATING` antes
de iniciar o processo. Se houver desconexão sem resultado, `protect` e `resume` recusam a
liberação. Conferir o término no host e registrar a evidência por `reconcile-command` antes
de prosseguir; liberar o lock de transporte sozinho não comprova o término de um subprocesso.

A retomada exige o identificador da intervenção, evidência da validação e um commit completo
com a correção, já integrado à `main`. Para homologação ainda em curso, manter a pausa. Ao
retomar, restaurar somente os workflows que estavam ativos antes; não reativar os previamente
desativados. Não reexecutar automaticamente runs antigos nem disparar uma publicação: eventos
perdidos exigem reconciliação pelo fluxo normal, na revisão integrada e validada.

Esta coordenação é um comando operacional, não uma autorização adicional para publicar código.
O fluxo normal continua passando por PR; exceções exigem a autorização explícita do usuário.
O histórico do Vega (run `34592882916`) confirmou que filas exclusivas dos Actions não coordenam
uma publicação externa. A pausa atua sobre os workflows já existentes, inclusive revisões antigas.

Procedimento e matriz: [coordenação de intervenções](../homologacao/deploy-intervencao-coordenada-v1.md).

O workflow `Build & Deploy containers` publica backend e frontend administrativos a partir
de código integrado em `main`. Deve aceitar `push` e `workflow_dispatch`; a detecção de
módulos só executa para `refs/heads/main`. Todos os jobs de publicação dependem dessa detecção,
inclusive os que usam `always()`. Selecionar outra branch ou uma tag não libera o deploy.

Reativar um workflow não recupera o evento perdido durante a pausa. Após a reativação,
consultar a revisão atual e procurar uma execução correspondente; se ela não existir,
disparar manualmente `main`. O checkout e as imagens continuam vinculados ao SHA congelado
pelo evento, com testes, fila, rollback, retenção e confirmação das revisões publicadas.
A detecção compara o histórico com a revisão efetivamente publicada, recuperando módulos
pendentes mesmo quando o evento manual não contém `github.event.before`.

Argos, Psique e Íris devem reconhecer tanto `push` quanto `workflow_dispatch` da aplicação,
sempre na branch `main` e no mesmo SHA. Uma execução de outro commit, branch, tag ou PR não
libera os agentes. O push de origem testa e empacota a imagem imutável do agente e confirma, em
até dois minutos, que o workflow central da mesma revisão foi registrado. Essa confirmação não
aguarda a fila terminar e não acessa nenhum VPS.

A publicação automática do agente começa somente pelo evento `workflow_run: completed` de
`Build & Deploy containers`. A continuação localiza a execução `push` do próprio agente no mesmo
SHA, exige sucesso tanto dela quanto da aplicação, faz checkout dessa revisão e, quando houver
artefato, baixa-o pelo ID exato do run de origem. Ausência de execução de origem torna a
continuação não aplicável; falha da aplicação ou do teste mantém o agente na versão anterior.
É proibido liberar por ancestralidade presumida, usar o SHA corrente da branch no lugar do SHA do
evento ou manter um runner fazendo polling durante toda a fila central.

A resolução do run de origem ocorre antes da fila compartilhada do VPS e não contém SSH, SCP ou
rsync. Somente depois dessas provas o job remoto entra em `deploy-vps-163-245-202-80`. As imagens
empacotadas de Psique e Íris têm retenção de sete dias para sobreviver à fila central ampliada.
Uma execução manual do próprio agente continua sendo uma recuperação operacional explícita e não
substitui a validação normal do push.

Uma nova execução manual não substitui os testes locais nem autoriza código fora do PR.
Não reexecutar um run histórico esperando que ele publique o HEAD atual: o GitHub preserva
o SHA e a referência do evento original.

## Validação do backend antes da integração

O workflow `Backend CI` deve executar a suíte completa de `backend/ads-service` em Pull Requests
que alterem o backend, seus recursos empacotados ou seu contrato de CI. O comando integral
`mvn -B test` cobre também `ArquiteturaTest`; uma seleção por módulo pode ajudar no diagnóstico,
mas não substitui essa validação. Empacotamento e verificação dos recursos ocorrem após os testes.
Falhas interrompem o job e os relatórios Surefire são preservados inclusive quando houver erro.
O workflow de PR tem permissão somente de leitura do código e não publica aplicação ou biblioteca.

Bancos H2 de testes Spring precisam ter identidade própria por contexto. Nas anotações de
testes com preparação e limpeza próprias, a URL inclui também a identidade da classe, para
que o cache Spring não reutilize um contexto com dados deixados por outra classe.
O fechamento de um contexto não pode apagar tabelas de outro nem
permitir que seus registros se misturem. A análise arquitetural produtiva usa a origem do
bytecode para excluir classes de teste, preservando todas as classes produtivas e suas internas.
Fixtures de homologação não autorizam novas dependências entre módulos produtivos.

Para a JVM de testes com limite de 3 GB, `src/test/resources/spring.properties` limita o
cache Spring a oito contextos. O H2 em memória é descartado ao fechar a última conexão
(`DB_CLOSE_DELAY=0`), liberando também o banco após encerrar o pool do contexto.

Antes de solicitar publicação, executar localmente os testes relevantes, revisar o diff e,
após correções de defeitos, concluir as duas rodadas consecutivas previstas na matriz local.
Referência: [isolamento dos testes do backend](../homologacao/actions-backend-isolamento-testes-2026-09-08.md).

## Recuperação de 08/09/2026

- PR #5140 mergeado em `3c7e537f04c82733e44690d9071aa83f63ff1385`.
- Workflow `243638963` observado como `disabled_manually` e reativado com autorização do usuário.
- Tentativa real de `workflow_dispatch` em `main` recusada com HTTP 422:
  `Workflow does not have 'workflow_dispatch' trigger`.
- Último run disponível, `34186720906`, pertence a `ad2929c6`, anterior ao merge solicitado.
- O consumidor `wait-for-app-deployment.mjs` também filtrava somente `push`; adicionar apenas
  o gatilho ao publicador manteria a espera dos agentes bloqueada.

## Correção da espera incompatível com a fila em 09/09/2026

Os runs `34356522474` de Argos, `34356522469` de Íris, `34361476862` do Argos seguinte e
`34356522612` de Psique aprovaram seus jobs de teste e imagem, mas falharam após 2.400 segundos
aguardando os deploys centrais correspondentes, que continuavam `pending`. Na mesma janela, um run
central criado às 11:56 UTC só iniciou seus jobs às 16:08 UTC. A fila estava saudável e
processando em ordem; o timeout fixo era menor que o atraso legítimo que o próprio `queue: max`
permite.

Três alternativas foram comparadas:

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Aumentar o polling para seis horas | Mudança pequena | Ainda incompatível com até cem itens e ocupa runner | Rejeitada |
| Aceitar deploy verde de commit posterior | Reduz espera | Presume compatibilidade entre revisões | Rejeitada |
| Continuar pelo evento de conclusão do mesmo SHA | Não ocupa runner na fila e preserva revisão | Exige coordenar run e artefato de origem | Adotada |

O contrato executável é `scripts/coordinate-agent-deployment.mjs`, protegido por
`scripts/coordinate-agent-deployment.test.mjs`. A espera antiga por conclusão foi removida.
Matriz e evidências: [coordenação sem polling](../homologacao/actions-dependencia-aplicacao-sem-polling-2026-09-09.md).

Alternativas avaliadas:

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Aguardar outro merge funcional | Usa o gatilho já publicado | Prazo indefinido; não resolve a recuperação manual | Não atende à retomada solicitada |
| Commit vazio para provocar push | Aciona o workflow existente | Altera o histórico só para operar e mantém a lacuna de recuperação | Não adotada |
| Gatilho manual em main e coordenação dos agentes | Recuperação explícita, repetível e auditável | Correção pequena, exige PR antes de publicar | Adotada |

## Matriz local da correção

Definida antes da validação final. As dependências GitHub e VPS dos testes são locais/simuladas;
não há campanhas, vídeos pagos, envio de mensagens ou métricas comerciais de teste em produção.

| Controle | Evidência exigida |
| --- | --- |
| Coordenação dos agentes | Push e retomada manual do mesmo SHA; espera, sucesso, falha e timeout; rejeição de branch/tag/PR/commit diferente |
| Fronteira do workflow | Gatilho manual presente; detecção restrita a main; todos os publicadores dependem dela |
| Recuperação do diff | Backend, frontend e vídeo pendentes identificados pelas respectivas revisões publicadas |
| Segurança operacional | Contratos de deploy transacional, saúde, fila e resiliência do host preservados |
| Sintaxe e integração | Actionlint com suporte a `queue: max`, Node e revisão do diff |

O teste de regressão reproduziu quatro falhas antes da correção: seleção do run manual,
consulta restrita a push, falha manual ignorada e ausência de gatilho/limite a main.
Depois da última correção, executar duas rodadas consecutivas de toda a matriz sem falhas.

Resultado desta recuperação: duas rodadas completas e consecutivas com **8/8 controles**
aprovados em cada uma, incluindo **16 testes Node** por rodada. O teste de integração executa
o bloco Bash real de detecção do workflow contra histórico Git isolado e SSH simulado,
com `EVENT_BEFORE` vazio e um commit posterior somente de documentação. Backend/frontend
pendentes foram reconhecidos; o módulo de vídeo sem alterações não foi selecionado.
As assinaturas SHA-256 dos três arquivos de implementação/teste permaneceram iguais entre
as rodadas. Actionlint validou todos os workflows com a revisão fixada pelo repositório.

Na conferência externa final, o workflow está `active`, mas o SHA `3c7e537f` continua com
zero execuções desse publicador. O frontend reporta `489d57e1` em `/healthz`; a página de Ciclos
não aparece em Chromium desktop/iPhone emulado, e o endpoint
`/api/business-process-chains/learning-cycles/v1/products/3` retorna HTTP 404. A navegação
de diagnóstico bloqueou requisições de escrita. **A correção permanece local, sem PR ou
publicação; esses resultados não são homologação da funcionalidade em produção.**

## Confirmação após o PR

1. Conferir que o workflow está `active`; o merge da correção gera um novo `push`.
2. Acompanhar a execução desse SHA; usar o gatilho manual em `main` somente se for necessário
   recuperar uma execução ausente. Não enfileirar publicação duplicada de um run em andamento.
3. Confirmar sucesso dos jobs aplicáveis, saúde e commit do backend/frontend publicados.
4. Validar **Cadeias de Valor → Ciclos de aprendizado e vendas** pela interface, incluindo
   as atividades de vídeo, em desktop e celular. Não criar artefatos comerciais para um smoke.
5. Conferir os agentes dependentes no SHA correspondente. Runs antigos com timeout preservam
   o histórico; não comprovam o deploy atual.

Referências: [API de workflows do GitHub](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event)
e [semântica de reexecução](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs).
