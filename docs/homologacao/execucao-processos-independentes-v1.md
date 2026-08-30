# Homologação — execução de processos independentes v1

## Objetivo comercial

Permitir que uma pergunta factual de mercado seja executada antes de existir produto, preservando a
sequência correta `evidência → oportunidade → produto`. A primeira aplicação é a atividade **Reunir
evidências factuais de mercado**, de Argos, no processo `pde-opportunity-discovery`.

## Matriz definida antes dos testes

| Dimensão        | Cenário                                            | Critério de aceite                                                                                                       |
| --------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Caminho feliz   | Abrir `/business-process-executions`               | O catálogo mostra somente processos publicados com escopo independente e destaca objetivo, gatilho e resultado esperado  |
| Caminho feliz   | Iniciar descoberta factual                         | Uma única execução, ciclo técnico e tarefa BPM de Argos são persistidos com a mesma referência operacional               |
| Idempotência    | Repetir o mesmo `requestKey`                       | A API devolve a execução existente e não duplica ciclo, tarefa nem custo                                                 |
| Validação       | Omitir tema/pergunta obrigatória                   | A API responde 400 sem persistir execução ou tarefa                                                                      |
| Validação       | Informar campo desconhecido ou tipo não textual    | A API responde 400 e explica o contrato rejeitado                                                                        |
| Escopo          | Tentar processo de produto, rascunho ou aposentado | O backend bloqueia o disparo; a tela nunca infere elegibilidade                                                          |
| Integração      | Disparar `pde-opportunity-discovery`               | O backend usa o ciclo canônico de Product Discovery; o worker continua consumindo somente seu endpoint `pending` oficial |
| Persistência    | Aplicar o changelog em MySQL 5.7                   | Escopo, chave idempotente, entrada estruturada, FK e campos `DATETIME` são criados e a reaplicação não altera dados      |
| Orquestração    | Haver atividades sucessoras                        | Todas as decisões de liberação permanecem no backend; a tela não chama worker nem próxima etapa                          |
| Observabilidade | Consultar histórico e detalhe                      | A tela apresenta referência, entrada, estado, atividades, tentativas, horários, erro, tokens e custo quando persistidos  |
| Falha funcional | Tarefa terminar bloqueada                          | A execução aparece `BLOCKED` com a causa persistida e sem ser tratada como venda                                         |
| Falha técnica   | Adaptador do processo ausente                      | O processo aparece indisponível com motivo e o comando fica desabilitado                                                 |
| Métricas        | Execução sem modelo ou preço                       | Ausência de custo não vira zero; tarefa técnica ou aprovação não vira receita ou venda                                   |
| Segregação      | Criar execução independente                        | Nenhum `productId` ou `experimentId` é fabricado; dados locais usam chaves próprias e não contaminam métricas comerciais |
| Concorrência    | Duplo clique/reenvio da criação                    | O botão fica em carregamento e a chave idempotente protege o backend                                                     |
| Desktop         | Chromium 1440 × 900                                | Formulário, catálogo, execução e histórico funcionam sem overflow horizontal                                             |
| Mobile          | iPhone 15 Pro e Pixel 7                            | Campos, botão, estados e histórico permanecem legíveis, tocáveis e sem overflow horizontal                               |

## Estratégia local

Os testes de backend usam adaptador e repositórios controlados; os testes do adaptador de descoberta
usam `ProductDiscoveryService` como test double. O frontend usa respostas HTTP controladas. Nenhum
worker produtivo, pesquisa externa, venda, publicação, contato ou gasto é necessário para homologar o
contrato local.
