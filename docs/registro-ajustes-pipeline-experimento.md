# Registro de Ajustes — Pipeline de Experimento

Este documento é o diário operacional do ciclo contínuo de testes e correções do pipeline de experimento.

## Objetivo

Registrar, de forma rastreável, cada erro identificado em execução, o diagnóstico completo (incluindo banco/logs via MCP Server), os ajustes aplicados e o resultado do novo teste.

## Ciclo operacional padrão

1. **Executar teste** do pipeline.
2. **Capturar erro** (stack trace, payload, endpoint, horário, contexto).
3. **Analisar logs e dados** via MCP Server (`https://mcpserverdigi.shop/mcp`, JSON-RPC).
4. **Comparar com documentação canônica**:
   - `docs/canonical/system-governance-canon.v2.md`
   - `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
5. **Aplicar ajuste** em código/contrato/prompt/mapeamento.
6. **Registrar evidências** do ajuste neste documento.
7. **Executar novo teste** e registrar resultado.
8. **Repetir ciclo** até estabilizar.

---

## Modelo obrigatório para registro de incidente

> Copiar este bloco para cada novo incidente.

### Incidente `INC-XXXX` — `<título curto>`

- **Data/Hora (UTC):**
- **Responsável:**
- **Módulo:** (backend / frontend / ai-worker / facebook-ads-worker / outro)
- **Ambiente:**
- **Execução/Teste:**

#### 1) Erro observado
- **Sintoma:**
- **Endpoint/rota/fila:**
- **Status code:**
- **Mensagem de erro literal:**
- **Payload enviado (literal):**

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:**
- **Dados de banco consultados via MCP:**
- **Trecho canônico consultado:**
- **Validação/regra que rejeitou:**
- **Causa raiz:**

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):**
- **O que a especificação esperava (literal):**
- **Diferença objetiva:**
- **Ação corretiva recomendada:**

#### 4) Ajuste aplicado
- **Tipo de ajuste:** (prompt / contrato / validação / mapeamento / SQL / outro)
- **Arquivos alterados:**
- **Resumo técnico da mudança:**

#### 5) Reteste
- **Procedimento de reteste:**
- **Resultado:**
- **Evidências (logs/ids/prints):**
- **Status final:** (Resolvido / Parcial / Pendente)

#### 6) Próximo passo
- **Ação seguinte:**
- **Dono da ação:**
- **Prazo:**

---

## Registro cronológico

> Novas entradas sempre no topo.

### INC-0010 — Verificação da tentativa `9f5de7ad` (LANDING_PAGE_HTML com divergência de binding de imagens)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend + ai-worker (contrato entre `landing-page-image-planning` e `landing-page-html`)
- **Ambiente:** Execução remota exibida no Marketing Hub (`experiments/15`)
- **Execução/Teste:** Tentativa `9f5de7ad` (erro registrado em `28/04/2026, 12:51:31 BRT`)

#### 1) Erro observado
- **Sintoma:** etapa `LANDING_PAGE_HTML` finalizou com 422 por quebra de vínculo canônico de imagens por seção.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de imagens: landing-page-html deve reproduzir o binding explícito canônico do landing-page-image-planning por sectionId/imageBindingKey`.
- **Payload enviado (literal):** `landingPageHtml.htmlDocument` concluído pelo worker, rejeitado no backend por divergência frente ao `landingPageImagePlanning.images[]`.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** `java_module_logs` (`ai-worker`, 500 linhas) retornou apenas cauda operacional recente; a janela da falha `12:51:31 BRT` já não estava mais presente no tail disponível.
- **Dados de banco consultados via MCP:** `experiment_pipeline_generation_job` confirmou job `LANDING_PAGE_HTML` (`model=gpt-5.2`, `status=FAILED`, `created_at=2026-04-28 15:49:00 UTC`) com rejeição 422 no endpoint `/api/internal/experiment-pipeline/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`; `experiment` confirmou planejamento de imagens persistido com 9 bindings canônicos para o experimento 15.
- **Trecho canônico consultado:** regra de vínculo explícito entre `landingPageImagePlanning.images[*].sectionId` + `images[*].imageBindingKey` e reprodução obrigatória no artefato `landing-page-html`.
- **Validação/regra que rejeitou:** validação determinística de binding por seção/imagem no backend ao concluir o job `LANDING_PAGE_HTML`.
- **Causa raiz provável:** o HTML gerado não refletiu 1:1 o mapa de imagens canônico do planejamento (seção sem chave esperada, chave trocada entre seções ou seção extra/ausente).

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML final que não reproduziu integralmente o binding explícito do plano de imagens (conforme mensagem 422).
- **O que a especificação esperava (literal):** para cada seção do plano, o HTML deve manter o par canônico `sectionId/imageBindingKey` sem substituições, omissões ou inversões.
- **Diferença objetiva:** divergência entre o binding esperado em `landing-page-image-planning` e o binding efetivamente materializado em `landing-page-html`.
- **Ação corretiva recomendada:** antes do `/complete`, validar localmente no worker a matriz `sectionId -> imageBindingKey` extraída do HTML contra o `landingPageImagePlanning.images[]` do payload do job; bloquear finalização quando houver qualquer delta.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** documentação operacional de incidente.
- **Arquivos alterados:**
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** registro formal da tentativa `9f5de7ad` com diagnóstico 422 no formato SOP obrigatório (modelo entregue vs esperado vs diferença vs ação corretiva).

