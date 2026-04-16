# Avatar Sales Video — Plano Básico de Implementação para Reinício do Desenvolvimento

## Objetivo deste documento

Este documento organiza, de forma simples e operacional, o que ainda falta para o módulo **Avatar Sales Video** sair do estágio atual e avançar para uma operação de staging confiável, seguida de rollout controlado e evolução gradual para produção.

Ele foi escrito para servir como **guia de execução para o Codex**, com:
- backlog dividido por sprints;
- critérios de pronto;
- espaço de fechamento por sprint;
- espaço de handoff entre sprints;
- reforço da regra arquitetural de persistência via backend.

---

## Regra arquitetural obrigatória

**Assim como no padrão operacional adotado em outros módulos do Marketing Hub, toda integração com base de dados relacional deve acontecer exclusivamente através do `backend/ads-service`.**

### Isso significa:

- `frontend` fala apenas com o backend;
- `ai-worker` fala com o backend e com OpenAI;
- `video-management-service` fala com o backend e com providers de vídeo;
- nenhum worker ou serviço auxiliar deve acessar banco relacional diretamente;
- todo estado canônico do domínio deve continuar centralizado no backend.

### Observação importante

A referência ao padrão usado no OPRM é **somente arquitetural/operacional**.  
Este documento **não assume integração existente entre OPRM e Avatar Sales Video**.

---

## Ponto de partida atual

O módulo já possui:

- domínio e APIs de `salesvideo` implementados no backend;
- geração de script via `ai-worker`;
- tela administrativa no frontend;
- `video-management-service` criado, com polling, dispatcher, upload de assets e provider `stub`.

O principal gap atual está no fato de que o fluxo técnico existe, mas o render ainda não opera com provider real de produção.

---

## Meta desta fase

Levar o módulo do estado atual para:

1. **staging confiável com provider real**
2. **observabilidade mínima operacional**
3. **rollout controlado por tenant/feature flag**
4. **base pronta para evolução comercial posterior**

---

# Sprint V1 — Provider real de render

## Objetivo

Substituir a dependência exclusiva do provider `stub` por um **adapter real de avatar falante/render final**, mantendo a arquitetura atual e a persistência somente via backend.

## Escopo

- implementar adapter real no `video-management-service`;
- manter `stub` como opção de desenvolvimento/homologação;
- mapear `provider_job_id`, estados externos e erros externos para o modelo interno do backend;
- concluir o fluxo real:
  - request render
  - claim
  - dispatch
  - polling ou webhook
  - download do resultado
  - upload de assets para o backend
  - complete ou fail
- tratar expiração do asset externo quando aplicável.

## Entregáveis esperados

- adapter real implementado;
- configuração por provider documentada;
- normalização de erros do provider;
- metadata mínima do provider devolvida ao backend;
- validação técnica do fluxo real em ambiente controlado.

## Critério de pronto

- um job real percorre o ciclo completo até `VIDEO_READY` em staging;
- falhas do provider são traduzidas para códigos internos coerentes;
- vídeo, poster e legenda continuam sendo registrados no backend via endpoint interno.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### O que foi implementado
- 

### Arquivos alterados
- 

### Endpoints/contratos afetados
- 

### Providers suportados
- 

### Limitações restantes
- 

### Pendências carregadas para a Sprint V2
- 

### Evidências/testes executados
- 

---

# Sprint V2 — Robustez do ciclo assíncrono

## Objetivo

Fortalecer o comportamento do fluxo quando houver timeout, falha parcial, expiração, retry e concorrência.

## Escopo

- revisar política de `claim`, `heartbeat`, timeout e expiração;
- implementar tratamento mais robusto de retries técnicos;
- proteger contra claim duplicado;
- evitar jobs órfãos;
- revisar comportamento em assets expirados;
- reforçar coerência entre estado externo do provider e estado interno do backend;
- documentar fallback operacional por ambiente.

## Entregáveis esperados

- política de timeout, retry e expiração definida;
- proteção contra processamento duplicado;
- fluxo de retry técnico previsível;
- fallback operacional documentado;
- tratamento de inconsistências de job endurecido.

## Critério de pronto

- jobs travados ou expirados podem ser tratados sem intervenção manual em banco;
- retry não duplica processamento silenciosamente;
- backend continua sendo o estado canônico do fluxo.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### O que foi implementado
- 

