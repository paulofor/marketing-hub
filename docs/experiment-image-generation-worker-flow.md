# Evolução do framework de experimento: geração de imagens via Worker AI

Este documento descreve a evolução do fluxo para sair de **planejamento de imagens** e chegar em **imagens geradas e publicadas**, seguindo o pipeline existente do projeto.

## Objetivo

Implementar um fluxo assíncrono em que, após o usuário aprovar o planejamento, o backend cria jobs de geração e o Worker AI executa a criação em batch na OpenAI, salva os artefatos no Cloudflare e devolve os links para o backend (incluindo o resultado do módulo de “webnização”).

---

## Fluxo alvo (alto nível)

1. Usuário conclui o planejamento de imagens no framework do experimento e clica em **Gerar imagens**.
2. Front-end envia a ação ao backend (`POST` de solicitação de geração).
3. Backend registra jobs pendentes de geração de imagem (status inicial) por item planejado.
4. Worker AI (novo serviço/scheduler) busca jobs pendentes no backend e faz `claim`.
5. Worker AI agrupa prompts elegíveis e envia para OpenAI em modo batch, usando o **mesmo modelo de geração de imagem dos criativos**.
6. Ao concluir cada imagem, Worker AI salva no Cloudflare e notifica o backend com metadados e URL(s).
7. A imagem fica disponível para o módulo de tratamento de imagem (“webnizar”).
8. O módulo de tratamento processa periodicamente novas imagens, publica versão web e notifica o backend com URL pública final.
9. Backend disponibiliza a URL final para front-end e para o gerador de HTML posterior.

---

## Contratos sugeridos

## Backend (novos endpoints internos/externos)

### Endpoints externos (front-end -> backend)

- `POST /api/experiments/{experimentId}/framework-images/generate`
  - Cria jobs de geração para os itens do plano.
  - Idempotente: não recriar job ativo para o mesmo item planejado.
- `GET /api/experiments/{experimentId}/framework-images`
  - Lista estado por item (planejada, gerando, gerada, webnizada, erro) e URLs disponíveis.

### Endpoints internos (worker -> backend)

- `GET /api/internal/framework-image/jobs/pending?limit=20`
- `POST /api/internal/framework-image/jobs/{jobId}/claim`
- `POST /api/internal/framework-image/jobs/{jobId}/stage`
- `POST /api/internal/framework-image/jobs/{jobId}/complete`
- `POST /api/internal/framework-image/jobs/{jobId}/fail`

### Endpoint interno (módulo de webnização -> backend)

- `POST /api/internal/framework-image/assets/{assetId}/web-ready`
  - Atualiza URL final pública otimizada para web.

---

## Modelo de estados do job

Estados recomendados para rastreabilidade ponta a ponta:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

Stages detalhados:

- `WAITING_AI_WORKER`
- `CLAIMED`
- `SENT_TO_OPENAI_BATCH`
- `WAITING_OPENAI_BATCH`
- `OPENAI_IMAGE_READY`
- `UPLOADED_TO_CLOUDFLARE`
- `NOTIFIED_BACKEND`
- `WAITING_WEBNIZATION`
- `WEB_READY`
- `FAILED`

---

## Responsabilidades por camada

## Front-end

- Exibir botão “Gerar imagens” após existir planejamento válido.
- Mostrar progresso por item (fila, processamento, pronto, erro).
- Consumir endpoint de listagem/status para atualizar UI.

## Backend

- Orquestração de estado dos jobs.
- Idempotência e prevenção de concorrência.
- Persistência de metadados necessários para rastreio e auditoria.
- Exposição de dados para front-end e gerador de HTML.

## Worker AI (novo domínio, ex.: `frameworkimage`)

- Scheduler para polling de jobs pendentes.
- Cliente de backend para `list/claim/stage/complete/fail`.
- Cliente OpenAI em batch usando o mesmo modelo dos criativos.
- Upload para Cloudflare.
- Notificação de conclusão para backend.

## Módulo de webnização

- Polling de assets novos prontos para tratamento.
- Conversão para formato otimizado web.
- Publicação em URL pública final.
- Callback para backend com URL final.

---

## Regras obrigatórias do domínio

- Reutilizar pipeline existente de imagem sempre que possível (`CreativeImageOptimizer` e upload compatível com backend).
- Qualquer registro gerado por Worker AI deve preencher `modelo` e `prompt`.
- Worker AI **não acessa banco diretamente**; toda leitura/escrita via endpoint do backend.
- Priorizar filtros no backend ao listar pendências (evitar processamento em memória desnecessário).

---

## Observabilidade

- Incluir `jobId`, `experimentId`, `assetId`, `workerId`, `model`, `batchId` nos logs.
- Métricas recomendadas:
  - jobs pendentes/processando/falhos;
  - latência por estágio;
  - taxa de sucesso de batch OpenAI;
  - tempo até `WEB_READY`.

---

## Estratégia de implementação (incremental)

1. **Backend**: criar entidade de job + endpoints internos + endpoint externo para disparo.
2. **Worker AI**: criar pacote `frameworkimage` com `Service`, `Scheduler`, `BackendClient`, `OpenAiBatchClient`.
3. **Cloudflare**: integrar upload e retorno de metadados no `complete`.
4. **Webnização**: ligar consumo de novas imagens e callback de URL final.
5. **Front-end**: botão de disparo + timeline/status.
6. **Hardening**: retry/backoff, idempotência, timeout de job stale, dashboards.

