# Plano de implementação — identificação de visitante recorrente na landing do experimento

## Objetivo

Permitir responder, com rastreabilidade operacional e confiança maior que a atual, se uma mesma pessoa provável acessou a landing de um experimento mais de uma vez em horários diferentes.

O estado atual mede `sessionId` da landing, suficiente para analytics de sessão, mas insuficiente para afirmar recorrência de pessoa/dispositivo entre visitas futuras. A evolução proposta separa:

- `visitorId`: identificador persistente first-party de visitante provável no mesmo navegador/dispositivo;
- `sessionId`: identificador de sessão/aba carregada;
- eventos: interações observadas na landing (`page_view`, `section_view_time` e futuros eventos de CTA).

## Princípios obrigatórios

1. Resolver a causa-raiz: a limitação atual não está na consulta, mas na ausência de identificador persistente de visitante.
2. Preservar simplicidade: usar identificador first-party sem login como primeira versão.
3. Não armazenar dado pessoal desnecessário: evitar IP puro; se for necessário correlacionar rede, usar hash com salt no backend e documentar a finalidade.
4. Separar metadado interno de artefato final: nenhum marcador técnico deve contaminar HTML/JSON publicado para o cliente.
5. Manter compatibilidade: eventos antigos sem `visitorId` devem continuar sendo lidos como analytics de sessão.

## Escopo fora da primeira versão

- Identificação determinística de pessoa real entre dispositivos diferentes.
- Login/autenticação do visitante da landing.
- Integração imediata com CRM externo.
- Fingerprinting agressivo do navegador.

## Etapa 1 — Decisão canônica e contrato de dados

### Objetivo da etapa

Formalizar a regra de identificação de visitante recorrente e o contrato mínimo dos eventos de analytics da landing.

### Escopo para uma execução de prompt

- Atualizar o documento canônico relacionado a campanhas/analytics de experimento.
- Definir os campos contratuais dos eventos públicos:
  - `eventId`;
  - `eventType`;
  - `visitorId`;
  - `sessionId`;
  - `sectionId`;
  - `elapsedMs` / `visibleMs`;
  - `pageUrl`;
  - `occurredAt`;
  - `userAgent`.
- Definir semântica:
  - `visitorId` identifica visitante provável no mesmo navegador/dispositivo;
  - `sessionId` identifica uma sessão de navegação;
  - recorrência exige o mesmo `visitorId` em mais de uma sessão ou em `page_view`s separados por janela mínima configurada.
- Definir janela de deduplicação de `page_view` repetido em curto intervalo, sugerida inicialmente em 3 segundos.

### Critérios de aceite

- Cânone atualizado com a diferença entre visitante, sessão e evento.
- Regra explícita de que `visitorId` não prova pessoa real, apenas visitante provável.
- Regra de compatibilidade para eventos legados sem `visitorId`.

### Prompt sugerido

```text
Atualize o cânone de publicação/campanha para formalizar analytics de visitante recorrente na landing do experimento. Defina visitorId, sessionId, deduplicação de page_view e compatibilidade com eventos legados sem visitorId. Não altere código nesta etapa.
```

## Etapa 2 — Modelo de banco e Liquibase

### Objetivo da etapa

Criar estrutura relacional para consultas eficientes de visitante recorrente sem depender apenas de parsing textual do payload.

### Escopo para uma execução de prompt

- Criar changelog Liquibase YAML compatível com MySQL 5.7.
- Avaliar uma das duas abordagens abaixo e justificar a escolha:
  1. adicionar colunas normalizadas em `experiment_funnel_event`; ou
  2. criar tabela derivada `experiment_landing_analytics_event` vinculada ao evento original.
- Campos mínimos recomendados:
  - `id`;
  - `experiment_id`;
  - `funnel_event_id`;
  - `visitor_id`;
  - `session_id`;
  - `event_type`;
  - `section_id`;
  - `page_url`;
  - `user_agent_hash` ou `user_agent` normalizado, conforme decisão canônica;
  - `occurred_at`;
  - `created_at`.
- Índices mínimos:
  - `(experiment_id, visitor_id, occurred_at)`;
  - `(experiment_id, session_id, occurred_at)`;
  - `(experiment_id, event_type, occurred_at)`.

### Critérios de aceite

- Changelog em YAML com `databaseChangeLog`, `preConditions` com `dbms:mysql`, `splitStatements: true` e `stripComments: true`.
- Compatibilidade mental validada com MySQL 5.7.
- Nenhuma quebra nos eventos existentes.

### Prompt sugerido

```text
Implemente a migração Liquibase MySQL 5.7 para normalizar analytics de landing por visitorId/sessionId. Leia docs/database/liquibase-mysql57.md antes de editar. Preserve compatibilidade com experiment_funnel_event legado e adicione índices para recorrência por experimento.
```

