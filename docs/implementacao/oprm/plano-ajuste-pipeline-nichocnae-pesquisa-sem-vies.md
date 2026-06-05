# Plano de implementação — ajuste do pipeline NichoCNAE para pesquisa de rotina sem viés de solução

## 1. Objetivo

Revisar o pipeline **OPRM NichoCNAE** exibido na tela `/oprm/pipeline` e propor ajustes para que a pesquisa inicial do nicho seja **neutra em relação a solução, IA, automação, produto ou oferta**.

A fonte primária desta revisão é o **código atual**. A documentação canônica foi usada apenas como referência de fronteira, porque pode estar desatualizada em relação ao comportamento implementado.

O objetivo comercial continua sendo gerar vendas, mas seguindo a ordem correta do Marketing Hub:

> **Dor → Resultado → Mecanismo → Prova → Oferta**

Portanto, a fase NichoCNAE deve descobrir primeiro a rotina real e as dores concretas do público. A solução com IA deve aparecer depois, como mecanismo candidato, nunca como premissa da pesquisa.

---

## 2. Diagnóstico executivo

O pipeline atual tem boa separação modular entre backend e coletor OPRM, usa etapas com responsabilidades claras e mantém o backend como fonte de verdade. Porém, há três pontos principais onde o viés de solução pode entrar ou ser preservado:

1. **Entrada enviesada do nome do nicho**
   - A etapa zero copia `candidate.getCandidateNicheName()` diretamente para o ciclo (`cycle.setNicheName(...)`).
   - Se o candidato já chega como `IA para crescimento de ...`, todo o ciclo passa a usar esse enquadramento como nome operacional do nicho.
   - Isso contamina prompt, queries, busca pública, síntese e materialização final.

2. **Prompt da etapa de seed ainda mistura rotina com descoberta comercial/oferta**
   - O prompt afirma que o objetivo é rotina, mas também manda cobrir `produtos/serviços` e permite `PRODUCT_SERVICE_DISCOVERY` e `OFFER_PATTERN_DISCOVERY`.
   - Essa mistura puxa a busca para soluções existentes, apps, sistemas, cursos e ofertas antes de mapear o dia a dia.

3. **Extração e quality gate favorecem mecanismo/automação cedo demais**
   - O extrator classifica `sistema`, `automação` e `ia` como `MECHANISM_OPPORTUNITY` com confiança alta.
   - O quality gate exige `mechanismOpportunityCount > 0` para aprovação, o que força o pipeline a encontrar mecanismo já na fase de rotina.
   - Isso pode fazer uma pesquisa enviesada parecer “completa” por ter muitos sinais de solução.

Conclusão: **não basta alterar a tela ou a nomenclatura visual**. A causa-raiz está no contrato de entrada do ciclo, no prompt/schema de seed, nos objetivos de query, na taxonomia de sinais e nos critérios de aprovação.

---

## 3. Mapa do pipeline atual conforme o código

### 3.1 Etapa zero — `oprmRoutineResearchOrchestrator`

**Responsabilidade atual:** selecionar o próximo candidato de alto score e criar o ciclo pai de pesquisa.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routineresearchorchestrator/service/BackendRoutineResearchOrchestratorService.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchorchestrator/RoutineResearchOrchestratorProcessor.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchorchestrator/RoutineResearchOrchestratorInitialScheduler.java`

Comportamento observado:

- O backend busca o próximo candidato pendente por score.
- Cria `OprmRoutineResearchCycle` com CNAE, descrição, score, origem e status `RUNNING`.
- Copia o nome do candidato para o ciclo com `cycle.setNicheName(candidate.getCandidateNicheName())`.
- Atualiza o candidato para `RESEARCH_RUNNING`.

Risco de viés:

- Se `candidateNicheName` já foi gerado por uma etapa anterior com linguagem de solução, como `IA para crescimento de ...`, esse texto vira a identidade do ciclo.
- As etapas posteriores usam `nicheName` como insumo direto.

Ajuste necessário:

- Criar uma normalização de nome operacional neutro antes de salvar o ciclo.
- Preservar o nome original como rastreabilidade, mas não usá-lo como seed principal de pesquisa de rotina quando contiver termos de solução.

---

### 3.2 Etapa um — `oprmRoutineResearchCycle`

**Responsabilidade atual:** controlar/detalhar o ciclo de rotina já aberto.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routineresearchcycle/service/BackendRoutineResearchCycleService.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchcycle/RoutineResearchCycleService.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchcycle/RoutineResearchCycleProcessor.java`