---

## Etapas de execução para o Codex

> Objetivo desta seção: permitir implementação em PRs pequenos, revisáveis e com rollback simples.

### Etapa 1 — Backend: fila de jobs e contratos internos

**Entrega**
- Criar entidade/tabela de job de geração de imagem do framework.
- Criar enum de status/stage conforme este documento.
- Implementar endpoints internos:
  - `GET /api/internal/framework-image/jobs/pending`
  - `POST /api/internal/framework-image/jobs/{jobId}/claim`
  - `POST /api/internal/framework-image/jobs/{jobId}/stage`
  - `POST /api/internal/framework-image/jobs/{jobId}/complete`
  - `POST /api/internal/framework-image/jobs/{jobId}/fail`

**Checklist Codex**
- Criar migration Liquibase incremental (sem alterar changelog antigo).
- Adicionar repository + service + controller interno.
- Garantir idempotência para não duplicar job ativo do mesmo item.
- Cobrir com testes de service/controller.

**Critério de pronto**
- Worker consegue listar e fazer claim de jobs via API interna.

### Etapa 2 — Backend: disparo externo e consulta de progresso

**Entrega**
- Criar endpoint externo de disparo:
  - `POST /api/experiments/{experimentId}/framework-images/generate`
- Criar endpoint externo de listagem:
  - `GET /api/experiments/{experimentId}/framework-images`

**Checklist Codex**
- Mapear planejamento existente para itens geráveis.
- Criar jobs pendentes apenas para itens elegíveis.
- Expor payload de status para o front (incluindo erros e timestamps).
- Adicionar testes de API e regra de idempotência.

**Critério de pronto**
- Front-end já consegue disparar e acompanhar status sem Worker implementado.

### Etapa 3 — Worker AI: novo domínio `frameworkimage`

**Entrega**
- Criar pacote `frameworkimage` no Worker:
  - `FrameworkImageScheduler`
  - `FrameworkImageService`
  - `FrameworkImageBackendClient`

**Checklist Codex**
- Implementar polling periódico de jobs pendentes.
- Claim por job com `workerId`.
- Atualização de stage em cada transição.
- Tratamento de erro com `fail` e mensagem auditável.

**Critério de pronto**
- Worker consome fila e atualiza stages mesmo com geração fake/mockada.

### Etapa 4 — Worker AI: OpenAI batch usando mesmo modelo dos criativos

**Entrega**
- Implementar cliente batch para geração de imagem.
- Reutilizar configuração/modelo do fluxo de criativos.

**Checklist Codex**
- Serializar requests em arquivo batch.
- Criar/poll de batch OpenAI com timeout.
- Correlacionar `custom_id` com `jobId`/item.
- Persistir `modelo` e `prompt` no callback de conclusão.

**Critério de pronto**
- Jobs chegam até `OPENAI_IMAGE_READY` com resposta válida.

### Etapa 5 — Worker AI: upload Cloudflare e callback de conclusão

**Entrega**
- Salvar imagem gerada no Cloudflare.
- Notificar backend com metadados de asset e URL de origem.

**Checklist Codex**
- Implementar cliente de upload com retry/backoff.
- Enviar `complete` com referência do arquivo.
- Atualizar stage para `UPLOADED_TO_CLOUDFLARE` e `NOTIFIED_BACKEND`.

**Critério de pronto**
- Backend recebe imagem gerada e exibe URL inicial por item.

### Etapa 6 — Webnização: processamento assíncrono e URL final

**Entrega**
- Integrar módulo de webnização ao novo tipo de asset.
- Publicar callback:
  - `POST /api/internal/framework-image/assets/{assetId}/web-ready`

**Checklist Codex**
- Detectar novas imagens pendentes de webnização.
- Converter para formato web otimizado.
- Publicar URL final e atualizar stage `WEB_READY`.
- Cobrir cenário de reprocessamento idempotente.

**Critério de pronto**
- Front recebe URL final web-ready para renderização e HTML generator.

### Etapa 7 — Front-end: UX de disparo e acompanhamento

**Entrega**
- Botão “Gerar imagens”.
- Timeline/status por item.
- Estados de erro e reprocessamento.

**Checklist Codex**
- Conectar `POST generate` e `GET status`.
- Exibir progresso em tempo real (polling).
- Exibir `modelo` e origem quando disponível.
- Validar tratamento de loading/empty/error.

**Critério de pronto**
- Usuário executa ponta a ponta sem intervenção manual.

### Etapa 8 — Hardening, observabilidade e rollout

**Entrega**
- Métricas, logs estruturados e alertas.
- Feature flag para ativação gradual.

**Checklist Codex**
- Logar `jobId`, `experimentId`, `assetId`, `batchId`, `workerId`.
- Criar dashboard mínimo (fila, falha, latência).
- Definir política de timeout para jobs stale.
- Planejar rollout por porcentagem/ambiente.

**Critério de pronto**
- Fluxo resiliente e monitorável em produção.

---

## Critérios de aceite

- Usuário consegue disparar geração após planejamento.
- Cada item planejado gera no máximo 1 job ativo por vez.
- Imagem gerada chega ao backend com `modelo` e `prompt`.
- URL web final é disponibilizada para front-end e para o gerador de HTML.
- Falhas ficam auditáveis com erro, estágio e timestamps.