#### 5) Reteste
- **Procedimento de reteste:** revalidação via MCP Server com `initialize`, `tools/list`, `db_query` e `java_module_logs`.
- **Resultado:** verificação concluída com sucesso no MCP (conectividade OK e evidências de banco coletadas); pendente apenas nova execução integrada para confirmar correção funcional.
- **Evidências (logs/ids/prints):** job com path `/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`, mensagem 422 literal e lista canônica de 9 pares `sectionId/imageBindingKey` do planejamento da landing no experimento 15.
- **Status final:** Pendente.

#### 6) Próximo passo
- **Ação seguinte:** reexecutar `LANDING_PAGE_HTML` no experimento 15 após validar previamente o binding `sectionId/imageBindingKey` contra o `landingPageImagePlanning` aprovado.
- **Dono da ação:** Time do AI Worker + operação do experimento.
- **Prazo:** imediato (próxima janela de reprocessamento).

#### 7) Confirmação MCP (nova tentativa)
- **MCP Server utilizado:** `https://mcpserverdigi.shop/mcp` (JSON-RPC; chamadas `initialize`, `tools/list`, `tools/call`).
- **Amostra 1 — job da tentativa `9f5de7ad`:**
  - `experiment_pipeline_generation_job` retornou `status=FAILED`, `section=LANDING_PAGE_HTML`, `model=gpt-5.2`, `created_at=2026-04-28 15:49:00 UTC`.
  - `error_message` confirmou 422 literal por divergência de binding de imagens e `path` com o identificador completo da tentativa: `/api/internal/experiment-pipeline/jobs/9f5de7ad-ea5f-4345-b877-7444a06992ee/complete`.
- **Amostra 2 — vínculo canônico esperado no experimento 15 (`experiment.landing_page_image_planning`):**
  - O artefato persistido contém 9 entradas em `landingPageImagePlanning.images[]`.
  - Pares canônicos extraídos via MCP:
    - `nav-identity -> nav-identity-strip`
    - `hero-split-form -> hero-whats-preco-pdf`
    - `pain-quanto-custa-some -> pain-cards-preco-sumi`
    - `mechanism-ciclo-evolucao-8s -> mechanism-timeline-8w`
    - `proof-previa-pdf-minikit -> proof-packshot-pdf-minikit`
    - `offer-gerar-amostra-pdf -> offer-3steps-flow`
    - `objection-anti-preco-pratica -> usage-whats-flow-cards`
    - `faq-objections -> faq-trust-privacy`
    - `footer-legal -> footer-legal-strip`
- **Conclusão confirmada com MCP:** a tentativa `9f5de7ad` foi rejeitada por não reproduzir fielmente, no HTML final, o binding explícito canônico por `sectionId/imageBindingKey` já aprovado no `landing_page_image_planning` do experimento 15.

### INC-0009 — Verificação da tentativa `b244db1f` (LANDING_PAGE_HTML com divergência de superfície)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend + ai-worker (integração de contrato)
- **Ambiente:** Execução remota exibida no Marketing Hub (`experiments/15`)
- **Execução/Teste:** Tentativa `b244db1f` (erro registrado em `28/04/2026, 10:14:37 BRT`)