Comportamento observado:

- Lista ciclos com status `RUNNING`.
- Detalha totais de queries, fontes, snapshots e sinais.
- Não transforma o nicho; apenas expõe estado operacional.

Risco de viés:

- Baixo na lógica própria da etapa.
- O risco é carregar o `nicheName` enviesado criado na etapa zero.

Ajuste necessário:

- Incluir no detalhe operacional campos que ajudem a auditar neutralidade: nome original, nome normalizado, estratégia de pesquisa e score de risco de viés.

---

### 3.3 Etapa dois — `oprmNicheResearchSeedBuilder`

**Responsabilidade atual:** usar IA no próprio `oprm-coletor-mei` para transformar o CNAE/ciclo em seed e queries.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderPromptBuilder.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/OpenAiNicheResearchSeedBuilderClient.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderSchema.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderValidator.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/nicheresearchseedbuilder/service/BackendNicheResearchSeedBuilderService.java`

Comportamento observado:

- O prompt diz que a etapa deve conhecer a rotina sem criar oferta.
- Porém, exige queries cobrindo `dores comerciais e produtos/serviços`.
- O schema/validação permitem objetivos: `ROUTINE_DISCOVERY`, `NICHE_OWNER_QUESTION_DISCOVERY`, `FINAL_CUSTOMER_QUESTION_DISCOVERY`, `SALES_PAIN_DISCOVERY`, `PRODUCT_SERVICE_DISCOVERY` e `OFFER_PATTERN_DISCOVERY`.
- O backend aceita e persiste até 15 queries.

Risco de viés:

- Alto.
- `PRODUCT_SERVICE_DISCOVERY` e `OFFER_PATTERN_DISCOVERY` antecipam etapa comercial.
- Se o nome do nicho contiver `IA`, a IA tende a gerar queries de solução.
- A regra “cada query deve conter o nome do nicho” replica o viés do nome em todas as queries.

Ajuste necessário:

- Dividir explicitamente a seed em **modo rotina neutra** e, futuramente, **modo mecanismo/oferta**.
- Nesta primeira fase, remover objetivos comerciais/oferta do contrato aceito pela rotina.
- Adicionar validação determinística que rejeite termos proibidos quando eles vierem do modelo e não da descrição CNAE/fonte orgânica.

---

### 3.4 Etapa três — `oprmSourceSearcher`

**Responsabilidade atual:** executar queries em provedor público e registrar fontes candidatas.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcesearcher/SourceSearcherProcessor.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcesearcher/DuckDuckGoHtmlSourceSearchProvider.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcesearcher/service/BackendSourceSearcherService.java`

Comportamento observado:

- O processor executa a query exatamente como recebida.
- O provedor usa DuckDuckGo HTML e retorna até 20 resultados por query.
- Não há classificação de domínio/fonte por intenção de rotina versus solução.

Risco de viés:

- Médio/alto, herdado da etapa dois.
- Se as queries buscam solução, as fontes candidatas também serão majoritariamente solução.
- O pipeline não mede concentração de domínios de software, cursos, SaaS ou marketplaces.

Ajuste necessário:

- Criar política de busca/fonte para rotina neutra.
- Adicionar classificação determinística de fonte candidata com pelo menos: `ROTINA_PUBLICA`, `PERGUNTA_PUBLICA`, `CONTEUDO_EDUCACIONAL`, `SOLUCAO_VENDOR`, `OFERTA_CURSO`, `MARKETPLACE`, `BAIXA_RELEVANCIA`.
- Na fase de rotina, reduzir ou bloquear fontes `SOLUCAO_VENDOR` e `OFERTA_CURSO`, salvo quando forem usadas apenas como evidência secundária.