### Regras novas de timeout/heartbeat/retry
- 

### Falhas tratadas
- 

### Limitações restantes
- 

### Pendências carregadas para a Sprint V3
- 

### Evidências/testes executados
- 

---

# Sprint V3 — Observabilidade e confiabilidade

## Objetivo

Dar visibilidade operacional real ao módulo para permitir monitoramento e resposta rápida a incidentes.

## Escopo

- instrumentar métricas de:
  - jobs recebidos
  - jobs concluídos
  - jobs com falha
  - latência total do render
  - backlog pendente
  - retries por provider
  - expiração de assets
- padronizar logs com campos de correlação:
  - `jobId`
  - `profileId`
  - `provider`
  - `providerJobId`
  - `tenant`
- definir dashboards mínimos;
- configurar alertas básicos;
- revisar rastreabilidade entre backend, `ai-worker` e `video-management-service`.

## Entregáveis esperados

- métricas mínimas implementadas;
- logs correlacionáveis padronizados;
- dashboards operacionais mínimos;
- alertas básicos configurados;
- documentação operacional inicial.

## Critério de pronto

- é possível identificar rapidamente:
  - qual provider falhou;
  - onde o job travou;
  - quanto tempo o fluxo levou;
  - se o backlog está acumulando;
  - se há aumento anormal de retry ou falha.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### Métricas adicionadas
- 

### Logs/correlation fields adicionados
- 

### Dashboards configurados
- 

### Alertas configurados
- 

### Limitações restantes
- 

### Pendências carregadas para a Sprint V4
- 

### Evidências/testes executados
- 

---

# Sprint V4 — Compliance, consentimento e governança

## Objetivo

Fechar os pré-requisitos mínimos de governança para uso produtivo de avatar falante e render com réplica pessoal, quando aplicável.

## Escopo

- definir contrato mínimo de consentimento;
- bloquear render produtivo quando faltarem pré-condições de compliance;
- registrar de forma auditável:
  - script aprovado
  - provider usado
  - modelo usado
  - momento da publicação
- reforçar trilha de auditoria;
- separar claramente cenários de teste e cenários produtivos;
- documentar política mínima de revisão humana antes da publicação.

## Entregáveis esperados

- checklist de compliance no backend;
- bloqueios de workflow quando faltarem pré-condições;
- rastreabilidade mínima de prompt/model/provider/publicação;
- documentação operacional de governança.

## Critério de pronto

- nenhum render produtivo com avatar pessoal passa sem sinais mínimos de consentimento e rastreabilidade;
- o histórico do backend permite auditoria posterior.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### Regras de compliance implementadas
- 

### Campos/contratos adicionados
- 

### Bloqueios de workflow implementados
- 

### Limitações restantes
- 

### Pendências carregadas para a Sprint V5
- 

### Evidências/testes executados
- 

---

# Sprint V5 — Validação E2E em staging

## Objetivo

Validar o ciclo completo do módulo em staging com cenários reais de uso e falha.

## Escopo

- validar o fluxo ponta a ponta em staging;
- testar cenários de:
  - sucesso
  - timeout
  - falha de provider
  - asset expirado
  - retry
  - reprocessamento manual
  - publicação de slot na landing
- revisar consistência entre backend, worker, módulo de vídeo e frontend;
- consolidar checklist técnico de readiness.

## Entregáveis esperados

- bateria mínima E2E em staging;
- relatório de cenários validados;
- checklist de readiness;
- riscos residuais documentados.

## Critério de pronto

- o time consegue operar o fluxo completo em staging sem depender de intervenção manual na base;
- falhas esperadas têm comportamento previsível;
- publicação técnica na landing é validada de ponta a ponta.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### Cenários E2E validados
- 

### Problemas encontrados
- 

### Correções aplicadas
- 

### Riscos residuais
- 

### Pendências carregadas para a Sprint V6
- 

### Evidências/testes executados
- 

---

# Sprint V6 — Rollout controlado e baseline operacional

## Objetivo

Liberar o módulo de forma gradual, com controle por tenant/flag e monitoramento próximo.

## Escopo

- ativar o módulo por feature flag ou tenant;
- começar com poucas ofertas/perfis;
- monitorar diariamente o comportamento inicial;
- registrar baseline de métricas;
- definir rollback simples;
- documentar o procedimento operacional do primeiro rollout.

