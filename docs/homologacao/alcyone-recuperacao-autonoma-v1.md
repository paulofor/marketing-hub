# Recuperação automática da preparação de comunicação — 06/10/2026

## Evidência e escopo

Tela, APIs oficiais e MCP confirmaram Alcyone #11, cadeia #26, processo #113 v11,
execução #44, experimento #97, plano #34 v4, em `WAITING_INPUT`. Não existe tarefa
de Íris. Atena #18 mantém `INSUFFICIENT_EVIDENCE`; Plutus #71 rejeitou as premissas.
A versão `alcyone-private-v3` usa fixtures e conserva a homologação #583–587.
Não há prova da entrega personalizada integrada nem política mínima de margem aprovada.

O histórico #19/#71 e outro plano com Atena #14 comprovam que a passagem concluída
podia depender de um comando manual. A transação própria corrigiu a gravação,
mas uma interrupção entre o commit da proposta e o evento ainda exige recuperação.
A retomada do processo deve recuperar a mesma proposta sem executar Atena novamente.
Uma rejeição já recebida não é uma passagem perdida e não pode criar outra avaliação.

## Alternativas

| Alternativa                                 | Benefício                                                                  | Risco/custo e aderência                                                           | Decisão                                          |
| ------------------------------------------- | -------------------------------------------------------------------------- | --------------------------------------------------------------------------------- | ------------------------------------------------ |
| Outbox transacional para todas as passagens | Recuperação durável mesmo sem processo ativo                               | Exige migração e operação de nova fila; maior alcance que o caso comprovado       | Reservar para necessidade transversal comprovada |
| Fila própria de propostas sem parecer       | Recuperação especializada e independente do BPM                            | Duplica consumo de pendências e precisa reconstruir autorização, pausa e contexto | Não criar mecanismo paralelo                     |
| Reconciliar no processo ativo existente     | Reutiliza controle de contexto, pausa e diário; encaminhamento idempotente | Menor esforço; cobre apenas a preparação ativa e mantém resultados concluídos     | Escolhida                                        |

Esta correção não transforma fixtures em geração, não aprova economia nem substitui
a implementação da entrega. Autorizações financeiras, decisões comerciais, STOP,
pausa e história permanecem separados. Rejeição deve ser explicada pela fonte real.

## Matriz local definida antes da implementação

| Caso                   | Aceite                                                                                        |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| Passagem perdida       | Reconciliação do processo recupera proposta concluída e uma única validação de Plutus         |
| Outra execução         | Produto, plano e IDs distintos recebem o mesmo comportamento                                  |
| Repetição/concorrência | Reutiliza validação pendente/concluída, sem Atena nem tarefa financeira duplicada             |
| Rejeição funcional     | Conserva parecer e custo, explica rejeição e não dispara revisão idêntica                     |
| Versão/identidade      | Recusa proposta anterior à execução, versão antiga ou outro produto/experimento               |
| Autoridade             | STOP, pausa, histórico encerrado e processo sem vez não disparam recuperação                  |
| Falha de integração    | Preserva proposta paga e erro auditável; nenhuma conclusão fictícia                           |
| Caminho antes válido   | Prontidão completa mantém o disparo automático canônico de Íris                               |
| Observabilidade        | Evento da recuperação correlaciona execução, proposta e validação; leitura não escreve        |
| UI                     | O contrato existente de status expõe a causa; conferir desktop, iPhone e Pixel sem provedores |

Dados locais são sintéticos; mocks não constituem provas de entrega, faturamento ou mercado.
Nenhuma chamada paga, publicação comercial ou campanha integra a matriz.

## Reprodução do contrato de tela

Executar `ProcessRunProjectedActivityPersistenceTest` com a propriedade
`-Dpreparation-recovery.fixture-output=<arquivo.json>` exporta status, diário persistido
e projeção sintética de atividades. O teste de navegador
`infra/testing/process-automation/preparation-recovery-browser.mjs` recebe esse arquivo
em `PREPARATION_RECOVERY_FIXTURE`, usa o frontend local em `PROCESS_FRONTEND_URL`
(padrão `http://127.0.0.1:4173`) e grava capturas em `PROCESS_TEST_ARTIFACTS`.
Requisições externas e WebSockets são bloqueados; requisições de escrita fazem o teste falhar.

A matriz não afirma que Dédalo já integra a geração. A decisão de margem mínima segue
ausente no parecer #71; não há novas entradas que justifiquem repetir os agentes pagos.

## Resultado local

- Backend completo: 4.072 casos, zero falhas/erros, 27 dispensas já declaradas pela suíte.
  A recuperação usa a fila financeira real em JPA para duas identidades independentes,
  conserva a proposta paga, consulta a versão exata e não repete uma rejeição.
- Diário e controle: passagem registrada uma vez, nenhum aceite artificial, STOP/pausa/versão
  retirada/vez na fila preservados; caminho com entradas prontas continua disparando a atividade.
- Frontend: build e 44 testes dos painéis de processo aprovados. Desktop, iPhone 15 Pro e Pixel 7
  exibem o diário exportado do backend, mantêm zero objetivos comprovados e não fazem escrita.
  A fixture conserva datas ISO/UTF-8 e o responsável da atividade; o teste bloqueia rede externa.
- Spotless nos Java alterados, diff, sintaxe JavaScript e parser YAML do OpenAPI conferidos.
  Pacote de evidências: 317 arquivos/61 manifestos, 15 testes aprovados; contrato do CI: 13 testes.
  JAR local: 4.227 classes idênticas às testadas, 754 recursos íntegros e catálogo inicializado
  com 518 cartões. Os 9 testes do verificador de recursos também passaram.
- Nenhum container temporário, nova inferência, compra, campanha ou alteração produtiva foi
  necessário para a matriz. A publicação e a conferência produtiva serão vinculadas ao PR.