---

### 3.5 Etapa quatro — `oprmSourceFetcher`

**Responsabilidade atual:** coletar snapshot curto das fontes candidatas.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcefetcher/JsoupPublicSourceFetcher.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcefetcher/SourceFetcherProcessor.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcefetcher/service/BackendSourceFetcherService.java`

Comportamento observado:

- Coleta HTML público com Jsoup.
- Registra log do payload bruto recebido, conforme regra de ingestão.
- Persiste apenas metadados, snippet e `shortExcerpt`; não armazena HTML completo.

Risco de viés:

- Baixo na coleta.
- Médio na seleção de quais fontes serão coletadas, porque depende da etapa três.

Ajuste necessário:

- Levar a classificação de fonte até o snapshot.
- Persistir indicadores de neutralidade, como `sourceIntent`, `vendorSolutionRisk`, `offerPageRisk` e `routineEvidenceScore`.
- Não alterar a política de armazenamento curto; ela está correta para evitar contaminação de artefato e excesso de dados.

---

### 3.6 Etapa cinco — `oprmSignalExtractor`

**Responsabilidade atual:** extrair sinais estruturados a partir de snapshots curtos.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/signalextractor/SignalExtractorEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/signalextractor/service/BackendSignalExtractorService.java`
- `backend/ads-service/src/test/java/com/marketinghub/oprm/nichocnae/signalextractor/architecture/OprmSignalExtractorArchitectureTest.java`

Comportamento observado:

- Usa regras determinísticas simples por palavras-chave.
- Classifica `agenda`, `atendimento`, `cliente`, `serviço`, `rotina` como `ROUTINE_TASK`.
- Classifica `whatsapp`, `mensagem`, `confirmar`, `lembrete` como `COMMERCIAL_TASK`.
- Classifica `organizar`, `controle`, `processo`, `sistema`, `automação`, `ia` como `MECHANISM_OPPORTUNITY`.
- Se nada for encontrado, cria `LANGUAGE_MARKER`.

Risco de viés:

- Alto para a palavra `ia` e médio para `automação`/`sistema`, porque essas palavras recebem sinal de mecanismo com confiança 88.
- Isso reforça a solução antes de validar a dor.

Ajuste necessário:

- Separar taxonomia de sinais em dois grupos:
  - **Sinais de rotina:** tarefas, fricções, perguntas, objetos de trabalho, linguagem, contexto, frequência, atores envolvidos.
  - **Sinais de mecanismo:** processo, checklist, script, planilha, automação, IA, ferramenta, treinamento.
- Na fase de rotina, mecanismos devem ser capturados como `MECHANISM_CANDIDATE_SECONDARY`, sem serem requisito de aprovação.
- `IA`, `automação`, `software`, `app`, `sistema`, `ferramenta`, `curso` devem alimentar um `solutionBiasRiskScore` quando aparecem cedo demais ou concentrados em fontes vendor.

---

### 3.7 Etapa seis — `oprmRoutineSynthesizer`

**Responsabilidade atual:** sintetizar cartão de rotina a partir dos sinais estruturados.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routinesynthesizer/RoutineSynthesizerEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinesynthesizer/service/BackendRoutineSynthesizerService.java`

Comportamento observado:

- Monta blocos de rotina, dores, resultados, mecanismos e evidências.
- Cada bloco cita sinais e evidências.
- O bloco de mecanismo inclui `MECHANISM_OPPORTUNITY`, `PROOF_SIGNAL`, `LANGUAGE` e `LANGUAGE_MARKER`.
- O texto final usa `pending.nicheName()` no título de cada bloco.

Risco de viés:

- Médio/alto.
- Se o nome do nicho estiver enviesado, ele aparece em todos os blocos.
- O bloco de mecanismo pode receber sinais fracos ou linguagem de fallback, misturando evidência de rotina com oportunidade de solução.

Ajuste necessário:

- Usar o nome neutro operacional no texto principal.
- Criar bloco separado de `observações de possível viés de solução`, que não entra como argumento comercial.
- Em rotina neutra, o bloco de mecanismos deve ter peso secundário e deve ser explicitamente “candidato posterior”, não conclusão.

---

### 3.8 Etapa sete — `oprmRoutineQualityGate`

**Responsabilidade atual:** decidir se o cartão está pronto para alimentar hipóteses comerciais.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routinequalitygate/RoutineQualityGateEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinequalitygate/service/BackendRoutineQualityGateService.java`

