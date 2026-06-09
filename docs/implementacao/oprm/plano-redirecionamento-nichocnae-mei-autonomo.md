# Plano de redirecionamento do OPRM NichoCNAE para público-alvo MEI autônomo

## 1. Objetivo do plano

Redirecionar o pipeline OPRM NichoCNAE para construir um entendimento profundo e atualizado do **público-alvo de profissionais MEI/autônomos no Brasil**, sem avançar para produto, oferta, campanha, promessa comercial ou mecanismo de venda nesta fase.

O foco desta mudança é transformar o pipeline de uma pesquisa ampla por CNAE em uma pesquisa de **pessoa real que executa o trabalho**:

- profissional autônomo;
- MEI prestador de serviço;
- dono-operador que executa pessoalmente a atividade;
- trabalhador por conta própria que depende da própria rotina, agenda, atendimento, cobrança, reputação e aquisição de clientes para faturar.

O resultado esperado é que o OPRM entregue uma base mais útil para as etapas posteriores do Marketing Hub, descrevendo **comportamentos, rotinas, dores, sonhos, medos, linguagem e contexto real de trabalho** do MEI/autônomo brasileiro.

## 2. Princípios obrigatórios

Toda implementação deste plano deve obedecer aos seguintes princípios:

1. **Respeitar a arquitetura atual do sistema**
   - O backend continua sendo a única camada que acessa o banco de dados.
   - O coletor OPRM deve consumir exclusivamente endpoints do módulo OPRM no backend principal.
   - Controllers, services, DTOs, entidades, repositórios, Liquibase, frontend e testes devem seguir os padrões já existentes do projeto.
   - Novos endpoints só devem ser criados após verificar se já existe contrato aderente.

2. **Foco exclusivo em público-alvo, não em produto**
   - Não construir oferta.
   - Não sugerir produto digital.
   - Não criar hipótese de mecanismo.
   - Não criar promessa, headline, campanha ou landing page.
   - Não perguntar “o que vender para esse público” nesta fase.

3. **Foco em MEI/autônomo brasileiro**
   - A pesquisa deve ser Brasil-first.
   - Usar português do Brasil.
   - Priorizar fontes brasileiras, dados recentes e comportamento atual do mercado brasileiro.
   - Evitar material antigo, genérico ou ultrapassado quando houver fonte mais recente.

4. **Foco em comportamento humano e operacional**
   - Rotina real.
   - Atividades repetidas.
   - Dores sentidas.
   - Sonhos e aspirações.
   - Medos, frustrações e limitações.
   - Linguagem usada pelo próprio público.
   - Canais usados para trabalhar, vender, atender e cobrar.

5. **Fontes novas e rastreáveis**
   - Priorizar dados dos últimos 24 meses quando possível.
   - Aceitar fontes mais antigas apenas quando forem estruturais, oficiais ou ainda claramente válidas.
   - Registrar data de coleta, data de publicação quando disponível, domínio/fonte e motivo de uso.
   - Classificar risco de fonte antiga ou desatualizada.

6. **Uso de IA como apoio, não como fonte de verdade**
   - Modelos de texto podem gerar queries, classificar fontes, sintetizar sinais e separar personas.
   - A evidência deve vir de dados públicos, fontes recentes, banco de dados e registros rastreáveis.
   - Toda síntese deve preservar vínculo com evidências.

## 3. Resultado final esperado

Ao final do redirecionamento, cada ciclo NichoCNAE aprovado deve produzir um **perfil de público-alvo MEI/autônomo**, contendo no mínimo:

- CNAE base;
- nome neutro do nicho;
- ocupações/autodenominações prováveis dentro do CNAE;
- tipo de atuação predominante: autônomo solo, atendimento em domicílio, ponto fixo, parceria/salão, oficina, online, rua, evento, recorrente ou sob demanda;
- comportamento de aquisição de clientes;
- rotina diária/semanal;
- tarefas repetidas;
- gargalos operacionais;
- dores emocionais e práticas;
- sonhos e aspirações;
- medos e inseguranças;
- linguagem real usada pelo público;
- canais usados: WhatsApp, Instagram, Facebook, Google, marketplaces, indicação, grupos locais ou outros;
- sinais de atualidade das fontes;
- score de aderência a MEI/autônomo;
- score de evidência comportamental;
- score de risco de informação antiga;
- score de risco de desvio para empresa estruturada ou produto/oferta.

## 4. Etapas de implantação

