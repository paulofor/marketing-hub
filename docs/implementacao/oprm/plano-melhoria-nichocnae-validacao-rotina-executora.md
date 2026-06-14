# Plano de implementação — OPRM NichoCNAE: validação da rotina executora antes da reprovação genérica

## 1. Contexto

Este plano nasce da análise dos ciclos NichoCNAE **#49, #50, #51 e #52** do CNAE `9602501` — Cabeleireiros, manicure e pedicure — com foco no subnicho de manicure autônoma em domicílio.

O ciclo evoluiu de forma útil, mas não chegou a um nicho aprovado:

| Ciclo | Status final | Leitura operacional |
|---:|---|---|
| #49 | `TOO_CORPORATE` | O sistema encontrou dor comercial, mas a evidência ainda puxava para empresa/gestão estruturada. |
| #50 | `TOO_CORPORATE` | A tentativa automática manteve foco em agenda/captação, com pouca evidência e ainda corporativa. |
| #51 | `OUTDATED_SOURCES` | O risco corporativo caiu, mas a qualidade/recência das fontes ainda não sustentou aprovação. |
| #52 | `GENERIC` | Fontes e sinais melhoraram, mas a rotina continuou sem tarefas concretas da executora. |

Até a análise, **não havia ciclo #53 persistido** para esse candidato, indicando provável parada pelo limite de tentativas automáticas.

## 2. Diagnóstico de negócio

O nicho encontrado tem potencial real de venda:

> Manicure autônoma que atende em domicílio e mantém carteira de clientes quinzenais recorrentes por WhatsApp, indicação e Instagram, lidando com faltas, remarcações e cobrança.

Esse nicho contém os elementos centrais de venda:

- **Dor:** agenda vazia, faltas, remarcações, cliente que some, insegurança para cobrar e instabilidade de renda.
- **Resultado desejado:** agenda previsível, clientes fixas quinzenais, menos buracos na agenda e renda mais estável.
- **Mecanismo plausível futuro:** rotina simples de confirmação, cobrança de sinal, reativação e retorno programado pelo WhatsApp.
- **Prova possível:** relatos públicos, comentários, vídeos, dúvidas e fontes brasileiras sobre atendimento domiciliar, preço, cobrança, retorno e rotina.
- **Oferta futura possível:** método para transformar atendimentos avulsos em carteira recorrente quinzenal.

O problema, portanto, **não é o nicho**. O problema é que o pipeline reprova como `GENERIC` quando existe dor vendável, mas falta evidência concreta da rotina manual executada.

## 3. Causa-raiz

A causa-raiz observada nos ciclos é a mistura de duas validações diferentes dentro do mesmo gate:

1. **Validação de potencial comercial** — dor, recorrência, aquisição, cobrança, canal e resultado desejado.
2. **Validação de rotina executora** — tarefas manuais, materiais, deslocamento, tempo, retrabalho, biossegurança e problemas práticos durante o atendimento.

Nos ciclos analisados, a validação comercial evoluiu bem, mas a validação da rotina executora ficou fraca. O #52 registrou:

- `tarefasConcretasDistintas=0`;
- `rotinaApenasGestaoAgendaAtendimentoOrganizacao=true`;
- `rotinaRevelaTarefasReaisExecutor=false`;
- `dorPratica=0`;
- `mixMinimoMeiAutonomo=false`.

Isso mostra que o ciclo entendeu agenda, captação e cobrança, mas não provou suficientemente o trabalho real da manicure: preparar materiais, deslocar, higienizar, fazer pé e mão, lidar com retrabalho, reclamações, tempo e reposição de insumos.

## 4. Objetivo da melhoria

Criar uma melhoria no pipeline NichoCNAE para que, quando houver dor vendável e fit MEI/autônomo suficientes, mas faltar evidência da rotina real do executor, o sistema **não reprove imediatamente como genérico**.

Em vez disso, o pipeline deve abrir uma pesquisa complementar focada exclusivamente em tarefas reais do executor.

## 5. Nova decisão de qualidade proposta

Adicionar o status operacional:

```text
NEEDS_EXECUTOR_ROUTINE_EVIDENCE
```

Rótulo de negócio sugerido:

```text
Precisa evidência da execução real
```

Esse status deve ser usado quando:

- existe dor vendável suficiente;
- existe fit MEI/autônomo suficiente;
- existem sinais de aquisição/canal/recorrência suficientes;
- existem fontes recentes e brasileiras suficientes;
- mas `rotinaRevelaTarefasReaisExecutor=false` ou `tarefasConcretasDistintas` está abaixo do mínimo.

