# Plano de implementação — NichoCNAE focado em rotina real e dificuldades do nicho

## 1. Objetivo

Revisar o pipeline **OPRM NichoCNAE** exibido na tela `/oprm/pipeline` e propor ajustes para que a pesquisa inicial entregue uma visão realista do nicho:

- o que o profissional faz no dia a dia;
- quais tarefas se repetem;
- quais dificuldades aparecem na rotina;
- quais perdas, atritos, dúvidas e improvisos são observáveis;
- quais palavras o próprio público usa para descrever sua realidade.

Esta fase **não deve executar framework comercial**, não deve propor solução e não deve preparar hipótese de produto. A função do NichoCNAE, neste ponto, é levantar realidade operacional confiável.

A fonte primária desta revisão é o **código atual**. A documentação canônica foi usada apenas como referência de fronteira, porque pode estar desatualizada em relação ao comportamento implementado.

---

## 2. Decisão de escopo

A pesquisa NichoCNAE deve parar em:

1. **rotina real do nicho**;
2. **tarefas concretas executadas pelo profissional**;
3. **dificuldades e fricções observadas**;
4. **perguntas recorrentes do profissional e do cliente final**;
5. **contexto operacional**: agenda, atendimento, materiais, fornecedores, sazonalidade, cobrança, comunicação, retrabalho, faltas, atrasos e organização;
6. **evidências públicas curtas**, com fontes e trechos suficientes para auditoria.

Ficam fora deste plano:

- criação de produto;
- criação de promessa;
- criação de oferta;
- criação de campanha;
- criação de landing page;
- definição de mecanismo de solução;
- recomendação de IA, automação, app, sistema, curso ou ferramenta.

Esses itens podem existir em outro fluxo futuro, mas não fazem parte da pesquisa de rotina do NichoCNAE.

---

## 3. Diagnóstico executivo

O pipeline atual já tem boa separação modular entre backend e coletor OPRM, usa etapas com responsabilidades claras e mantém o backend como fonte de verdade. O problema principal é que alguns contratos e heurísticas ainda permitem que a pesquisa seja contaminada por linguagem de solução antes de entender a rotina.

Pontos de causa-raiz encontrados no código:

1. **Entrada enviesada do nome do nicho**
   - A etapa zero copia `candidate.getCandidateNicheName()` diretamente para o ciclo com `cycle.setNicheName(...)`.
   - Se o candidato chega como `IA para crescimento de ...`, todo o ciclo passa a pesquisar a tese de IA, não a ocupação real.
   - Esse nome passa para prompt, queries, busca pública, sinais, síntese e perfil final.

2. **Prompt da etapa de seed mistura rotina com intenção comercial**
   - O prompt afirma que o objetivo é conhecer a rotina.
   - Porém, também manda cobrir `produtos/serviços` e permite objetivos como `PRODUCT_SERVICE_DISCOVERY` e `OFFER_PATTERN_DISCOVERY`.
   - Isso puxa a busca para páginas de solução, ferramentas, cursos e comparativos, antes de mapear o trabalho cotidiano.

3. **Extração de sinais e quality gate valorizam solução cedo demais**
   - O extrator classifica termos como `sistema`, `automação` e `ia` como oportunidade operacional relevante.
   - O quality gate atual exige contagem de oportunidade de solução para aprovar o cartão.
   - Assim, uma pesquisa contaminada por páginas de software/IA pode parecer mais completa do que uma pesquisa realista de rotina.

Conclusão: o ajuste deve corrigir a causa-raiz na entrada, no prompt, nos objetivos aceitos, na seleção/classificação de fontes, na taxonomia de sinais, na síntese e no gate de qualidade.

---

## 4. Mapa do pipeline atual conforme o código

### 4.1 Etapa zero — `oprmRoutineResearchOrchestrator`

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

Risco para a pesquisa realista:

- Se `candidateNicheName` contém tese de solução, o pipeline passa a pesquisar a tese, não o nicho.
- Exemplo: `IA para crescimento de Cabeleireiros, manicure e pedicure` leva o sistema a procurar IA/crescimento, quando deveria procurar rotina de cabeleireiros, manicures e pedicures.

Ajuste necessário:

- Gerar um **nome operacional neutro** para a pesquisa.
- Preservar o nome original apenas para auditoria.
- Usar a descrição CNAE como fallback quando o nome original estiver contaminado.

