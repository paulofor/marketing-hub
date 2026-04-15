# OPRM — Versionamento de Contrato Backend ↔ Worker

## 1. Objetivo

Definir a estratégia mínima de versionamento do contrato HTTP entre backend principal e worker OPRM, iniciada na Sprint 1.

## 2. Convenção de versão

- Série inicial: **v1**
- Valor de contrato no payload: `contractVersion: "1.0"`
- Documento canônico OpenAPI da série: `docs/novos-modulos/OPRM/contracts/oprm-backend-integration-openapi.v1.yaml`

## 3. Regras de compatibilidade

### 3.1 Mudança compatível
Exemplos:
- adição de campo opcional
- adição de enum sem quebrar consumidores existentes
- ampliação de metadados opcionais

Ação:
- mantém caminho da API (`/api/oprm/*`)
- incrementa versão menor (`1.0` → `1.1`)
- atualiza OpenAPI v1 e histórico de implementação

### 3.2 Mudança incompatível
Exemplos:
- remoção de campo obrigatório
- troca de semântica em campo existente
- mudança de tipo incompatível
- alteração de transição de estados de job

Ação:
- cria nova série principal (v2)
- publica novo arquivo OpenAPI (`...openapi.v2.yaml`)
- mantém janela de convivência entre v1 e v2
- registra ADR quando houver impacto cross-domain

## 4. Regra de validação runtime

- Backend deve validar `contractVersion` em endpoints de entrada do worker (`claim`, `status`, `heartbeat`).
- Worker deve registrar erro operacional quando receber `422` por incompatibilidade de versão.
- Chamadas com contrato incompatível não devem ser processadas parcialmente.

## 5. Política de erro HTTP da Sprint 1

- `200`: resposta síncrona de leitura ou claim bem-sucedido
- `202`: operação assíncrona aceita (status/artifact/feedback/heartbeat)
- `204`: claim sem job disponível
- `404`: job inexistente
- `409`: conflito de lock/transição/idempotência
- `422`: payload inválido ou versão incompatível
- `5xx`: falha interna do backend

## 6. Governança

- O contrato OpenAPI é a referência primária para integração backend ↔ OPRM.
- DTOs comuns em `oprm/src/main/java/com/marketinghub/oprm/integration/contract` devem permanecer alinhados ao OpenAPI.
- Nenhum endpoint novo deve ser adicionado sem atualização simultânea de OpenAPI, histórico e documentação de versão.
