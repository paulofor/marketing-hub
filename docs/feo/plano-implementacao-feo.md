# Plano de Implementação — FEO: Fábrica de Entregáveis de Oferta

## 1. Objetivo do módulo

A **FEO — Fábrica de Entregáveis de Oferta** é um módulo pós-validação do Marketing Hub.

Ela só deve começar a trabalhar quando um experimento comercial tiver sido considerado bem-sucedido e o **backend** registrar uma requisição formal de fabricação.

A FEO não cria a hipótese, não valida a oferta e não redefine a promessa comercial. Ela transforma uma **oferta validada** em entregáveis digitais concretos, versionados, rastreáveis e revisáveis pelo usuário.

Fluxo principal:

```text
Hipótese
  ↓
Experimento
  ↓
Validação comercial
  ↓
Registro da requisição de fabricação no backend
  ↓
FEO consulta o backend em polling
  ↓
FEO executa pipeline de criação dos entregáveis
  ↓
FEO publica status, eventos e artefatos no backend
  ↓
Usuário acompanha, revisa e intervém pela UI
```

## 2. Regras arquiteturais obrigatórias

### 2.1. A FEO nunca acessa banco diretamente

Regra absoluta:

```text
A FEO não pode acessar MySQL, PostgreSQL, Redis, MinIO, S3, storage interno ou qualquer banco/tabela diretamente.
```

Toda leitura e escrita deve acontecer por meio de APIs do backend.

Permitido:

```text
FEO → Backend API → Banco / Storage / Artefatos
```

Proibido:

```text
FEO → Banco
FEO → Tabelas do backend
FEO → Storage interno sem mediação do backend
FEO → Migrações Liquibase do backend
```

### 2.2. Backend é a fonte de verdade operacional

O backend deve ser dono de:

- criação da requisição de fabricação;
- persistência da fila;
- status da requisição;
- lock/claim/lease de processamento;
- histórico de eventos;
- armazenamento dos artefatos publicados;
- permissões do usuário;
- decisões de aprovação/reprovação/intervenção;
- exposição dos dados para a UI.

### 2.3. FEO é worker Spring Boot em polling

A FEO deve ser uma aplicação Java Spring Boot separada, com diretório, build, imagem Docker e container próprios.

Ela deve:

- subir como worker;
- expor healthcheck/actuator;
- consultar o backend em intervalo configurável;
- reservar uma requisição pendente antes de processar;
- executar o pipeline;
- publicar progresso no backend;
- respeitar pausas, reprovações e intervenções do usuário;
- finalizar ou falhar a requisição com relatório.

### 2.4. A FEO não altera a promessa validada

A FEO pode estruturar, detalhar e materializar entregáveis, mas não pode alterar:

- promessa central;
- resultado validado;
- mecanismo central;
- nicho validado;
- oferta validada;
- claims comerciais usados no experimento vencedor.

Se o pipeline detectar inconsistência, deve pedir intervenção do usuário ou marcar a requisição como bloqueada.

## 3. Artefatos iniciais da FEO

Sugestão inicial de artefatos canônicos:

```text
feo.fabricationRequest.v1
feo.fabricationContext.v1
feo.offerDeliverablePlan.v1
feo.deliverableSpec.v1
feo.contentModuleSpec.v1
feo.digitalAssetDraft.v1
feo.digitalAssetReview.v1
feo.digitalAssetFinal.v1
feo.offerDeliveryManifest.v1
feo.fabricationReport.v1
```

### 3.1. Estados principais da requisição

```text
REQUESTED
CLAIMED
CONTEXT_LOADED
PLANNING
WAITING_USER_REVIEW
GENERATING_DRAFTS
WAITING_USER_APPROVAL
ASSEMBLING_PACKAGE
COMPLETED
FAILED
CANCELED
BLOCKED
```

### 3.2. Estados de cada entregável

```text
PLANNED
DRAFTING
DRAFT_READY
NEEDS_REVIEW
APPROVED
REJECTED
REWORK_REQUESTED
FINALIZED
FAILED
```

## 4. Contrato mínimo backend ↔ FEO

### 4.1. Endpoints necessários no backend

Os nomes abaixo são sugestivos. O Codex deve adaptar ao padrão real do backend, sem quebrar convenções existentes.

