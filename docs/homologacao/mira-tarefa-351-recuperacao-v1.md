# Recuperação da tarefa #351 de Mira

## Diagnóstico confirmado em 08/09/2026

A tarefa #351 (`prototypeCorrection`, processo #70 v8) está bloqueada por ausência da
implantação de `mira-private-v2`. A rejeição funcional original da Psique está na tarefa #350:
o estado final escondia a rotina. A correção já está no commit publicado
`d1c41901ddf9dc1d3e74487a549678ae3f35c4be` e no artefato oficial do CI #34177540044.

Antes da recuperação, o VPS não tinha container de Mira nem serviço na porta 5180;
`/mira-private/version-diagnostics.json` respondia 404. Vega executava sua imagem v7 do
commit `20c1037ce8e98160a4527d13b311ce8d60a1b37d`. O proxy ativo não continha as rotas
isoladas de Mira que já existem no repositório. A tarefa #351 utilizou raciocínio `max`.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir a tarefa sem implantar a versão | Ação rápida | Repete bloqueio conhecido e consome IA | Descartada |
| Reimplementar a correção | Permite outra solução | Retrabalho sem evidência de defeito no código corrigido | Descartada |
| Validar a versão publicada, corrigir incompatibilidades e implantar imagens pelos Dockerfiles versionados | Fecha a causa operacional e preserva a rastreabilidade | Exige prova local e conferência do isolamento | Escolhida |

O pedido atual autoriza a implantação técnica e as retentativas pela tela. Não autoriza
fabricar aprovação nem evidência comercial. O planejamento de Vega e o experimento #91
permanecem fora das mutações desta recuperação.

## Matriz definida antes da execução local

| Controle | Critério |
| --- | --- |
| Proveniência | Base publicada vinculada ao commit, patch local delimitado e Dockerfiles versionados; imagem conferida por camadas e configuração |
| Caminho feliz | Rotina gerada, utilizada e visível depois da conclusão; retorno funciona |
| Retomada | Estado final e sessão mantidos ao recarregar, sem regeneração |
| Validações e falhas | Token inválido, entrada incompleta e falha recuperável não viram sucesso |
| Segurança | Cenário fora do escopo bloqueado; sem pagamento ou efeitos comerciais |
| Integração | Frontend real e backend PDE local; testes do harness com sessões segregadas |
| Observabilidade | Versão, produto, eventos, resultado, captura e falha correlacionados |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 |
| Isolamento | Proxy aponta Mira ao container próprio; recriar Mira preserva container e imagem de Vega |
| Processo | Retentativa preserva #351, conclui a correção e exige nova homologação da v2 |
| Limpeza | Compose exclusivo da sandbox encerrado com volumes e órfãos removidos |

Uma rodada local completa sem defeitos encerra a homologação. Se houver correção durante a
rodada, serão exigidas duas rodadas completas consecutivas sem falhas após o último ajuste.

## Correções e prevenção

- `blockedActivities` da correção contém os pareceres independentes, sem substituir a rejeição
  original pela própria tentativa bloqueada. Tentativas anteriores ficam em `correctionAttempts`.
  O teste parametrizado reproduziu falha com uma e duas tentativas anteriores antes da correção.
- O resultado da homologação mantém rotina, limites e retomada, publica um título semântico de
  conclusão e apresenta o estado consultado como texto, eliminando um controle inerte.
- O favicon local evita uma requisição inexistente durante a avaliação do navegador.
- A montagem inicial em checkout isolado omitiu recursos externos do Maven. O backend recusou
  inicializar e o rollback restaurou a imagem anterior. Nenhuma tarefa nova havia sido criada.
  A segunda imagem contém os 314 recursos previstos pelo POM e inicializa 115 cartões reais.
  A validação agora compara os arquivos versionados com o JAR e instancia o catálogo com o
  classloader do executável antes de publicar. O workflow de containers executa esse contrato.

## Validações intermediárias

Após o ajuste de empacotamento, as rodadas `release2` e `release3` terminaram consecutivamente sem falhas:
156 testes Java por rodada, 15 jornadas Playwright em três dispositivos, cinco cenários do harness
real, contratos de isolamento, proxy exato, retomada, segregação e lint do workflow. Os quatro
controles negativos/positivos de empacotamento e o catálogo do JAR passaram nas duas rodadas.
O smoke do catálogo também passou dentro da imagem Docker final, sem rede e sem banco produtivo.

A implantação usa o commit publicado `d1c41901` mais somente o patch causal de Mira;
as mudanças locais anteriores do processo de Vega não integram a imagem implantada.

## Primeira recuperação operacional

A publicação autorizada está saudável e foi conferida no MCP e nos diagnósticos públicos:

- Backend: `marketinghub-backend:mira351-e5325331263290af-r2`.
- Mira: `marketing-hub/pde-platform-frontend-mira:mira351-e5325331263290af`,
  `mira-private-v2`, produto 10, container próprio na porta 5180.
- Proxy: configuração idêntica à versionada, validada com `nginx -t` e recarregada.
- Vega: imagem `ghcr.io/paulofor/pde-platform-frontend-v7:20c1037ce8e98160a4527d13b311ce8d60a1b37d`
  e mesmo container, sem recriação.