## 6. Novo próximo movimento automático

Adicionar o próximo movimento:

```text
BUSCAR_TAREFAS_REAIS_EXECUTOR
```

Descrição:

```text
Pesquisar relatos e tarefas concretas do executor em fontes públicas brasileiras.
```

Esse próximo movimento já apareceu nas notas do #52, mas deve virar fluxo estruturado, com comportamento específico no seed, na busca e no gate.

## 7. Regra de decisão do gate

### 7.1. Antes

Quando a rotina não revelava tarefas reais do executor, o ciclo podia cair em `GENERIC`.

### 7.2. Depois

Quando o nicho tiver boa dor vendável, bom fit MEI/autônomo e boa aquisição/canais, mas faltar rotina executora, o gate deve retornar:

```text
NEEDS_EXECUTOR_ROUTINE_EVIDENCE
```

Somente retornar `GENERIC` quando o próprio subnicho estiver amplo, repetitivo, sem especificidade ou sem evidência mínima geral.

## 8. Pesquisa complementar de rotina executora

Quando o próximo movimento for `BUSCAR_TAREFAS_REAIS_EXECUTOR`, a etapa `oprmNicheResearchSeedBuilder` deve gerar queries com prioridade alta para execução manual.

### 8.1. Priorizar

- rotina real do atendimento em domicílio;
- passo a passo do serviço;
- materiais e maleta;
- deslocamento;
- tempo de atendimento;
- esterilização e higiene;
- cutilagem, esmaltação, pé e mão;
- unha quebrada, esmalte descascado e retrabalho;
- reclamações de clientes;
- reposição de insumos;
- dificuldades físicas e logísticas;
- relatos, vídeos, comentários e dúvidas de profissionais brasileiros.

### 8.2. Reduzir temporariamente

Durante essa pesquisa complementar, reduzir prioridade para:

- agenda;
- WhatsApp;
- Instagram;
- indicação;
- fidelização;
- cobrança;
- pacote;
- marketing;
- software/app/sistema;
- curso;
- franquia;
- salário;
- CBO como fonte principal.

Esses itens podem aparecer apenas como apoio, não como eixo dominante.

## 9. Exemplos de queries recomendadas

Para o caso manicure autônoma domiciliar, gerar queries como:

- `rotina real manicure atendimento em domicílio passo a passo Brasil`;
- `manicure autônoma prepara maleta materiais antes de atendimento`;
- `manicure domicílio esterilização alicate higiene relato profissional`;
- `tempo atendimento pé e mão cutilagem esmaltação domicílio`;
- `manicure a domicílio deslocamento material dificuldade relato`;
- `cliente reclama esmalte descascou manicure retrabalho relato`;
- `manicure autônoma unha quebrada cliente reclama conserto`;
- `dia a dia manicure autônoma atendimento em casa da cliente`;
- `vlog manicure autônoma domicílio rotina clientes Brasil`;
- `dificuldades manicure domicílio materiais atraso higiene retrabalho`.

## 10. Critérios mínimos para passar pela rotina executora

A pesquisa complementar só deve liberar o ciclo para nova síntese quando encontrar, no mínimo:

- 5 tarefas concretas distintas da executora;
- 3 dores práticas ligadas à execução do serviço;
- 2 fontes com relato, vídeo, comentário ou pergunta real de profissional/cliente;
- 1 evidência de retrabalho ou falha no atendimento;
- 1 evidência de material, tempo, deslocamento ou biossegurança;
- 2 fontes brasileiras recentes ou com forte relevância local.

## 11. Ajustes técnicos propostos

### 11.1. Backend — status e contrato

- Adicionar `NEEDS_EXECUTOR_ROUTINE_EVIDENCE` aos status aceitos pelo gate.
- Adicionar esse status ao endpoint de conclusão/falha da etapa de qualidade.
- Incluir no Swagger OPRM NichoCNAE.
- Garantir que o status seja considerado recuperável para reprocessamento automático, respeitando limite de tentativas.

### 11.2. Coletor OPRM — quality gate

- Ajustar `RoutineQualityGateEngine` para diferenciar:
  - nicho realmente genérico;
  - nicho comercialmente promissor com lacuna de rotina executora.
- Retornar `NEEDS_EXECUTOR_ROUTINE_EVIDENCE` quando `routineRevealsExecutorTasks=false` e os demais sinais comerciais forem fortes.
- Persistir `proximoMovimentoCodigo=BUSCAR_TAREFAS_REAIS_EXECUTOR`.

### 11.3. Seed builder

