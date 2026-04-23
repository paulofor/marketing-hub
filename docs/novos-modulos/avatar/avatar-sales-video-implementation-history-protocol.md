# Avatar Sales Video — Protocolo Canônico de Histórico de Implantação

## 1. Finalidade

Este documento define o protocolo oficial para o **histórico de implantação** do módulo **Avatar Sales Video**.

Ele existe para garantir que toda evolução relevante do módulo deixe um registro:

- cumulativo;
- factual;
- cronológico;
- útil para continuidade;
- útil para auditoria;
- útil para evitar retrabalho e rediscussão de decisões já tomadas.

Este documento é voltado principalmente para uso do **Codex** e de qualquer outro agente ou operador que continue o desenvolvimento do módulo ao longo do tempo.

---

## 2. Relação com outros documentos do módulo

Este protocolo deve ser usado em conjunto com:

- `docs/canonical/system-governance-canon.v2.md`
- `docs/novos-modulos/avatar/avatar-sales-video-canonical-rules.md`
- `docs/novos-modulos/avatar/avatar-sales-video-canonical-artifacts-initial.md`
- `docs/novos-modulos/avatar/avatar-sales-video-restart-plan.md`

## Regra importante

Este documento **não substitui**:
- plano de implementação;
- documento canônico de regras;
- documento canônico de artefatos;
- ADRs;
- documentação de API;
- changelog técnico de release.

Ele define apenas **como registrar o histórico operacional e incremental do trabalho realizado**.

---

## 3. Princípios do histórico

## 3.1 O histórico é cumulativo

As entradas não devem apagar o que foi registrado antes.  
Elas devem se somar ao histórico existente.

## 3.2 O histórico é factual

O histórico deve registrar:

- o que foi feito;
- o que não foi feito;
- o que ficou pendente;
- o que mudou;
- o que foi decidido;
- o que continua em aberto.

O histórico não deve:

- exagerar o grau de prontidão;
- usar linguagem vaga para esconder pendências;
- descrever hipótese como conclusão;
- descrever intenção como implementação concluída.

## 3.3 O histórico é cronológico

Cada entrada deve indicar claramente:

- data;
- etapa/sprint;
- status;
- continuidade.

## 3.4 O histórico deve explicar o “por quê” quando necessário

Como em boas práticas de decision records, o valor do histórico não está só em registrar **o que** mudou, mas também, quando relevante, **por que** a mudança foi feita e quais foram suas consequências. ADRs são úteis exatamente por preservar o contexto e a justificativa das decisões arquiteturais, não apenas o resultado final. citeturn464885search0turn464885search2

## 3.5 O histórico deve viver junto do código

Registros de decisão e de evolução técnica são mais úteis quando ficam no repositório e em formato leve/versionável, como markdown, para facilitar leitura, diff e continuidade. citeturn464885search0turn464885search5

---

## 4. Quando uma nova entrada deve ser criada

Uma nova entrada deve ser criada sempre que houver pelo menos um dos eventos abaixo:

- conclusão de sprint;
- conclusão de etapa relevante do plano;
- mudança relevante em contrato;
- mudança relevante em arquitetura;
- mudança relevante em provider;
- mudança relevante em política de retry, timeout, heartbeat ou expiração;
- mudança relevante em observabilidade;
- mudança relevante em compliance/governança;
- validação E2E relevante;
- rollout controlado;
- mudança importante de frontend administrativo;
- correção estrutural que altere o comportamento do módulo.

## Regra prática

Se a mudança for grande o suficiente para exigir explicação no próximo handoff, então ela merece entrada no histórico.

---

## 5. O que cada entrada deve conter

Cada entrada do histórico deve conter, no mínimo, os campos abaixo.

## 5.1 Data
Data da atualização.

## 5.2 Etapa
Nome da sprint, fase ou marco.

Exemplos:
- `Sprint V1`
- `Sprint V3`
- `Hotfix de retry técnico`
- `Validação E2E em staging`

## 5.3 Status
Situação resumida da etapa.

Exemplos:
- `concluída`
- `parcialmente concluída`
- `concluída com pendências`
- `em progresso`
- `bloqueada`

## 5.4 Resumo executivo
Resumo curto do que a entrada representa.

## 5.5 O que foi implementado
Lista factual das entregas realizadas.

## 5.6 O que foi alterado
Lista de arquivos, módulos, endpoints, contratos ou componentes afetados.

## 5.7 Contratos/artefatos afetados
Quais contratos, estados, DTOs, endpoints, métricas, eventos ou artefatos foram impactados.

## 5.8 Testes e validações executados
Quais testes foram rodados, o que foi validado e em qual ambiente.

## 5.9 Limitações e pendências
O que ainda não ficou pronto, o que continua aberto e quais restrições permanecem.

## 5.10 Próximo passo sugerido
Indicação clara do que o próximo ciclo deve atacar.

---

## 6. Estrutura canônica de cada entrada

Toda nova entrada deve seguir este formato.