#### 1) Erro observado
- **Sintoma:** etapa `LANDING_PAGE_HTML` terminou com erro de contrato de superfície.
- **Endpoint/rota/fila:** fechamento do job em `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec`.
- **Payload enviado (literal):** `landingPageHtml.htmlDocument` concluído pelo worker, porém rejeitado na validação determinística do backend.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não disponível neste ambiente local (sem servidor MCP configurado no container).
- **Dados de banco consultados via MCP:** não disponível neste ambiente local.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (contrato de superfície por seção para `landing-page-html`).
- **Validação/regra que rejeitou:** comparação exata entre superfícies esperadas do wireframe e superfícies encontradas no HTML final.
- **Causa raiz provável:** o HTML gerado para a tentativa não preservou correspondência 1:1 de `sectionId` + atributos `data-surface-*` exigidos pelo `sectionOrder.surfaceSpec` canônico.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML final aceito sintaticamente, mas com conjunto de superfícies divergente do wireframe aprovado.
- **O que a especificação esperava (literal):** todas as seções do `landingPageWireframe.sectionOrder` renderizadas no HTML com `data-section-id`, `data-surface-token`, `data-surface-style` e `data-surface-contrast` equivalentes.
- **Diferença objetiva:** divergência de superfície (falta/sobra de seção e/ou atributos de superfície diferentes do wireframe/preset por seção).
- **Ação corretiva recomendada:** regenerar `LANDING_PAGE_HTML` usando exatamente o wireframe/preset já aprovados e validar localmente a matriz `sectionId + surfaceSpec` antes do `/complete`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** documentação operacional de incidente.
- **Arquivos alterados:**
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** inclusão do registro formal da verificação da tentativa `b244db1f`, com diagnóstico estruturado 422 e ação corretiva orientada a contrato canônico.

#### 5) Reteste
- **Procedimento de reteste:** não executado neste container (somente registro e análise documental da tentativa remota).
- **Resultado:** pendente de nova execução integrada no ambiente alvo.
- **Evidências (logs/ids/prints):** print da UI com tentativa `b244db1f` e mensagem de erro de divergência de superfície.
- **Status final:** Pendente (aguarda nova tentativa após regeneração do HTML).

#### 6) Próximo passo
- **Ação seguinte:** disparar nova execução de `LANDING_PAGE_HTML` no experimento 15 após confirmar alinhamento estrito de `surfaceSpec` por seção.
- **Dono da ação:** Time do AI Worker + responsável da operação do experimento.
- **Prazo:** imediata (na próxima janela de reprocessamento).

#### 7) Confirmação MCP (amostras da execução mais recente do experimento 15)
- **MCP Server utilizado:** `https://mcpserverdigi.shop/mcp` (JSON-RPC, ferramentas `db_query` e `java_module_logs`).
- **Amostra 1 — tentativa com erro (`b244db1f-aa3d-41a5-9c65-5e68cf372016`):**
  - `experiment_pipeline_generation_job.error_message` retornou `422` com mensagem literal de divergência de superfície.
  - Em `request_body_json` da mesma tentativa (Prompt v2), o `landingPageWireframe.sectionOrder` trazia `footer-legal` com `contrastMode=normal`.
  - No mesmo prompt, `landingPageDesignPreset.sectionPresets` trazia `footer-legal` com `contrastMode=soft`.
- **Amostra 2 — estado mais recente persistido no experimento 15 (`experiment.landing_page_html`):**
  - Extração do HTML persistido mostrou `data-section-id="footer-legal"` com `data-surface-contrast="soft"`.
  - Isso confirma o contrato atual: `surfaceToken` vem do wireframe, enquanto `style/contrast` devem seguir o design preset por seção.
- **Conclusão confirmada com MCP:** a diferença crítica observada na execução recente foi a seção `footer-legal` (`normal` no wireframe vs `soft` no design preset), validando que o worker precisa priorizar preset para `style/contrast` na etapa `LANDING_PAGE_HTML`.

### INC-0008 — LANDING_PAGE_HTML não validava surfaceSpec quando prompt vinha no formato “Prompt v2”

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Tentativa `ca004a13-e78a-4ada-81d2-f7d4bbfe00ec` (experimento 15)

