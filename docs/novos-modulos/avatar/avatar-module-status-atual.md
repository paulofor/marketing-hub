# Avatar Sales Video — Status Atual do Módulo

- **Data de atualização:** 2026-04-16
- **Escopo:** `docs/novos-modulos/avatar/` + estado observado dos módulos `backend/ads-service`, `ai-worker`, `frontend` e `video-management-service`.
- **Objetivo deste documento:** consolidar o que já está implementado, o que está em progresso e os gaps para produção do módulo de vídeo com avatar falando.

---

## 1) Resumo executivo

O módulo de **Avatar Sales Video** já saiu da fase apenas conceitual e possui implementação funcional em múltiplos módulos:

- domínio e APIs de `salesvideo` implementados no backend;
- geração de script OpenAI operacional no `ai-worker` via APIs internas;
- tela administrativa no frontend para perfil, script, render, slots e retry;
- serviço dedicado `video-management-service` criado, com provider `stub`, polling e upload de assets para o backend.

No momento, o status geral é **MVP técnico parcialmente concluído**: fluxo ponta a ponta existe para desenvolvimento/homologação, mas ainda com limitações importantes (provider real de render, hardening operacional e rollout gradual).

---

## 2) Status por frente

## 2.1 Arquitetura e governança

**Status:** 🟢 Definido e aprovado

- Arquitetura canônica do módulo de avatar está formalizada e com versionamento.
- Plano atualizado de Avatar Sales Video define backend como fonte de verdade, `ai-worker` isolado para OpenAI e módulo de vídeo para ciclo técnico.
- Especificações complementares de composição de cena e módulo de vendas por diálogo também estão documentadas.

## 2.2 Backend (`backend/ads-service`)

**Status:** 🟢 Base implementada

Entregas observadas:

- migração de fundação do domínio `sales_video`;
- entidades, repositórios, DTOs e serviços para perfil, script, job, eventos, slot e histórico;
- endpoints administrativos para criação/listagem/aprovação e solicitação de render;
- endpoints internos para `ai-worker` (OpenAI jobs) e `video-management-service` (claim/progress/complete/fail);
- mecanismos de retry/auto-retry e limpeza de assets expirados.

Leitura de status: o backend já está operando como orquestrador canônico do fluxo.

## 2.3 AI Worker (`ai-worker`)

**Status:** 🟢 Implementado para etapa OpenAI de script

Entregas observadas:

- scheduler dedicado de jobs de script (`SCRIPT_PENDING`);
- serviço de claim/processamento/conclusão/falha via endpoints internos do backend;
- cliente OpenAI (Responses API) com retorno estruturado (hook, script, CTA, caption e storyboard JSON);
- flags de configuração para ativação, batch size e parâmetros do processamento.

Leitura de status: geração textual por IA está integrada ao pipeline e respeita backend como fonte de verdade.

## 2.4 Frontend (`frontend`)

**Status:** 🟢 MVP administrativo implementado

Entregas observadas:

- página de detalhe de perfil de vídeo com operações principais;
- fluxo de solicitar geração de script, aprovar script, solicitar render e reprocessar jobs;
- acompanhamento de jobs/eventos;
- gestão de slots da landing com histórico;
- suporte a seleção de `providerFamily` (`OPENAI`/`EXTERNAL_VIDEO_MODULE`).

Leitura de status: a operação manual do funil técnico já está disponível para time interno.

## 2.5 Módulo de vídeo (`video-management-service`)

**Status:** 🟡 Implementado em modo inicial (stub)

Entregas observadas:

- aplicação Spring Boot própria, com poller e dispatcher;
- cliente para endpoints internos de jobs no backend;
- upload de vídeo/poster/legenda para endpoint interno de assets;
- provider `stub` com geração de artefatos fictícios e reporte de progresso;
- testes unitários básicos no próprio módulo.

Gap principal:

- ainda não há integração de produção com provider real de avatar falante/render final.

---

## 3) Escopo funcional: implementado vs pendente

## Implementado

- orquestração central no backend;
- workflow de script com OpenAI;
- aprovação editorial de script;
- criação e acompanhamento de jobs de render;
- ingestão e publicação técnica de assets no backend;
- UI administrativa para operar o fluxo;
- trilha de eventos de jobs e reprocessamento.

## Em progresso / pendente

- provider real de vídeo avatar (substituir/expandir `stub`);
- critérios de fallback multi-provider em ambiente real;
- validações de compliance/consentimento específicas de réplica pessoal no fluxo de produção;
- hardening de observabilidade (SLOs, alertas e dashboards por tenant/provedor);
- validação E2E em ambiente de staging com carga real.

---

## 4) Riscos atuais

1. **Risco de “falso pronto”**: fluxo funcionando com `stub` pode mascarar falhas de integração real de render.
2. **Risco operacional**: sem SLO/alerta completos, incidentes em polling/webhook podem demorar a ser detectados.
3. **Risco de governança de conteúdo**: persistência e trilha de `modelo`/`prompt` precisam ser auditadas continuamente em cenários de expansão.
4. **Risco de UX comercial**: operação técnica disponível não garante ainda melhor taxa de conversão sem playbooks e experimentação contínua.

---

## 5) Próximos passos recomendados (ordem sugerida)

1. **Provider real de avatar falante (P0)**
   - implementar adapter real no `video-management-service`;
   - mapear erros do provider para códigos internos do backend;
   - validar ciclo completo `REQUEST_RENDER -> READY` em staging.

2. **Observabilidade e confiabilidade (P0)**
   - definir SLOs mínimos (sucesso por job, latência P95, retry rate);
   - configurar dashboards e alertas por estado de job e por provider;
   - revisar políticas de timeout, heartbeat e expiração.

3. **Rollout controlado (P1)**
   - ativar por feature flag/tenant;
   - começar por poucas ofertas com monitoramento diário;
   - registrar baseline de métricas antes de escalar.

4. **Conversão e produto (P1)**
   - consolidar playbooks de objeção/CTA por nicho;
   - executar testes A/B entre variações de script/avatar;
   - fechar feedback loop entre eventos de funil e revisão de script.

---

## 6) Classificação geral do módulo (hoje)

- **Maturidade técnica:** **Média** (arquitetura sólida + fluxo implementado, porém ainda dependente de provider real para produção plena).
- **Maturidade de produto:** **Média-baixa** (base pronta para operar, mas otimização comercial e rollout ainda em evolução).
- **Prontidão para produção ampla:** **Parcial**.