Comportamento observado:

- Exige fontes, sinais, especificidade, confiança e baixa duplicação.
- Exige mix mínimo com pergunta, dor, mecanismo e tarefa.
- Aprova como `LIGHTLY_RESEARCHED` quando `readyForHypothesis=true`.

Risco de viés:

- Alto por exigir mecanismo como condição de aprovação.
- A pesquisa pode ser considerada incompleta se encontrar rotina/dor forte, mas ainda não mecanismo.
- Ou pode ser aprovada por mecanismos enviesados de IA, mesmo com rotina fraca.

Ajuste necessário:

- Trocar o requisito de mecanismo por requisito de **rotina + dor + pergunta/linguagem + evidência multiforme**.
- Criar métricas separadas:
  - `routineEvidenceScore`
  - `painEvidenceScore`
  - `languageEvidenceScore`
  - `sourceDiversityScore`
  - `solutionBiasRiskScore`
- Bloquear aprovação quando `solutionBiasRiskScore` estiver alto e a rotina/dor estiver fraca.

---

### 3.9 Etapa oito — `oprmEnrichedNicheMaterializer`

**Responsabilidade atual:** materializar o cartão aprovado em `market_niche` e `market_niche_enrichment_profile`, sem criar hipótese/oferta.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/enrichednichematerializer/EnrichedNicheMaterializerEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/enrichednichematerializer/service/BackendEnrichedNicheMaterializerService.java`

Comportamento observado:

- Cria persona, padrões de linguagem, gatilhos e objeções determinísticas.
- Persiste rotina, dores, resultados, mecanismos, evidências e scores no perfil enriquecido.
- Atualiza o ciclo e o candidato para `ENRICHED_NICHE_CREATED`.

Risco de viés:

- Médio, por materializar o que veio antes.
- A etapa em si não inventa oferta, mas pode eternizar o viés em `market_niche` e `market_niche_enrichment_profile`.

Ajuste necessário:

- Persistir também o contexto de neutralidade da pesquisa:
  - `researchMode=ROUTINE_NEUTRAL`
  - `originalNicheName`
  - `neutralNicheName`
  - `solutionBiasRiskScore`
  - `sourceIntentMix`
- Bloquear materialização automática se o quality gate detectar alto viés de solução.

---

## 4. Princípios arquiteturais para a implementação

1. **Backend continua sendo a fonte de verdade**
   - Entidades, status, contadores, contratos internos e persistência ficam no backend.
   - O coletor OPRM executa as etapas e chama apenas endpoints do próprio módulo OPRM.

2. **Sem chamada direta entre módulos**
   - Frontend, coletor e demais serviços não devem falar diretamente entre si.
   - Todo contrato operacional passa pelo backend OPRM.

3. **Mudanças em Java devem manter comentários de responsabilidade**
   - Toda classe alterada deve ter comentário de responsabilidade básica.
   - Todo método novo/alterado deve ter comentário breve.

4. **Sem alterar `ArquiteturaTest` sem solicitação direta**
   - Ajustes de arquitetura devem priorizar novos testes focados na regra de neutralidade.
   - Alterar testes ArchUnit apenas se a nova estrutura exigir e se for escopo explícito.

5. **Sem JSON dentro de JSON**
   - Campos como `sourceIntentMix` devem ter colunas ou DTOs próprios, não JSON textual embutido em campo funcional.

6. **Sem metadado técnico no artefato final**
   - Scores e flags de auditoria podem existir em campos internos/contratuais.
   - Não devem ser serializados dentro de textos finais como rotina, dores ou evidência.

---

## 5. Plano por fases

### Fase 0 — Congelar diagnóstico e alinhar regra canônica

**Objetivo:** transformar esta decisão em regra explícita antes de mexer no pipeline produtivo.

Tarefas:

1. Atualizar `docs/canonical/oprm-canon.v1.md` com regra: pesquisa inicial NichoCNAE é neutra de solução.
2. Definir que IA, automação, app, sistema, ferramenta, curso, template e oferta só podem aparecer na pesquisa inicial se surgirem organicamente nas fontes, não no enquadramento do seed.
3. Definir separação entre:
   - `ROUTINE_NEUTRAL_RESEARCH`
   - `MECHANISM_DISCOVERY`
   - `OFFER_DISCOVERY`
4. Registrar a decisão em `docs/registros/oprm1.md`.

Critério de aceite:

- Cânone do OPRM descreve a fronteira de rotina neutra.
- Nenhuma etapa posterior pode usar solução como premissa da etapa de rotina.

---

### Fase 1 — Neutralizar a entrada do ciclo

**Objetivo:** impedir que um nome de nicho enviesado contamine todo o ciclo.

Backend — tarefas:

1. Criar normalizador de nome operacional no pacote OPRM NichoCNAE, por exemplo:
   - `NeutralNicheNameBuilder`
   - ou método privado coeso em serviço dedicado, se a responsabilidade ficar pequena.
2. O normalizador deve remover prefixos/enquadramentos de solução, como:
   - `IA para crescimento de`
   - `automação para`
   - `sistema para`
   - `app para`
   - `curso para`
   - `ferramenta para`
   - `marketing digital para`
3. Priorizar `cnaeDescription` como fallback seguro quando o nome do candidato ficar vazio ou genérico.
4. Adicionar campos persistidos por migration Liquibase MySQL 5.7, se necessário:
   - `original_niche_name`
   - `neutral_niche_name`
   - `research_mode`
   - `solution_bias_risk_score`
5. Ajustar `BackendRoutineResearchOrchestratorService#createCycle` para gravar nome neutro no campo usado pelas etapas seguintes e preservar o original em campo próprio.