#### 1) Erro observado
- **Sintoma:** job `LANDING_PAGE_HTML` falhou no backend com `422` por divergência de superfície (`surfaceSpec`), apesar de o worker ter seguido para `/complete`.
- **Endpoint/rota/fila:** `POST /api/internal/experiment-pipeline/jobs/{jobId}/complete`.
- **Status code:** 422.
- **Mensagem de erro literal:** `Divergência de superfície: landing-page-html deve reproduzir exatamente landing-page-wireframe.sectionOrder.surfaceSpec`.
- **Payload enviado (literal):** conteúdo de `landingPageHtml` com `htmlDocument` de 36199 caracteres; logs do worker mostram seções enviadas sem correspondência exata 1:1 com `sectionOrder` esperado no wireframe.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** `java_module_logs` do `ai-worker` com job `ca004a13...` mostrando envio para `/complete` e retorno 422.
- **Dados de banco consultados via MCP:** `experiment_pipeline_generation_job` (erro 422) e `experiment.landing_page_wireframe` (surfaceSpec esperado por seção para o experimento 15).
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` e regra de contrato da etapa `landing-page-html` no prompt.
- **Validação/regra que rejeitou:** validação de igualdade exata `expectedSurfaces` vs `actualSurfaces` no backend.
- **Causa raiz:** no worker, a extração de `surfaceSpec` esperado do `request_body_json` dependia apenas do marcador textual `Wireframe da landing:`. No formato atual (“Prompt v2”, com `1) Wireframe aprovado (JSON):`), a extração retornava vazio e a validação preventiva do worker era pulada.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** HTML com conjunto de `data-section-id` diferente do `sectionOrder` canônico (faltando seção esperada e incluindo seção extra).
- **O que a especificação esperava (literal):** reprodução exata de `landingPageWireframe.sectionOrder[*].surfaceSpec` no HTML (`data-section-id`, `data-surface-token`, `data-surface-style`, `data-surface-contrast`) sem sobras/faltas.
- **Diferença objetiva:** worker não validou localmente devido falha de parsing do wireframe no prompt v2; backend validou e rejeitou.
- **Ação corretiva recomendada:** robustecer extração do wireframe no worker para múltiplos marcadores de prompt e fallback por chave `landingPageWireframe`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** parsing + validação preventiva + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** extração do JSON de wireframe agora aceita marcadores legados e do prompt v2 (`Wireframe aprovado (JSON)`), com fallback por chave `landingPageWireframe`; adicionada cobertura de teste para falha rápida no cenário de prompt v2 quando o HTML não reproduz `surfaceSpec`.

#### 5) Reteste
- **Procedimento de reteste:** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Resultado:** não executado com sucesso por dependência privada inacessível no ambiente (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`).
- **Evidências (logs/ids/prints):** erro Maven `401 Unauthorized` ao resolver artefato no GitHub Packages.
- **Status final:** Parcial (correção aplicada; reteste automatizado pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar a suíte de testes do `ai-worker` em ambiente autenticado e disparar nova tentativa de `LANDING_PAGE_HTML` para o experimento 15.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima janela de validação integrada.

### INC-0007 — LANDING_PAGE_HTML com contrato de saída em HTML puro (sem JSON)

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão de contrato para etapa `LANDING_PAGE_HTML`

#### 1) Erro observado
- **Sintoma:** retorno do modelo em JSON (ou JSON malformado) para uma etapa cujo artefato final precisa ser documento HTML completo.
- **Endpoint/rota/fila:** consumo de resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro de contrato no worker).
- **Mensagem de erro literal:** variava entre parsing JSON e quebra de contrato de conteúdo.
- **Payload enviado (literal):** respostas com envelope JSON contendo `htmlDocument` ao invés de resposta HTML pura.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado nesta revisão local.
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** artefato `LANDING_PAGE_HTML` em `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`.
- **Validação/regra que rejeitou:** validação local do worker para seção `LANDING_PAGE_HTML`.
- **Causa raiz:** contrato de prompt permitia deriva para JSON, aumentando risco de `json dentro de campo texto de json` e parse frágil.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** JSON (inclusive versões malformadas) com `landingPageHtml.htmlDocument`.
- **O que a especificação esperava (literal):** HTML puro completo (`<!doctype html> ... </html>`), com CSS/JS inline quando necessário.
- **Diferença objetiva:** envelope JSON em vez de documento HTML direto.
- **Ação corretiva recomendada:** exigir somente HTML puro no prompt e rejeitar qualquer resposta não-HTML para `LANDING_PAGE_HTML`.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato + validação + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/main/resources/prompts/experiment/landing-html.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** removido fallback de extração leniente de JSON para esta etapa; o worker agora aceita apenas HTML puro (ou bloco markdown com HTML), rejeitando retorno JSON para `LANDING_PAGE_HTML`; prompt atualizado para retornar exclusivamente HTML.

