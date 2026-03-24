# Arquitetura Canônica do Módulo de Avatar

- **Versão:** v1.1.0
- **Data de revisão:** 2026-03-24
- **Autor:** Codex (GPT-5.3-Codex)
- **Status:** approved

## 1) Objetivo (`v1.0.0`)

Definir a arquitetura canônica para um módulo plugável de **Avatar Management** no Marketing Hub, com foco em:

1. Orquestrar jobs assíncronos de renderização com múltiplos providers.
2. Ingerir o ativo final para storage controlado pelo cliente (evitando dependência de URL temporária de provider).
3. Garantir governança mínima para uso de avatar pessoal (consentimento + auditoria).
4. Permitir operação orientada a experimento (A/B), com rastreio de custo, latência e fallback por job.

---

## 2) Limites de Escopo (`v1.0.0`)

### 2.1 Incluído no escopo

- Contrato **provider-agnostic** para treinar avatar, renderizar, consultar status e obter link de download.
- Pipeline assíncrono com webhook/polling, retry com backoff+jitter e idempotência.
- Ingestão imediata de mídia para object storage do cliente + entrega por URL assinada.
- Modelo de estados de job (treino e render) com transições explícitas.
- Política de fallback entre providers.
- Requisitos mínimos de consentimento para personal replica, com `modelo`, `prompt` e trilha de auditoria.

### 2.2 Fora do escopo

- Definição de estratégia criativa (script, copy, oferta e guidelines editoriais).
- Editor de vídeo avançado no frontend.
- Garantia de performance de mídia (CTR/CVR/CPA) por si só.
- Estratégia jurídica completa por jurisdição (este documento define requisitos técnicos mínimos, não parecer legal).

---

## 3) Contrato do Provider Adapter (`v1.0.0`)

O adapter deve abstrair diferenças entre APIs de providers e expor uma interface única para o orquestrador.

### 3.1 Contrato funcional mínimo

```ts
interface ProviderAdapter {
  // Gestão de avatares
  listAvatars(tenantId: string, filters?: AvatarFilters): Promise<Avatar[]>;
  createAvatar(tenantId: string, spec: AvatarSpec): Promise<Avatar>;
  trainReplica(
    tenantId: string,
    trainingSpec: TrainingSpec,
    consentArtifact: ConsentArtifact
  ): Promise<TrainingJobRef>;
  getTrainingStatus(trainingJobId: string): Promise<TrainingStatus>;

  // Renderização
  renderVideo(spec: RenderJobSpec): Promise<ProviderRenderRef>;
  getRenderStatus(providerJobId: string): Promise<RenderStatus>;
  getDownloadLink(providerJobId: string): Promise<ExpiringUrl>;
  cancel(providerJobId: string): Promise<void>;

  // Webhook
  validateWebhook(headers: unknown, body: unknown): Promise<boolean>;
  parseWebhook(body: unknown): Promise<ProviderWebhookEvent>;

  // Capacidades
  capabilities(): ProviderCapabilities;
}
```

### 3.2 Requisitos não funcionais do adapter

- **Idempotência:** operações críticas devem aceitar `idempotency_key`.
- **Observabilidade:** logs estruturados com `tenant_id`, `job_id`, `provider`, `attempt`, `latency_ms`, `error_code`.
- **Segurança:** segredos em secret manager; proibição de token/URL assinada em log.
- **Compatibilidade:** payload normalizado no domínio interno antes de mapear para API externa.

---

## 4) Estados do Job (`v1.0.0`)

### 4.1 Estados canônicos de RenderJob

- `CREATED`
- `QUEUED`
- `DISPATCHING`
- `PROVIDER_RUNNING`
- `WAITING_CALLBACK`
- `POLLING`
- `DOWNLOADING`
- `INGESTING`
- `READY`
- `FAILED`
- `CANCELED`

### 4.2 Regras de transição

- `CREATED -> QUEUED -> DISPATCHING -> PROVIDER_RUNNING`
- Se provider suporta webhook: `PROVIDER_RUNNING -> WAITING_CALLBACK`
- Se provider não suporta webhook confiável: `PROVIDER_RUNNING -> POLLING`
- Job concluído no provider: `WAITING_CALLBACK|POLLING -> DOWNLOADING -> INGESTING -> READY`
- Erro recuperável: transição para retry (`DISPATCHING` ou `POLLING`) com incremento de tentativa.
- Erro não recuperável/limite excedido: `FAILED`.
- Cancelamento explícito: `CANCELED` (se provider aceitar cancelamento).

### 4.3 Estados canônicos de TrainingJob

- `CREATED`, `QUEUED`, `RUNNING`, `READY`, `FAILED`, `CANCELED`.

### 4.4 Regras de auditoria de estado

Cada transição deve persistir: `from_state`, `to_state`, `timestamp`, `actor`, `reason`, `provider_context`.

---

## 5) Política de Fallback (`v1.0.0`)

### 5.1 Objetivo

Maximizar taxa de sucesso respeitando custo/SLA/compliance do job.

### 5.2 Regras canônicas

