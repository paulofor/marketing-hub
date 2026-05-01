# Análise do pipeline atual de geração de landing e plano de melhoria por sprints

## 1) Escopo desta análise

Este documento analisa o pipeline atual de landing do Marketing Hub (da geração de artefatos até publicação no Lead Portal) e propõe um plano de evolução em sprints, com foco em elevar qualidade visual, previsibilidade de renderização e aderência canônica.

Referências principais usadas nesta análise:
- `docs/pipeline-landing-experimento.md`
- `docs/experiment-pipeline-artifacts-visual.md`
- `backend/ads-service/.../ExperimentPipelineSection.java`
- `backend/ads-service/.../LandingHtmlModule.java`
- `backend/ads-service/.../ExperimentPipelineGenerationService.java`
- `docs/pesquisa-profunda/pesquisa-profunda-html-estilos.md`

---

## 2) Como o pipeline atual funciona (estado real)

### 2.1 Encadeamento de etapas

No fluxo operacional documentado, a landing depende desta sequência:
1. `LANDING_PAGE_COPY`
2. `LANDING_PAGE_WIREFRAME`
3. `LANDING_PAGE_IMAGE_PLANNING`
4. geração das imagens (`WEB_READY`)
5. `LANDING_PAGE_HTML`
6. aplicar HTML no formulário
7. aprovar fluxo no Lead Portal.

Isso já é uma base sólida de governança operacional, inclusive com check explícito para evitar publicação com placeholder.

### 2.2 Dependências técnicas entre seções

No backend, a enum do pipeline define dependências formais entre seções (predecessor), garantindo ordem de execução e bloqueio de execução fora da cadeia prevista.

### 2.3 Papel do LHM no estado atual

O `LandingHtmlModule` já consolida 4 artefatos canônicos (`wireframe`, `copy`, `design preset`, `image planning`) e monta HTML com:
- resolução de hero,
- montagem por seção,
- injeção de copy/fAQ/CTA,
- binding de imagens por `sectionId`,
- inclusão de formulário quando detectado.

Também existe suporte a CSS via contrato (`baseCss`/`lhmRuntime.baseCss`), o que já permite desacoplar parte da estética.

### 2.4 Orquestração e robustez

O serviço de pipeline controla:
- fila sequencial (evita concorrência de etapas no mesmo experimento),
- fechamento de jobs stale,
- pré-validações antes da etapa de HTML,
- retomada automática após readiness de imagem.

Esse ponto é maduro do lado de operação/fila.

---

## 3) Diagnóstico: principais gaps atuais

## 3.1 Gap de arquitetura visual

Apesar da boa base de artefatos, ainda há sinais de decisão visual em excesso dentro do Java do LHM (heurísticas de hero/sumário, composição textual e detecção de formulário). Isso reduz determinismo real de design e dificulta evolução estética por contrato.

## 3.2 Gap de design system

Pelo estudo de páginas premium, o sistema precisa evoluir para:
- macro-seções como composição narrativa,
- elementos internos padronizados (tipografia, botões, campos, cards, FAQ, pricing),
- tokens como fonte primária de estilo.

Hoje o pipeline já recebe `design preset`, mas ainda falta transformar isso em **component registry + slot resolver** com governança forte de variantes visuais.

## 3.3 Gap de qualidade visual consistente

O documento de pesquisa aponta que a diferença entre páginas fortes e páginas fracas raramente está em “mais CSS”; está em:
- hierarquia tipográfica correta,
- CTA visualmente forte,
- forms legíveis,
- prova visual e narrativa,
- contraste e densidade por seção.

Esse padrão ainda não está totalmente operacionalizado como contrato de renderização (com checklist automático por seção).

## 3.4 Gap de auditoria pós-render

Já existe validação de pipeline, mas ainda é recomendável ampliar auditorias pós-render para:
- acessibilidade mínima (labels/alt/focus),
- qualidade de performance (imagens, lazy-loading, tamanhos),
- consistência visual mínima por tokens.

---

## 4) Direção de melhoria (alvo arquitetural)

## 4.1 Modelo-alvo

Adotar formalmente o fluxo:

`wireframe + copy-slots + design-tokens + runtime-assets -> validator -> component registry -> slot resolver -> template renderer -> auditorias pós-render`.

## 4.2 Princípios de implementação

1. **Renderer sem adivinhação**: renderizar apenas o que está no contrato aprovado.
2. **Section as composition, elements as system**: seção compõe narrativa; elementos internos são primitives globais.
3. **Tokens primeiro**: cores, tipografia, spacing, radius, shadow e estados de foco vindos do preset.
4. **Fallback explícito e rastreável**: nada de fallback silencioso.
5. **Gates de qualidade automáticos**: bloquear publicação quando critérios mínimos não forem atendidos.

---