```text
GET  /api/feo/requests/pending
POST /api/feo/requests/{requestId}/claim
POST /api/feo/requests/{requestId}/heartbeat
GET  /api/feo/requests/{requestId}/context
POST /api/feo/requests/{requestId}/events
POST /api/feo/requests/{requestId}/status
POST /api/feo/requests/{requestId}/artifacts
GET  /api/feo/requests/{requestId}/commands
POST /api/feo/requests/{requestId}/commands/{commandId}/ack
POST /api/feo/requests/{requestId}/release
POST /api/feo/requests/{requestId}/fail
POST /api/feo/requests/{requestId}/complete
```

### 4.2. Comandos vindos da UI/backend para a FEO

```text
PAUSE
RESUME
CANCEL
RETRY_STEP
REGENERATE_DELIVERABLE
APPROVE_PLAN
REJECT_PLAN
APPROVE_DELIVERABLE
REQUEST_REWORK
CHANGE_PRIORITY
ADD_USER_INSTRUCTION
```

A FEO deve consultar comandos durante o pipeline e obedecer comandos pendentes antes de avançar para novas etapas.

## 5. Pipeline lógico da FEO

```text
1. Polling
2. Claim da requisição
3. Carregamento do contexto validado
4. Validação de consistência
5. Planejamento dos entregáveis
6. Revisão/aprovação humana opcional do plano
7. Geração dos rascunhos
8. Revisão/intervenção por entregável
9. Geração final
10. Montagem do pacote de entrega
11. Relatório final
12. Conclusão
```

## 6. Telas necessárias para o usuário

A FEO precisa de UI desde cedo, porque o usuário precisa acompanhar, aprovar, pausar, corrigir e reprocessar etapas.

### 6.1. Menu principal

Novo item no menu:

```text
FEO
```

Subáreas:

```text
Dashboard
Requisições
Pipeline
Entregáveis
Revisões
Operações
```

### 6.2. Tela 1 — Dashboard da FEO

Objetivo: visão geral da operação.

Deve mostrar:

- requisições pendentes;
- requisições em processamento;
- requisições aguardando revisão;
- requisições concluídas;
- requisições com erro;
- tempo médio de fabricação;
- último heartbeat do worker;
- status geral da FEO.

Ações:

- abrir requisição;
- filtrar por status;
- ver falhas recentes;
- acessar operações.

### 6.3. Tela 2 — Lista de Requisições de Fabricação

Objetivo: gerenciar a fila.

Colunas sugeridas:

- ID;
- oferta validada;
- nicho;
- experimento de origem;
- status;
- prioridade;
- etapa atual;
- worker responsável;
- última atualização;
- pendência humana.

Ações:

- abrir detalhes;
- pausar;
- cancelar;
- reprocessar;
- alterar prioridade;
- adicionar instrução humana.

### 6.4. Tela 3 — Detalhe da Requisição

Objetivo: acompanhar uma fabricação específica.

Blocos:

- resumo da oferta validada;
- promessa central;
- resultado prometido;
- mecanismo central;
- experimento vencedor;
- métricas de validação;
- timeline de eventos;
- etapa atual;
- erros e alertas;
- comandos disponíveis.

Ações:

- pausar;
- retomar;
- cancelar;
- solicitar reprocessamento;
- enviar observação/instrução;
- abrir plano de entregáveis;
- abrir entregáveis gerados.

### 6.5. Tela 4 — Plano de Entregáveis

Objetivo: revisar o plano antes da geração pesada.

Deve mostrar:

- lista de entregáveis planejados;
- papel de cada entregável na promessa;
- formato sugerido;
- profundidade;
- ordem de consumo;
- dependências;
- critérios de qualidade;
- relação com dor, resultado, mecanismo, prova e oferta.

Ações:

- aprovar plano;
- rejeitar plano;
- pedir ajuste;
- remover entregável;
- adicionar entregável;
- mudar ordem;
- editar instruções.

### 6.6. Tela 5 — Entregáveis

Objetivo: acompanhar e revisar cada ativo.

Deve mostrar:

- nome do entregável;
- tipo;
- status;
- versão;
- resumo;
- preview;
- origem/lineage;
- pendências;
- data de criação;
- última atualização.

Ações:

- abrir preview;
- aprovar;
- reprovar;
- pedir refação;
- baixar/exportar quando disponível;
- comparar versões;
- ver histórico.

