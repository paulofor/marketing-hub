# OPRM — Especificação de Telas, Navegação e Implementação por Sprints

## 1. Objetivo

Este documento define as telas necessárias para operar e visualizar o **Occupation Persona Routine Mapper (OPRM)** dentro do Marketing Hub.

O objetivo é transformar o OPRM de um módulo técnico orientado a artefatos em um **produto interno utilizável**, permitindo:

- selecionar ocupações/personas
- rodar o processamento do OPRM
- visualizar rotina, dores, resultados e mecanismos
- transformar sinais do OPRM em ofertas dentro do Marketing Hub
- revisar evidências e lineage
- acompanhar feedback e aprendizado
- monitorar a operação técnica do worker

Além da especificação funcional das telas, este documento também organiza a implementação da UI do OPRM em **sprints**.

---

## 2. Decisão de navegação

O OPRM deve entrar no Marketing Hub como um **novo item de menu principal**.

### 2.1 Nome do item de menu
Nome sugerido:
- `OPRM`

Nome expandido na página:
- `Occupation Persona Routine Mapper`

Nome alternativo, se quiser algo mais orientado a negócio:
- `Rotina da Persona`

### 2.2 Posição no menu
O OPRM deve aparecer como item de primeiro nível no menu lateral do Marketing Hub.

Sugestão de ordem:
1. Dashboard
2. Hipóteses
3. Ofertas
4. Landing Pages
5. Experimentos
6. **OPRM**
7. MOIS
8. MDS
9. Operações

### 2.3 Justificativa
O OPRM não deve ficar escondido como submenu técnico de outro módulo porque:
- ele tem workflow próprio
- ele alimenta diretamente criação de oferta
- ele tem tela de operação própria
- ele combina visão de negócio e visão operacional

---

## 3. Estrutura de navegação interna do OPRM

Ao entrar no menu `OPRM`, o usuário verá uma navegação interna própria.

Seções internas sugeridas:
- Ocupações
- Rotina
- Oferta
- Evidências
- Feedback
- Operações

Regra:
- a navegação principal do Marketing Hub leva para o módulo
- a navegação interna do OPRM leva para as telas do módulo

---

## 4. Princípios de UX

### 4.1 O OPRM deve ser operável por fluxo de negócio
A UI não deve obrigar o usuário a pensar em nomes técnicos de artefatos.

O fluxo deve ser:
1. escolher ocupação
2. entender rotina
3. identificar dor
4. selecionar resultado
5. selecionar mecanismo
6. montar oferta
7. revisar evidência
8. acompanhar feedback

### 4.2 Separação entre visão de negócio e visão técnica
O módulo deve oferecer:
- telas orientadas a decisão comercial
- telas orientadas a operação técnica

### 4.3 O OPRM não deve começar por CRUD genérico
A primeira versão da UI não deve ser uma lista crua de artefatos.
Ela deve ser uma experiência guiada.

---

## 5. Telas do OPRM

O OPRM deve ter 6 telas principais.

---

## 5.1 Tela 1 — Workspace de Ocupações

### Objetivo
Ser a porta de entrada do módulo.

### Nome sugerido da tela
- `Ocupações`

### O que mostra
Lista de ocupações/personas analisadas pelo OPRM com:
- nome da ocupação
- nicho
- status da última execução
- confiança geral
- quantidade de dores detectadas
- quantidade de oportunidades detectadas
- última atualização
- origem do último processamento
- ação de reprocessar

### Componentes
- busca por ocupação
- filtros por status
- filtros por confiança
- tabela ou grid de ocupações
- botão `Rodar OPRM`
- botão `Ver rotina`
- botão `Ir para oferta`

### Ações principais
- selecionar ocupação
- disparar novo processamento
- abrir rotina
- abrir builder de oferta

### Origem de dados
- jobs do OPRM
- `occupationSeed`
- `occupationPersonaRoutineCard`
- status operacional persistido no backend

---

## 5.2 Tela 2 — Rotina da Persona