As etapas abaixo foram desenhadas para serem executadas por um modelo de IA desenvolvedor, uma etapa por vez, com escopo claro, validação objetiva e baixo risco de conflito.

---

## Etapa 1 — Atualizar regra canônica OPRM para foco MEI/autônomo

### Objetivo

Registrar oficialmente que o pipeline NichoCNAE, quando originado do levantamento MEI, deve priorizar a construção de público-alvo de profissionais autônomos brasileiros.

### Escopo de implementação

1. Atualizar `docs/canonical/oprm-canon.v1.md` adicionando uma seção de regra obrigatória para **público-alvo MEI/autônomo**.
2. A regra deve deixar explícito que a pesquisa inicial deve construir entendimento de comportamento, rotina, dores, sonhos e linguagem do MEI/autônomo.
3. A regra deve proibir avanço para produto/oferta nesta fase.
4. A regra deve exigir fontes brasileiras recentes sempre que possível.
5. Atualizar testes relacionados se houver validação automatizada de regra canônica.
6. Registrar a tarefa em `docs/registros/oprm1.md`.

### Critérios de aceite

- O cânone diferencia claramente `CNAE` de `público-alvo MEI/autônomo`.
- O documento deixa claro que o alvo prioritário é o profissional que executa o trabalho, não apenas a empresa/CNAE.
- O documento proíbe produto, oferta, campanha e promessa comercial nesta fase.
- O documento exige atualidade e Brasil-first.

---

## Etapa 2 — Mapear o estado atual do pipeline e pontos de acoplamento

### Objetivo

Identificar exatamente onde o pipeline hoje gera, transporta, valida, sintetiza e materializa informações do NichoCNAE, para fazer a mudança sem quebrar a arquitetura.

### Escopo de implementação

1. Mapear no backend as classes das etapas:
   - `routine-research-orchestrator`;
   - `routine-research-cycle`;
   - `niche-research-seed-builder`;
   - `source-searcher`;
   - `source-fetcher`;
   - `signal-extractor`;
   - `routine-synthesizer`;
   - `routine-quality-gate`;
   - `enriched-niche-materializer`.
2. Mapear no coletor OPRM os processors, clients, prompts, validators e engines dessas etapas.
3. Mapear tabelas envolvidas no banco via MCP, sem acesso direto fora do backend.
4. Mapear a tela `/oprm/pipeline` e telas de detalhe relacionadas.
5. Produzir um diagnóstico curto indicando quais pontos precisam de alteração para suportar público-alvo MEI/autônomo.

### Critérios de aceite

- O diagnóstico lista arquivos/classes por etapa.
- O diagnóstico separa mudanças necessárias em backend, coletor, banco, frontend e testes.
- Nenhuma alteração funcional deve ser feita nesta etapa, exceto documentação do diagnóstico se necessário.

---

## Etapa 3 — Criar contrato de dados para perfil de público-alvo MEI/autônomo

### Objetivo

Definir uma estrutura persistente e rastreável para representar público-alvo MEI/autônomo sem misturar produto ou oferta.

### Escopo de implementação

1. Avaliar se a estrutura atual `market_niche_enrichment_profile` comporta os novos campos sem perda semântica.
2. Se necessário, criar nova tabela, por exemplo `oprm_mei_audience_profile`, via Liquibase YAML compatível com MySQL 5.7.
3. Campos recomendados:
   - `id`;
   - `research_cycle_id`;
   - `routine_card_id`;
   - `source_niche_candidate_id`;
   - `market_niche_id` quando existir;
   - `cnae_code`;
   - `cnae_description`;
   - `neutral_niche_name`;
   - `audience_name`;
   - `occupation_terms`;
   - `work_mode`;
   - `customer_acquisition_behavior`;
   - `daily_routine_summary`;
   - `recurring_tasks_summary`;
   - `operational_pains_summary`;
   - `emotional_pains_summary`;
   - `dreams_summary`;
   - `fears_summary`;
   - `language_patterns`;
   - `channels_used`;
   - `recent_source_summary`;
   - `autonomous_professional_fit_score`;
   - `behavioral_evidence_score`;
   - `source_freshness_score`;
   - `outdated_source_risk_score`;
   - `structured_business_drift_risk_score`;
   - `solution_language_risk_score`;
   - `created_at`;
   - `updated_at`.
4. Criar entidade JPA com `@Column(name = "...")` em todos os campos com risco de divergência.
5. Criar repository dentro do pacote repository adequado.
6. Criar DTOs apenas no módulo OPRM.
7. Adicionar testes unitários/repositório conforme padrão do backend.