### 6.7. Tela 6 — Revisões e Intervenções

Objetivo: centralizar pontos em que o usuário precisa agir.

Deve mostrar:

- plano aguardando aprovação;
- entregáveis aguardando aprovação;
- erros que precisam de decisão;
- dúvidas geradas pelo pipeline;
- comandos pendentes;
- instruções humanas já enviadas.

Ações:

- aprovar em lote;
- reprovar em lote;
- responder dúvida;
- adicionar instrução global;
- liberar continuação.

### 6.8. Tela 7 — Operações da FEO

Objetivo: visão técnica/operacional.

Deve mostrar:

- workers ativos;
- último heartbeat;
- polling interval;
- requisição atualmente em execução;
- falhas recentes;
- retries;
- tempo por etapa;
- correlationId;
- logs resumidos;
- versão do módulo;
- health status.

Ações:

- forçar novo polling;
- pausar consumo global;
- retomar consumo global;
- ver detalhes de erro;
- reexecutar etapa segura;
- exportar relatório técnico.

## 7. Plano de implementação em sprints

## Sprint 0 — Alinhamento canônico e preparação

Objetivo: preparar o terreno antes de codificar.

### Backend

- Mapear onde ficam os experimentos validados.
- Identificar como o backend registra sucesso de experimento.
- Definir o ponto exato onde será criada a requisição FEO.
- Criar documento de contrato inicial backend ↔ FEO.

### FEO

- Criar diretório planejado do módulo: `feo/`.
- Definir nome técnico da aplicação.
- Definir pacotes base.
- Definir variáveis de ambiente.

### Frontend

- Definir entrada de menu FEO.
- Definir rotas planejadas.
- Criar wireframe simples das telas.

### Documentação

- Criar `docs/feo/feo-implementation-plan.md`.
- Criar `docs/feo/feo-canonical-artifacts.md`.
- Criar `docs/feo/feo-api-contract.md`.

### Critério de aceite

- Nenhuma implementação funcional ainda é necessária.
- O Codex deve entregar documentação clara, nomes finais e pontos de integração.

### Registro da sprint pelo Codex

```text
Status:
O que foi implementado:
Arquivos alterados:
Contratos definidos:
Pendências:
Orientação para próxima sprint:
```

---

## Sprint 1 — Contrato backend e modelo de requisição

Objetivo: implementar no backend a base da fila de fabricação.

### Backend

- Criar entidade/tabela de requisição de fabricação.
- Criar tabela/eventos de histórico da requisição.
- Criar estrutura para comandos/intervenções do usuário.
- Criar endpoints de listagem, detalhe e status.
- Criar endpoint de criação manual de requisição para teste.
- Criar endpoint futuro para criação automática a partir de experimento validado.

### FEO

- Ainda não processa nada.
- Criar DTOs equivalentes aos contratos do backend.

### Frontend

- Criar tela inicial de lista de requisições usando dados reais do backend.
- Criar tela de detalhe básica.

### Critério de aceite

- O backend consegue registrar e listar requisições FEO.
- A UI consegue exibir requisições.
- A FEO ainda não precisa consumir a fila.

---

## Sprint 2 — Worker Spring Boot com polling e claim

Objetivo: criar a aplicação FEO como worker real.

### FEO

- Criar projeto Java Spring Boot em `feo/`.
- Configurar Maven/Gradle conforme padrão do repo.
- Configurar Dockerfile e entrada no docker-compose.
- Configurar `@Scheduled` para polling.
- Implementar cliente HTTP para o backend.
- Buscar requisições pendentes.
- Fazer claim/lease de uma requisição.
- Enviar heartbeat.
- Marcar requisição como `CLAIMED` e depois `CONTEXT_LOADED` quando aplicável.
- Não acessar banco.

### Backend

- Implementar endpoint de claim com proteção contra corrida.
- Implementar lease/lock temporal.
- Implementar endpoint de heartbeat.
- Implementar liberação de requisição quando worker falhar ou expirar lease.

### Frontend

- Exibir worker responsável.
- Exibir heartbeat.
- Exibir etapa atual.

### Critério de aceite

- A FEO roda separada.
- A FEO consulta o backend periodicamente.
- A FEO reserva uma requisição sem tocar no banco diretamente.
- Dois workers não podem processar a mesma requisição ao mesmo tempo.