---

### 4.2 Etapa um — `oprmRoutineResearchCycle`

**Responsabilidade atual:** controlar e detalhar o ciclo já aberto.

Arquivos principais:

- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routineresearchcycle/service/BackendRoutineResearchCycleService.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchcycle/RoutineResearchCycleService.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routineresearchcycle/RoutineResearchCycleProcessor.java`

Comportamento observado:

- Lista ciclos com status `RUNNING`.
- Detalha totais de queries, fontes, snapshots e sinais.
- Não transforma o conteúdo do nicho; apenas expõe o estado operacional.

Risco para a pesquisa realista:

- Baixo na lógica própria da etapa.
- O risco é exibir e propagar o nome enviesado criado na etapa zero.

Ajuste necessário:

- Exibir no detalhe operacional:
  - nome original recebido;
  - nome neutro usado na pesquisa;
  - modo da pesquisa: `ROUTINE_REALITY_RESEARCH`;
  - risco de contaminação por linguagem de solução.

---

### 4.3 Etapa dois — `oprmNicheResearchSeedBuilder`

**Responsabilidade atual:** usar IA no próprio `oprm-coletor-mei` para transformar CNAE/ciclo em seed e queries.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderPromptBuilder.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/OpenAiNicheResearchSeedBuilderClient.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderSchema.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/nicheresearchseedbuilder/NicheResearchSeedBuilderValidator.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/nicheresearchseedbuilder/service/BackendNicheResearchSeedBuilderService.java`

Comportamento observado:

- O prompt diz que a etapa deve conhecer a rotina.
- O prompt também pede cobertura de `produtos/serviços`.
- O contrato aceita objetivos de query ligados a descoberta de produto e padrão de oferta.
- O backend persiste até 15 queries retornadas pela IA.

Risco para a pesquisa realista:

- Alto.
- A IA pode criar queries procurando solução quando deveria procurar rotina, tarefas e dificuldades.
- A regra “cada query deve conter o nome do nicho” replica o viés quando o nome está contaminado.

Ajuste necessário:

- O prompt deve solicitar apenas rotina, tarefas, dificuldades, perguntas e contexto operacional.
- O schema e o backend devem aceitar apenas objetivos compatíveis com rotina real.
- Queries com termos de solução devem ser rejeitadas, salvo quando o termo fizer parte literal da descrição CNAE ou aparecer como linguagem orgânica em fonte pública depois da busca.

---

### 4.4 Etapa três — `oprmSourceSearcher`

**Responsabilidade atual:** executar queries em provedor público e registrar fontes candidatas.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcesearcher/SourceSearcherProcessor.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcesearcher/DuckDuckGoHtmlSourceSearchProvider.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcesearcher/service/BackendSourceSearcherService.java`

Comportamento observado:

- O processor executa a query exatamente como recebida.
- O provedor usa DuckDuckGo HTML e retorna até 20 resultados por query.
- Não há classificação de fonte por aderência à rotina real.

Risco para a pesquisa realista:

- Médio/alto, herdado da etapa dois.
- Se a query busca solução, as fontes candidatas serão dominadas por solução.
- O pipeline não mede se a fonte é relato de rotina, conteúdo educativo, página de ferramenta, página de curso ou página comercial.

Ajuste necessário:

- Classificar intenção da fonte antes de persistir.
- Preferir fontes que descrevem rotina, problemas operacionais, perguntas reais, guias práticos e conteúdo público não vendedor.
- Marcar fontes comerciais como risco de contaminação, sem usá-las como base principal da pesquisa.

---

### 4.5 Etapa quatro — `oprmSourceFetcher`

**Responsabilidade atual:** coletar snapshot curto das fontes candidatas.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcefetcher/JsoupPublicSourceFetcher.java`
- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/sourcefetcher/SourceFetcherProcessor.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/sourcefetcher/service/BackendSourceFetcherService.java`

Comportamento observado:

- Coleta HTML público com Jsoup.
- Registra log do payload bruto recebido, conforme regra de ingestão.
- Persiste apenas metadados, snippet e `shortExcerpt`; não armazena HTML completo.

Risco para a pesquisa realista:

- Baixo na coleta.
- Médio na seleção de quais fontes serão coletadas, porque depende da etapa três.

Ajuste necessário:

- Propagar a classificação de fonte até o snapshot.
- Persistir indicadores simples:
  - `sourceIntent`;
  - `routineEvidenceScore`;
  - `commercialPageRisk`;
  - `solutionLanguageRisk`.
- Manter a política de snapshot curto; ela está correta.

---

### 4.6 Etapa cinco — `oprmSignalExtractor`

**Responsabilidade atual:** extrair sinais estruturados a partir de snapshots curtos.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/signalextractor/SignalExtractorEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/signalextractor/service/BackendSignalExtractorService.java`
- `backend/ads-service/src/test/java/com/marketinghub/oprm/nichocnae/signalextractor/architecture/OprmSignalExtractorArchitectureTest.java`