## Etapa 3 — Backend: ingestão do `visitorId` e normalização

### Objetivo da etapa

Fazer o backend aceitar `visitorId`, validar o contrato e persistir evento normalizado para consulta recorrente.

### Escopo para uma execução de prompt

- Atualizar DTO público de analytics da landing para incluir `visitorId`.
- Atualizar serviço de registro de analytics para:
  - registrar o payload bruto recebido antes de normalizar;
  - validar campos obrigatórios por `eventType`;
  - salvar evento legado em `experiment_funnel_event` como hoje;
  - salvar/atualizar a estrutura normalizada criada na etapa 2;
  - aplicar deduplicação de `page_view` em janela curta quando definido no cânone.
- Adicionar logs com contexto operacional:
  - slug;
  - experimentId;
  - eventId;
  - visitorId;
  - sessionId;
  - eventType.
- Adicionar testes unitários do serviço.

### Critérios de aceite

- Eventos com `visitorId` são persistidos e consultáveis.
- Eventos sem `visitorId` continuam aceitos como legado, quando o cânone permitir.
- Blocos `catch` alterados registram log com exceção completa.
- Toda classe/método Java alterado contém comentário de responsabilidade conforme regra do projeto.

### Prompt sugerido

```text
Implemente no backend a ingestão e normalização de visitorId nos eventos públicos de analytics da landing. Preserve gravação em experiment_funnel_event, grave também a estrutura normalizada, aplique deduplicação de page_view conforme cânone e adicione testes unitários. Antes de alterar classes Java, leia e preserve os comentários de responsabilidade de classes e métodos.
```

## Etapa 4 — Script público da landing: geração de `visitorId`

### Objetivo da etapa

Gerar e enviar `visitorId` first-party no script injetado na landing publicada.

### Escopo para uma execução de prompt

- Atualizar o script de analytics injetado na landing para:
  - gerar `visitorId` persistente em `localStorage` ou cookie first-party;
  - manter `sessionId` em `sessionStorage`;
  - enviar ambos em todos os eventos;
  - não quebrar navegadores com restrição de storage;
  - usar fallback seguro quando `crypto.randomUUID` não existir.
- Garantir que o script não injete comentários/flags técnicas no artefato final além do atributo funcional já contratado.
- Adicionar teste garantindo presença de `visitorId` no payload do script e ausência de metadado técnico indevido.

### Critérios de aceite

- Novo acesso no mesmo navegador mantém `visitorId` e muda `sessionId` quando a sessão muda.
- Evento `page_view` contém `visitorId`, `sessionId`, `pageUrl`, `occurredAt` e `userAgent`.
- Teste de regressão cobre geração/envio do `visitorId`.

### Prompt sugerido

```text
Atualize o script público de analytics da landing para criar visitorId persistente first-party e enviar visitorId/sessionId em todos os eventos. Garanta fallback sem quebrar navegadores restritos e adicione teste de regressão contra contaminação técnica do artefato publicado.
```

## Etapa 5 — Backend: API de recorrência por visitante

### Objetivo da etapa

Expor endpoint específico para responder se visitantes prováveis voltaram em horários diferentes.

### Escopo para uma execução de prompt

- Criar endpoint no backend do experimento, por exemplo:
  - `GET /api/experiments/{experimentId}/landing-analytics/visitors`
  - ou evoluir o endpoint atual de analytics com seção `visitors`.
- Retornar resumo por visitante:
  - `visitorId` mascarado/abreviado;
  - total de sessões;
  - total de `page_view`s válidos;
  - primeiro acesso;
  - último acesso;
  - intervalo entre primeiro e último acesso;
  - quantidade de páginas distintas;
  - último `userAgent` ou família de dispositivo, conforme cânone;
  - flag `recurrent`.
- Definir regra inicial de recorrência:
  - `recurrent = true` quando houver pelo menos duas sessões do mesmo `visitorId`; ou
  - pelo menos dois `page_view`s válidos separados por janela mínima maior que a deduplicação.
- Documentar no Swagger/OpenAPI.

### Critérios de aceite

- Endpoint responde claramente quantos visitantes prováveis são recorrentes.
- Consultas usam filtros no SQL e índices adequados, sem pós-processamento pesado em memória.
- Testes cobrem visitante recorrente, visitante único e eventos legados sem `visitorId`.

### Prompt sugerido

```text
Crie a API backend de visitantes recorrentes da landing por experimento. A resposta deve agrupar por visitorId, indicar sessões, page_views válidos, primeiro/último acesso e flag recurrent. Use SQL/índices para filtrar no banco e adicione testes unitários.
```