---

## Sprint 3 — Contexto de fabricação validado

Objetivo: fazer o backend entregar para a FEO todo o contexto necessário, já consolidado.

### Backend

- Criar endpoint de contexto da requisição.
- Consolidar dados da hipótese, experimento vencedor, oferta validada, campanha, landing e métricas.
- Não enviar dados crus desnecessários.
- Enviar um pacote de contexto versionado.

### FEO

- Consumir `fabricationContext`.
- Validar campos obrigatórios.
- Bloquear requisição se faltar promessa, resultado, mecanismo ou oferta.
- Publicar evento de validação do contexto.

### Frontend

- Mostrar na tela de detalhe o contexto validado.
- Destacar promessa, resultado, mecanismo, prova e oferta.
- Mostrar alerta se houver bloqueio.

### Critério de aceite

- A FEO consegue carregar o contexto completo via backend.
- Falta de dados críticos gera status `BLOCKED` com motivo claro.

---

## Sprint 4 — Planejamento dos entregáveis

Objetivo: gerar o primeiro artefato central da FEO: `offerDeliverablePlan`.

### FEO

- Implementar etapa de planejamento dos entregáveis.
- Produzir plano com lista de entregáveis, papéis, formatos e ordem de consumo.
- Relacionar cada entregável com a promessa validada.
- Publicar o plano no backend como artefato.
- Marcar requisição como `WAITING_USER_REVIEW` quando exigir aprovação.

### Backend

- Persistir artefato publicado pela FEO.
- Expor plano para a UI.
- Permitir comandos de aprovação/rejeição/ajuste.

### Frontend

- Criar tela Plano de Entregáveis.
- Permitir aprovar, rejeitar e pedir ajustes.
- Permitir adicionar instrução humana.

### Critério de aceite

- O plano é gerado e exibido na UI.
- O usuário consegue aprovar ou solicitar ajuste.
- A FEO só avança quando o backend indicar liberação.

---

## Sprint 5 — Geração de rascunhos dos entregáveis

Objetivo: gerar versões iniciais dos entregáveis planejados.

### FEO

- Implementar pipeline por entregável.
- Gerar `deliverableSpec` e `digitalAssetDraft`.
- Publicar cada rascunho no backend.
- Atualizar progresso por entregável.
- Respeitar pausa/cancelamento.
- Gerar rascunhos de forma idempotente.

### Backend

- Persistir versões dos rascunhos.
- Expor histórico de versões.
- Expor status por entregável.

### Frontend

- Criar tela Entregáveis.
- Exibir lista, status e preview básico.
- Permitir abrir cada entregável.

### Critério de aceite

- A FEO gera rascunhos para entregáveis planejados.
- A UI mostra progresso e conteúdo produzido.
- Cada rascunho tem versão e lineage.

---

## Sprint 6 — Revisão, intervenção humana e refação

Objetivo: permitir que o usuário intervenha antes da finalização.

### FEO

- Implementar leitura de comandos pendentes.
- Implementar refação de entregável específico.
- Aplicar instruções humanas sem perder lineage.
- Registrar motivo da refação.
- Evitar loop infinito de reprocessamento.

### Backend

- Criar endpoints para comandos de revisão.
- Criar modelo de instruções humanas.
- Registrar decisões do usuário.

### Frontend

- Criar tela Revisões e Intervenções.
- Mostrar pendências humanas.
- Permitir aprovar/reprovar/pedir refação.
- Permitir instrução por entregável e instrução global.

### Critério de aceite

- O usuário consegue parar o pipeline em pontos relevantes.
- A FEO obedece comandos vindos do backend.
- Reprocessamento gera nova versão, não sobrescreve a anterior.

---

## Sprint 7 — Finalização e pacote de entrega

Objetivo: montar o pacote final dos entregáveis aprovados.

### FEO

- Gerar `digitalAssetFinal`.
- Gerar `offerDeliveryManifest`.
- Gerar `fabricationReport`.
- Publicar todos os artefatos finais no backend.
- Marcar requisição como `COMPLETED`.

### Backend

- Persistir pacote final.
- Expor download/export/preview quando aplicável.
- Associar pacote à oferta validada.

### Frontend

