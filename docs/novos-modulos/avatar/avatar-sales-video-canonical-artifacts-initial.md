# Avatar Sales Video — Documento Canônico Inicial de Artefatos

## 1. Finalidade

Este documento define o **cânone inicial de artefatos** do módulo **Avatar Sales Video** no Marketing Hub.

Ele existe para:

- padronizar os artefatos produzidos e consumidos pelo módulo;
- reduzir ambiguidades entre backend, `ai-worker`, `video-management-service` e frontend;
- permitir versionamento, lineage, validação e evolução controlada;
- orientar o Codex em mudanças sem quebrar contratos já definidos;
- separar artefatos de domínio de artefatos técnicos transitórios.

Este documento é **inicial**.  
Ele define o primeiro conjunto canônico de artefatos necessários para operar o módulo com segurança e evoluí-lo sem drift semântico.

---

## 2. Relação com outros documentos canônicos

Este documento deve ser lido em conjunto com:

- `docs/canonical/system-governance-canon.v2.md`
- `docs/avatar/avatar-sales-video-canonical-rules.md`

## Regra obrigatória

O documento de regras do módulo prevalece em caso de dúvida sobre:

- fronteiras de responsabilidade;
- persistência;
- fluxo entre serviços;
- publicação;
- compliance;
- rollout;
- anti-padrões.

Este documento trata do **cânone de artefatos**, não substitui o documento canônico de regras.

---

## 3. Princípios do cânone de artefatos

## 3.1 Artefato não é tabela

Um artefato canônico é uma **unidade semântica tipada e versionada do domínio ou da operação do módulo**.

A persistência relacional pode representar esse artefato em uma ou mais tabelas, mas:

- a tabela **não redefine** o significado do artefato;
- o backend persiste e serve o artefato;
- o significado canônico do artefato é definido neste documento.

## 3.2 Backend como fonte de verdade operacional

O backend é a **fonte de verdade operacional e persistente** dos artefatos do módulo.

Isso significa que:

- workers não têm estado canônico próprio;
- providers externos não definem o estado final do domínio;
- o frontend não cria artefatos canônicos fora do backend.

## 3.3 Schema-first

Todo artefato canônico deve ter:

- tipo explícito;
- versão explícita;
- envelope comum;
- payload com estrutura conhecida;
- regras mínimas de lineage;
- campo ou mecanismo de status quando aplicável.

## 3.4 Artefatos devem ser estáveis o suficiente para consumo

Artefatos devem poder ser consumidos por:

- backend;
- `ai-worker`;
- `video-management-service`;
- frontend administrativo;
- runtime público da landing;

sem depender de interpretação informal de texto.

## 3.5 Artefatos de domínio e artefatos transitórios não são a mesma coisa

Nem toda resposta de provider deve virar artefato canônico.

### Deve virar artefato canônico:
- o que tem valor de domínio;
- o que precisa ser auditado;
- o que precisa ser versionado;
- o que precisa ser exibido, reaproveitado ou publicado.

### Não deve virar artefato canônico:
- qualquer payload transitório sem valor durável;
- respostas efêmeras sem necessidade de reuso;
- dados técnicos temporários que só existem durante uma chamada.

---

## 4. Envelope canônico comum

Todo artefato canônico do módulo deve seguir um envelope lógico comum.

## 4.1 Campos comuns obrigatórios

- `artifactId`
- `artifactType`
- `schemaVersion`
- `module`
- `tenantId`
- `createdAt`
- `updatedAt`
- `producedBy`
- `lineage`
- `payload`

## 4.2 Campos comuns recomendados

- `statusLabel`
- `productId`
- `landingPageId`
- `profileId`
- `scriptId`
- `jobId`
- `slotId`
- `correlationId`
- `notes`
- `checks`

## 4.3 Estrutura lógica recomendada

```json
{
  "artifactId": "uuid-or-stable-id",
  "artifactType": "avatar.salesVideoProfile.v1",
  "schemaVersion": "1.0.0",
  "module": "avatar-sales-video",
  "tenantId": "tenant-x",
  "productId": 123,
  "landingPageId": 456,
  "profileId": 789,
  "jobId": null,
  "slotId": null,
  "statusLabel": "ACTIVE",
  "createdAt": "2026-04-16T00:00:00Z",
  "updatedAt": "2026-04-16T00:00:00Z",
  "correlationId": "corr-xyz",
  "producedBy": {
    "component": "backend/ads-service",
    "actorType": "system",
    "actorId": "sales-video-service"
  },
  "lineage": {
    "parentArtifactIds": [],
    "sourceJobIds": [],
    "sourceAssetIds": []
  },
  "checks": {
    "validated": true,
    "approved": false
  },
  "payload": {}
}
```