#### 5) Reteste
- **Procedimento de reteste:** execução de `ExperimentPipelineOpenAiClientTest`.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401).
- **Evidências (logs/ids/prints):** comando `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** validar em ambiente integrado que a etapa `LANDING_PAGE_HTML` responde consistentemente com HTML puro.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima execução do pipeline.

### INC-0006 — Bloqueio explícito de JSON serializado dentro de `landingPageHtml.htmlDocument`

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão pós-incidente `INC-0005` para prevenir `json dentro de campo texto de json`

#### 1) Erro observado
- **Sintoma:** risco de o modelo devolver `htmlDocument` com JSON serializado (ex.: `"{\"headline\":\"...\"}"`) em vez de HTML puro.
- **Endpoint/rota/fila:** consumo da resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro de contrato no worker).
- **Mensagem de erro literal:** n/a (ajuste preventivo orientado por revisão).
- **Payload enviado (literal):** cenário-alvo: `landingPageHtml.htmlDocument` contendo objeto JSON serializado como texto.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado nesta revisão local.
- **Dados de banco consultados via MCP:** não aplicável.
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (`LANDING_PAGE_HTML` exige documento HTML).
- **Validação/regra que rejeitou:** nova validação local no worker para recusar JSON serializado em `htmlDocument`.
- **Causa raiz:** instrução do prompt e validação não deixavam explícita a proibição de JSON stringificado dentro do campo textual.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `"{\"headline\":\"Plano de Conteúdo\",\"cta\":\"Quero a prévia\"}"` dentro de `htmlDocument`.
- **O que a especificação esperava (literal):** HTML puro completo em `htmlDocument` (`<!doctype html>...` / `<html ...>`).
- **Diferença objetiva:** JSON serializado em campo de texto destinado a HTML.
- **Ação corretiva recomendada:** reforçar prompt para serialização correta e bloquear no worker qualquer `htmlDocument` com aparência de JSON.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + validação + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/main/resources/prompts/experiment/landing-html.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionado bloqueio explícito de contrato para `htmlDocument` com JSON serializado; prompt da etapa `landing-html` ganhou regra explícita "não colocar JSON dentro de campo texto de JSON"; teste unitário incluído para garantir rejeição desse formato.

#### 5) Reteste
- **Procedimento de reteste:** execução da suíte `ExperimentPipelineOpenAiClientTest`.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401).
- **Evidências (logs/ids/prints):** comando `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` no módulo `ai-worker`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar suíte de testes do `ai-worker` em ambiente com acesso ao GitHub Packages e monitorar novas execuções da etapa `LANDING_PAGE_HTML`.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima janela de integração.

### INC-0005 — Falha em `LANDING_PAGE_HTML` por JSON inválido com quebra de linha literal no `htmlDocument`

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "HTML da Landing")

#### 1) Erro observado
- **Sintoma:** tentativa da etapa `LANDING_PAGE_HTML` finalizou com erro na desserialização do retorno do modelo.
- **Endpoint/rota/fila:** consumo da resposta da OpenAI no `ExperimentPipelineOpenAiClient`.
- **Status code:** n/a (erro interno do worker antes de chamada de conclusão no backend).
- **Mensagem de erro literal:** `Illegal unquoted character ((CTRL-CHAR, code 10)): has to be escaped using backslash to be included in string value`.
- **Payload enviado (literal):** resposta textual contendo objeto JSON com `landingPageHtml.htmlDocument` entre aspas, porém com quebra de linha literal dentro da string (sem escape JSON), por exemplo: `"...<head>...</head>\n<body>...` com quebra física de linha antes do fechamento da string.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste ajuste local; diagnóstico baseado no erro literal exibido na UI e no fluxo de parsing do worker.
- **Dados de banco consultados via MCP:** não aplicável (falha ocorre antes de persistência/validação do backend).
- **Trecho canônico consultado:** `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (seção `LANDING_PAGE_HTML`, exigência de `htmlDocument` válido).
- **Validação/regra que rejeitou:** parser JSON do worker (`ObjectMapper.readValue`) rejeitou string JSON malformada.
- **Causa raiz:** o modelo retornou `htmlDocument` como string JSON com quebra de linha literal não escapada, tornando o bloco inválido para parser estrito.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `{"landingPageHtml":{"htmlDocument":"<!DOCTYPE html>...<head>...</head>` + quebra de linha literal + `<body>...</body></html>"}}`.
- **O que a especificação esperava (literal):** JSON válido ou HTML puro; se usar JSON string, quebras devem estar escapadas (`\\n`) e aspas internas escapadas.
- **Diferença objetiva:** presença de caractere de nova linha bruto dentro de string JSON.
- **Ação corretiva recomendada:** aplicar extração leniente de `htmlDocument` para tolerar esse formato específico e preservar pipeline; manter pressão no prompt para retorno em HTML puro ou JSON estrito.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** parsing + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClient.java`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionado fallback leniente para extrair e decodificar `htmlDocument` quando o JSON do modelo vier malformado por quebra de linha literal em string; incluído teste unitário cobrindo este cenário.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário focado no cliente OpenAI do pipeline.
- **Resultado:** não executado integralmente no ambiente atual por dependência privada indisponível (`com.marketinghub:ads-service:0.0.1-SNAPSHOT`, HTTP 401 em GitHub Packages).
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` (módulo `ai-worker`) retornou `DependencyResolutionException` por `Unauthorized (401)` na resolução de `ads-service`.
- **Status final:** Parcial (correção aplicada; validação automatizada pendente em ambiente com credencial de pacote).

#### 6) Próximo passo
- **Ação seguinte:** monitorar novas execuções de `LANDING_PAGE_HTML` e, se reincidir, reforçar instrução do prompt para priorizar retorno em HTML puro.
- **Dono da ação:** Time do AI Worker.
- **Prazo:** próxima execução do experimento 15.

### INC-0004 — Ajuste de granularidade do schema de `landingPageDesignPreset` após revisão

- **Data/Hora (UTC):** 2026-04-28
- **Responsável:** Assistente Codex
- **Módulo:** backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Revisão do PR `Fix landing design preset schema to enforce canonical required tokens`

#### 1) Erro observado
- **Sintoma:** Ajuste anterior tornou o schema da etapa `LANDING_PAGE_DESIGN_PRESET` rígido em excesso para campos não bloqueantes da validação canônica de conclusão.
- **Endpoint/rota/fila:** geração de payload `response_format` para jobs `LANDING_PAGE_DESIGN_PRESET`.
- **Status code:** n/a (ajuste preventivo de contrato antes de execução).
- **Mensagem de erro literal:** n/a.
- **Payload enviado (literal):** n/a.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** referência do incidente anterior com 422 (`theme.spacing é obrigatório`) já confirmada.
- **Dados de banco consultados via MCP:** referência do incidente anterior para experimentId `15`.
- **Trecho canônico consultado:** regras de tokens mínimos do `landingPageDesignPreset` (tema mínimo + `componentPresets.cta`/`trust`) e validações de backend em `completeJob`.
- **Validação/regra que rejeitou:** previamente, `theme.spacing` ausente; nesta revisão, foco em evitar sobre-restrição desnecessária.
- **Causa raiz:** schema anterior passou a exigir muitos subcampos detalhados além do mínimo bloqueante.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** no incidente original, preset sem `theme.spacing`.
- **O que a especificação esperava (literal):** presença dos tokens mínimos canônicos para prosseguir (`theme.palette`, `theme.typography`, `theme.spacing`, `theme.accessibility`, `componentPresets.cta`, `componentPresets.trust`) e `proof.showIdentity=true` para validação operacional.
- **Diferença objetiva:** ausência de token mínimo (incidente original) e excesso de rigidez do schema no ajuste anterior.
- **Ação corretiva recomendada:** manter exigência dos mínimos canônicos/bloqueantes e relaxar campos não críticos para reduzir rejeições desnecessárias.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** contrato + teste.
- **Arquivos alterados:**
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `backend/ads-service/src/test/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationServiceTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** schema de `landingPageDesignPreset` foi recalibrado para exigir o núcleo mínimo bloqueante (tema mínimo + `componentPresets` essenciais, incluindo `proof.showIdentity`) com flexibilidade em campos complementares; teste unitário atualizado para validar os tokens mínimos.