Comportamento observado:

- Usa regras determinísticas simples por palavras-chave.
- Classifica tarefas de agenda/atendimento/cliente como `ROUTINE_TASK`.
- Classifica problemas como `PAIN_POINT`.
- Também classifica termos de solução como sinal positivo, incluindo `sistema`, `automação` e `ia`.

Risco para a pesquisa realista:

- Alto quando termos de solução viram sinal positivo da rotina.
- A etapa deve descrever o que acontece no trabalho, não validar uma proposta de solução.

Ajuste necessário:

- Reorganizar a taxonomia para rotina e dificuldade:
  - `ROUTINE_TASK`;
  - `OPERATIONAL_FRICTION`;
  - `PAIN_POINT`;
  - `NICHE_OWNER_QUESTION`;
  - `FINAL_CUSTOMER_QUESTION`;
  - `COMMERCIAL_OBJECT`;
  - `LANGUAGE_MARKER`;
  - `CONTEXT_MARKER`;
  - `SEASONALITY_MARKER`;
  - `SOLUTION_LANGUAGE_RISK`.
- Termos como IA, automação, app, sistema, ferramenta e curso devem gerar risco de contaminação quando aparecem cedo demais ou em fontes comerciais.
- Esses termos não devem ser requisito nem sinal positivo para aprovação da pesquisa de rotina.

---

### 4.7 Etapa seis — `oprmRoutineSynthesizer`

**Responsabilidade atual:** sintetizar cartão de rotina a partir dos sinais estruturados.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routinesynthesizer/RoutineSynthesizerEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinesynthesizer/service/BackendRoutineSynthesizerService.java`

Comportamento observado:

- Monta blocos de rotina, dores, resultados, oportunidades e evidências.
- Cada bloco cita sinais e evidências.
- O texto final usa `pending.nicheName()` no título de cada bloco.

Risco para a pesquisa realista:

- Médio/alto.
- Se o nome do nicho estiver enviesado, ele aparece em todos os blocos.
- A síntese atual ainda organiza parte do conteúdo pensando em uso comercial posterior.

Ajuste necessário:

- Usar nome neutro operacional.
- Trocar a estrutura do cartão para visão de rotina real:
  - resumo do dia a dia;
  - tarefas recorrentes;
  - dificuldades observadas;
  - dúvidas do profissional;
  - dúvidas do cliente final;
  - pontos de perda de tempo, dinheiro, energia ou qualidade;
  - linguagem pública do nicho;
  - evidências e fontes.
- Remover qualquer bloco que sugira solução, produto, hipótese ou caminho comercial.

---

### 4.8 Etapa sete — `oprmRoutineQualityGate`

**Responsabilidade atual:** decidir se o cartão está pronto para alimentar etapas posteriores.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/routinequalitygate/RoutineQualityGateEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/routinequalitygate/service/BackendRoutineQualityGateService.java`

Comportamento observado:

- Exige fontes, sinais, especificidade, confiança e baixa duplicação.
- Exige mix mínimo com pergunta, dor, oportunidade e tarefa.
- Aprova quando considera o cartão suficiente.

Risco para a pesquisa realista:

- Alto por exigir oportunidade como condição de aprovação.
- Uma pesquisa boa de rotina pode ser reprovada por não sugerir solução.
- Uma pesquisa enviesada por IA/software pode ser aprovada porque gerou muitos sinais de solução.

Ajuste necessário:

- Aprovar apenas pela qualidade da rotina e das dificuldades observadas.
- Critérios mínimos recomendados:
  - tarefas recorrentes identificadas;
  - dificuldades concretas identificadas;
  - perguntas reais do profissional ou cliente;
  - evidências vindas de múltiplas fontes;
  - baixa duplicação;
  - baixo risco de linguagem de solução;
  - textos específicos e auditáveis.