---

## 5. Convenção de naming

## 5.1 Padrão obrigatório

Formato recomendado:

`avatar.<artifactName>.v<major>`

### Exemplos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`
- `avatar.salesVideoStoryboard.v1`
- `avatar.salesVideoRenderRequest.v1`
- `avatar.salesVideoRenderJob.v1`
- `avatar.salesVideoJobEvent.v1`
- `avatar.salesVideoAssetBundle.v1`
- `avatar.landingVideoSlotBinding.v1`
- `avatar.landingVideoSlotHistorySnapshot.v1`
- `avatar.salesVideoComplianceRecord.v1`

## 5.2 Regras de naming

- usar nomes de domínio claros;
- evitar nomes genéricos como `data`, `result`, `payloadFinal`;
- distinguir artefato de solicitação, execução, resultado e publicação;
- distinguir artefato canônico de objeto técnico transitório.

---

## 6. Labels de ciclo de vida do artefato

Os labels abaixo são do **artefato**, não necessariamente do job.

## Labels recomendados

- `DRAFT`
- `READY`
- `APPROVED`
- `ACTIVE`
- `PUBLISHED`
- `FAILED`
- `ARCHIVED`

## Regras

- nem todo artefato precisa usar todos os labels;
- jobs continuam tendo seu próprio conjunto de estados operacionais;
- `statusLabel` de artefato não deve substituir o status operacional do job;
- quando houver ambos, deve ficar claro qual é o status do artefato e qual é o status da execução.

---

## 7. Lineage canônico

Todo artefato relevante deve deixar trilha mínima de origem.

## 7.1 Campos mínimos de lineage

- `parentArtifactIds`
- `sourceJobIds`
- `sourceAssetIds`

## 7.2 Regras

- artefatos derivados devem apontar para os artefatos pais;
- se um artefato nasce de um job, o `jobId` ou `sourceJobIds` deve ser preservado;
- se um artefato representa publicação de asset, os assets usados devem poder ser rastreados;
- lineage deve permitir auditoria sem depender de inferência manual.

---

# 8. Catálogo inicial de artefatos canônicos

---

## 8.1 `avatar.salesVideoProfile.v1`

### Finalidade
Representa a configuração canônica de um tipo de vídeo de vendas para uma oferta, produto ou landing.

### Produzido por
- backend

### Consumido por
- backend
- frontend
- `ai-worker`
- `video-management-service`

---

## 8.10 `avatar.salesVideoCommercialPlaybook.v1`

### Finalidade
Representa variações comerciais iniciais (objeção + CTA) por nicho para orientar iterações da Sprint V7.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo

### Campos mínimos recomendados
- `profileId`
- `tenantId`
- `nicheKey`
- `variantKey`
- `objectionText`
- `ctaText`
- `active`

---

## 8.11 `avatar.salesVideoConversionEvent.v1`

### Finalidade
Representa fato de conversão associado ao módulo para aprendizado comercial e comparação entre script/provider.

### Produzido por
- backend (via endpoint canônico de ingestão)

### Consumido por
- backend
- frontend administrativo

### Campos mínimos recomendados
- `profileId`
- `jobId` (opcional)
- `scriptId` (opcional)
- `tenantId`
- `eventType`
- `eventValue`
- `currencyCode`
- `occurredAt`
- `source`

---

## 8.12 `avatar.salesVideoPerformanceSummary.v1`

### Finalidade
Projeção agregada para revisão comercial inicial da Sprint V7, consolidando eventos totais, leads, compras e receita por variação.

### Produzido por
- backend

### Consumido por
- frontend administrativo
- operação comercial

### Campos mínimos recomendados
- `profileId`
- `tenantId`
- `totalEvents`
- `totalLeads`
- `totalPurchases`
- `totalRevenue`
- `variants[]` com `scriptId`, `providerName`, `variantKey`, `events`, `leads`, `purchases`, `revenue`

### Pais típicos
- nenhum obrigatório

### Payload mínimo recomendado

