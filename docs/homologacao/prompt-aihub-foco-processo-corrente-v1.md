# Revisão do prompt AIHUB para o processo corrente

Data: 18/09/2026. Escopo: modelo compartilhado do botão **Prompt para AIHUB**, prévia e cópia.

## Evidência e decisão

A leitura de `ProductProcessContextCopy.tsx` confirmou um único template Markdown seguido do
contexto oficial completo. O template acumulava exemplos de um tipo específico, um inventário
de conceitos de agentes e repetição obrigatória da matriz. Os testes unitários e de navegador
exigiam literalmente o exemplo de Quartzo, mantendo esse viés mesmo em processos de outros tipos.
Histórico consultado: `prompt-aihub-correcao-sistemica-v1.md`, cânone de aperfeiçoamento e
`docs/registros/loops.md`. Esta revisão não diagnostica tarefas produtivas anteriores.

| Alternativa                                                      | Benefício                                            | Risco e esforço                                                                | Decisão      |
| ---------------------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------ | ------------ |
| Remover só o exemplo de tipo                                     | Alteração pequena                                    | Mantém repetições e falta de orientação sobre testes unitários                 | Insuficiente |
| Consolidar o template e remeter detalhes aos cânones pertinentes | Objetivo direto com contratos e contexto preservados | Baixo esforço; exige conferir proteções e cópia                                | Escolhida    |
| Criar templates separados por tipo/agente                        | Especialização detalhada                             | Maior esforço e risco de regras divergentes; desnecessário para o pedido atual | Não adotada  |

Mantidos: autoridade do backend, tipo/ficha aprovados, gates, rentabilidade, autonomia local,
prevenção sistêmica, evidências, limites de publicação e dados oficiais sem truncamento.
Incluída revisão explícita de testes unitários de cada módulo alterado, fixtures/mocks,
regressões, execução local e proibição de enfraquecer proteções para aprovar.
O detalhamento de tipos e finanças continua nos cânones; o template não muda os contratos.
Nenhum endpoint novo é necessário: trata-se do texto estático acoplado à cópia existente.

Referência consultada: [OpenAI — Message formatting with Markdown and XML](https://developers.openai.com/api/docs/guides/prompt-engineering#message-formatting-with-markdown-and-xml),
em 18/09/2026. Aplicação: objetivo, instruções e contexto em seções separadas; regras estáveis
antes dos dados variáveis. Não foi feita chamada paga para avaliar comportamento de modelo.

## Matriz definida antes dos testes

| Critério                   | Validação                                                                                        |
| -------------------------- | ------------------------------------------------------------------------------------------------ |
| Texto e proteções          | Teste do template: escopo corrente, causas, testes por módulo, preservação de gates e publicação |
| Generalização              | Mesmo template para produtos/processos distintos; dados específicos somente no contexto          |
| Cópia e integração         | Testes React e build real com APIs simuladas: texto integral, prévia e clipboard                 |
| Falhas                     | Carregamento, erro de clipboard, seleção manual, retentativa e consulta incompleta               |
| Isolamento                 | Troca de produto/ciclo/processo sem dados anteriores, sem mutações de backend                    |
| Observabilidade e métricas | Confirmação de cópia, erro visível e resultado dos testes; nenhum evento de venda/IA             |
| Dispositivos               | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; contexto seguro e HTTP                       |
| Regressão                  | Suíte de contexto/prompt/painéis, TypeScript, build, formatação, bash -n, ShellCheck e diff      |

Executar uma rodada relevante. Corrigir defeitos encontrados e repetir as validações afetadas;
quantidade fixa de rodadas não substitui evidência funcional.

## Resultado

Comparação do template: **18.636 → 7.811 caracteres (redução de 58%)**. A fotografia
oficial acrescentada ao prompt permanece integral; não houve resumo ou mudança de endpoint.
O ganho medido é redução textual e manutenção dos contratos de cópia, não melhoria de vendas
nem de desempenho de um modelo, que não foi executado nesta homologação.

| Verificação                                         | Resultado                                                                                         |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Testes React/contrato de contexto, prompt e painéis | 119 casos únicos validados; ver detalhe abaixo                                                    |
| TypeScript                                          | Aprovado: `npm --prefix frontend run typecheck`                                                   |
| Build                                               | Aprovado: `npm --prefix frontend run build`                                                       |
| Prompt na tela e clipboard real                     | 6/6 combinações aprovadas: desktop, iPhone 15 Pro e Pixel 7, HTTP e contexto seguro               |
| Isolamento e limites                                | Contexto único, troca de produto/processo/ciclo, zero mutações de backend nos testes de navegador |
| Falhas de cópia e consulta                          | Seleção manual, retentativa, carregamento e ausência de dados aprovados                           |
| Formatação e diff                                   | Prettier nos arquivos de prompt/testes/relatório e `git diff --check` aprovados                   |
| Runner shell existente                              | `bash -n` e depois `shellcheck` aprovados; script sem alterações                                  |

A primeira execução da suíte de 10 arquivos teve 118 casos aprovados e uma falha no novo
assert textual: ele esperava “não contorne gates”, enquanto o template dizia “nem contorne
gates”. Corrigida essa expectativa sem retirar a proteção e ampliado o caso de troca de
contexto para outro produto e processo. Os 7 casos do arquivo afetado passaram na execução
final; os 112 casos dos demais arquivos já haviam passado e não sofreram alterações.
Build, TypeScript e as seis combinações de navegador passaram após o ajuste pertinente.
Não se repetiu a matriz para cumprir uma contagem artificial.

Evidências locais em `artifacts/process-context-copy/foco-corrente-20260918/`:
`frontend.log`, `prompt-recheck.log`, `typecheck.log`, `build.log`, `aihub-browser.log` e
`aihub-browser/results.json`. Textos efetivamente colados e capturas estão na mesma pasta
`aihub-browser/`. Conferida visualmente a prévia `iphone-http-preview.png`: texto legível
no card, confirmação e expansão presentes. Servidor e navegador temporários encerrados.

Limites: celulares emulados no Chromium, sem Safari físico; APIs simuladas e nenhuma chamada
paga a modelos, consulta de tarefa produtiva ou alteração de estado comercial. O build emite
os avisos existentes de API CJS do Vite e tamanho do bundle, sem falha de compilação.
O prompt orienta ações futuras; estes testes não comprovam que um agente sempre o obedecerá.
Mudanças somente na worktree, sem commit, PR ou publicação. A atualização da tela publicada
depende do fluxo normal de revisão e deploy do frontend.