- Remover qualquer exigência de oportunidade de solução.

---

### 4.9 Etapa oito — `oprmEnrichedNicheMaterializer`

**Responsabilidade atual:** materializar o cartão aprovado em `market_niche` e `market_niche_enrichment_profile`.

Arquivos principais:

- `oprm-coletor-mei/src/main/java/com/marketinghub/nichocnae/enrichednichematerializer/EnrichedNicheMaterializerEngine.java`
- `backend/ads-service/src/main/java/com/marketinghub/oprm/nichocnae/enrichednichematerializer/service/BackendEnrichedNicheMaterializerService.java`

Comportamento observado:

- Cria campos complementares e persiste rotina, dores, resultados, oportunidades, evidências e scores no perfil enriquecido.
- Atualiza ciclo e candidato para `ENRICHED_NICHE_CREATED`.

Risco para a pesquisa realista:

- Médio, por eternizar no perfil aquilo que veio das etapas anteriores.
- Se a pesquisa estiver contaminada, o perfil enriquecido também fica contaminado.

Ajuste necessário:

- Materializar apenas campos compatíveis com pesquisa de rotina:
  - rotina;
  - tarefas;
  - dificuldades;
  - perguntas;
  - contexto operacional;
  - linguagem;
  - evidências;
  - fontes;
  - scores de qualidade e risco de contaminação.
- Usar nome neutro em `market_niche.name`.
- Preservar nome original apenas como auditoria.

---

## 5. Princípios arquiteturais para implementação

1. **Backend continua sendo fonte de verdade**
   - Entidades, status, contadores, contratos internos e persistência ficam no backend.
   - O coletor OPRM executa etapas e chama apenas endpoints do próprio módulo OPRM.

2. **Sem chamada direta entre módulos**
   - Frontend, coletor e demais serviços não devem falar diretamente entre si.
   - Todo contrato operacional passa pelo backend OPRM.

3. **Java com comentários obrigatórios**
   - Toda classe alterada deve ter comentário de responsabilidade básica.
   - Todo método novo ou alterado deve ter comentário breve.

4. **Sem alterar `ArquiteturaTest` sem solicitação direta**
   - Ajustes devem priorizar testes unitários de regra de negócio.
   - Testes ArchUnit só devem ser alterados se a mudança arquitetural for explicitamente necessária.

5. **Sem JSON dentro de JSON**
   - Indicadores como `sourceIntent` e `solutionLanguageRisk` devem ter contrato/coluna próprios ou DTO estruturado.
   - Não serializar JSON técnico dentro de texto funcional.

6. **Sem metadado técnico em texto final**
   - Scores e flags podem existir em campos próprios.
   - Não devem aparecer misturados em resumos de rotina ou dificuldades.

---

## 6. Plano por fases

### Fase 0 — Alinhar regra de escopo

**Objetivo:** deixar explícito que NichoCNAE pesquisa rotina e dificuldades, não solução.

Tarefas:

1. Atualizar `docs/canonical/oprm-canon.v1.md` com a regra de pesquisa de realidade operacional.
2. Definir `ROUTINE_REALITY_RESEARCH` como modo da pesquisa inicial.
3. Registrar que a pesquisa deve parar em rotina, tarefas, dificuldades, perguntas, contexto e evidências.
4. Registrar que solução, produto, campanha e hipótese pertencem a outro fluxo.
5. Registrar a decisão em `docs/registros/oprm1.md`.

Critério de aceite:

- O cânone OPRM deixa claro que o NichoCNAE não deve procurar solução na fase inicial.

---

### Fase 1 — Neutralizar a entrada do ciclo

**Objetivo:** impedir que um nome contaminado direcione a pesquisa.

Backend — tarefas:

1. Criar normalizador de nome operacional no pacote OPRM NichoCNAE.
2. Remover prefixos e enquadramentos de solução, como:
   - `IA para crescimento de`;
   - `automação para`;
   - `sistema para`;
   - `app para`;
   - `software para`;
   - `curso para`;
   - `ferramenta para`;
   - `marketing digital para`.