```json
{
  "videoKind": "HERO",
  "title": "Vídeo principal da landing",
  "language": "pt-BR",
  "targetDurationSeconds": 30,
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "personaStyle": "consultivo",
  "voiceStyle": "natural",
  "landingPageId": 456,
  "productId": 123
}
```

### Invariantes
- deve existir antes de qualquer geração de script ou render;
- é a âncora principal do fluxo;
- não representa o job;
- não representa o asset final.

---

## 8.2 `avatar.salesVideoScriptVersion.v1`

### Finalidade
Representa uma versão editorial do script do vídeo, com seus campos principais aprováveis e auditáveis.

### Produzido por
- `ai-worker` via backend
- operador humano via backend

### Consumido por
- backend
- frontend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "version": 3,
  "source": "OPENAI",
  "model": "gpt-4o",
  "scriptText": "texto completo do script",
  "hookText": "gancho inicial",
  "ctaText": "chamada para ação",
  "captionText": "legenda resumida",
  "approval": {
    "approved": true,
    "approvedBy": "user@example.com",
    "approvedAt": "2026-04-16T00:00:00Z"
  }
}
```

### Invariantes
- deve ser versionado;
- render produtivo deve depender de versão aprovada;
- não deve ser sobrescrito de forma destrutiva;
- deve preservar rastreabilidade de origem.

---

## 8.3 `avatar.salesVideoStoryboard.v1`

### Finalidade
Representa o storyboard estruturado da peça, quando existir, para orientar render, composição de cena ou validação editorial.

### Produzido por
- `ai-worker` via backend
- backend em fluxo manual estruturado

### Consumido por
- backend
- frontend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`

### Payload mínimo recomendado

```json
{
  "scenes": [
    {
      "sceneId": "scene-1",
      "purpose": "hook",
      "speechText": "texto falado",
      "visualIntent": "avatar em destaque",
      "durationSeconds": 5
    }
  ],
  "totalDurationSeconds": 30
}
```

### Invariantes
- storyboard não substitui o script completo;
- cenas devem ser rastreáveis por ordem e propósito;
- se existir, deve ser coerente com a versão de script usada no render.

---

## 8.4 `avatar.salesVideoRenderRequest.v1`

### Finalidade
Representa a intenção canônica de renderizar uma peça a partir de um perfil e de um script aprovado.

### Produzido por
- backend

### Consumido por
- backend
- `video-management-service`

### Pais típicos
- `avatar.salesVideoProfile.v1`
- `avatar.salesVideoScriptVersion.v1`
- opcionalmente `avatar.salesVideoStoryboard.v1`

### Payload mínimo recomendado

```json
{
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "requestedBy": "user@example.com",
  "requestedAt": "2026-04-16T00:00:00Z",
  "scriptVersion": 3,
  "renderMode": "PRODUCTION"
}
```

### Invariantes
- não é o job em si;
- não representa progresso;
- é o artefato de solicitação que dá origem ao job.

---

## 8.5 `avatar.salesVideoRenderJob.v1`

### Finalidade
Representa a unidade canônica de execução assíncrona do render.

### Produzido por
- backend

### Consumido por
- backend
- `video-management-service`
- frontend

### Pais típicos
- `avatar.salesVideoRenderRequest.v1`

### Payload mínimo recomendado

```json
{
  "jobType": "RENDER",
  "status": "VIDEO_REQUESTED",
  "providerFamily": "EXTERNAL_VIDEO_MODULE",
  "providerName": "provider-x",
  "providerJobId": null,
  "progressPercent": 0,
  "retryAttempt": 1,
  "failureCode": null,
  "failureDetail": null,
  "requestedAt": "2026-04-16T00:00:00Z",
  "startedAt": null,
  "finishedAt": null,
  "expiresAt": null
}
```

### Invariantes
- status operacional do render pertence aqui;
- o provider pode influenciar, mas não dita o estado final do domínio;
- retries devem ser auditáveis;
- histórico deve ser preservado por eventos, não por sobregravação sem trilha.

---

## 8.6 `avatar.salesVideoJobEvent.v1`

### Finalidade
Representa a trilha de eventos do job para auditoria, operação e diagnóstico.

### Produzido por
- backend
- opcionalmente a partir de reportes vindos de workers

