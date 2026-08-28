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

## Matriz ponta a ponta

| Área            | Cenário                                       | Critério de aprovação                                                                   |
| --------------- | --------------------------------------------- | --------------------------------------------------------------------------------------- |
| Caminho feliz   | Abrir pelo botão do card                      | Navega para `/products/:productId/value-chain-history` e identifica o mesmo produto     |
| Histórico       | Processos concluído e atual                   | Entrada, saída, permanência, objetivo, evidência e custo aparecem na ordem temporal     |
| Subprocesso     | Entrada ainda não registrada                  | Tela informa ausência da data sem fabricar início, saída ou permanência                 |
| Continuidade    | Subprocesso intermediário concluído           | Próximo subprocesso aparece planejado sem execução inventada                            |
| Continuidade    | Último subprocesso concluído                  | Próxima atividade do processo pai aparece com acesso às atividades                      |
| Retentativa     | Tarefa antiga bloqueada e instância concluída | Estado consolidado da instância prevalece e o bloqueio histórico não retém a posição    |
| Fim da cadeia   | Não existe sucessor publicado                 | Tela não fabrica próximo passo                                                          |
| Custo           | Cobertura completa, parcial e não reportada   | Subtotal conhecido e lacunas ficam explícitos; ausente não vira total zero              |
| Integração      | Consulta individual                           | Frontend chama produto e `GET /api/products/value-chain-positions/{productId}`          |
| Validação       | Produto inexistente ou API indisponível       | Backend responde 404 e a tela mostra falha sem histórico inventado                      |
| Observabilidade | Fonte do dado                                 | Posição, datas, evidências e custos vêm do backend persistido                           |
| Métrica         | Acompanhamento operacional                    | Etapa atual e total de etapas permitem localizar gargalo sem contar artefato como venda |
| Segregação      | Dados de teste                                | Testes usam mocks/local e não alteram produção, campanhas, custos ou métricas humanas   |
| Desktop         | Chromium 1440×1000                            | Botão, resumo e linha do tempo ficam legíveis e sem overflow                            |
| Mobile          | iPhone 15 Pro e Pixel 7                       | Resumo e fatos empilham; links e status permanecem acessíveis e sem overflow            |

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