3. Usar `cnaeDescription` como fallback quando o nome normalizado ficar fraco.
4. Persistir, se necessário por Liquibase MySQL 5.7:
   - `original_niche_name`;
   - `neutral_niche_name`;
   - `research_mode`;
   - `solution_language_risk_score`.
5. Ajustar `BackendRoutineResearchOrchestratorService#createCycle` para usar o nome neutro nas etapas seguintes.

Coletor — tarefas:

1. Ajustar DTOs de pending/detail para receber nome neutro, nome original e modo da pesquisa.
2. Garantir logs com `researchCycleId`, `sourceNicheId`, `originalNicheName`, `neutralNicheName` e `researchMode`.

Testes:

1. Teste unitário de normalização de nomes contaminados.
2. Teste do orquestrador garantindo ciclo com nome neutro.
3. Teste garantindo preservação do nome original para auditoria.

Critério de aceite:

- Um candidato `IA para crescimento de Cabeleireiros, manicure e pedicure` deve iniciar pesquisa como `Cabeleireiros, manicure e pedicure` ou nome equivalente neutro.

---

### Fase 2 — Reescrever seed e queries para rotina/dificuldades

**Objetivo:** impedir que a IA gere queries procurando solução.

Backend — tarefas:

1. Alterar validação de `BackendNicheResearchSeedBuilderService` para aceitar apenas objetivos de rotina:
   - `ROUTINE_DISCOVERY`;
   - `ROUTINE_TASK_DISCOVERY`;
   - `OPERATIONAL_DIFFICULTY_DISCOVERY`;
   - `NICHE_OWNER_QUESTION_DISCOVERY`;
   - `FINAL_CUSTOMER_QUESTION_DISCOVERY`;
   - `LANGUAGE_DISCOVERY`;
   - `OPERATIONAL_CONTEXT_DISCOVERY`.
2. Bloquear objetivos ligados a produto, oferta, campanha ou solução.
3. Rejeitar queries com termos de solução quando eles não forem parte literal do CNAE:
   - `IA`;
   - `inteligência artificial`;
   - `automação`;
   - `software`;
   - `sistema`;
   - `app`;
   - `ferramenta`;
   - `curso`;
   - `template`;
   - `oferta`;
   - `landing page`.
4. Registrar falhas de contrato com log contextual para reprocessamento.

Coletor — tarefas:

1. Ajustar `NicheResearchSeedBuilderPromptBuilder` para prompt de rotina pura.
2. Ajustar `NicheResearchSeedBuilderSchema` para os objetivos permitidos.
3. Ajustar `NicheResearchSeedBuilderValidator` para rejeitar query contaminada.
4. Manter chamadas ao modelo encapsuladas no pacote da etapa.

Prompt proposto para a etapa dois:

```text
Objetivo: pesquisar a rotina real do nicho CNAE, sem assumir solução, IA, automação, produto, curso, ferramenta ou oferta.
Gere queries sobre dia a dia, tarefas, dificuldades, decisões, atendimento, agenda, materiais, clientes, retrabalho, perdas, sazonalidade, cobrança, comunicação e linguagem do público.
Não proponha solução. Não procure produto. Não procure oferta. Não procure ferramenta.
```

Testes:

1. Teste do prompt garantindo regra explícita de rotina/dificuldades.
2. Teste do validator rejeitando query com `IA para crescimento...`.
3. Teste do backend rejeitando objetivo comercial.
4. Teste garantindo aceitação de 12–15 queries neutras.

Critério de aceite:

- Nenhuma query da pesquisa inicial deve nascer procurando solução.

---

### Fase 3 — Classificar fonte pela aderência à rotina real

**Objetivo:** evitar que páginas comerciais dominem a base de evidência.

Backend — tarefas:

1. Adicionar campos em `OprmSourceCandidate` e/ou `OprmSourceSnapshot`:
   - `source_intent`;
   - `routine_evidence_score`;
   - `commercial_page_risk`;
   - `solution_language_risk`.
2. Atualizar endpoints de conclusão da etapa três e quatro para receber esses campos.
3. Atualizar detalhes da tela para exibir mix de fontes por intenção.

Coletor — tarefas:

1. Criar classificador determinístico de domínio, título e snippet.
2. Classificar fontes em grupos como:
   - `ROUTINE_CONTENT`;
   - `PUBLIC_QUESTION`;
   - `EDUCATIONAL_CONTEXT`;
   - `PROFESSIONAL_GUIDE`;
   - `COMMERCIAL_PAGE`;
   - `SOLUTION_PAGE`;
   - `LOW_RELEVANCE`.