## 5) Plano de melhoria por sprints

## Sprint 1 — Fundamentos de design system e contrato de render

**Objetivo:** criar base visual premium com pouca ruptura.

**Entregas**
- Definir pacote mínimo de primitives no LHM: `hero-title`, `section-title`, `body`, `btn-primary|secondary`, `field`, `card`, `faq-item`.
- Consolidar tokens obrigatórios no `landingPageDesignPreset` (tipografia, spacing, radius, shadow, focus-ring, cores de superfície/contraste).
- Formalizar mapeamento `componentKey -> template parcial` (registry inicial) para blocos mais comuns (`hero-form-split`, `proof`, `offer-cards`, `faq`).
- Bloquear render quando faltar token crítico ou slot obrigatório.

**Critérios de aceite**
- 100% das novas landings usam tokens em vez de CSS hardcoded ad-hoc.
- CTA e formulário respeitam ranges mínimos de legibilidade e touch target.
- Nenhum fallback silencioso em parse de artefato crítico.

## Sprint 2 — Determinismo forte de composição (slots e seções)

**Objetivo:** retirar heurística estrutural do Java e migrar para contrato explícito.

**Entregas**
- Introduzir/fortalecer `slotDefs` obrigatórios no wireframe por `componentKey`.
- Resolver copy por `slotKey` em vez de inferência por texto solto.
- Remover gradualmente heurísticas de hero/summary/form-detection quando o contrato já trouxer os dados.
- Validar binding estrito de imagens por `sectionId` + `imageBindingKey`.

**Critérios de aceite**
- Redução mensurável de incidentes 422 por divergência estrutural de landing.
- HTML final sem inferência de blocos principais fora do contrato.

## Sprint 3 — Auditorias pós-render + qualidade comercial

**Objetivo:** transformar qualidade visual/comercial em gate automatizado.

**Entregas**
- Auditorias pós-render obrigatórias (a11y/perf/consistência visual):
  - labels visíveis,
  - `alt` em imagens informativas,
  - foco perceptível,
  - contraste mínimo por variante,
  - `loading`/`decoding`/`sizes` em imagens.
- Gate de continuidade de mensagem (ad -> landing -> CTA) e cobertura de narrativa (Dor -> Resultado -> Mecanismo -> Prova -> Oferta).
- Score de qualidade de landing por experimento com relatório no job.

**Critérios de aceite**
- Job `LANDING_PAGE_HTML` só conclui com score mínimo aprovado.
- Relatório de auditoria anexado ao histórico do job.

## Sprint 4 — Operação, observabilidade e rollout seguro

**Objetivo:** escalar com segurança e aprendizado contínuo.

**Entregas**
- Modo de rollout por feature-flag (`lhm.registry.enabled`, `lhm.audit.gate.enabled`).
- Painel operacional com métricas por etapa: taxa de falha por seção, retrabalho, tempo total, taxa de placeholder, score médio de qualidade.
- Playbook de incidentes de contrato (400/422) com diagnóstico padronizado em payload literal vs esperado.

**Critérios de aceite**
- Queda sustentada de retrabalho em `LANDING_PAGE_HTML`.
- Redução de tempo de publicação com aumento de consistência visual.

---

## 6) Backlog técnico recomendado (priorização)

### Alta prioridade
1. Registry inicial de componentes de seção e slots obrigatórios.
2. Endurecimento de validação de `designPreset` com tokens mandatórios.
3. Remoção de fallback silencioso em artefatos críticos.
4. Auditoria mínima de a11y/perf no pós-render.

### Média prioridade
1. Biblioteca de variantes de superfície (normal/high/soft) por seção.
2. Score de qualidade visual/comercial por landing.
3. Telemetria de mismatch de mensagem entre ad/copy/cta.

### Baixa prioridade
1. Catálogo de temas por nicho.
2. Benchmarking automático de densidade/ordem de seções por segmento.

---

## 7) Riscos e mitigação

- **Risco:** quebrar compatibilidade de landings legadas.
  - **Mitigação:** feature flags + fallback controlado por versão de contrato.
- **Risco:** aumento inicial de falhas por validação mais rígida.
  - **Mitigação:** fase de warning antes de gate hard-fail.
- **Risco:** dependência de qualidade do artifacto de entrada.
  - **Mitigação:** validação antecipada (copy/wireframe/design) com mensagens de erro objetivas.

---

## 8) Resultado esperado de negócio

Com a evolução proposta, o pipeline tende a ganhar:
- maior consistência visual entre experimentos,
- menor incidência de retrabalho técnico,
- melhor continuidade de mensagem anúncio -> landing -> CTA,
- aumento de conversão por melhora de clareza, prova e usabilidade da página.

Esse resultado mantém aderência ao cânone do sistema: robustez arquitetural a serviço de conversão e avanço de funil.