### Objetivo
Mostrar o retrato operacional da persona ocupacional.

### Nome sugerido da tela
- `Rotina`

### O que mostra
- resumo da rotina
- tarefas principais
- restrições principais
- ferramentas principais
- workarounds observados
- dores principais
- resultados desejados
- oportunidades de mecanismo

### Componentes
- cabeçalho com nome da ocupação
- bloco `Resumo da rotina`
- bloco `Top tarefas`
- bloco `Top restrições`
- bloco `Top dores`
- bloco `Resultados desejados`
- bloco `Oportunidades de mecanismo`
- seletor de modo:
  - resumo executivo
  - detalhe por evidência

### Ações principais
- marcar dor principal
- marcar resultado principal
- marcar mecanismo candidato
- enviar seleção para builder de oferta

### Origem de dados
- `occupationPersonaRoutineCard`
- `routineTaskPattern`
- `routineConstraintSignal`
- `routinePainSignal`
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`

---

## 5.3 Tela 3 — Builder de Oferta

### Objetivo
Transformar a saída do OPRM em oferta utilizável dentro do Marketing Hub.

### Nome sugerido da tela
- `Oferta`

### O que mostra
Um workspace estruturado para montar:

- Persona
- Dor principal
- Resultado desejado
- Mecanismo sugerido
- Prova inicial
- Oferta proposta

### Componentes
- card `Persona`
- card `Dor`
- card `Resultado`
- card `Mecanismo`
- card `Prova`
- editor de texto da `Oferta`
- seção de observações
- preview estruturado no formato:
  - dor
  - resultado
  - oferta
  - mecanismo
  - prova

### Ações principais
- escolher uma dor
- escolher um resultado
- escolher um mecanismo
- escrever/refinar a oferta
- exportar para pipeline de hipótese
- exportar para landing
- exportar para experimento

### Origem de dados
- `routinePainSignal`
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`
- `dorResultadoOfertaMecanismoProvaInput`

### Regra importante
A tela não deve gerar a oferta “do nada”.
Ela deve trabalhar como:
- seleção
- curadoria
- composição comercial

---

## 5.4 Tela 4 — Evidências e Fontes

### Objetivo
Permitir auditoria, confiança e revisão do que sustentou a análise do OPRM.

### Nome sugerido da tela
- `Evidências`

### O que mostra
- fontes estruturadas usadas
- fontes web capturadas
- trechos relevantes
- lineage do artefato
- confiança por sinal
- relação fonte → inferência

### Componentes
- timeline de geração
- tabela de fontes
- painel de excerpts/evidências
- filtros por tipo de fonte
- filtros por artefato
- painel de lineage

### Ações principais
- abrir fonte
- filtrar evidências
- comparar evidências por ocupação
- rastrear origem de uma dor ou mecanismo

### Origem de dados
- `occupationProfileSnapshot`
- `occupationWebSourceSnapshot`
- `occupationContextSignal`
- `occupationTaskEvidence`
- lineage persistido dos artefatos

---

## 5.5 Tela 5 — Feedback e Aprendizado

### Objetivo
Mostrar como o OPRM está aprendendo a partir da performance downstream.

### Nome sugerido da tela
- `Feedback`

### O que mostra
- snapshots de feedback loop
- histórico por ocupação
- hipóteses relacionadas
- aderência entre rotina e performance
- reponderação de sinais
- evolução da confiança

### Componentes
- gráfico de confiança ao longo do tempo
- lista de snapshots
- tabela de histórico por ocupação
- comparativo antes/depois
- painel de hipóteses relacionadas

### Ações principais
- abrir snapshot
- comparar execuções
- revisar recalibração
- abrir hipótese relacionada

### Origem de dados
- `occupationFeedbackLoopSnapshot`
- histórico persistido por ocupação
- `HypothesisPerformanceSnapshot`
- `HypothesisRoutineFit`

---

## 5.6 Tela 6 — Operações do OPRM

### Objetivo
Permitir monitoramento técnico do módulo.

### Nome sugerido da tela
- `Operações`

