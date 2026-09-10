# Vega — responsável e entrada da homologação técnica

Data: 2026-09-10. Escopo: corrigir a identificação de Harness como agente e a liberação
indevida da tarefa 3.5 quando ainda não existe alvo executável. Não representa implementação
ou aprovação do protótipo v8.

## Evidências e decisão

- Tela `/products/4/value-chain-history/processes/70/activities` abriu sem erro de JavaScript.
  API: `GET /api/business-processes/70/products/4/activity-executions?learningCycleId=2&chainId=14`.
- MCP confirmou #377 `BLOCKED`, `source_reference=experiment:92`, agente 2 (Psique),
  instância 239, atividade `technicalHomologation`. Erro: URL do PDE inválida.
- O log de Psique registra a falha em `2026-09-10T13:47:31.665Z`. O filtro do backend
  por `taskId=377` retornou zero linhas; a ausência de linha não substitui o registro persistido.
- #371, #354 e #349 de Mira concluíram com referência de produto e URL privada aceita.
- `LearningCycleConstructionContext` retorna deliberadamente URL nula e plano `PLANNED`.
  O produto 4 tem definição comercial `v1`, sem `privatePrototypeAcceptance` ou
  `agentValidationPlan`. #376 entregou uma especificação de acesso, não uma implantação.
- O script de navegador existente usa rota, sessão e controles específicos de Mira. Apontá-lo
  para a versão comercial anterior de Vega não comprova o sucessor e produziria outra falha.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Resolver o responsável pelo catálogo de agentes na leitura e adicionar a validação de entrada | Nome sempre ligado ao agente real; impede novas tarefas sem alvo | Esforço moderado; mantém metadados divergentes e pode mudar a apresentação do histórico | Viável, mas não corrige o cadastro na origem |
| Publicar uma nova versão do processo com responsável e pré-requisitos corrigidos | Preserva integralmente a definição v8 e explicita a evolução do fluxo | Esforço alto; exige migração das cadeias, apesar de não haver mudança de sequência | Viável, mas desproporcional à correção de metadados |
| Corrigir os metadados da versão vigente e validar o alvo antes da tarefa | Corrige a origem, evita retrabalho e preserva tarefas e versões anteriores | Esforço moderado; exige migração validada e não substitui a implementação do protótipo | Adotada |

Repetir a tarefa ou apontar para uma URL histórica não são soluções válidas: não produzem
o protótipo do segundo ciclo nem seus cenários próprios de homologação.

## Matriz definida antes da homologação

| Controle | Critério |
| --- | --- |
| Reprodução #377 | Plano do segundo ciclo com URL nula bloqueado antes de criar tarefa |
| Caminho válido | Protótipo privado aceito com produto, referência, URL e versão coerentes permite homologação |
| Falhas de contrato | URL ausente/inválida, credencial/query, identidade/versão divergentes e aceitação ausente bloqueiam |
| Histórico e isolamento | Não reaproveitar outra versão/produto, nem alterar tarefas ou métricas comerciais |
| Tela e comando | Responsável Psique; harness explicado como ferramenta; motivo claro e retentativa indisponível |
| Migração MySQL 5.7 | Corrige metadados do processo vigente; preserva chaves, tarefas e fluxo; reaplicação idempotente |
| Executor | Falta de alvo produz diagnóstico específico antes de navegador ou chamada externa |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação local |
| Regressões | Testes pertinentes de backend, worker e frontend; duas rodadas após a última correção |
| Limpeza | Compose exclusivo encerrado com volumes e órfãos removidos |

## Resultados locais

Duas rodadas completas e consecutivas, `final1` e `final2`, passaram após a última correção,
sem alterações de implementação entre elas. Cada rodada aprovou os dez controles do runner:

| Verificação por rodada | Resultado |
| --- | --- |
| Backend: comandos HTTP, disponibilidade, contexto, próximo trabalho, auditoria e arquitetura | 288 testes, zero falhas/erros/ignorados |
| Psique: suíte Java completa | 92 testes, zero falhas/erros/ignorados |
| Harness de navegador: testes com HTTP e Chromium locais | 7 testes aprovados |
| Cliente administrativo | Desktop, iPhone 15 Pro e Pixel 7 aprovados |
| Liquibase real | MySQL 5.7, migração e duas reaplicações aprovadas; histórico e tarefas preservados |
| Contratos, formatação e workflow | Validador Liquibase, Spotless dos arquivos Java alterados, Actionlint e revisão de whitespace aprovados |
| Isolamento e limpeza | Nenhuma escrita produtiva; Compose, rede e volume exclusivos removidos nas duas rodadas |

Uma rodada preparatória expôs divergência de serialização de data na fixture local
(segundos numéricos em vez de ISO-8601). A simulação passou a reproduzir a API e ganhou
assertiva da data na tela. Essa divergência não foi encontrada na API publicada.
As duas rodadas acima foram realizadas depois dessa correção; rodadas preparatórias não
entram na contagem final.

O teste HTTP usa controller, service, regra de disponibilidade e resolvedor de próximo
trabalho reais, com persistência e alvo simulados. O teste Liquibase usa banco físico
sintético separado. O navegador lê o contrato gerado pelo teste HTTP e fixtures de aprendizado
identificadas como dados de teste; não cria tarefas, métricas, sessões ou vendas no produto.

O frontend não teve código alterado. Foi executado localmente o bundle já publicado,
baixado somente para leitura, com todas as APIs interceptadas; não foi realizado novo build
do frontend. Identidade dos arquivos:

- `index-DF0RogqE.js`: SHA-256 `39adadbe1c27d44d27550fc3335015306841d9129dfef1ffb4b4e8517974d1cf`.
- `index-BvzkE73l.css`: SHA-256 `0dc12e37d8c30850cc23ce2c53a3fc74b99a517304b5a5f649b71f0f15d61481`.

Logs, contratos gerados, contagens e capturas locais estão em
`artifacts/vega377/final1/` e `artifacts/vega377/final2/` (artefatos ignorados pelo Git).
O runner reproduzível fica em `infra/testing/vega-harness-readiness/`.
A prova física da migração também foi adicionada ao workflow Liquibase do PR; nenhum
workflow foi disparado para testar a correção.

## Limites da entrega

A tarefa produtiva #377 permanece bloqueada e nenhuma nova tarefa foi criada. A correção
local elimina a identificação incorreta e a liberação de uma execução sem pré-requisitos;
não representa homologação do Vega. Sua versão executável aceita, o contrato de integração
com o ciclo e os cenários próprios do produto continuam necessários. O script atual de Mira
não comprova essa versão do Vega.

Nenhum commit, PR, imagem de produção ou deploy foi realizado. Não houve uso de modelo pago,
campanha ou gasto comercial. Houve migração local do caso afetado, não bootstrap de todo o
banco de produção nem execução integrada de todos os serviços do Marketing Hub.
