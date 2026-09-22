# Homologação dos cinco critérios comerciais PDE — 22/09/2026

Escopo: tornar desejo, facilidade, prova de valor, continuidade paga e aprendizado com margem
explícitos nas atividades existentes. A referência comercial #91 é uma hipótese de participação;
esta entrega não altera os experimentos #91/#92 nem comprova vendas.

## Matriz definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Migração MySQL 5.7 / Liquibase | Cadeia v18 com seis processos; novas versões e atividades relacionais coerentes; grafo original preservado exceto descrições. |
| História e isolamento | Diagramas/atividades antigos, tarefas, custos e vínculos de produtos sintéticos intactos; nenhuma mutação de campanha, experimento ou orçamento. |
| Reaplicação e rollback | Sem versões/atividades duplicadas; rollback muda somente disponibilidade das novas definições e preserva evidências. |
| Fonte ausente / conflito | Pré-condições impedem versão parcial, nós ausentes ou sobrescrita de uma versão criada por outra entrega. |
| Contrato dos cinco pontos | Saída, aceite e métrica explícitos; custo desconhecido não é zero; oferta vista diferente de resultado pronto; degustação condicional ao plano/tipo. |
| Tela | Objetivos completos nas atividades e nos textos de ajuda; desktop, iPhone 15 Pro e Pixel 7 legíveis sem perda de critérios. |
| Integração local | Dados pós-migração alimentam a interface local com API simulada; nenhum evento de teste vai à produção. |
| Regressões | Testes unitários do backend e validação estática Liquibase; nenhuma alteração de prompt operacional dos workers. |
| Produção | PR revisado, main integrada, workflows aplicáveis aprovados, saúde/versão e critérios conferidos na tela e API. |

## Limites

Os critérios são objetivos operacionais que chegam à solicitação BPM pelo contrato existente.
Não são novos validadores automáticos de cada evidência nem mudanças nos eventos dos PDEs.
Também não são alteração de oferta, formato ou autorização de IA/mídia. A chamada automática
de degustação não é criada: a integração passa a exigir explicitamente a referência/evidência
quando escolhida no plano, mantendo o percurso atual. Aprimoramento aqui é clareza da missão
dos responsáveis; ganho de conversão e margem continua dependendo de medição comercial.

## Definições revistas

| Processo | Versão anterior → nova | Atividades detalhadas |
| --- | --- | ---: |
| Estratégia, economia e protótipo privado | 6 → 7 | 3 |
| Protótipo, validação multiagente e aprovação | 8 → 9 | 5 |
| Comunicação e jornada de venda | 7 → 8 | 3 |
| Degustação e prova de valor | 2 → 3 | 6 |
| Operação e otimização do experimento | 5 → 6 | 6 |
| Ciclos de aprendizado e vendas | 4 → 5 | 4 |
| Venda, entrega e aprendizado | 8 → 9 | 2 |

A cadeia v18 referencia as novas versões dos macroprocessos afetados. As definições anteriores
continuam disponíveis para não bloquear execuções sem ficha fixada; nenhuma tarefa/produto/ciclo
é migrada. A seleção de novos trabalhos continua pelo contrato de versão publicada mais recente.
O rollback retira somente a disponibilidade das novas definições, sem apagar dados.

## Resultados locais

- Backend: 3.373 testes na suíte, zero falhas/erros; 20 condicionais não aplicáveis ao ambiente.
- Fixture física final: Liquibase/MySQL 5.7 aprovado, incluindo fontes inválidas, colisão,
  paridade dos objetivos, preservação de atividades/tarefas/custo/vínculo sintéticos,
  reaplicação e rollback com disponibilidade anterior preservada.
- Interface local: 21 páginas / 87 verificações de objetivos (29 em cada viewport), desktop,
  iPhone 15 Pro e Pixel 7; sem erro de página nem transbordamento horizontal.
- Build frontend, empacotamento backend e Spotless aprovados.
- Validação estática Liquibase, `bash -n`, ShellCheck do validador, Actionlint do workflow,
  sintaxe Python/Node e `git diff --check` aprovados.

A primeira rodada concorrente disputou classes do Maven e memória com o build frontend; foi
substituída pela execução sequencial completa acima. As falhas dos testes novos eram assertivas
da fixture e seleção do diagrama de ciclos; foram corrigidas localmente antes de publicação.
Comandos reprodutíveis: runner MySQL documentado em `docs/database/liquibase-mysql57.md`;
para a UI local em 4179, `node infra/testing/pde-commercial-principles/browser.cjs`.
A fixture exporta `backend/ads-service/target/principles/processes.json`, lido pela API simulada.