Coletor — tarefas:

1. Ajustar DTOs de pending/detail para receber o nome neutro e, quando necessário, o original.
2. Garantir logs com `researchCycleId`, `sourceNicheId`, `originalNicheName`, `neutralNicheName` e `researchMode`.

Testes:

1. Teste unitário para normalização de nomes com termos de solução.
2. Teste do orquestrador garantindo que ciclo criado usa nome neutro.
3. Teste garantindo que nome original é preservado para auditoria.

Critério de aceite:

- Um candidato chamado `IA para crescimento de Cabeleireiros, manicure e pedicure` deve gerar ciclo de rotina com nome operacional neutro, por exemplo `Cabeleireiros, manicure e pedicure` ou `Cabeleireiros, manicures e pedicures`.

---

### Fase 2 — Separar contrato de pesquisa de rotina e pesquisa comercial

**Objetivo:** impedir que a etapa de seed gere queries de produto/oferta durante rotina neutra.

Backend — tarefas:

1. Alterar validação de `BackendNicheResearchSeedBuilderService` para aceitar, no modo `ROUTINE_NEUTRAL_RESEARCH`, apenas objetivos de rotina:
   - `ROUTINE_DISCOVERY`
   - `NICHE_OWNER_QUESTION_DISCOVERY`
   - `FINAL_CUSTOMER_QUESTION_DISCOVERY`
   - `PAIN_DISCOVERY`
   - `LANGUAGE_DISCOVERY`
   - `OPERATIONAL_CONTEXT_DISCOVERY`
2. Remover ou bloquear para rotina neutra:
   - `PRODUCT_SERVICE_DISCOVERY`
   - `OFFER_PATTERN_DISCOVERY`
   - `SALES_PAIN_DISCOVERY`, se estiver puxando copy/oferta em vez de rotina.
3. Incluir validação de termos proibidos nas queries quando não forem parte do CNAE original:
   - `IA`, `inteligência artificial`, `automação`, `software`, `sistema`, `app`, `ferramenta`, `curso`, `template`, `oferta`, `landing page`.
4. Caso o modelo gere query enviesada, rejeitar a conclusão da etapa com erro claro e log contextual, para reprocessamento.