#### 5) Reteste
- **Procedimento de reteste:** execução da suíte `ExperimentPipelineGenerationServiceTest`.
- **Resultado:** aprovado.
- **Evidências (logs/ids/prints):** `../mvnw -Dtest=ExperimentPipelineGenerationServiceTest test` com `Tests run: 40, Failures: 0, Errors: 0`.
- **Status final:** Resolvido.

#### 6) Próximo passo
- **Ação seguinte:** monitorar novas execuções de `LANDING_PAGE_DESIGN_PRESET` no experimento 15 para confirmar redução de 422 por contrato.
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** próxima execução do pipeline.

### INC-0003 — Reforço de instruções no prompt do worker para cobertura 1:1 de `sectionId` em `images[]`

- **Data/Hora (UTC):** 2026-04-27
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "Planejamento de Imagens da Landing")

#### 1) Erro observado
- **Sintoma:** Etapa de planejamento de imagens permanece vulnerável a 422 por cobertura incompleta de `sectionId`.
- **Endpoint/rota/fila:** conclusão de job da etapa `LANDING_PAGE_IMAGE_PLANNING`.
- **Status code:** 422 `UNPROCESSABLE_ENTITY`.
- **Mensagem de erro literal:** `Planejamento de imagens incompleto: faltam sectionId do wireframe em images[]. Faltando: [objection-anti-preco-pratica]`.
- **Payload enviado (literal):** `images[]` sem item correspondente ao `sectionId` obrigatório listado acima.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico guiado por validação literal do backend + evidência de UI).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** regra de cobertura total em `landingPageImagePlanning.images[].sectionId` vs `landingPageWireframe.sectionOrder[*].sectionId`.
- **Validação/regra que rejeitou:** verificação de conjunto esperado vs planejado em `validateLandingImagePlanningArtifacts`.
- **Causa raiz:** instrução anterior não estava suficientemente prescritiva no template do worker para forçar validação interna de `missing/extras` antes da resposta final.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `images[]` sem `sectionId` `objection-anti-preco-pratica`.
- **O que a especificação esperava (literal):** `images[]` contendo todos os `sectionId` de `landingPageWireframe.sectionOrder`, sem faltas e sem excedentes.
- **Diferença objetiva:** omissão de seção obrigatória do wireframe no artefato final.
- **Ação corretiva recomendada:** fortalecer o template `landing-image-planning` com fluxo obrigatório explícito (`requiredSectionIds`, cálculo de `missing/extras` e bloqueio de finalização até zerar diferenças).

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + robustez backend + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-image-planning.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `backend/ads-service/src/main/java/com/marketinghub/experiment/pipeline/service/ExperimentPipelineGenerationService.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** template do worker ganhou fluxo interno obrigatório 1:1 por `sectionId`; teste unitário passou a validar presença das novas instruções; helper do backend para checklist no prompt passou a registrar log em caso de falha ao carregar wireframe, evitando fallback silencioso.

#### 5) Reteste
- **Procedimento de reteste:** execução de testes unitários focados em backend e ai-worker.
- **Resultado:** backend aprovado; ai-worker bloqueado por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` indisponível no ambiente.
- **Evidências (logs/ids/prints):**
  - `backend/ads-service`: suite `ExperimentPipelineGenerationServiceTest` aprovada.
  - `ai-worker`: erro de resolução de dependência durante `mvn test`.