## Entregáveis esperados

- rollout controlado habilitado;
- tenants/perfis iniciais definidos;
- baseline inicial de métricas registrada;
- procedimento de rollback documentado;
- relatório do primeiro ciclo de operação.

## Critério de pronto

- o módulo opera em escopo reduzido com monitoramento diário;
- existe baseline mínima de sucesso/falha/latência/retry;
- rollback operacional está definido.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### Flags/tenants habilitados
- 

### Baseline de métricas
- 

### Problemas observados no rollout
- 

### Mitigações aplicadas
- 

### Pendências carregadas para a próxima fase
- 

### Evidências/testes executados
- 

---

# Sprint V7 — Primeira camada de evolução comercial

## Objetivo

Iniciar a transição do módulo de “pipeline técnico funcional” para “pipeline técnico + aprendizado comercial”.

## Escopo

- consolidar playbooks iniciais de objeção e CTA por nicho;
- testar variações de script/avatar;
- conectar eventos de conversão com a operação do módulo;
- comparar resultados por perfil, script e provider;
- estruturar rotina mínima de revisão baseada em dados.

## Entregáveis esperados

- matriz inicial de variações;
- conexão mínima com eventos de conversão;
- relatório inicial de performance;
- rotina básica de revisão comercial.

## Critério de pronto

- o módulo passa a gerar aprendizado operacional/comercial além do simples render técnico;
- existe base inicial para testes A/B posteriores.

## Fechamento da sprint — preencher pelo Codex

### Status
- 

### Variações testadas
- 

### Eventos de conversão conectados
- 

### Principais aprendizados
- 

### Próximos ajustes sugeridos
- 

### Evidências/testes executados
- 

---

# Bloco obrigatório de handoff entre sprints

> Este bloco deve ser copiado e preenchido ao final de **toda sprint** para manter continuidade operacional e evitar perda de contexto.

## Handoff para a próxima sprint

### 1. Resumo factual do estado atual
- O que está concluído:
- O que está parcialmente concluído:
- O que ainda não começou:

### 2. Pendências carregadas
- Pendência 1:
- Pendência 2:
- Pendência 3:

### 3. Riscos abertos
- Risco 1:
- Risco 2:
- Risco 3:

### 4. Decisões tomadas nesta sprint
- Decisão 1:
- Decisão 2:
- Decisão 3:

### 5. Contratos/artefatos afetados
- Endpoint:
- DTO/schema:
- Eventos:
- Métricas:
- Flags:

### 6. Instruções para o próximo ciclo do Codex
- Prioridade imediata:
- O que não deve ser refeito:
- Onde continuar:

---

# Ordem recomendada de execução

1. Sprint V1 — Provider real de render
2. Sprint V2 — Robustez do ciclo assíncrono
3. Sprint V3 — Observabilidade e confiabilidade
4. Sprint V4 — Compliance, consentimento e governança
5. Sprint V5 — Validação E2E em staging
6. Sprint V6 — Rollout controlado e baseline operacional
7. Sprint V7 — Primeira camada de evolução comercial

---

# Regras para o Codex durante a execução

## 1. Não quebrar a arquitetura atual
- Não introduzir acesso direto a banco fora do backend.
- Não mover o estado canônico do domínio para workers auxiliares.
- Não acoplar o frontend diretamente a providers externos.

## 2. Não refazer o que já existe
- Partir do pressuposto de que backend, `ai-worker`, frontend e `video-management-service` já possuem base funcional relevante.
- Trabalhar incrementalmente sobre o estado atual.

## 3. Toda sprint deve deixar rastreabilidade
- Registrar o que foi feito.
- Registrar o que ficou pendente.
- Registrar o que mudou em contratos.
- Registrar testes executados.
- Registrar limitações abertas.

## 4. Priorizar robustez antes de escala
- Primeiro provider real.
- Depois robustez do ciclo.
- Depois observabilidade.
- Depois rollout.
- Só depois otimização comercial.

---

# Critério final desta fase

Esta fase será considerada concluída quando:

- existir pelo menos um provider real funcional;
- o ciclo assíncrono estiver endurecido;
- houver observabilidade mínima suficiente para operação;
- o fluxo estiver validado em staging ponta a ponta;
- o rollout controlado estiver operacional;
- toda persistência continuar centralizada no backend.

---
