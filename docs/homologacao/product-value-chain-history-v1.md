# Homologação — histórico do produto na cadeia de valor v1

## Objetivo

Comprovar que cada produto possui uma tela dedicada para acompanhar sua passagem pelos processos e
subprocessos da cadeia, exibindo data e hora de entrada e saída, permanência, custo conhecido e
cobertura financeira sem inferir conclusão ou custo ausente no frontend.

## Decisão de implementação

| Alternativa                                                   | Benefício                              | Risco/custo                                                       | Decisão    |
| ------------------------------------------------------------- | -------------------------------------- | ----------------------------------------------------------------- | ---------- |
| Manter apenas o histórico expansível no card                  | Nenhuma rota nova                      | Difícil acompanhar, comparar e compartilhar a visão de um produto | Descartada |
| Nova tela filtrando no navegador a lista de todos os produtos | Mudança pequena                        | Carrega dados alheios ao produto e escala mal                     | Descartada |
| Nova tela com endpoint específico por produto                 | Contrato direto, auditável e escalável | Exige método e teste backend adicionais                           | Escolhida  |

## Decisão para continuidade após conclusão

| Alternativa                                                | Benefício                           | Risco/custo                                                     | Decisão    |
| ---------------------------------------------------------- | ----------------------------------- | --------------------------------------------------------------- | ---------- |
| Inferir o sucessor no frontend                             | Alteração pequena                   | Contraria a verdade do backend e pode abrir etapa indevida      | Descartada |
| Mostrar diretamente o macroprocesso seguinte               | Deixa a cadeia visualmente contínua | Pula atividades obrigatórias ainda existentes no processo atual | Descartada |
| Priorizar a instância BPM e exibir o próximo passo oficial | Corrige a causa e preserva gates    | Exige ajuste backend e homologação integrada                    | Escolhida  |

## Decisão de desempenho — 2026-09-01

Evidência operacional: a primeira abertura consultava imediatamente a trilha integral. No Vega, o
backend carregava 27 tarefas e aproximadamente 300 mil caracteres de auditoria. A tabela
`agent_task` possuía 302 registros, 2,38 MB de payload auditável e nenhum índice em
`source_reference`, vínculo usado repetidamente por plano e experimento.

| Alternativa                                      | Benefício                             | Risco/custo                                             | Decisão    |
| ------------------------------------------------ | ------------------------------------- | ------------------------------------------------------- | ---------- |
| Paginar somente os itens já renderizados         | Alteração restrita ao frontend        | A varredura histórica já ocorreu no backend             | Descartada |
| Cachear a posição e o histórico                  | Resposta recorrente rápida            | Pode apresentar tarefa, custo ou etapa desatualizados   | Descartada |
| Resumo leve + histórico sob demanda + índice SQL | Primeira visão rápida e escala linear | Novo contrato e migração incremental compatível com 5.7 | Escolhida  |

## Matriz ponta a ponta

| Área            | Cenário                                       | Critério de aprovação                                                                 |
| --------------- | --------------------------------------------- | ------------------------------------------------------------------------------------- |
| Caminho feliz   | Abrir pelo botão do card                      | Navega para `/products/:productId/value-chain-history` e identifica o mesmo produto   |
| Primeira visão  | Abrir a tela                                  | Consulta somente o resumo; não busca produto completo, commits ou tarefas históricas  |
| Sob demanda     | Solicitar histórico detalhado                 | Busca o contrato auditável e os commits uma única vez e então mostra toda a cadeia    |
| Histórico       | Processos concluído e atual                   | Entrada, saída, permanência, objetivo, evidência e custo aparecem na ordem temporal   |
| Subprocesso     | Entrada ainda não registrada                  | Tela informa ausência da data sem fabricar início, saída ou permanência               |
| Continuidade    | Subprocesso intermediário concluído           | Próximo subprocesso aparece planejado sem execução inventada                          |
| Continuidade    | Último subprocesso concluído                  | Próxima atividade do processo pai aparece com acesso às atividades                    |
| Retentativa     | Tarefa antiga bloqueada e instância concluída | Estado consolidado da instância prevalece e o bloqueio histórico não retém a posição  |
| Fim da cadeia   | Não existe sucessor publicado                 | Tela não fabrica próximo passo                                                        |
| Custo           | Cobertura completa, parcial e não reportada   | Subtotal conhecido e lacunas ficam explícitos; ausente não vira total zero            |
| Integração      | Resumo individual                             | Frontend chama `GET /api/products/value-chain-positions/{productId}/summary`          |
| Integração      | Histórico solicitado                          | Frontend chama `GET /api/products/value-chain-positions/{productId}` e commits        |
| Validação       | Produto inexistente ou API indisponível       | Backend responde 404 e a tela mostra falha sem histórico inventado                    |
| Falha parcial   | Histórico indisponível após resumo válido     | Mantém a posição atual e oferece retentativa explícita sem fabricar a trilha          |
| Observabilidade | Fonte do dado                                 | Posição, datas, evidências e custos vêm do backend persistido                         |
| Métrica         | Desempenho                                    | Resumo inicial em até 5 s; nenhuma consulta histórica antes do clique                 |
| Banco           | Crescimento do histórico                      | Índice `source_reference(191)` existe e é reaplicável no MySQL 5.7                    |
| Segregação      | Dados de teste                                | Testes usam mocks/local e não alteram produção, campanhas, custos ou métricas humanas |
| Desktop         | Chromium 1440×1000                            | Botão, resumo e linha do tempo ficam legíveis e sem overflow                          |
| Mobile          | iPhone 15 Pro e Pixel 7                       | Resumo e fatos empilham; links e status permanecem acessíveis e sem overflow          |

