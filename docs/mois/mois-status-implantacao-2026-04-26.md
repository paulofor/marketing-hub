# MOIS — Levantamento atualizado de implantação (26/04/2026)

## Escopo deste levantamento

Este levantamento consolida o estado atual do módulo MOIS com foco em:

1. situação da tela **/mois/research-sources** (Locais de pesquisa MOIS);
2. status da iniciativa de **pesquisa dos destaques por fonte**;
3. estágio operacional do fluxo de coleta automática e integração com workspace.

Data de referência: **26/04/2026**.

---

## 1) Situação da tela “Locais de pesquisa MOIS”

### Status: **Implantada no frontend (MVP estático)**

- A rota `/mois/research-sources` está publicada no roteamento principal do frontend.
- A tela lista fontes recomendadas (Meta Ad Library, TikTok Creative Center, Google Ads Transparency Center, YouTube, Hotmart, Monetizze, Eduzz, Google Trends, ClickBank, Digistore24 e JVZoo).
- Cada fonte possui descrição, categoria, “quando usar” e link externo com abertura em nova aba (`target="_blank"`).

### Leitura operacional

- A tela atende o objetivo de **guia de pesquisa** para orientar o operador humano antes da extração.
- Atualmente funciona como **catálogo curado estático** (não há ingestão dinâmica por API para essa página específica).

---

## 2) Situação da “pesquisa dos destaques de cada fonte”

### Status: **Parcialmente implantada (no fluxo de coleta automática, ainda sem consolidação analítica por fonte)**

O que já está implementado:

- O fluxo de coleta automática gera referências com campos de destaque de performance por item coletado:
  - `successScore`
  - `successSignal`
  - `confidenceLevel`
  - `engagementRelative`
  - `recurrenceScore`
  - `evidenceScore`
- Há filtros por fonte, nicho, score mínimo e confiança.
- Há ações operacionais por referência (favoritar, descartar, importar, importar e iniciar extração) e lineage.

O que ainda não está implementado (lacuna principal):

- Não existe hoje um endpoint/tela dedicada de **“destaques agregados por fonte”** (ex.: top insights por fonte com consolidação analítica por período).
- No módulo MOIS, as referências de coleta ainda são seedadas em memória para contrato/smoke (não são conectores reais de produção por fonte).

### Conclusão objetiva sobre os destaques por fonte

- Existe o **destaque por referência coletada** (nível item).
- Ainda falta o **destaque consolidado por fonte** (nível analítico agregado).

---

## 3) Situação macro da implantação do MOIS

### Estado do módulo

- Arquitetura separada do MOIS em módulo próprio (`/mois`) está em vigor, com backend principal atuando como façade/gateway.
- Serviço MOIS está previsto em container próprio (`8094`) no `deploy/docker-compose.yml`.
- Integração do backend com o módulo MOIS está parametrizada via `integrations.mois.module.*`.

### Estado de execução por sprint (coleta automática)

- Sprint 0: concluída.
- Sprint 1: concluída.
- Sprint 2: concluída.
- Sprint 3: concluída (UI de coleta automática).
- Sprint 4: concluída (importação + extração + lineage).
- Sprint 5: **concluída**, com baseline operacional, observabilidade mínima (`collection-ops/summary`), retry com backoff/rate limit, fallback de indisponibilidade e rollout gradual por workspace.

---

## 4) Diagnóstico final (objetivo)

1. A iniciativa da tela em anexo (**Locais de pesquisa MOIS**) está implantada e navegável.
2. A iniciativa de “destaques” avançou no nível de score/sinal por referência coletada.
3. Ainda há gap para entregar o que normalmente se entende por “destaques de cada fonte” no nível executivo (agregado por fonte e período).
4. O ciclo de coleta automática concluiu a Sprint 5 com endurecimento operacional (observabilidade mínima, rollout com segurança e fallback definido para indisponibilidade de fonte).

---

## 5) Próximo passo recomendado (curto prazo)

Priorizar uma entrega incremental de **Resumo por Fonte** com:

- endpoint de agregação por fonte/tempo (top promessas, top provas, score médio, volume, confiança);
- card executivo no workspace/coleta automática “Destaques por fonte”;
- persistência relacional dos resultados de coleta para auditoria histórica;
- telemetria mínima (latência por fonte, falhas, taxa de coleta útil).

Isso fecha a lacuna entre “listar fontes para pesquisar” e “mostrar, de forma acionável, os destaques extraídos de cada fonte”.

---


## 6) Validação direta do plano de sprints (pergunta: “tudo foi implementado?”)

### Resposta curta
**Não 100%.** O plano de sprints foi concluído formalmente até a Sprint 5, mas ainda existem lacunas explicitamente registradas no próprio escopo pós-sprint.

### Matriz de verificação

| Item do plano | Situação | Evidência atual |
|---|---|---|
| Sprint 0 a Sprint 5 | Concluídas | Relatórios de sprint e status consolidado neste ciclo |
| Monitoração mínima operacional | Implementado | Endpoint `collection-ops/summary` e telemetria por workspace |
| Retry/backoff + rate-limit | Implementado em baseline | Execução de coleta com retry controlado e estatísticas por fonte |
| Rollout gradual por workspace | Implementado | Gate de habilitação por workspace no fluxo de criação de job |
| Fallback de indisponibilidade de fonte | Implementado | Coleta marca falha por fonte indisponível com degradação previsível |
| Persistência relacional + auditoria histórica | **Pendente** | Coleta/lineage ainda em memória (volátil) |
| Destaques agregados por fonte (nível executivo) | **Pendente** | Existe destaque por item; falta consolidação analítica por fonte/período |
| Conectores reais por fonte (produção) | **Pendente** | Fluxo ainda opera com seed/simulação para contrato/smoke |

### Conclusão objetiva
- A entrega está **completa no recorte de MVP + endurecimento operacional da Sprint 5**.
- A entrega **não está completa no recorte de produção plena** (persistência relacional, conectores reais e analytics agregada por fonte ainda pendentes).



### Atualização arquitetural (MOIS -> Backend para persistência de coleta)

- Foi implementado fluxo de persistência operacional via backend para estado de coleta automática (`job`, `references`, `lineage`, `runtime`, `sourceOps`).
- Endpoints internos adicionados no backend:
  - `PUT /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs/{jobId}`
  - `GET /api/v1/mois/persistence/collection-jobs?workspaceId&status`
- O MOIS agora consulta/sincroniza estado por esse contrato (MOIS -> Backend), reduzindo acoplamento da operação à memória local do módulo.
