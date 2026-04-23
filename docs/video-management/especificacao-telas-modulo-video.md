# Especificação de Telas — Módulo de Vídeo (`video-management-service`)

## 1. Objetivo

Definir as telas operacionais necessárias para permitir interação humana com o ciclo de vida de jobs de vídeo, mantendo o backend como fonte de verdade e o `video-management-service` como worker técnico de execução.

Este documento segue o eixo do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

- **Dor**: operação sem visibilidade de fila, falhas e tempo de processamento.
- **Resultado**: operação previsível, auditável e com menor tempo de resposta para incidentes.
- **Mecanismo**: painel de fila + detalhe + ações operacionais + observabilidade.
- **Prova**: métricas e histórico de execução por job/provider.
- **Oferta**: fluxo de vídeo confiável para sustentar entrega de produtos digitais.

---

## 2. Escopo

### 2.1 Incluído

- Telas administrativas no frontend principal do Marketing Hub.
- Consulta e acompanhamento de jobs de vídeo.
- Ações operacionais controladas (reprocessar, cancelar, expirar).
- Visualização de métricas operacionais e eventos de execução.

### 2.2 Fora de escopo

- Edição de conteúdo criativo profundo (roteiro completo, edição de mídia frame a frame).
- Player/editor de vídeo avançado.
- Integração direta frontend → banco de dados.

---

## 3. Perfis de usuário

1. **Operador de Conteúdo**
   - Monitora fila e valida conclusão de jobs.
2. **Gestor de Operações**
   - Acompanha SLAs, backlog e taxa de falhas.
3. **Suporte Técnico**
   - Diagnostica falhas e executa ações corretivas.

---

## 4. Jornada de interação (alto nível)

1. Usuário abre **Fila de Jobs de Vídeo**.
2. Filtra por status/provider/período.
3. Identifica job com problema ou atraso.
4. Abre **Detalhe do Job** para timeline e payloads relevantes.
5. Executa ação operacional permitida (reprocessar/cancelar/expirar).
6. Acompanha resultado no **Dashboard Operacional**.

---

## 5. Especificação das telas

## Tela A — Fila de Jobs de Vídeo

### Objetivo
Permitir triagem rápida da operação e priorização de incidentes.

### Componentes

- **Cards de resumo** (topo):
  - `VIDEO_REQUESTED` (quantidade)
  - `VIDEO_PROCESSING` (quantidade)
  - `VIDEO_READY` (últimas 24h)
  - `VIDEO_FAILED` (últimas 24h)
- **Filtros**:
  - Status
  - Provider (`STUB`, `REAL`, `HEYGEN`, `SYNTHESIA`, etc.)
  - Tenant
  - Período (`requestedAt` / `updatedAt`)
  - Prioridade
- **Tabela de jobs**:
  - Job ID
  - Profile ID
  - Status
  - Provider
  - Worker atual
  - Tentativas
  - Percentual de progresso
  - `requestedAt`
  - `updatedAt`
  - Duração acumulada
- **Ações por linha**:
  - Ver detalhe
  - Reprocessar (se elegível)
  - Cancelar (se elegível)

### Regras de UX

- Ordenação padrão: jobs mais críticos primeiro (`VIDEO_FAILED`, depois `VIDEO_PROCESSING` antigos).
- Destaque visual para jobs acima do SLA.
- Atualização automática (polling de UI configurável, ex.: 15s).
- Estados de vazio explícitos com orientação de próximo passo.

---

## Tela B — Detalhe do Job

### Objetivo
Dar rastreabilidade ponta a ponta de um job específico.

### Seções

1. **Resumo do job**
   - Job ID, status atual, provider, providerJobId, worker, tenant.
2. **Timeline de execução**
   - Eventos: `claim`, `heartbeat`, `progress`, `complete`, `fail`, `expired`.
   - Timestamp e mensagem de cada evento.
3. **Diagnóstico técnico**
   - `failureCode`, `failureDetail`, `retryable`, `retryReason` (quando houver falha).
4. **Assets vinculados**
   - IDs de asset (vídeo, poster, caption).
   - Links para download/preview (quando disponíveis).
5. **Dados de entrada relevantes**
   - `profileId`, `scriptId`, `providerName`, metadados normalizados.

### Ações

- Reprocessar job.
- Marcar como expirado.
- Cancelar job.
- Copiar JSON técnico para suporte.

### Regras de UX

- Mostrar primeiro “o que fazer agora” (callout operacional).
- Logs/eventos em ordem cronológica.
- Preservar legibilidade: esconder campos técnicos avançados atrás de “ver mais”.

---

## Tela C — Dashboard Operacional de Vídeo

### Objetivo
Fornecer visão macro de saúde do módulo para decisão rápida.

### Blocos principais