## Regra de repetição

Uma rodada local completa sem defeitos conclui a homologação. Se a rodada revelar defeito, a
causa-raiz deve ser corrigida e duas rodadas completas consecutivas sem falhas passam a ser
obrigatórias.

## Resultado de 2026-08-25

A homologação inicial revelou incompatibilidades locais de ícone e de alvo TypeScript. Após a
correção, a suíte ampla também expôs uma oscilação de tempo em teste antigo do Estúdio quando
executada em paralelo; o mesmo teste passou isoladamente e as rodadas finais foram executadas com um
worker para eliminar contenção da medição.

Duas rodadas locais completas e consecutivas terminaram sem falhas. Cada rodada aprovou:

- 1.823 testes do backend, com 1.821 executados e 2 ignorados, além do Spotless;
- 400 testes do frontend, tipagem, build de produção e Prettier nos arquivos alterados;
- botão no card, navegação e linha do tempo em desktop, iPhone 15 Pro e Pixel 7;
- ausência de overflow horizontal, erros JavaScript, histórico inventado e acesso agregado
  desnecessário ao catálogo;
- contrato HTTP individual, resposta 404 para produto inexistente e Swagger formatado.

Nenhuma chamada de escrita, campanha, gasto, venda, métrica humana ou dado produtivo foi alterado.

## Resultado de 2026-08-28 — continuidade após o 4.2

A consulta somente de leitura ao ambiente operacional confirmou a divergência do Rigel: o 4.2
estava concluído, mas a tarefa bloqueada #244 ainda escondia a instância BPM #129, concluída pela
retentativa #248. A composição publicada confirmou que o próximo passo ainda pertence ao processo
4: `Integrar canal, checkout, acesso e eventos`.

Uma rodada local completa terminou sem revelar defeitos:

- 1.973 testes do backend, com 1.971 executados e 2 ignorados, além do Spotless;
- 418 testes do frontend, tipagem, build de produção e Prettier;
- precedência da instância BPM, segregação entre tentativas e ausência de sucessor fabricado;
- exibição e abertura do próximo passo em Chromium desktop, iPhone 15 Pro e Pixel 7, sem overflow
  horizontal ou erro JavaScript.

Os testes usaram dados locais e interceptações HTTP. Nenhuma tarefa, evento comercial, venda,
publicação, campanha, gasto ou dado produtivo foi criado ou alterado.

## Resultado de 2026-09-01 — histórico sob demanda

A investigação de produção confirmou que a tela individual ainda iniciava toda a reconstrução
histórica. Para o Vega, o resolvedor encontrava 27 tarefas e aproximadamente 300 mil caracteres de
resultado e evidência. A tabela `agent_task` possuía 302 linhas, 2,38 MB de payload auditável e
nenhum índice em `source_reference`. O endpoint não registrava erro, mas o custo crescia com o
histórico global mesmo quando o usuário queria apenas saber a posição atual.

Uma rodada local completa e sequencial terminou sem revelar defeito no fluxo:

- 2.203 testes do backend, com 2.200 executados e 3 ignorados, além de Spotless;
- 455 testes do frontend, tipagem e build de produção;
- validação estática dos changelogs e migração física, reaplicável, no MySQL 5.7;
- resumo inicial sem consulta de histórico ou commits em 545 ms no desktop, 573 ms no iPhone 15
  Pro e 529 ms no Pixel 7;
- carregamento do histórico e dos commits somente após o clique, uma vez no caminho feliz;
- preservação do resumo e retentativa explícita quando o histórico falha;
- zero overflow horizontal e zero erro JavaScript nos três dispositivos.

Uma tentativa anterior executada ao mesmo tempo que Maven e Docker gerou timeout em dois testes
antigos e não relacionados do frontend. Ambos passaram isoladamente e a suíte integral passou com
um único worker, confirmando contenção local de CPU/I/O e descartando regressão funcional.

Os testes usaram banco efêmero e interceptações HTTP locais. Nenhuma tarefa, campanha, evento
comercial, gasto, venda ou dado produtivo foi criado ou alterado.