Coletor — tarefas:

1. Ajustar `NicheResearchSeedBuilderPromptBuilder` para prompt de rotina pura.
2. Ajustar `NicheResearchSeedBuilderSchema` para objetivos permitidos por modo.
3. Ajustar `NicheResearchSeedBuilderValidator` para rejeitar termos de solução em rotina neutra.
4. Manter modelo/IA encapsulado no pacote da etapa, conforme cânone OPRM.

Prompt proposto para a etapa dois:

```text
Objetivo: pesquisar a rotina real do nicho CNAE, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.
Gere queries sobre dia a dia, tarefas, perguntas, dificuldades, decisões, atendimento, agenda, materiais, clientes, retrabalho, perdas, sazonalidade e linguagem do público.
Não use termos de solução, exceto se fizerem parte literal do CNAE ou do nome neutro validado.
```

Testes:

1. Teste do prompt garantindo presença de regra anti-solução.
2. Teste do validator rejeitando query com `IA para crescimento...`.
3. Teste do backend rejeitando `OFFER_PATTERN_DISCOVERY` em modo rotina neutra.
4. Teste garantindo que 12–15 queries neutras são aceitas.

Critério de aceite:

- Nenhuma query da fase de rotina neutra deve nascer procurando IA, app, sistema, curso, ferramenta ou oferta.

---

### Fase 3 — Classificar intenção de fonte e reduzir fontes vendor/oferta

**Objetivo:** evitar que a busca pública seja dominada por páginas de solução.

Backend — tarefas:

1. Adicionar campos em `OprmSourceCandidate` e/ou `OprmSourceSnapshot`:
   - `source_intent`
   - `vendor_solution_risk`
   - `offer_page_risk`
   - `routine_evidence_score`
2. Atualizar endpoints de conclusão da etapa três e quatro para receber esses campos.
3. Atualizar detalhes da tela para exibir mix de fontes por intenção.

Coletor — tarefas:

1. Criar classificador determinístico de domínio/título/snippet antes de concluir `SourceSearcher`.
2. Classificar como risco alto quando URL/título/snippet contiver padrões como:
   - `/pricing`, `/preco`, `/planos`, `/software`, `/app`, `/curso`, `/comprar`, `/oferta`, `/landing`, `CRM`, `SaaS`.
3. Limitar fontes vendor/oferta por query na rotina neutra.
4. Preferir fontes de rotina, perguntas, fóruns públicos, guias operacionais, sindicatos, associações, documentos técnicos e conteúdo educacional não vendedor.

Testes:

1. Teste de classificação de fonte vendor.
2. Teste de classificação de fonte rotina/pergunta.
3. Teste garantindo que fontes de solução não dominam a lista persistida.

Critério de aceite:

- A etapa três deve registrar e expor a intenção das fontes.
- O pipeline deve conseguir explicar se a pesquisa foi baseada em rotina real ou em páginas de solução.

---

### Fase 4 — Reestruturar taxonomia de sinais para rotina primeiro

**Objetivo:** separar evidência de rotina de mecanismo candidato.

Coletor — tarefas:

1. Ajustar `SignalExtractorEngine` para nova taxonomia mínima:
   - `ROUTINE_TASK`
   - `OPERATIONAL_FRICTION`
   - `PAIN_POINT`
   - `NICHE_OWNER_QUESTION`
   - `FINAL_CUSTOMER_QUESTION`
   - `COMMERCIAL_OBJECT`
   - `LANGUAGE_MARKER`
   - `PROOF_SIGNAL`
   - `MECHANISM_CANDIDATE_SECONDARY`
   - `SOLUTION_BIAS_MARKER`
2. Remover `ia` como gatilho direto de mecanismo aprovado.
3. Quando `ia`, `automação`, `software`, `app`, `sistema`, `ferramenta` aparecerem cedo, gerar também `SOLUTION_BIAS_MARKER`.
4. Reduzir confiança padrão de mecanismo quando a evidência vem de fonte vendor.

Backend — tarefas:

1. Atualizar validações/contadores da etapa cinco para reconhecer os novos tipos.
2. Garantir que os detalhes da etapa exibam sinais de viés separadamente.