3. Priorizar fontes dos quatro primeiros grupos.
4. Limitar fontes comerciais e páginas de solução para não contaminarem a síntese.

Testes:

1. Teste de classificação de fonte de rotina.
2. Teste de classificação de página comercial.
3. Teste garantindo que páginas comerciais não dominam as fontes persistidas.

Critério de aceite:

- A etapa três consegue explicar se a pesquisa foi baseada em rotina real ou em página comercial.

---

### Fase 4 — Reestruturar sinais para rotina e dificuldades

**Objetivo:** fazer a extração descrever a realidade do trabalho, não solução.

Coletor — tarefas:

1. Ajustar `SignalExtractorEngine` para taxonomia de rotina:
   - `ROUTINE_TASK`;
   - `OPERATIONAL_FRICTION`;
   - `PAIN_POINT`;
   - `NICHE_OWNER_QUESTION`;
   - `FINAL_CUSTOMER_QUESTION`;
   - `COMMERCIAL_OBJECT`;
   - `LANGUAGE_MARKER`;
   - `CONTEXT_MARKER`;
   - `SEASONALITY_MARKER`;
   - `SOLUTION_LANGUAGE_RISK`.
2. Termos de solução devem gerar risco, não sinal positivo.
3. Reduzir confiança de sinais extraídos de página comercial.
4. Preservar evidência curta e domínio da fonte para auditoria.

Backend — tarefas:

1. Atualizar validações e contadores da etapa cinco.
2. Expor sinais de risco separadamente dos sinais de rotina.
3. Garantir que sinais de risco não somam como suficiência da pesquisa.

Testes:

1. Teste de extração de tarefa de rotina.
2. Teste de extração de dificuldade operacional.
3. Teste em que `IA` gera `SOLUTION_LANGUAGE_RISK`, não sinal positivo.
4. Teste de contadores por novo tipo.

Critério de aceite:

- Um snapshot sobre agenda, atendimento e faltas deve enriquecer rotina/dificuldade.
- Um snapshot sobre software/IA deve alertar risco de contaminação.

---

### Fase 5 — Ajustar síntese e quality gate para visão realista

**Objetivo:** aprovar somente cartões que representem bem a rotina e as dificuldades.

Coletor — tarefas:

1. Ajustar `RoutineSynthesizerEngine` para gerar cartão com blocos:
   - rotina observada;
   - tarefas recorrentes;
   - dificuldades concretas;
   - perguntas do profissional;
   - perguntas do cliente final;
   - contexto operacional;
   - linguagem do nicho;
   - evidências e fontes;
   - alertas de contaminação por solução, quando existirem.
2. Ajustar `RoutineQualityGateEngine` para critérios mínimos:
   - `routineTaskCount > 0`;
   - `operationalDifficultyCount > 0` ou `painPointCount > 0`;
   - `questionSignalCount > 0` ou `languageMarkerCount > 0`;
   - fontes distintas suficientes;
   - baixa duplicação;
   - especificidade textual;
   - baixo `solutionLanguageRiskScore`.
3. Remover exigência de sinal de solução para aprovação.
4. Bloquear aprovação quando o conteúdo for dominado por linguagem de solução.

Backend — tarefas:

1. Persistir scores de qualidade da rotina:
   - `routineEvidenceScore`;
   - `difficultyEvidenceScore`;
   - `sourceDiversityScore`;
   - `solutionLanguageRiskScore`.
2. Expor esses campos nos detalhes da etapa sete e oito.
3. Atualizar status/notas para explicar a decisão em linguagem operacional.

Testes:

1. Gate aprova rotina rica sem qualquer solução sugerida.
2. Gate reprova conteúdo dominado por IA/software.
3. Gate reprova texto genérico ou duplicado.

Critério de aceite:

- O cartão aprovado deve permitir ao usuário entender como o nicho trabalha e onde sofre, sem sugerir o que vender.

---

### Fase 6 — Materialização, tela e dados históricos

**Objetivo:** tornar a pesquisa auditável e evitar que registros antigos contaminados continuem sendo usados como verdade.

Backend — tarefas:

1. Atualizar materialização para preservar apenas campos de rotina real e dificuldades.
2. Garantir que `market_niche.name` use nome neutro.
3. Preservar `originalNicheName` apenas como auditoria.
4. Criar diagnóstico para localizar ciclos/perfis com termos de solução no nome ou no conteúdo principal.
5. Preferir novo ciclo neutro em vez de editar manualmente textos antigos.

Frontend — tarefas:

1. Atualizar `/oprm/pipeline` para exibir:
   - nome original;
   - nome neutro pesquisado;
   - modo da pesquisa;
   - mix de objetivos das queries;
   - mix de intenção das fontes;
   - risco de linguagem de solução;
   - motivo da decisão do gate.
2. Mostrar resumo simples nos cards e detalhes sob expansão/modal.
3. Evitar excesso de informação e preservar foco do usuário: rotina, dificuldades, evidências.

Testes:

1. Teste backend de DTO dos detalhes.
2. Teste de materialização garantindo nome neutro.
3. Teste de diagnóstico para detectar registros contaminados.
4. Teste frontend dos componentes de resumo, se houver infraestrutura disponível.

Critério de aceite:

- A tela deve mostrar claramente se a pesquisa representa a rotina real do nicho ou se houve risco de contaminação por solução.

---

## 7. Ordem recomendada de implementação

1. **Fase 0** — alinhar regra de escopo.
2. **Fase 1** — neutralizar nome na origem.
3. **Fase 2** — bloquear queries enviesadas.
4. **Fase 5 parcial** — remover exigência de solução no gate.
5. **Fase 4** — refinar taxonomia de sinais para rotina/dificuldade.
6. **Fase 3** — classificar fontes e reduzir páginas comerciais.
7. **Fase 6** — melhorar tela, materialização e dados históricos.

Justificativa:

- Primeiro corrige a causa-raiz de contaminação: entrada e prompt.
- Depois impede aprovação indevida por conteúdo de solução.
- Em seguida melhora classificação, observabilidade e dados históricos.

---

## 8. Riscos e mitigação

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| Bloquear termos demais e reduzir evidências | Pipeline pode ficar sem fontes suficientes | Primeiro marcar risco e limitar peso; bloquear apenas quando a concentração for alta |
| Modelo continuar gerando queries contaminadas | Seed falha repetidamente | Prompt objetivo, validator determinístico e log claro para reprocessamento |
| Alterar contrato quebrar tela | Regressão frontend | Versionar campos novos como opcionais primeiro e atualizar tela na fase adequada |
| Gate aprovar rotina superficial | Perfil final fraco | Exigir tarefas, dificuldades, perguntas/linguagem, fontes distintas e especificidade |
| Backfill perder auditoria | Histórico fica inconsistente | Preferir novo ciclo neutro e preservar ciclo antigo como histórico |

---

## 9. Checklist técnico por PR futuro

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

## 10. Exemplo esperado para a tela anexada

Entrada observada:

```text
IA para crescimento de Cabeleireiros, manicure e pedicure
CNAE 9602501
```

Com o plano implementado, o ciclo deveria operar internamente como:

```text
originalNicheName: IA para crescimento de Cabeleireiros, manicure e pedicure
neutralNicheName: Cabeleireiros, manicures e pedicures
researchMode: ROUTINE_REALITY_RESEARCH
```

Queries esperadas:

- `rotina de cabeleireiros manicures e pedicures em salão pequeno`;
- `problemas de agenda em salão de beleza pequeno`;
- `como manicures organizam horários e retornos de clientes`;
- `dificuldades no atendimento diário de cabeleireiros autônomos`;
- `faltas cancelamentos e atrasos em salão de beleza`;
- `controle de materiais produtos e procedimentos em salão de beleza`;
- `perguntas frequentes de clientes de manicure e pedicure`.

Queries que devem ser rejeitadas na pesquisa de rotina:

- `IA para crescimento de salão de beleza`;
- `automação para manicure vender mais`;
- `software para salão de beleza com inteligência artificial`;
- `curso de marketing digital para cabeleireiros`;
- `oferta pronta para salão de beleza`.

Saída esperada do NichoCNAE:

- visão realista da rotina;
- tarefas recorrentes;
- dificuldades concretas;
- perguntas recorrentes;
- linguagem usada pelo público;
- evidências públicas curtas e auditáveis;
- alerta quando a pesquisa estiver contaminada por linguagem de solução.