- **Status final:** Parcial (ajuste aplicado; validação automatizada completa depende de credencial/repositório para dependência privada).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar etapa de planejamento de imagens no experimento 15 e validar que `images[]` cobre integralmente `sectionOrder` (incluindo `objection-*`, `offer-*`, `faq-*` quando presentes).
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** imediato (próxima execução).

### INC-0002 — 422 no planejamento de imagens por cobertura incompleta de `sectionId`

- **Data/Hora (UTC):** 2026-04-27
- **Responsável:** Assistente Codex
- **Módulo:** ai-worker + backend
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Pipeline do experimento `15` (etapa "Planejamento de Imagens da Landing")

#### 1) Erro observado
- **Sintoma:** Etapa finalizou com erro e bloqueou continuidade do pipeline.
- **Endpoint/rota/fila:** conclusão de job da etapa `LANDING_PAGE_IMAGE_PLANNING`.
- **Status code:** 422 `UNPROCESSABLE_ENTITY`.
- **Mensagem de erro literal:** `Planejamento de imagens incompleto: faltam sectionId do wireframe em images[]. Faltando: [objection-anti-preco-pratica, faq-objections]`.
- **Payload enviado (literal):** não incluía todos os `sectionId` presentes no `landingPageWireframe.sectionOrder`.