### Consumido por
- backend
- frontend
- operação técnica

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "eventType": "PROGRESS_REPORTED",
  "oldStatus": "VIDEO_REQUESTED",
  "newStatus": "VIDEO_PROCESSING",
  "message": "provider aceitou o render",
  "details": {
    "progressPercent": 35
  },
  "occurredAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- eventos devem ser cumulativos;
- eventos não substituem o job;
- eventos precisam ser suficientes para reconstruir a história operacional.

---

## 8.7 `avatar.salesVideoProviderExecution.v1`

### Finalidade
Representa o registro técnico da execução junto ao provider externo, quando for necessário preservar detalhes operacionais além do job.

### Produzido por
- `video-management-service` via backend

### Consumido por
- backend
- operação técnica

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "providerName": "provider-x",
  "providerJobId": "ext-123",
  "requestFingerprint": "hash-ou-fingerprint",
  "externalStatus": "processing",
  "externalMetadata": {
    "region": "us-east-1"
  },
  "lastSyncedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- este artefato é técnico e secundário;
- ele não substitui o job canônico;
- só deve existir quando agregar valor de auditoria/operação.

---

## 8.8 `avatar.salesVideoAssetBundle.v1`

### Finalidade
Representa o conjunto canônico de assets resultantes de um render que ficaram disponíveis no backend.

### Produzido por
- backend, após ingestão de assets enviados pelo `video-management-service`

### Consumido por
- backend
- frontend
- landing pública

### Pais típicos
- `avatar.salesVideoRenderJob.v1`

### Payload mínimo recomendado

```json
{
  "videoAssetId": 1001,
  "posterAssetId": 1002,
  "captionAssetId": 1003,
  "assetUrls": {
    "video": "/assets/1001",
    "poster": "/assets/1002",
    "caption": "/assets/1003"
  },
  "technicalMetadata": {
    "providerName": "provider-x",
    "providerJobId": "ext-123"
  }
}
```

### Invariantes
- publicação deve depender deste artefato ou de seus equivalentes canônicos;
- URLs efêmeras de provider não devem ser tratadas como publicação final;
- vídeo, poster e legenda devem permanecer rastreáveis como assets distintos.

---

## 8.9 `avatar.landingVideoSlotBinding.v1`

### Finalidade
Representa a associação canônica entre um bundle de vídeo pronto e um slot publicado na landing.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo
- runtime público da landing

### Pais típicos
- `avatar.salesVideoAssetBundle.v1`
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "landingPageId": 456,
  "slotName": "hero-video",
  "assetBundleRef": {
    "videoAssetId": 1001,
    "posterAssetId": 1002,
    "captionAssetId": 1003
  },
  "playback": {
    "autoplay": true,
    "muted": true,
    "loop": false,
    "controlsEnabled": true,
    "lazyLoad": true
  },
  "publishedBy": "user@example.com",
  "publishedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- a landing pública só deve depender do que estiver ligado canonicamente ao slot;
- o slot é artefato de publicação, não de render;
- o slot não deve apontar para provider diretamente.

---

## 8.10 `avatar.landingVideoSlotHistorySnapshot.v1`

### Finalidade
Representa o snapshot histórico de alteração/publicação de um slot.

### Produzido por
- backend

### Consumido por
- backend
- frontend administrativo
- operação/auditoria

### Pais típicos
- `avatar.landingVideoSlotBinding.v1`

### Payload mínimo recomendado

```json
{
  "slotName": "hero-video",
  "snapshotType": "PUBLISHED",
  "previousAssetBundleRef": null,
  "currentAssetBundleRef": {
    "videoAssetId": 1001,
    "posterAssetId": 1002,
    "captionAssetId": 1003
  },
  "changedBy": "user@example.com",
  "changedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- histórico deve ser cumulativo;
- snapshots devem permitir reconstituir a linha do tempo da publicação;
- o histórico não deve depender de inferência sobre logs soltos.

---

## 8.11 `avatar.salesVideoComplianceRecord.v1`

### Finalidade
Representa o registro mínimo de compliance e consentimento associado ao uso produtivo do avatar, quando aplicável.

### Produzido por
- backend
- fluxo administrativo/manual

### Consumido por
- backend
- frontend administrativo
- `video-management-service` apenas via decisão do backend

### Pais típicos
- `avatar.salesVideoProfile.v1`

### Payload mínimo recomendado

```json
{
  "consentRequired": true,
  "consentCaptured": true,
  "consentReference": "doc-or-record-id",
  "reviewRequired": true,
  "reviewCompleted": true,
  "reviewedBy": "user@example.com",
  "reviewedAt": "2026-04-16T00:00:00Z"
}
```

### Invariantes
- render produtivo com avatar pessoal não deve ignorar este registro quando exigido;
- compliance não deve ser inferido apenas por convenção;
- a ausência deste artefato pode bloquear o fluxo, quando a política exigir.

---

# 9. Relações canônicas entre artefatos

## 9.1 Cadeia principal do fluxo

```text
salesVideoProfile
  -> salesVideoScriptVersion
      -> salesVideoStoryboard (opcional)
          -> salesVideoRenderRequest
              -> salesVideoRenderJob
                  -> salesVideoJobEvent(s)
                  -> salesVideoProviderExecution (opcional)
                  -> salesVideoAssetBundle
                      -> landingVideoSlotBinding
                          -> landingVideoSlotHistorySnapshot(s)