1. Aplicar **hard gates** antes de score:
   - `requiresPersonalReplica=true` => somente providers com suporte e fluxo de consentimento.
   - `needsWebhook=true` => priorizar providers com callback confiável.
   - Restrições de duração/formato/idioma => eliminar incompatíveis.
2. Rankear candidatos por score ponderado (`QualityFit`, `CostFit`, `LatencyFit`, `ReliabilityFit`, `FeatureFit`).
3. Tentativa `0` usa melhor score.
4. Em falha transitória (timeout, 429, 5xx), aplicar retry com backoff+jitter e limite de tentativas.
5. Ao exceder tentativas no provider atual, trocar para próximo provider elegível.
6. Persistir no job: `fallback_used`, `fallback_from`, `fallback_to`, `fallback_reason`.

### 5.3 Condições de falha definitiva

- Sem provider elegível após hard gates.
- Excedido `maxAttempts` global do job.
- Falha de ingestão sem recuperação.
- Falha de compliance (ex.: personal replica sem consentimento válido).

---

## 6) Estratégia de Ingest para URLs Expiráveis (`v1.0.0`)

### 6.1 Princípio

**Nunca** depender de URL final do provider como origem de consumo de produção.

### 6.2 Pipeline obrigatório

1. Detectar conclusão do job (webhook ou polling).
2. Obter `download_url`.
3. Iniciar **download imediato em streaming**.
4. Validar arquivo (duração, codec, resolução, tamanho, integridade).
5. (Opcional) Transcodar variantes de compatibilidade.
6. Upload para storage do cliente (`tenant/campaign/job/asset`).
7. Registrar metadata/hash do ativo.
8. Expor URL assinada curta para consumo (UI/integrações).

### 6.3 Garantias mínimas

- Retenção e ciclo de vida controlados pelo cliente.
- Capacidade de revogação de acesso por expiração/rotação de assinatura.
- Reprocessamento idempotente por `asset_hash`.

---

## 7) Requisitos de Consentimento (`v1.0.0`)

### 7.1 Escopo de obrigatoriedade

Aplicável a qualquer fluxo com **personal replica** (avatar vinculado a pessoa real identificável).

### 7.2 Campos obrigatórios no registro de saída do Worker IA

Todo registro de entidade produzido por processo do Worker IA deve armazenar:

- `modelo` (identificação do modelo/fornecedor usado)
- `prompt` (entrada textual efetivamente utilizada)

> Esses campos são mandatórios no momento da criação do registro.

### 7.3 ConsentArtifact mínimo

- `consent_artifact_id`
- `person_id` (ou identificador equivalente)
- `consent_text_version`
- `consent_recorded_at`
- `consent_recording_hash`
- `captured_by`
- `provider`
- `revocation_status`
- `revoked_at` (quando aplicável)

### 7.4 Trilha de auditoria mínima

- Evento de coleta de consentimento.
- Evento de treino de replica vinculado ao consentimento.
- Evento de renderização vinculado à versão do avatar.
- Evento de publicação/uso do asset.
- Evento de revogação/deleção (quando ocorrer).

### 7.5 Regras de bloqueio

- Sem `ConsentArtifact` válido => bloquear `trainReplica`.
- Sem `modelo` e `prompt` => bloquear persistência da saída de IA.
- Consentimento revogado => bloquear novos renders com a replica associada.

---

## 8) Versionamento e Governança do Documento (`v1.0.0`)

- Este documento é a **fonte canônica** para arquitetura do módulo de avatar.
- Mudanças arquiteturais devem atualizar:
  1. Seção impactada.
  2. Versão do documento.
  3. **Decision Log**.
- Formato de versão: `MAJOR.MINOR.PATCH`.

---

## 9) Decision Log

| ID | Data | Decisão | Impacto |
|---|---|---|---|
| DL-001 | 2026-03-24 | Definido `avatar-module-architecture.md` como documento canônico derivado do deep research report. | Reduz divergência de entendimento entre times e concentra referência arquitetural em um único arquivo. |
| DL-002 | 2026-03-24 | Adotado contrato provider-agnostic obrigatório (`ProviderAdapter`) com capacidades e webhook parsing. | Facilita troca/adição de providers sem reescrever o domínio interno. |
| DL-003 | 2026-03-24 | Estados de job de render e treino padronizados com transições auditáveis. | Aumenta previsibilidade operacional, debugging e rastreabilidade. |
| DL-004 | 2026-03-24 | Fallback multi-provider definido com hard gates de compliance e score técnico-econômico. | Melhora resiliência e protege SLA/custo sem violar requisitos de consentimento. |
| DL-005 | 2026-03-24 | Pipeline de ingest imediato para URLs expiráveis tornou-se obrigatório. | Evita perda de ativos por expiração e garante controle de armazenamento no cliente. |
| DL-006 | 2026-03-24 | Tornado obrigatório persistir `modelo` e `prompt` em todo registro produzido por IA, com trilha de auditoria e bloqueio por ausência. | Fortalece compliance, auditoria e investigação posterior de incidentes. |