### Critérios de aceite

- O contrato persiste público-alvo sem produto, oferta ou campanha.
- O vínculo com ciclo/cartão/CNAE fica rastreável.
- Liquibase segue as regras MySQL 5.7 do projeto.
- Classes Java novas/alteradas possuem comentários de responsabilidade e comentários de métodos em português.

---

## Etapa 4 — Reorientar a etapa de seed para queries de MEI/autônomo

### Objetivo

Fazer a etapa `niche-research-seed-builder` gerar pesquisas explicitamente voltadas a MEI/autônomos brasileiros, com foco em comportamento, rotina, sonhos e dores.

### Escopo de implementação

1. Alterar o prompt da etapa de seed no coletor OPRM.
2. Incluir instruções obrigatórias:
   - pesquisar profissional autônomo/MEI;
   - pesquisar trabalhador por conta própria;
   - pesquisar dono-operador;
   - pesquisar como a pessoa consegue clientes;
   - pesquisar como atende, cobra, agenda, compra materiais, entrega serviço e lida com retrabalho;
   - pesquisar sonhos e objetivos pessoais/profissionais;
   - pesquisar medos e inseguranças;
   - pesquisar linguagem real em pt-BR.
3. Proibir termos de produto/oferta/IA como direção de pesquisa.
4. Adicionar novos objetivos de query, se necessário:
   - `MEI_ROUTINE_DISCOVERY`;
   - `AUTONOMOUS_WORK_MODE_DISCOVERY`;
   - `CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY`;
   - `DAILY_OPERATION_PAIN_DISCOVERY`;
   - `EMOTIONAL_PAIN_DISCOVERY`;
   - `DREAM_DISCOVERY`;
   - `FEAR_DISCOVERY`;
   - `CHANNEL_BEHAVIOR_DISCOVERY`;
   - `LANGUAGE_DISCOVERY`;
   - `SOURCE_FRESHNESS_DISCOVERY`.
5. Atualizar schema, validator, DTOs e backend para aceitar somente objetivos compatíveis.
6. Garantir que queries tenham marcadores Brasil/MEI/autônomo/profissional quando fizer sentido.

### Critérios de aceite

- As queries geradas deixam claro quem é a pessoa pesquisada.
- O pipeline para de depender apenas do nome do CNAE como segmento.
- O validador bloqueia queries genéricas ou direcionadas para solução.
- O schema continua evitando JSON dentro de JSON em campos textuais.

---

## Etapa 5 — Adicionar classificação de fonte por atualidade e aderência a MEI/autônomo

### Objetivo

Garantir que a coleta priorize fontes recentes, brasileiras e aderentes ao comportamento de MEI/autônomos.

### Escopo de implementação

1. Evoluir a classificação da etapa `source-searcher` para incluir:
   - `sourceFreshnessScore`;
   - `outdatedSourceRisk`;
   - `brazilRelevanceScore`;
   - `autonomousProfessionalEvidenceScore`;
   - `structuredBusinessDriftRisk`.
2. Classificar fontes por tipo:
   - fonte oficial brasileira;
   - conteúdo setorial recente;
   - relato/pergunta real de profissional;
   - conteúdo de rede social/comunidade;
   - notícia recente;
   - página comercial;
   - conteúdo antigo ou sem data;
   - conteúdo de empresa estruturada que não representa autônomo.
3. Penalizar fonte antiga quando houver alternativa recente.
4. Priorizar fontes dos últimos 24 meses quando disponíveis.
5. Persistir data de publicação quando for possível extrair do resultado ou da página.
6. Não armazenar HTML completo; manter política de snapshot curto.

### Critérios de aceite

- Fontes antigas ficam marcadas com risco, não tratadas como verdade principal.
- Fontes brasileiras recentes sobem no ranking.
- Fontes de empresa estruturada não dominam a leitura do autônomo.
- O snapshot curto preserva os novos indicadores.

---

## Etapa 6 — Avaliar uso de redes sociais e comunidades públicas

### Objetivo

Investigar se redes sociais e comunidades públicas podem melhorar a leitura de linguagem, dores, sonhos e comportamento atual do MEI/autônomo brasileiro.

### Escopo de implementação