## Etapa 6 — Frontend: aba de analytics com recorrência

### Objetivo da etapa

Mostrar na UI do experimento se houve visitantes prováveis recorrentes e em quais horários.

### Escopo para uma execução de prompt

- Investigar o módulo backend responsável e confirmar o endpoint criado na etapa 5.
- Atualizar a aba de analytics da landing para incluir:
  - card `Visitantes prováveis`;
  - card `Visitantes recorrentes`;
  - tabela de visitantes recorrentes;
  - primeiro acesso;
  - último acesso;
  - número de sessões;
  - número de page views;
  - observação clara: “visitante provável por navegador/dispositivo, não pessoa comprovada”.
- Manter a tela simples, objetiva e orientada à decisão comercial.

### Critérios de aceite

- Usuário consegue responder visualmente se houve recorrência.
- A tela não afirma “mesma pessoa” de forma determinística quando a evidência é apenas `visitorId` first-party.
- Estados de loading/erro/vazio estão cobertos.

### Prompt sugerido

```text
Atualize a aba de analytics da landing no frontend para exibir visitantes prováveis e visitantes recorrentes usando o endpoint backend existente. Mostre primeiro/último acesso, sessões e page views, com aviso claro de que é identificação provável por navegador/dispositivo.
```

## Etapa 7 — Backfill e compatibilidade operacional

### Objetivo da etapa

Preservar leitura dos dados antigos e preparar migração segura de eventos legados.

### Escopo para uma execução de prompt

- Criar rotina pontual ou comando administrativo para normalizar eventos antigos quando possível.
- Para eventos sem `visitorId`, manter apenas `sessionId` e marcar confiança como `SESSION_ONLY`.
- Definir níveis de confiança:
  - `VISITOR_ID`: há identificador persistente first-party;
  - `SESSION_ONLY`: apenas sessão da landing;
  - `LEGACY_PAYLOAD`: payload antigo/parcial.
- Atualizar endpoint para indicar nível de confiança.

### Critérios de aceite

- Dados do experimento 37 continuam aparecendo como histórico, mas sem falsa afirmação de pessoa recorrente.
- Eventos novos entram com `VISITOR_ID`.
- Documentação explica a diferença entre histórico e dados novos.

### Prompt sugerido

```text
Implemente compatibilidade/backfill dos eventos antigos de analytics da landing. Classifique a confiança como VISITOR_ID, SESSION_ONLY ou LEGACY_PAYLOAD e garanta que o experimento 37 continue consultável sem afirmar recorrência de pessoa quando só existir sessionId.
```

## Etapa 8 — Validação em produção e checklist de diagnóstico

### Objetivo da etapa

Validar de ponta a ponta se a nova medição responde à pergunta comercial sem criar ruído nos funis.

### Escopo para uma execução de prompt

- Criar checklist operacional para testar em uma landing publicada:
  1. primeiro acesso gera `visitorId` e `sessionId`;
  2. refresh imediato não duplica `page_view` válido fora da regra;
  3. nova sessão no mesmo navegador preserva `visitorId` e muda `sessionId`;
  4. outro navegador gera outro `visitorId`;
  5. UI mostra recorrência corretamente.
- Consultar banco via MCP para validar persistência.
- Consultar logs do backend via MCP para validar ingestão.
- Registrar resultado em `/docs/registros/experimentos.md`.

### Critérios de aceite

- Evidência de banco confirma recorrência por `visitorId` em teste controlado.
- Logs mostram ingestão sem erros.
- UI mostra o resultado de forma simples e sem afirmar identidade real acima da evidência.

### Prompt sugerido

```text
Execute a validação operacional da identificação de visitantes recorrentes em uma landing publicada. Use MCP para consultar banco e logs, valide geração de visitorId/sessionId, deduplicação de page_view e atualização da UI. Registre o resultado em docs/registros/experimentos.md.
```

## Sequência recomendada de execução

1. Etapa 1 — cânone e contrato.
2. Etapa 2 — banco.
3. Etapa 3 — backend ingestão.
4. Etapa 4 — script público.
5. Etapa 5 — API de recorrência.
6. Etapa 6 — frontend.
7. Etapa 7 — legado/backfill.
8. Etapa 8 — validação operacional.

## Resultado esperado ao final

Ao final do plano, para cada experimento com landing publicada, o sistema deverá responder:

- quantos visitantes prováveis acessaram a landing;
- quantos voltaram em horários diferentes;
- quais foram os horários de primeiro e último acesso;
- quantas sessões cada visitante provável gerou;
- qual o nível de confiança da identificação;
- quais dados são históricos legados e quais já usam `visitorId` persistente.
