# Framework de experimento — fluxo de geração e webnização de imagens

## Objetivo

Evoluir o framework de experimento para sair do **planejamento de imagens** (`landing_page_image_planning`) e executar a **geração real em lote no Worker IA**, com retorno assíncrono ao backend e posterior **webnização** (publicação web) para consumo por frontend e gerador de HTML.

## Escopo funcional

Fluxo alvo solicitado:

1. Usuário conclui o planejamento das imagens e solicita a criação.
2. Backend registra a solicitação e enfileira jobs.
3. Worker IA detecta jobs de geração de imagem para experimento.
4. Worker IA envia geração para OpenAI em batch usando o mesmo modelo dos criativos.
5. Worker IA salva imagem no Cloudflare e notifica backend.
6. Backend disponibiliza imagem para o módulo de tratamento.
7. Módulo de webnização converte/publica versão web e atualiza URL pública no backend para frontend e gerador de HTML.

---

## Arquitetura proposta

## 1) Backend: orquestração dos jobs

### 1.1 Novo recurso: `experiment_landing_image_job`

Criar entidade/tabela para jobs de imagens da landing:

- `id` (UUID)
- `experiment_id` (FK)
- `section_id` (referência do item de `landing_page_image_planning.images[]`)
- `prompt` (LONGTEXT)
- `status` (`PENDING`, `PROCESSING`, `GENERATED`, `WEBNIZING`, `COMPLETED`, `FAILED`)
- `generation_model` (modelo OpenAI usado)
- `generation_prompt` (prompt efetivo usado na geração)
- `cloudflare_key` (objeto bruto após geração)
- `cloudflare_url` (URL temporária/armazenamento)
- `web_url` (URL pública final para consumo web)
- `error_message`
- `worker_id`
- `created_at`, `started_at`, `finished_at`, `updated_at`

> Requisito de rastreabilidade: todo registro produzido pelo Worker IA deve persistir `modelo` e `prompt`.

### 1.2 Endpoint público para disparo pelo usuário

`POST /api/experiments/{experimentId}/pipeline/landing-images/generate`

Comportamento:

- valida que existe `landing_page_image_planning`;
- evita duplicação de jobs ativos (`PENDING`/`PROCESSING`/`WEBNIZING`);
- cria jobs por item planejado (`images[]`);
- retorna resumo da fila criada.

### 1.3 Endpoints internos para o Worker IA

Base sugerida: `/api/internal/experiment-landing-images/jobs`

- `GET /pending?limit=20`
- `POST /{jobId}/claim`
- `POST /{jobId}/generated` (recebe `cloudflare_key`, `cloudflare_url`, `model`, `prompt`)
- `POST /{jobId}/web-ready` (recebe `web_url`)
- `POST /{jobId}/fail`

### 1.4 Endpoint interno para módulo de webnização

Base sugerida: `/api/internal/experiment-landing-images/webnization`

- `GET /pending?limit=50` (retorna jobs em `GENERATED`)
- `POST /{jobId}/start` (marca `WEBNIZING`)
- `POST /{jobId}/complete` (persiste `web_url` e marca `COMPLETED`)
- `POST /{jobId}/fail`

---

## 2) Worker IA: geração batch na OpenAI

## 2.1 Novo pacote no worker

Criar pacote seguindo padrão existente:

- `com.marketinghub.worker.experimentlandingimage`
  - `ExperimentLandingImageService`
  - `ExperimentLandingImageScheduler`
  - `ExperimentLandingImageBackendClient`
  - `ExperimentLandingImageOpenAiClient`
  - `ExperimentLandingImageStorageClient` (Cloudflare)

## 2.2 Comportamento do serviço

A cada ciclo:

1. listar jobs pendentes no backend;
2. fazer claim de cada job;
3. montar lote OpenAI (`/v1/batches`) para prompts pendentes;
4. baixar resultados, aplicar pipeline de otimização (`CreativeImageOptimizer`);
5. salvar no Cloudflare;
6. notificar backend com `model`, `prompt`, `cloudflare_key`, `cloudflare_url`.

## 2.3 Regras de geração

- reutilizar o mesmo `openai.image-model` já usado por criativos;
- manter padrão de tratamento de erros transitórios (429/5xx) com retry;
- limitar lote por janela (ex.: 20 jobs/ciclo) para evitar sobrecarga;
- para cada job registrar prompt efetivo final (incluindo contexto da seção).

---

## 3) Módulo de tratamento de imagem (webnização)

## 3.1 Entrada

Consumir jobs em `GENERATED` com origem `experiment_landing_image_job`.

## 3.2 Saída

Após converter e publicar versão web:

- enviar `web_url` para o backend;
- backend marca `COMPLETED`;
- URL final fica disponível:
  - no frontend (preview/gestão);
  - para o gerador de HTML posterior.

## 3.3 Cadência

Processo recorrente (polling), mantendo idempotência:

- se `web_url` já existir, ignorar;
- em falha temporária, reprocessar;
- em falha final, marcar `FAILED` com motivo.

---

## Contratos mínimos (payloads)

## Disparo de geração (frontend/backend)

```json
{
  "regenerateFailedOnly": false,
  "maxImages": 20
}
```

## Worker → backend (imagem gerada)

```json
{
  "model": "gpt-image-1",
  "prompt": "Prompt efetivo utilizado...",
  "cloudflareKey": "experiments/123/landing/hero-01.jpg",
  "cloudflareUrl": "https://assets.cloudflare.../hero-01.jpg"
}
```

## Webnização → backend (imagem pronta para web)

```json
{
  "webUrl": "https://cdn.seudominio.com/experiments/123/landing/hero-01.webp"
}
```

---

## Estados e transições

- `PENDING` → `PROCESSING` (worker claim)
- `PROCESSING` → `GENERATED` (upload Cloudflare concluído)
- `GENERATED` → `WEBNIZING` (worker/webnizer claim)
- `WEBNIZING` → `COMPLETED` (URL pública web persistida)
- Qualquer estado ativo → `FAILED`

Regras adicionais:

- permitir retry de `FAILED` via ação explícita;
- impedir `claim` concorrente do mesmo job;
- manter auditoria de `worker_id`, timestamps e erro.

---

## Segurança e observabilidade

- Endpoints internos protegidos por token interno/chave de serviço.
- Logs com `jobId`, `experimentId`, `sectionId` e `workerId`.
- Métricas:
  - jobs criados por experimento;
  - tempo médio por etapa (`PROCESSING`, `WEBNIZING`);
  - taxa de falha por etapa.

---

## Roadmap incremental recomendado

1. **Fase 1 (Backend)**: entidade + endpoints de job + disparo manual pelo usuário.
2. **Fase 2 (Worker IA)**: scheduler + client backend + OpenAI batch + upload Cloudflare.
3. **Fase 3 (Webnização)**: polling de `GENERATED` + publicação web + callback backend.
4. **Fase 4 (Frontend/HTML)**: exibir URLs web e integrar no gerador de HTML.

Com isso, o fluxo completo solicitado passa a operar de ponta a ponta, de forma assíncrona e resiliente.