Testes:

1. Teste de extração de rotina sem mecanismo.
2. Teste em que `IA` gera marcador de risco, não aprovação automática de mecanismo.
3. Teste de contadores por novo tipo.

Critério de aceite:

- Um snapshot sobre agenda, atendimento e faltas deve enriquecer rotina/dor mesmo sem citar solução.
- Um snapshot sobre software/IA deve ser tratado com cautela e risco de viés.

---

### Fase 5 — Ajustar síntese e quality gate para aprovar rotina, não solução

**Objetivo:** fazer a aprovação depender da qualidade da rotina/dor, não da presença obrigatória de mecanismo.

Coletor — tarefas:

1. Ajustar `RoutineSynthesizerEngine` para:
   - usar nome neutro;
   - separar bloco de rotina/dor de bloco de mecanismo candidato;
   - não misturar `LANGUAGE_MARKER` com mecanismo;
   - gerar bloco de risco de viés quando houver `SOLUTION_BIAS_MARKER`.
2. Ajustar `RoutineQualityGateEngine` para substituir `hasMinimumSignalMix` atual por mix de rotina neutra:
   - rotina/tarefa presente;
   - dor/fricção presente;
   - pergunta ou linguagem presente;
   - diversidade mínima de fonte;
   - risco de viés abaixo do limite.
3. Criar `solutionBiasRiskScore` calculado por concentração de sinais/fonte de solução.
4. Bloquear `readyForHypothesis=true` quando houver alto risco de viés e baixa evidência de rotina.

Backend — tarefas:

1. Persistir novos scores no cartão de rotina.
2. Expor novos campos nos detalhes da etapa sete e oito.
3. Atualizar `BackendRoutineQualityGateService` para usar os novos contadores.

Testes:

1. Quality gate aprova rotina rica sem mecanismo explícito.
2. Quality gate reprova pesquisa dominada por IA/software com pouca rotina.
3. Quality gate mantém reprovação por genericidade/duplicação.

Critério de aceite:

- O pipeline deve aprovar um cartão quando a rotina e as dores estão claras, mesmo sem solução definida.
- O pipeline deve reprovar cartão que apenas confirma uma tese de IA sem evidência forte do dia a dia.

---

### Fase 6 — Materialização e tela com rastreabilidade de neutralidade

**Objetivo:** tornar auditável para o usuário por que o nicho enriquecido é confiável e livre de viés inicial.

Backend — tarefas:

1. Atualizar `BackendEnrichedNicheMaterializerService` para persistir campos de neutralidade no perfil enriquecido.
2. Garantir que `market_niche.name` use nome neutro e não a tese de solução.
3. Manter `originalNicheName` apenas como auditoria.

Frontend — tarefas:

1. Atualizar `/oprm/pipeline` para exibir:
   - nome original;
   - nome neutro pesquisado;
   - modo da pesquisa;
   - mix de objetivos das queries;
   - mix de intenção das fontes;
   - score de risco de viés;
   - motivo da decisão do quality gate.
2. Evitar excesso de informação: mostrar resumo no card e detalhes em expansão/modal.
3. Todos os links de manual/apoio devem abrir com `target="_blank"`.

Testes:

1. Teste frontend dos componentes de resumo, se houver infraestrutura disponível.
2. Testes backend de DTO dos detalhes.
3. Teste de materialização garantindo que nome de solução não vira `market_niche.name`.

Critério de aceite:

- A tela deve deixar claro se a pesquisa foi realmente focada em rotina ou se houve risco de contaminação por solução.

---

### Fase 7 — Backfill controlado dos ciclos já contaminados

**Objetivo:** corrigir registros já materializados com viés sem apagar rastreabilidade.

Tarefas:

1. Criar diagnóstico SQL/MCP para localizar ciclos/perfis com termos de solução no nome:
   - `IA para`
   - `automação para`
   - `sistema para`
   - `app para`
   - `software para`
   - `curso para`