1. Fazer uma avaliação técnica e jurídica das fontes permitidas.
2. Priorizar apenas dados públicos, respeitando termos de uso e privacidade.
3. Fontes candidatas:
   - YouTube comentários/títulos/descrições públicas quando permitido;
   - Reddit público quando houver comunidades brasileiras relevantes;
   - TikTok/Instagram apenas se houver mecanismo permitido e estável de acesso público, sem scraping proibido;
   - Facebook/Grupos apenas se houver fonte pública e permitida;
   - Reclame Aqui, fóruns, comentários em portais e comunidades públicas quando aderentes;
   - Google Trends ou fontes abertas de tendência quando aplicável.
4. Criar uma etapa opcional, se aprovada, por exemplo `social-behavior-searcher`.
5. Essa etapa deve coletar somente sinais comportamentais e linguagem, nunca dados pessoais sensíveis.
6. Adicionar logs de ingestão do payload bruto quando houver fluxo de ingestão.

### Critérios de aceite

- Nenhuma fonte social é integrada sem avaliação de permissão e estabilidade.
- Dados pessoais não são persistidos.
- A saída é comportamento agregado, linguagem e sinais de dor/sonho.
- A etapa é opcional e rastreável, sem quebrar o pipeline principal.

---

## Etapa 7 — Criar etapa de IA para segmentação comportamental de MEI/autônomo

### Objetivo

Usar modelo de texto para transformar evidências coletadas em segmentos comportamentais claros, sem criar produtos.

### Escopo de implementação

1. Criar uma etapa após `signal-extractor` ou após `routine-synthesizer`, por exemplo `mei-audience-segmenter`.
2. Entrada da etapa:
   - CNAE;
   - nome neutro;
   - fontes recentes;
   - snapshots;
   - sinais extraídos;
   - indicadores de fonte;
   - rotina e dificuldades.
3. Saída da etapa:
   - segmentos de MEI/autônomo dentro do CNAE;
   - descrição comportamental de cada segmento;
   - rotina dominante;
   - dores práticas;
   - dores emocionais;
   - sonhos;
   - medos;
   - canais usados;
   - frases/linguagem observada;
   - evidências vinculadas;
   - score de aderência a autônomo;
   - score de atualidade.
4. Proibir explicitamente produto, oferta, preço, promessa, campanha e solução.
5. Validar a saída com schema estruturado e validator determinístico.
6. Persistir a saída em tabela própria ou perfil enriquecido.

### Critérios de aceite

- A IA separa públicos diferentes dentro do CNAE.
- A saída descreve pessoas e comportamentos, não produtos.
- Cada afirmação relevante possui evidência ou resumo de fonte rastreável.
- O validator bloqueia linguagem de oferta ou solução.

---

## Etapa 8 — Evoluir extração de sinais para dores, sonhos, medos e canais

### Objetivo

Ampliar a extração determinística/IA dos sinais para além de tarefas operacionais, capturando dimensões essenciais de marketing.

### Escopo de implementação

1. Adicionar ou ajustar tipos de sinal:
   - `AUTONOMOUS_WORK_MODE`;
   - `CUSTOMER_ACQUISITION_BEHAVIOR`;
   - `CHANNEL_USAGE`;
   - `OPERATIONAL_PAIN`;
   - `EMOTIONAL_PAIN`;
   - `DREAM_SIGNAL`;
   - `FEAR_SIGNAL`;
   - `STATUS_DESIRE`;
   - `TIME_PRESSURE`;
   - `INCOME_INSTABILITY`;
   - `TRUST_REPUTATION_CONCERN`;
   - `PRICE_INSECURITY`;
   - `CLIENT_NO_SHOW_OR_CANCELLATION`.
2. Atualizar sintetizador para criar blocos separados:
   - rotina;
   - comportamento de clientes;
   - canais;
   - dores práticas;
   - dores emocionais;
   - sonhos;
   - medos;
   - linguagem.
3. Manter evidência curta por sinal.
4. Evitar termos de solução.

### Critérios de aceite

- O cartão deixa de ser apenas operacional e passa a ser comportamental.
- O usuário consegue entender claramente quem é o MEI/autônomo pesquisado.
- Os sinais continuam rastreáveis e auditáveis.

---

## Etapa 9 — Criar gate de qualidade específico para público MEI/autônomo

### Objetivo

Impedir que pesquisas genéricas, antigas, corporativas ou orientadas a solução avancem como público-alvo válido.

### Escopo de implementação

1. Criar ou evoluir o `routine-quality-gate` para avaliar também:
   - aderência a MEI/autônomo;
   - evidência comportamental;
   - atualidade das fontes;
   - diversidade de fontes brasileiras;
   - risco de fonte antiga;
   - risco de desvio para empresa estruturada;
   - risco de linguagem de solução/produto.