- Exibir pacote final.
- Exibir manifesto.
- Exibir relatório de fabricação.
- Permitir baixar/exportar artefatos quando o backend suportar.

### Critério de aceite

- Uma requisição completa gera pacote final rastreável.
- O usuário consegue ver o que foi produzido e por quê.

---

## Sprint 8 — Operações, observabilidade e hardening

Objetivo: tornar o módulo operável com segurança.

### FEO

- Expor actuator health.
- Expor métricas básicas.
- Registrar correlationId em todos os eventos.
- Implementar retry/backoff.
- Implementar timeout por etapa.
- Implementar proteção contra processamento duplicado.
- Implementar graceful shutdown.

### Backend

- Expor dados operacionais da FEO para a UI.
- Exibir falhas, retries, heartbeats e tempos por etapa.
- Implementar limpeza/expiração de leases.

### Frontend

- Criar tela Operações.
- Mostrar workers, falhas, métricas e heartbeats.
- Permitir pausar/retomar consumo global, se o backend suportar.

### Critério de aceite

- O módulo é monitorável.
- Falhas são visíveis.
- Requisições travadas podem ser recuperadas.

---

## Sprint 9 — Integração automática com experimento validado

Objetivo: criar requisição FEO automaticamente quando um experimento atingir critério de sucesso.

### Backend

- Identificar gate de validação do experimento.
- Criar regra para gerar `fabricationRequest`.
- Evitar duplicidade para o mesmo experimento/variante.
- Permitir criação manual e automática.

### FEO

- Consumir requisições reais originadas do fluxo de experimento.
- Validar contexto real.

### Frontend

- Mostrar origem da requisição.
- Criar link entre requisição FEO e experimento vencedor.

### Critério de aceite

- Um experimento validado gera requisição FEO.
- A FEO inicia o trabalho via polling.
- O usuário consegue acompanhar tudo pela UI.

---

## Sprint 10 — Testes contratuais e estabilização

Objetivo: garantir que FEO, backend e frontend não quebrem contratos.

### Backend

- Testes de endpoints FEO.
- Testes de claim concorrente.
- Testes de status e comandos.
- Testes de criação automática por experimento validado.

### FEO

- Testes de client backend.
- Testes do polling.
- Testes de idempotência.
- Testes de comandos.
- Testes de falha e retry.

### Frontend

- Testes das telas principais.
- Testes dos fluxos de aprovação/reprovação.

### Documentação

- Atualizar `AGENTS.md` se necessário.
- Atualizar plano com o que foi implementado.
- Criar histórico de implantação.

### Critério de aceite

- Contratos estáveis.
- Fluxo ponta a ponta funcionando.
- Codex deixa registro claro do que ficou pendente.

## 8. Ordem recomendada das telas por sprint

```text
Sprint 1: Lista de Requisições + Detalhe básico
Sprint 2: Status do worker + heartbeat no detalhe
Sprint 3: Contexto validado da oferta
Sprint 4: Plano de Entregáveis
Sprint 5: Lista e preview dos Entregáveis
Sprint 6: Revisões e Intervenções
Sprint 7: Pacote Final e Relatório
Sprint 8: Operações da FEO
Sprint 9: Origem no Experimento Validado
Sprint 10: Polimento, filtros, testes e hardening
```

## 9. Instrução curta para o Codex em cada sprint

Use este padrão ao iniciar cada sprint:

```text
Implemente a Sprint X do plano da FEO.
Respeite obrigatoriamente:
- A FEO nunca acessa banco de dados diretamente.
- Toda leitura/escrita da FEO deve passar pelo backend.
- O backend é a fonte de verdade operacional.
- A FEO é worker Spring Boot em polling.
- A FEO só processa requisições formalmente registradas no backend.
- A FEO não altera promessa, resultado, mecanismo ou oferta validada.
- Registre ao final o que foi feito, arquivos alterados, testes, pendências e orientação para a sprint seguinte.
```

## 10. Resultado esperado ao final

Ao final das sprints, o Marketing Hub terá:

- módulo FEO separado;
- worker Spring Boot em polling;
- integração backend ↔ FEO sem acesso direto a banco;
- fila de fabricação persistida pelo backend;
- pipeline de criação de entregáveis;
- artefatos versionados;
- revisão e intervenção humana;
- telas de acompanhamento;
- operações e observabilidade;
- acionamento automático após experimento validado.