### O que mostra
- jobs em fila
- jobs em execução
- jobs falhos
- artefatos publicados
- heartbeat do worker
- health/readiness
- métricas principais
- últimos erros
- correlação por `correlationId`

### Componentes
- cards de status
- tabela de jobs
- painel de métricas
- painel de heartbeat
- tabela de falhas recentes
- busca por correlation id
- painel de artefatos publicados

### Métricas mínimas
- jobs claimed
- jobs succeeded
- jobs failed
- artifacts published
- publish failures
- duração do loop
- duração por fase

### Ações principais
- filtrar jobs
- abrir falha
- abrir artefato relacionado
- reexecutar job
- copiar `correlationId`

### Origem de dados
- endpoints operacionais persistidos no backend
- endpoints de observabilidade do worker
- heartbeat
- métricas agregadas
- status de publicação

---

## 6. Ordem recomendada de implementação

### 6.1 MVP das telas
Implementar primeiro:
1. Workspace de Ocupações
2. Rotina da Persona
3. Builder de Oferta
4. Operações do OPRM

Motivo:
- esse conjunto já permite operar o módulo
- esse conjunto já permite transformar rotina em oferta
- esse conjunto já permite monitorar se o módulo está funcionando

### 6.2 Segunda leva
Implementar depois:
5. Evidências e Fontes
6. Feedback e Aprendizado

Motivo:
- aumentam confiança e profundidade analítica
- não são o mínimo operacional para começar a usar o OPRM

---

## 7. Fluxo de uso esperado

Fluxo principal do usuário:

1. abrir `OPRM` no menu principal
2. entrar em `Ocupações`
3. escolher a ocupação
4. abrir `Rotina`
5. selecionar dor, resultado e mecanismo
6. abrir `Oferta`
7. montar a proposta
8. opcionalmente revisar `Evidências`
9. acompanhar `Feedback`
10. usar `Operações` para monitorar o worker

---

## 8. Integração com o restante do Marketing Hub

A UI do OPRM deve conversar com outros módulos.

### 8.1 Saídas da tela de Oferta
A tela `Oferta` deve permitir:
- enviar para hipótese
- enviar para landing
- enviar para experimento
- salvar draft interno

### 8.2 Integração com framework
O Builder de Oferta deve usar explicitamente:
- dor
- resultado
- oferta
- mecanismo
- prova

### 8.3 Integração com operações
A tela `Operações` deve refletir:
- jobs
- artefatos
- heartbeat
- status de publicação no backend

---

## 9. Regras de design e informação

### 9.1 Navegação principal
O OPRM entra como **novo item de menu principal** do Marketing Hub.

### 9.2 Navegação interna
A navegação interna deve ser curta e previsível:
- Ocupações
- Rotina
- Oferta
- Evidências
- Feedback
- Operações

### 9.3 Hierarquia visual
A UI deve priorizar:
1. ocupação
2. dor
3. resultado
4. mecanismo
5. oferta
6. prova

### 9.4 O que evitar
- lista crua de artefatos como tela inicial
- UI centrada em nomes técnicos
- excesso de telas técnicas antes das telas de negócio
- mistura de operação do worker com builder comercial na mesma tela

---

## 10. API/UI mapping inicial

### Tela Ocupações
Consumir:
- jobs resumidos
- última execução por ocupação
- rotina consolidada por ocupação

### Tela Rotina
Consumir:
- `occupationPersonaRoutineCard`
- sinais derivados

### Tela Oferta
Consumir:
- `dorResultadoOfertaMecanismoProvaInput`
- sinais selecionados da rotina

### Tela Evidências
Consumir:
- snapshots
- lineage
- excerpts

### Tela Feedback
Consumir:
- snapshots de feedback
- histórico por ocupação

### Tela Operações
Consumir:
- jobs
- status
- métricas
- heartbeat
- falhas

---

## 11. Implementação por sprints

Este plano assume **4 sprints de UI**, separadas da implementação do backend/worker já concluída.