2. Sugerir critérios mínimos:
   - pelo menos 3 fontes brasileiras relevantes;
   - pelo menos 2 fontes recentes quando disponíveis;
   - pelo menos 1 evidência de rotina;
   - pelo menos 1 evidência de aquisição/atendimento/canal;
   - pelo menos 1 dor prática;
   - pelo menos 1 dor emocional, sonho ou medo;
   - baixo risco de produto/oferta.
3. Status recomendados:
   - `MEI_AUDIENCE_READY`;
   - `NEEDS_MORE_MEI_RESEARCH`;
   - `OUTDATED_SOURCES`;
   - `TOO_CORPORATE`;
   - `SOLUTION_CONTAMINATED`;
   - `GENERIC`.

### Critérios de aceite

- O gate bloqueia conteúdo antigo ou corporativo demais.
- O gate exige sinais humanos/comportamentais, não apenas rotina técnica.
- O gate não aprova material contaminado por produto/oferta.

---

## Etapa 10 — Materializar perfil de público-alvo MEI/autônomo

### Objetivo

Persistir e expor o perfil aprovado para consumo posterior por MDS, MOIS e módulos de estratégia, mantendo a separação entre pesquisa de público e criação de produto.

### Escopo de implementação

1. Criar materializador específico ou evoluir `enriched-niche-materializer`.
2. Gerar um perfil final com:
   - resumo do público;
   - rotina;
   - comportamentos;
   - canais;
   - dores práticas;
   - dores emocionais;
   - sonhos;
   - medos;
   - linguagem;
   - fontes recentes;
   - scores.
3. Expor endpoint OPRM para detalhe do perfil.
4. Garantir que o perfil não contenha produto, oferta ou promessa comercial.
5. Adicionar teste de regressão contra contaminação por solução.

### Critérios de aceite

- O perfil final é útil para marketing, mas ainda não cria produto.
- O perfil é rastreável por ciclo, CNAE, fontes e scores.
- O payload final contém somente campos contratuais.

---

## Etapa 11 — Atualizar tela `/oprm/pipeline` e telas de detalhe

### Objetivo

Permitir que o usuário enxergue se o pipeline está realmente pesquisando MEI/autônomo e se as fontes são recentes.

### Escopo de implementação

1. Atualizar `/oprm/pipeline` para mostrar:
   - foco da pesquisa: `MEI_AUTONOMOUS_AUDIENCE_RESEARCH` ou equivalente;
   - nome neutro do CNAE;
   - público MEI/autônomo identificado;
   - score de aderência a autônomo;
   - score de atualidade;
   - risco de fonte antiga;
   - risco de desvio para empresa estruturada;
   - status do gate.
2. Atualizar telas de detalhe para mostrar:
   - queries geradas;
   - fontes recentes usadas;
   - fontes antigas penalizadas;
   - sinais de rotina, dores, sonhos, medos e canais;
   - justificativa do gate.
3. Manter interface simples, objetiva e orientada à decisão.

### Critérios de aceite

- O usuário entende rapidamente se o ciclo gerou público-alvo MEI/autônomo válido.
- A tela não fica excessivamente técnica.
- As ações disponíveis devem ser apenas as necessárias: reprocessar, pesquisar mais ou abrir detalhe.

---

## Etapa 12 — Testes, documentação e prevenção de recorrência

### Objetivo

Garantir que o redirecionamento permaneça estável e não volte a pesquisar produto/oferta ou empresa genérica.

### Escopo de implementação

1. Criar/atualizar testes de backend para:
   - validação de objetivos de query;
   - bloqueio de termos de solução;
   - persistência dos novos campos;
   - gate de qualidade MEI/autônomo;
   - materialização sem produto/oferta.
2. Criar/atualizar testes do coletor para:
   - prompt com foco MEI/autônomo;
   - schema de segmentação;
   - classificação de fonte recente/antiga;
   - risco de desvio corporativo.
3. Criar/atualizar testes de frontend quando houver alteração visual.
4. Atualizar Swagger dos endpoints OPRM afetados.
5. Registrar em `docs/registros/oprm1.md`.

### Critérios de aceite

- Testes unitários Java passam no módulo alterado.
- Testes frontend passam quando houver alteração na UI.
- Swagger documenta contratos novos/alterados.
- Documentação e testes ficam alinhados com o cânone.

## 5. Ordem recomendada de execução

A ordem mais segura é:

1. Etapa 1 — regra canônica.
2. Etapa 2 — diagnóstico de acoplamento.
3. Etapa 3 — contrato de dados.
4. Etapa 4 — seed MEI/autônomo.
5. Etapa 5 — classificação de fontes por atualidade e aderência.
6. Etapa 8 — novos sinais comportamentais.
7. Etapa 7 — segmentador IA de público MEI/autônomo.
8. Etapa 9 — gate de qualidade.
9. Etapa 10 — materialização.
10. Etapa 11 — frontend.
11. Etapa 12 — testes/documentação final.
12. Etapa 6 — redes sociais, somente após avaliação de permissão e estabilidade.

A etapa de redes sociais deve ser tratada como opcional e posterior, pois pode gerar risco técnico, jurídico e operacional se implementada antes de estabilizar o pipeline principal.

## 6. Prompts de referência para modelos de IA

### 6.1 Prompt para geração de queries

```text
Você está trabalhando no pipeline OPRM NichoCNAE do Marketing Hub.
Sua tarefa é gerar queries de pesquisa para entender o público-alvo de profissionais MEI/autônomos brasileiros dentro de um CNAE.

Não crie produto, oferta, promessa, campanha, mecanismo, headline ou hipótese comercial.
Pesquise apenas comportamento, rotina, dores, sonhos, medos, linguagem, canais e contexto real de trabalho.

Priorize Brasil, português do Brasil e fontes recentes.
Evite fontes antigas quando houver alternativas mais novas.
Inclua termos como MEI, autônomo, profissional por conta própria, prestador de serviço, dono-operador, WhatsApp, Instagram, agenda, cliente, cobrança, orçamento, indicação, atendimento ou materiais somente quando fizerem sentido para o CNAE.

Retorne JSON válido no schema solicitado.
```

### 6.2 Prompt para segmentação comportamental

```text
Você é especialista em marketing e segmentação de mercado no Brasil.
Analise as evidências coletadas para um CNAE e separe possíveis públicos de profissionais MEI/autônomos.

Para cada público, descreva:
- quem é a pessoa;
- como trabalha;
- como consegue clientes;
- como atende e cobra;
- quais dores práticas aparecem;
- quais dores emocionais aparecem;
- quais sonhos e aspirações aparecem;
- quais medos aparecem;
- quais canais usa;
- qual linguagem usa;
- quais evidências sustentam a leitura.

Não proponha produto, oferta, solução, campanha, promessa ou preço.
Se as evidências forem fracas, diga que precisa de mais pesquisa.
```

### 6.3 Prompt para avaliação de fonte

```text
Classifique esta fonte para pesquisa de público-alvo MEI/autônomo brasileiro.
Avalie:
- relevância Brasil;
- atualidade;
- aderência a profissional autônomo/MEI;
- risco de representar empresa estruturada em vez de autônomo;
- risco comercial ou linguagem de produto/oferta;
- utilidade para entender rotina, dores, sonhos, medos, canais e linguagem.

Não invente dados ausentes.
Se a data da fonte não estiver clara, marque risco de atualidade.
```

## 7. Riscos principais

1. **Voltar a pesquisar produto cedo demais**
   - Mitigação: validators determinísticos, gate de risco de solução e testes de regressão.

2. **Confundir MEI/autônomo com empresa pequena estruturada**
   - Mitigação: score de aderência a autônomo e risco de desvio corporativo.

3. **Usar fonte antiga e tirar conclusão ultrapassada**
   - Mitigação: score de atualidade, data de publicação e penalidade para fonte antiga.

4. **Coletar rede social de forma instável ou inadequada**
   - Mitigação: etapa opcional, avaliação de permissão, coleta apenas pública e agregada.

5. **Criar perfil bonito, mas sem evidência**
   - Mitigação: vínculo obrigatório com fontes, snapshots, sinais e scores.

## 8. Definição de pronto do redirecionamento

O redirecionamento estará pronto quando um ciclo NichoCNAE conseguir responder, com evidências recentes e brasileiras:

- Quem é o MEI/autônomo dentro desse CNAE?
- Como essa pessoa trabalha no dia a dia?
- Como ela consegue clientes?
- Onde ela atende, negocia e cobra?
- O que mais toma tempo, energia e dinheiro?
- O que ela quer conquistar?
- Do que ela tem medo?
- Que linguagem ela usa para descrever seus problemas?
- As fontes são atuais?
- O perfil é realmente de autônomo ou caiu em empresa estruturada?
- O material está livre de produto, oferta e solução precoce?