2. Marcar esses ciclos/perfis como candidatos a reprocessamento neutro.
3. Evitar editar manualmente textos de rotina/dor já gerados; preferir novo ciclo de pesquisa neutra.
4. Manter vínculo com ciclo anterior para auditoria, se for necessário.

Critério de aceite:

- O sistema não deve continuar usando como base principal um nicho enriquecido contaminado por tese de solução.

---

## 6. Ordem recomendada de implementação

1. **Fase 0** — alinhar cânone e regra.
2. **Fase 1** — neutralizar nome na origem.
3. **Fase 2** — bloquear queries enviesadas.
4. **Fase 5 parcial** — ajustar quality gate para não exigir mecanismo.
5. **Fase 4** — refinar taxonomia de sinais.
6. **Fase 3** — classificar fontes e reduzir vendor/oferta.
7. **Fase 6** — melhorar tela e materialização.
8. **Fase 7** — reprocessar registros antigos.

Justificativa:

- Primeiro corrige a causa-raiz de contaminação: entrada e prompt.
- Depois impede aprovação indevida por mecanismo.
- Em seguida melhora classificação e observabilidade.
- Por último trata dados históricos.

---

## 7. Riscos e mitigação

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| Reduzir demais fontes por bloquear termos de solução | Pipeline pode ficar sem evidência suficiente | Usar bloqueio progressivo: primeiro marcar risco, depois limitar, só bloquear quando concentração for alta |
| Modelo continuar gerando queries com IA | Seed falha repetidamente | Prompt + validator determinístico + log claro para causa-raiz |
| Alterar contrato quebra tela | Regressão frontend | Versionar campos novos como opcionais primeiro e atualizar tela na fase 6 |
| Quality gate aprovar rotina fraca sem mecanismo | Nicho enriquecido superficial | Reforçar critérios de rotina/dor/fonte/linguagem antes de remover requisito de mecanismo |
| Backfill modificar dados históricos sem rastreio | Perda de auditoria | Preferir novo ciclo neutro, mantendo ciclo antigo como evidência histórica |

---

## 8. Checklist técnico por PR futuro

Cada PR de implementação deve validar:

- [ ] Classes Java alteradas possuem comentário de responsabilidade.
- [ ] Métodos novos/alterados possuem comentário breve.
- [ ] Catches de `Exception`, `RuntimeException` ou integração registram log com contexto e stack trace.
- [ ] Backend continua sendo fonte de verdade.
- [ ] Coletor OPRM chama apenas endpoints do módulo OPRM.
- [ ] Não há JSON textual dentro de outro JSON.
- [ ] Não há metadado técnico em texto funcional final.
- [ ] Testes unitários do módulo Java alterado foram executados.
- [ ] Documentos canônicos e registros OPRM foram atualizados quando houver mudança de regra.

---

## 9. Resultado esperado para o exemplo da tela

Entrada observada:

```text
IA para crescimento de Cabeleireiros, manicure e pedicure
CNAE 9602501
```

Com o plano implementado, o ciclo deveria operar internamente como:

```text
originalNicheName: IA para crescimento de Cabeleireiros, manicure e pedicure
neutralNicheName: Cabeleireiros, manicures e pedicures
researchMode: ROUTINE_NEUTRAL_RESEARCH
```

Queries esperadas:

- `rotina de cabeleireiros manicures e pedicures em salão pequeno`
- `problemas de agenda em salão de beleza pequeno`
- `como manicures organizam horários e retornos de clientes`
- `dificuldades no atendimento diário de cabeleireiros autônomos`
- `faltas cancelamentos e atrasos em salão de beleza`
- `controle de materiais produtos e procedimentos em salão de beleza`
- `perguntas frequentes de clientes de manicure e pedicure`

Queries que devem ser rejeitadas na rotina neutra:

- `IA para crescimento de salão de beleza`
- `automação para manicure vender mais`
- `software para salão de beleza com inteligência artificial`
- `curso de marketing digital para cabeleireiros`
- `oferta pronta para salão de beleza`

Resultado esperado:

- O nicho enriquecido descreve rotina, dores, linguagem, evidências e resultados desejados.
- IA aparece apenas depois, na fase de mecanismo, se a rotina/dor justificar.