### Sprint UI-1 — navegação principal + Workspace de Ocupações
#### Objetivo
Colocar o OPRM visível no Marketing Hub como novo item de menu e entregar a porta de entrada do módulo.

#### Entregas
- novo item de menu principal `OPRM`
- rota principal do módulo
- layout base do OPRM
- tela `Ocupações`
- filtros básicos
- tabela/grid de ocupações
- ação de abrir rotina
- ação de reprocessar ocupação

#### Critério de pronto
- usuário consegue acessar o OPRM pelo menu lateral
- usuário consegue listar ocupações reais vindas do backend
- usuário consegue navegar para a tela de rotina

---

### Sprint UI-2 — Rotina da Persona + Builder de Oferta
#### Objetivo
Entregar o núcleo comercial do OPRM.

#### Entregas
- tela `Rotina`
- componentes de resumo executivo
- componentes de dores, resultados e mecanismos
- seleção de dor/resultado/mecanismo
- tela `Oferta`
- preview estruturado no modelo dor → resultado → oferta → mecanismo → prova
- ação de enviar para hipótese / landing / experimento

#### Critério de pronto
- usuário consegue sair de uma ocupação e montar uma oferta inicial
- a oferta é baseada em dados reais do OPRM, não em mock

---

### Sprint UI-3 — Evidências e Feedback
#### Objetivo
Aumentar confiança, auditabilidade e aprendizado visível do módulo.

#### Entregas
- tela `Evidências`
- timeline de geração
- tabela de fontes
- painel de excerpts
- tela `Feedback`
- histórico por ocupação
- snapshots de recalibração
- comparativo antes/depois

#### Critério de pronto
- usuário consegue entender de onde veio uma conclusão do OPRM
- usuário consegue visualizar como o módulo aprendeu ao longo do tempo

---

### Sprint UI-4 — Operações e hardening da UI
#### Objetivo
Fechar a camada operacional e estabilizar a experiência.

#### Entregas
- tela `Operações`
- tabela de jobs
- painel de heartbeat
- métricas principais
- falhas recentes
- busca por `correlationId`
- estados de loading, erro e vazio em todas as telas
- ajustes de navegação interna
- refinamento visual final

#### Critério de pronto
- usuário técnico consegue monitorar o OPRM sem sair do Marketing Hub
- todas as telas principais possuem tratamento de erro e navegação consistente

---

## 12. Checklist por sprint

### UI-1
- [ ] item de menu `OPRM` criado
- [ ] rota principal criada
- [ ] tela `Ocupações` implementada
- [ ] listagem real do backend funcionando
- [ ] ação `Ver rotina` funcionando

### UI-2
- [ ] tela `Rotina` implementada
- [ ] seleção de dor/resultado/mecanismo funcionando
- [ ] tela `Oferta` implementada
- [ ] preview estruturado funcionando
- [ ] ações de exportação configuradas

### UI-3
- [ ] tela `Evidências` implementada
- [ ] lineage visível
- [ ] tela `Feedback` implementada
- [ ] histórico por ocupação visível
- [ ] comparativo antes/depois funcionando

### UI-4
- [ ] tela `Operações` implementada
- [ ] heartbeat visível
- [ ] métricas visíveis
- [ ] falhas recentes visíveis
- [ ] estados de loading/erro/vazio revisados
- [ ] navegação interna refinada

---

## 13. Critério de pronto das telas

Uma tela do OPRM só deve ser considerada pronta quando:
- cumpre seu objetivo principal
- lê dados reais do backend
- não depende de mock manual no fluxo principal
- respeita a separação entre negócio e operação
- permite seguir para a próxima etapa do fluxo
- está documentada no histórico de implantação

---

## 14. Próximo documento recomendado

Depois desta especificação, o próximo documento ideal é:

- `oprm_ui_routes_and_components.md`

Conteúdo sugerido:
- rotas
- componentes principais
- DTOs de leitura por tela
- estados de loading/erro/vazio
- ações por tela
- integração frontend ↔ backend