- Pela tela, foi criada a tarefa #352; a #351 permaneceu como evidência histórica.

Os resultados seguintes estão registrados abaixo, preservando a sequência das causas confirmadas.


## Segunda causa confirmada na tarefa #352

A #352 consumiu a v2 no alvo, mas recebeu a aceitação histórica v1 dentro do contexto PDE e
nenhuma prova persistida da implantação manual autorizada. Repetiu o bloqueio pela ausência
de um workflow com seleção Mira. O diagnóstico público, o container e a imagem já eram v2.

Alternativas comparadas: coletor de diagnóstico no worker (automação adicional e novo deploy
do executor); novo formulário específico de implantação (mais contrato/interface); ou registrar
a prova real pelo editor existente e reconciliar o contexto no backend (menor mudança e mesma
fonte canônica). Escolhida a terceira: mantém a execução no worker e fornece dados auditáveis.

O teste reproduziu sete falhas com a implementação anterior. A correção alinha versão e
aceitação e exclui provas com produto, versão, URL, status ou identidade de imagem divergentes.
A #352 permanece no histórico; nenhuma aprovação foi escrita manualmente.


A validação posterior à correção de contexto encerrou nas rodadas `context1` e `context2`:
171 testes Java por rodada (incluindo os sete casos de divergência), 15 jornadas de navegador,
cinco cenários do harness, quatro testes de empacotamento, catálogo real do JAR, proxy e
lifecycle isolados, lint e envio do recibo pela UI com `PUT` interceptado por test double.
O formulário preservou integralmente o contrato anterior e os dados de identidade, preço e
experiência; apenas a evidência técnica foi adicionada. Spotless passou nas quatro classes
Java alteradas. A imagem final inicializou o catálogo dentro do Docker sem rede.


## Concorrência com publicações automáticas