- Ao receber `previousNextMoveCode=BUSCAR_TAREFAS_REAIS_EXECUTOR`, gerar famílias de queries focadas em execução manual.
- Bloquear dependência dominante de agenda/captação/cobrança nessa rodada.
- Incluir no prompt a regra explícita de buscar tarefas físicas, materiais, tempo, retrabalho, deslocamento e biossegurança.

### 11.4. Source searcher/fetcher

- Dar prioridade a fontes com indícios de relato real:
  - vídeo;
  - comentários;
  - fóruns;
  - posts sociais;
  - perguntas frequentes;
  - blogs pessoais/profissionais;
  - páginas brasileiras com rotina prática.
- Reduzir peso de fontes institucionais, CBO, salário, franquia, software e conteúdo genérico de gestão.

### 11.5. Signal extractor

- Reforçar extração de sinais do tipo:
  - `ROUTINE_TASK`;
  - `OPERATIONAL_PAIN`;
  - `TIME_PRESSURE`;
  - `REWORK_OR_COMPLAINT` se criado;
  - `MATERIAL_OR_SUPPLY_PAIN` se criado;
  - `HYGIENE_OR_SAFETY_ROUTINE` se criado;
  - `AUTONOMOUS_WORK_MODE`.

### 11.6. Frontend

- Exibir o novo status como “Precisa evidência da execução real”.
- Mostrar mensagem de negócio:
  > O nicho tem sinais de venda, mas falta comprovar a rotina manual executada pelo profissional.
- Exibir o próximo movimento recomendado:
  > Pesquisar tarefas reais, materiais, tempo, deslocamento, retrabalho e problemas práticos do atendimento.

## 12. Testes recomendados

### 12.1. Testes do gate

Criar testes para os cenários:

1. dor vendável forte + aquisição forte + fontes boas + falta rotina executora ⇒ `NEEDS_EXECUTOR_ROUTINE_EVIDENCE`;
2. rotina fraca + dor fraca + subnicho amplo ⇒ `GENERIC`;
3. rotina forte + dor forte + fontes boas ⇒ aprovação;
4. fonte antiga dominante ⇒ `OUTDATED_SOURCES`;
5. risco corporativo alto ⇒ `TOO_CORPORATE`.

### 12.2. Testes do seed builder

Validar que `BUSCAR_TAREFAS_REAIS_EXECUTOR` gera queries sobre:

- materiais;
- deslocamento;
- tempo;
- execução manual;
- higiene;
- retrabalho;
- reclamações;
- relatos reais.

E reduz a dominância de queries sobre:

- WhatsApp;
- Instagram;
- agenda;
- fidelização;
- cobrança;
- marketing.

### 12.3. Testes de arquitetura/contrato

- Atualizar Swagger.
- Validar status aceitos pelo backend.
- Garantir que a UI renderiza o novo status.
- Garantir que mensagens de ArchUnit, se alteradas, mantenham prefixo `[ARQUITETURA] `.

## 13. Rollout sugerido

1. Implementar novo status e novo próximo movimento no gate.
2. Ajustar prompt do seed builder para `BUSCAR_TAREFAS_REAIS_EXECUTOR`.
3. Ajustar busca/coleta para priorizar relatos de execução real.
4. Atualizar frontend e Swagger.
5. Rodar testes unitários do OPRM/coletor e backend.
6. Reprocessar o CNAE `9602501` manualmente uma vez.
7. Validar se o novo ciclo encontra tarefas concretas distintas e evita nova reprovação genérica.

## 14. Critério de sucesso

A melhoria será considerada efetiva quando um novo ciclo para o mesmo CNAE/subnicho conseguir produzir:

- pelo menos 5 tarefas concretas distintas;
- pelo menos 3 dores práticas da execução;
- evidência auditável brasileira;
- fontes recentes suficientes;
- rotina real do executor marcada como verdadeira;
- status aprovado para avançar à materialização ou, no mínimo, reprovação mais específica e acionável do que `GENERIC`.

## 15. Resultado esperado para vendas

Com essa melhoria, o Marketing Hub deve parar de tratar como genérico um nicho que já tem dor comercial validada, mas ainda carece de prova operacional.

O resultado esperado é chegar a um nicho mais forte, por exemplo:

> Manicure autônoma domiciliar que perde renda com faltas, remarcações e retrabalho, e precisa transformar atendimentos avulsos em carteira quinzenal previsível usando WhatsApp, sinal e rotina simples de retorno.

Esse nicho tem potencial para sustentar produtos digitais simples, práticos e vendáveis, sem pular prematuramente para oferta antes de entender a realidade operacional.