```md
## YYYY-MM-DD — <Etapa>

**Status:** <status>

### Resumo
- 

### O que foi implementado
- 

### O que foi alterado
- Arquivos:
  - 
- Módulos:
  - 
- Endpoints/contratos:
  - 

### Contratos e artefatos afetados
- 
- 
- 

### Testes e validações executados
- 
- 
- 

### Limitações e pendências
- 
- 
- 

### Próximo passo sugerido
- 
```

---

## 7. Bloco obrigatório de continuidade

Além da entrada principal, toda atualização relevante deve terminar com um bloco explícito de continuidade para o próximo ciclo.

```md
### Handoff para a próxima etapa
- Prioridade imediata:
- O que não deve ser refeito:
- Riscos abertos:
- Dependências externas:
- Onde continuar:
```

---

## 8. Regras de escrita para o Codex

## 8.1 Seja objetivo
Escreva de forma curta, clara e factual.

## 8.2 Não esconda pendências
Se algo ficou incompleto, registre como incompleto.

## 8.3 Não declare “pronto” cedo demais
Não registrar como concluído algo que ainda depende de:
- provider real não validado;
- observabilidade não implementada;
- compliance não tratado;
- E2E ainda não validado;
- rollout ainda não iniciado.

## 8.4 Não transforme o histórico em narrativa vaga
Evitar frases como:
- “foram feitos vários ajustes”
- “o módulo foi bastante melhorado”
- “foi refinado”
sem detalhar o que de fato mudou.

## 8.5 Registrar contexto quando a mudança for arquitetural
Se a etapa mudar uma decisão estrutural, explicar:
- o problema;
- a decisão;
- o impacto.

Isso segue a lógica de ADRs curtos e rastreáveis, que preservam o contexto, a decisão e suas consequências. citeturn464885search0turn464885search2

---

## 9. Anti-padrões proibidos no histórico

### Anti-padrão 1 — Histórico promocional
Usar o histórico como propaganda do que foi feito.

### Anti-padrão 2 — Histórico sem pendências
Registrar apenas sucessos e omitir o que ficou faltando.

### Anti-padrão 3 — Histórico sem continuidade
Não indicar o que o próximo ciclo deve fazer.

### Anti-padrão 4 — Histórico sem impacto contratual
Fazer mudanças em endpoints/DTOs/jobs e não registrar.

### Anti-padrão 5 — Histórico genérico demais
Registrar textos que não permitem ao próximo agente continuar.

### Anti-padrão 6 — Reescrever entradas antigas
Entradas antigas não devem ser reescritas para “parecerem melhores”.  
Se uma decisão mudou, registrar nova entrada explicando a mudança.

Isso é coerente com a lógica dos ADRs, que devem preservar a trilha de decisão ao longo do tempo e, quando necessário, serem supersedidos, não reescritos como se a decisão anterior nunca tivesse existido. citeturn464885search0turn464885search2

---

## 10. Organização recomendada do arquivo

## Opção recomendada

Um único arquivo cumulativo, por exemplo:

- `docs/novos-modulos/avatar/avatar-sales-video-implementation-history.md`

## Estrutura recomendada do arquivo

```md
# Avatar Sales Video — Histórico de Implantação

## Como ler este histórico
<breve explicação>

## Índice rápido
- 2026-04-16 — Sprint V1
- 2026-04-18 — Sprint V2
- 2026-04-22 — Sprint V3

## Entradas
...
```

## Regra

As entradas mais recentes podem ficar primeiro ou por ordem crescente, desde que o padrão seja consistente.  
Se houver índice rápido, ele deve ser atualizado.

---

## 11. Template inicial do arquivo de histórico

```md
# Avatar Sales Video — Histórico de Implantação

## Como ler este histórico

Este arquivo registra, de forma cumulativa, o que foi implementado no módulo Avatar Sales Video ao longo das sprints e etapas relevantes.

Cada entrada deve registrar:
- o que foi feito;
- o que mudou;
- o que ficou pendente;
- o que o próximo ciclo deve continuar.

---

## Índice rápido
- 

---

## Entradas

## YYYY-MM-DD — <Etapa>

**Status:** <status>

### Resumo
- 

### O que foi implementado
- 

### O que foi alterado
- Arquivos:
  - 
- Módulos:
  - 
- Endpoints/contratos:
  - 

### Contratos e artefatos afetados
- 

### Testes e validações executados
- 

### Limitações e pendências
- 

### Próximo passo sugerido
- 

### Handoff para a próxima etapa
- Prioridade imediata:
- O que não deve ser refeito:
- Riscos abertos:
- Dependências externas:
- Onde continuar:
```

---

## 12. Critério de conformidade com este protocolo

O histórico está em conformidade com este documento quando:

- toda etapa relevante gera entrada própria;
- a entrada é factual e cumulativa;
- há registro explícito de pendências;
- há registro explícito de continuidade;
- há rastreabilidade mínima de contratos/artefatos impactados;
- mudanças arquiteturais relevantes preservam contexto e consequência.

---

## 13. Próxima etapa recomendada

Depois da criação deste protocolo, o próximo passo recomendado é:

1. criar o arquivo real de histórico do módulo;
2. registrar a linha de base atual do estado do Avatar Sales Video;
3. passar a exigir que toda sprint futura atualize esse arquivo;
4. usar esse histórico como ponto de retomada oficial do Codex.

---
