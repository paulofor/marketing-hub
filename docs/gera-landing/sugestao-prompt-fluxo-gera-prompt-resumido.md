# Prompt reduzido — Fluxo 01 (Worker AI / Gera Landing)

Implemente em uma única execução o Fluxo 01 de geração de prompt do Gera Landing no repositório Marketing Hub, mantendo aderência aos cânones e ao modelo de dados vigente.

## Regras obrigatórias
- Preservar eixo: Dor → Resultado → Mecanismo → Prova → Oferta.
- Banco MySQL 5.7: somente backend acessa DB.
- Worker AI integra apenas via API HTTP interna do backend.
- Reusar contratos existentes; se faltar, criar endpoint + testes + documentação.
- Evitar JSON dentro de JSON em campo texto.

## Escopo técnico
1. **Polling no Worker**
   - Cron configurável `geralanding.execution.fixed-cron` (default `0 */1 * * * *`).
   - Limite `geralanding.execution.pending-limit` (default 20, mínimo 1).
   - Ler `GET /api/internal/geralanding/stage-executions/pending?limit={n}`.

2. **Contrato de pendências**
   - Campos obrigatórios: `experimentId`, `idJob`, `stageCode`.
   - Item inválido: ignorar com log estruturado.

3. **Filtro de etapa**
   - Normalizar `stageCode` (`trim + lowerCase`).
   - Processar apenas `landing-page-wireframe`.

4. **Montagem do prompt**
   - Template base `prompts/geralanding/{etapa}.md`.
   - Resolver `{prompt-*}` (recursivo) e `{dados-*}` (JSON legível).
   - Bloquear referência circular com erro explícito.

5. **Callback para backend**
   - `POST /api/internal/geralanding/stage-executions/{idJob}/receive-prompt`.
   - Usar exatamente o mesmo `idJob` recebido no `/pending`.
   - Body: `experimentId`, `stageCode`, `prompt`.

6. **Persistência e status no backend**
   - Buscar por `idJob` (primário), fallback `(experimentId, stageCode)`.
   - Salvar `prompt`, preencher `processing_started_at`, atualizar para `EM_PROCESSAMENTO`.
   - Responder `202 Accepted`.

7. **Schema/Liquibase**
   - Garantir `id_job` PK e `prompt/prompt_content` em LONGTEXT.
   - Se necessário, migration Liquibase YAML com preconditions MySQL.

8. **Observabilidade**
   - Logar `experimentId`, `idJob`, `stageCode`, resultado e tempo.
   - Em erro HTTP, logar URL, status e body.

## Testes obrigatórios
- Worker: mesmo `idJob` no callback, filtro por etapa, skip inválido, proteção contra ciclo.
- Backend: controller `/pending` e `/receive-prompt`; service para persistência + transição `EM_PROCESSAMENTO`.
- Atualizar testes afetados por contratos/regras canônicas.

## Uso de MCP (quando aplicável)
- Usar MCP Server `https://mcpserverdigi.shop/mcp` para checagens operacionais (db_health/logs).
- Em `400`: comparar requisição com DTO/contrato.
- Em `422`: reportar literal entregue vs esperado, diferença exata, validação que rejeitou e ação corretiva.

## Entregáveis
- Código backend + worker funcional.
- Testes passando nos módulos alterados.
- Atualização de `docs/gera-landing/modelo-canonico-gera-landing.md` e `docs/gera-landing/registros.md`.
- Resumo final com arquivos alterados, evidências de teste e riscos/pendências.