- **Backlog por status** (`VIDEO_REQUESTED`, `VIDEO_PROCESSING`).
- **Taxa de sucesso/falha** por janela de tempo.
- **Latência de render** (p50/p95/p99).
- **Retentativas com backend** por operação/status HTTP.
- **Falhas por provider e código de erro**.

### Alertas na interface

- Backlog acima de limiar operacional.
- Pico de `VIDEO_FAILED`.
- Aumento contínuo de retries `5xx/429`.

### Regras de UX

- Todo gráfico deve possuir definição objetiva (tooltip com fórmula e janela).
- Indicadores críticos devem aparecer acima da dobra.

---

## Tela D — Configuração Operacional (somente admin)

### Objetivo
Permitir ajuste seguro de comportamento operacional sem expor segredos.

### Campos configuráveis

- Habilitar/desabilitar polling do módulo.
- Intervalo de polling.
- Batch size de busca.
- Habilitar recuperação de órfãos e threshold.
- Modo provider real (feature flag).

### Regras obrigatórias

- Não exibir token sensível em texto aberto.
- Auditoria de alteração (quem alterou, quando, valor anterior/novo quando permitido).
- Alterações críticas exigem confirmação dupla.

---

## 6. Contratos de API necessários (backend)

> Observação: a UI deve consumir APIs do backend principal. Não haverá acesso direto ao banco.

### Leitura

- `GET /internal/video/jobs?status=&limit=`
- `GET /internal/video/jobs/{jobId}`
- `GET /internal/video/jobs/{jobId}/events` *(novo recomendado)*
- `GET /internal/video/jobs/metrics/summary` *(novo recomendado para cards/dashboard)*

### Ações operacionais

- `POST /internal/video/jobs/{jobId}/retry` *(novo recomendado)*
- `POST /internal/video/jobs/{jobId}/cancel` *(novo recomendado)*
- `POST /internal/video/jobs/{jobId}/expired` *(já existente no fluxo worker; avaliar exposição controlada para operação)*

### Segurança

- Escopos de autorização por papel: `video:read`, `video:operate`, `video:admin`.

---

## 7. Regras de negócio e governança de interface

1. Backend é a fonte de verdade para status e transições.
2. Frontend nunca deve inferir estado final de job fora dos contratos oficiais.
3. Ações operacionais devem validar elegibilidade no backend (nunca só na UI).
4. Toda ação crítica deve registrar trilha de auditoria.
5. Em caso de 422, seguir SOP canônico de diagnóstico com identificação exata do campo rejeitado.

---

## 8. Requisitos não funcionais

- **Performance**: listagem paginada com resposta alvo < 500ms em cenário nominal.
- **Observabilidade**: correlação por `jobId`, `profileId`, `provider`, `providerJobId`, `tenant`.
- **Confiabilidade**: revalidação de estado após ação operacional.
- **Usabilidade**: foco em informação essencial e comandos necessários, sem excesso cognitivo.

---

## 9. Critérios de aceite por tela

### Fila de Jobs

- Filtragem por status/provider/período funcionando.
- Ordenação por criticidade habilitada.
- Atualização periódica sem travamento da interface.

### Detalhe do Job

- Timeline completa dos eventos.
- Diagnóstico de erro visível e copiável.
- Ações exibidas somente quando elegíveis.

### Dashboard

- Métricas principais renderizadas com janela temporal selecionável.
- Alertas de limiar com destaque visual.

### Configuração

- Alterações aplicadas com confirmação e auditoria.
- Nenhum segredo exposto ao usuário.

---

## 10. Roadmap sugerido de implementação

1. **MVP Operacional (Sprint 1)**
   - Tela A (Fila)
   - Tela B (Detalhe)
   - Endpoint de eventos por job
2. **Consolidação Operacional (Sprint 2)**
   - Tela C (Dashboard)
   - Alertas visuais + filtros avançados
3. **Administração e governança (Sprint 3)**
   - Tela D (Configuração)
   - Auditoria de ações e permissões refinadas

---

## 11. Dependências técnicas e riscos

### Dependências

- Backend com endpoints de leitura e operação.
- Contrato de eventos por job padronizado.
- Permissões de acesso por perfil.

### Riscos

- Divergência entre payloads de UI e validações backend (risco de 422).
- Exposição indevida de dados técnicos/sensíveis.
- Sobrecarga de leitura sem paginação/caching adequado.

### Mitigações

- Contrato versionado e testes de contrato.
- Sanitização de dados sensíveis.
- Paginação, debounce de filtros e polling controlado.

---

## 12. Resultado esperado para o negócio

Com essas telas, a operação passa a ter clareza de fila, falhas e produtividade do pipeline de vídeo, reduzindo retrabalho e acelerando resposta a incidentes, com impacto direto na capacidade de entrega de ativos digitais confiáveis.