#### 2) Diagnóstico (MCP + Cânone)
- **Logs consultados via MCP:** não executado neste registro (diagnóstico guiado pelo erro literal retornado pela UI/back-end).
- **Dados de banco consultados via MCP:** não executado neste registro.
- **Trecho canônico consultado:** regra de `landingPageImagePlanning.images[].sectionId` e cobertura por seção.
- **Validação/regra que rejeitou:** backend compara conjunto esperado (`wireframe.sectionOrder[*].sectionId`) vs conjunto entregue (`images[*].sectionId`) e rejeita faltantes.
- **Causa raiz:** resposta do modelo sem checklist final de cobertura 1:1 entre wireframe e images.

#### 3) Divergência (formato obrigatório para 422)
- **O que o modelo entregou (literal):** `images[]` sem os `sectionId` `objection-anti-preco-pratica` e `faq-objections`.
- **O que a especificação esperava (literal):** `images[]` com 100% dos `sectionId` do `landingPageWireframe.sectionOrder`, sem omitir e sem inventar.
- **Diferença objetiva:** cobertura parcial de seções canônicas.
- **Ação corretiva recomendada:** reforçar no prompt checklist de cobertura obrigatório antes de finalizar.

#### 4) Ajuste aplicado
- **Tipo de ajuste:** prompt + teste.
- **Arquivos alterados:**
  - `ai-worker/src/main/resources/prompts/experiment/landing-image-planning.md`
  - `ai-worker/src/test/java/com/marketinghub/worker/experimentpipeline/ExperimentPipelineOpenAiClientTest.java`
  - `docs/registro-ajustes-pipeline-experimento.md`
- **Resumo técnico da mudança:** adicionada regra explícita de checklist de cobertura (`sectionOrder[*].sectionId` vs `images[*].sectionId`) e teste para garantir presença desta instrução no prompt gerado.

#### 5) Reteste
- **Procedimento de reteste:** execução de teste unitário focado no prompt da etapa de image planning.
- **Resultado:** reteste bloqueado por dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT` (401 no GitHub Packages) no ambiente atual.
- **Evidências (logs/ids/prints):** `mvn -Dtest=ExperimentPipelineOpenAiClientTest test` retornando erro de resolução de dependência.
- **Status final:** Parcial (ajuste aplicado; validação automatizada pendente de credenciais de pacote).

#### 6) Próximo passo
- **Ação seguinte:** reexecutar etapa "Planejamento de Imagens da Landing" do experimento 15 para validar cobertura completa em ambiente integrado.
- **Dono da ação:** Time de operação do pipeline.
- **Prazo:** imediato (próxima execução).

### INC-0001 — Criação do documento de rastreabilidade

- **Data/Hora (UTC):** 2026-04-26
- **Responsável:** Assistente + Time Marketing Hub
- **Módulo:** Processo transversal
- **Ambiente:** Repositório `marketing-hub`
- **Execução/Teste:** Preparação do ciclo de correção

#### 1) Erro observado
- **Sintoma:** Não havia documento único para rastrear ajustes do pipeline de experimento.
- **Impacto:** Risco de perda de contexto entre ciclos de teste/correção.

#### 2) Diagnóstico
- Necessidade de um template padronizado para registrar incidente, análise via MCP, aderência canônica e reteste.

#### 3) Ajuste aplicado
- Criação deste documento com:
  - ciclo operacional padrão;
  - modelo obrigatório de incidente;
  - seção de registro cronológico.

#### 4) Reteste
- Documento criado e pronto para receber os próximos incidentes reais.

#### 5) Próximo passo
- Executar o pipeline, enviar o primeiro erro e preencher `INC-0002` com diagnóstico completo.