```

## 9.2 Regras

- `salesVideoProfile` é a âncora do fluxo;
- `salesVideoScriptVersion` é versionado e reutilizável;
- `salesVideoRenderRequest` antecede o job;
- `salesVideoRenderJob` é a unidade de execução assíncrona;
- `salesVideoAssetBundle` representa o resultado pronto no backend;
- `landingVideoSlotBinding` representa publicação;
- `landingVideoSlotHistorySnapshot` representa histórico de publicação;
- `salesVideoComplianceRecord` pode funcionar como gating transversal do fluxo.

---

## 10. O que ainda não entra no cânone inicial

Os itens abaixo podem virar artefatos canônicos no futuro, mas **não entram ainda no cânone inicial**:

- experimentação comercial avançada do módulo;
- artefatos de avaliação de performance por script/avatar;
- artefatos de playbook de objeção por nicho;
- artefatos de otimização de conversão;
- artefatos de analytics agregados;
- artefatos de treinamento avançado de avatar;
- artefatos de biblioteca global de prompts do módulo.

Esses itens podem ser adicionados em versões futuras do cânone, quando deixarem de ser apenas operação auxiliar e passarem a ser parte estável do domínio.

---

## 11. Regras de versionamento do cânone

## 11.1 Versão do artefato

Cada artefato deve ter:
- `artifactType` com versão major;
- `schemaVersion` explícita no envelope.

## 11.2 Mudanças compatíveis

Mudanças compatíveis podem:
- adicionar campos opcionais;
- enriquecer metadata;
- refinar payload sem quebrar consumidores existentes.

## 11.3 Mudanças incompatíveis

Mudanças incompatíveis devem:
- criar nova versão major do `artifactType`;
- registrar migração;
- atualizar consumidores.

---

## 12. Regras para o Codex ao implementar artefatos

O Codex deve:

- tratar este documento como fonte de definição semântica dos artefatos;
- não reduzir artefatos a DTOs arbitrários sem lineage;
- não mover o significado do artefato para nomes de tabela ou implementação local;
- registrar mudanças de contrato;
- manter compatibilidade quando possível;
- evitar criar artefato canônico para dado que é apenas transitório.

O Codex não deve:

- duplicar artefatos com nomes parecidos e sem justificativa;
- usar nomes genéricos demais;
- misturar status de job com status de artefato sem distinção;
- criar publication state fora do backend;
- criar “resultado final” sem distinguir solicitação, execução e publicação.

---

## 13. Critério de conformidade com este cânone

Uma implementação está em conformidade com este documento quando:

- usa os nomes e papéis canônicos definidos aqui;
- respeita o envelope comum;
- preserva lineage mínima;
- distingue artefatos de domínio, execução e publicação;
- mantém persistência via backend;
- não trata resposta efêmera de provider como estado canônico sem mediação do backend.

---

## 14. Próxima etapa recomendada

Depois deste documento, a próxima etapa natural é:

**definir os schemas concretos de cada artefato**  
em documentos ou arquivos específicos, por exemplo:

- `/schemas/avatar/sales-video-profile.schema.json`
- `/schemas/avatar/sales-video-script-version.schema.json`
- `/schemas/avatar/sales-video-render-job.schema.json`
- etc.

Este documento define o **cânone inicial**.  
Os schemas detalhados e os contratos de API devem refinar esse cânone, não contradizê-lo.

---