O run [34186616085](https://github.com/paulofor/marketing-hub/actions/runs/34186616085),
commit `624b6131`, publicou `marketinghub-backend:latest` enquanto a #353 executava, substituindo
a imagem manual validada. O run seguinte, `34186628300`, ainda estava nos testes de construção;
foi cancelado antes do deploy. Quatro runs pendentes do mesmo workflow também foram cancelados.
`Build & Deploy containers` ficou temporariamente `disabled_manually`, com os demais workflows
preservados. Reativar quando o PR estiver pronto para merge e então fazer o merge é necessário para não sobrescrever novamente a correção.

A diferença entre a base inicial e o novo `main` (`ad2929c6`) continha apenas sete arquivos de
pesquisa/cards. Esses dados foram incorporados ao checkout isolado de entrega para preservar
as atualizações legítimas do catálogo. As alterações locais de estratégia/processo de Vega
continuam fora da imagem. Nenhum commit, push ou PR foi criado nesta recuperação.

A tarefa #353 concluiu com `COMPLETED`, decisão `READY` e raciocínio `max`. O parecer confirma
rotina e limites preservados, disponibilidade da v2 e retorno obrigatório a `technicalHomologation`.


## Bloqueio posterior da revalidação

Depois de #353 concluir, o GET das atividades ainda exibia `technicalHomologation` como
`COMPLETED`, usando a instância #207 e a tarefa #349 da v1/processo v7. O comando era recusado
antes de consultar o gate v8. A tela ficou sem ação para a homologação nova.

Foram comparadas uma nova referência global de ciclo (isolamento forte, mas mudança de
contratos e migração), criação antecipada de todas as ocorrências no callback (mais persistência
operacional) e elegibilidade de revalidação declarada pelo domínio (menor alteração, sem mudar
referências nem histórico). Escolhida a terceira, integrada ao motor genérico.

O teste com serviço da tela e gate reais reproduziu quatro falhas. A correção distingue aprovação
atual de conclusão histórica, exige prova posterior à última correção e cria nova ocorrência somente
pelo comando oficial. Os casos adicionais protegem aprovação atual, execução pendente/em andamento,
idempotência e uma segunda rejeição funcional. Nenhum estado histórico foi resetado.


## Validação final da revalidação

Após a última correção funcional, `complete1` e `complete2` passaram consecutivamente:
2.400 testes Java contabilizados por rodada, com 2.395 executados e cinco skips preexistentes,
zero falhas e zero erros; 15 jornadas Playwright; cinco cenários do harness real; quatro
contratos de empacotamento; isolamento/recriação do container, proxy e lint. Os skips são
fixtures condicionais de HTML, vídeo, integração MySQL e arquivo comercial, fora deste fluxo.
Todos os testes de Mira e da revalidação foram executados.

A verificação adicional na branch com as alterações anteriores de Vega passou em 144 testes
de integração dos serviços. Spotless validou as nove classes Java alteradas. A imagem final
foi recompilada após a formatação, contém 318 recursos externos íntegros e inicializou 119 cards
no smoke Docker sem rede. A revisão visual confirmou rotina, limites e consulta persistentes
na conclusão mobile. Não houve nova mudança funcional depois das duas rodadas completas.


## Rejeição superada ainda destacada na tela

Após criar #354 pela UI, o processo mostrava #350 como bloqueio atual durante a execução
da homologação. O teste ampliado com esse histórico reproduziu seis falhas. Foram comparados:
manter a mensagem com ressalva de histórico (baixo esforço, orientação ambígua); trocar a
prioridade global dos estados (simples, risco para outros processos); ou distinguir no gate
do domínio os pareceres superados pela correção (escopo menor e histórico preservado).
Escolhida a terceira alternativa.

O estado atual deixa de usar rejeições anteriores à correção válida. Bloqueios novos continuam
visíveis; uma falha técnica posterior também invalida a aprovação anterior. O contrato inclui
atividade pendente/em execução, cinco combinações de bloqueio e falha técnica após aprovação.
A descoberta reiniciou as duas rodadas completas antes da implantação desse refinamento.

A #354 concluiu com `APPROVED` para `mira-private-v2`. Pela tela, foi criada #355 para Psique
aderente, com `max`, sem reutilizar o parecer da #350.


## Aceite operacional confirmado

- #351 e #352: tentativas bloqueadas preservadas, sem reset ou aprovação artificial.
- #353: `prototypeCorrection`, `COMPLETED`, decisão `READY`, raciocínio `max`.
- #354: `technicalHomologation`, `COMPLETED`, `APPROVED` para `mira-private-v2`,
  cinco cenários e três dispositivos; evidências visuais 87–91; todos os 13 checks verdadeiros.
- #355: `psiqueAdherent`, `COMPLETED`, `APPROVED`, `max`, mesma v2 e sessão isolada;
  evidência 92 e nove checks verdadeiros. A avaliação confirmou a permanência da rotina,
  limites e ação de retomada. O processo avançou para `psiqueRecovery`.

As duas tarefas confirmam somente a validação sintética. `humanEvidenceClaimed=false` e
`commercialEvidenceClaimed=false`; sem compra, campanha, publicação comercial ou gasto de
mídia. O backend estimou US$ 1,7899192 de uso de modelo nas novas #352, #353 e #355; #354
executa o harness sem modelo e não reportou custo. Estimativas não são fatura do provedor.

A atividade solicitada foi concluída e a rejeição original foi reavaliada com aprovação real
do agente. As etapas seguintes do processo mantêm seus próprios critérios e aprovações.


## Fechamento das rodadas após o ajuste de estado

`closure1` e `closure2` encerraram consecutivamente sem falhas, após o último ajuste funcional:
2.406 testes Java contabilizados por rodada (2.401 executados e cinco skips preexistentes),
15 jornadas Playwright, cinco cenários do harness e quatro contratos de empacotamento, além de
proxy, lifecycle, lint e formulário com envio interceptado. A integração com a branch contendo
as alterações anteriores de Vega passou em 150 testes, sem falhas, erros ou skips.

Imagem final do backend: `marketinghub-backend:mira351-ad2929c665-0fd2d3cb`.
Base `ad2929c6650cf7a26939e4acb890611b73bc454d`, somente o patch causal de Mira,
prova portátil SHA-256 `347b30ba3236e571db2b322487663618176cf9cfa8bafab4e7c10e9fd0479e46`.
O smoke dentro da imagem inicializou 119 cards; os 318 recursos externos do JAR conferem
byte a byte com o checkout. As mudanças anteriores de Vega permanecem na branch e fora
dessa imagem de produção.

O workflow geral força `BACKEND_IMAGE_TAG=latest` no script de publicação. Por isso, reativá-lo
antes de a correção estar pronta para entrar em `main` voltaria a sobrescrever a imagem manual.
Próxima ação de governança: solicitar o PR; com ele pronto, reativar **Build & Deploy containers**
imediatamente antes do merge. O workflow é acionado por `push`, sem `workflow_dispatch`.


## Conferência final no host e na interface

A imagem final ficou `UP`, identificada pelo MCP e pela prova de conteúdo no VPS. O comando
foi limitado ao backend; os demais containers permaneceram com os mesmos IDs. Mira conserva
imagem e container exclusivos; Vega permaneceu no container `4b611ddbf6e3` e na imagem original.
A UI final respondeu HTTP 200, confirmou as três atividades concluídas, apresentou
`psiqueRecovery / NOT_STARTED` com o comando habilitado e não registrou erros de navegador.

A topologia Compose exclusiva foi encerrada com `--volumes --remove-orphans`; não restam
containers, redes ou volumes com o rótulo da solicitação. As imagens locais próprias foram removidas; no VPS foram
conferidas a imagem ativa e duas versões de rollback preservadas. Nenhum PR foi criado.

Evidências locais: `artifacts/mira-351/final-validation-summary.json`,
`production-acceptance-summary.json`, `final-ui-verification.json`, `final-activities-ui.png`,
`release-manifest.json`, `backend-final-remote-proof.json`, `backend-final-runtime.json` e
`cleanup-resources.json`. Os pareceres e capturas produtivos permanecem auditáveis nas
tarefas #353–#355 do Marketing Hub.
