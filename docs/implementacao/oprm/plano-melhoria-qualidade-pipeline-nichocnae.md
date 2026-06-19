# Plano de melhoria da qualidade do pipeline OPRM NichoCNAE

## 1. Metadados

- **Módulo:** OPRM NichoCNAE
- **Status:** Proposta de implementação
- **Prioridade:** Alta
- **Data:** 19/06/2026
- **Base da análise:** ciclos de pesquisa `68` a `75`
- **Relatórios analisados:**
  - `nicho-cnae68.md`;
  - `nicho-cnae69.md`;
  - `nicho-cnae70.md`;
  - `nicho-cnae71.md`;
  - `nicho-cnae72.md`;
  - `nicho-cnae73.md`;
  - `nicho-cnae74.md`;
  - `nicho-cnae75.md`.

---

## 2. Objetivo

Elevar o pipeline NichoCNAE de um mecanismo que encontra conteúdos semanticamente próximos para um sistema que **prova afirmações específicas sobre um executor específico**, utilizando evidências públicas rastreáveis, trechos exatos, fontes independentes e gates comerciais objetivos.

O pipeline melhorado deve:

1. descobrir subnichos operacionais plausíveis a partir de um CNAE amplo;
2. evitar escolher o vencedor apenas pela opinião prévia do modelo;
3. separar identidade do público de hipóteses de dor, canal e resultado;
4. buscar evidências de forma adaptativa, sem gerar dezenas de queries artificiais antecipadamente;
5. garantir que fonte, ator, contexto e afirmação estejam semanticamente alinhados;
6. diferenciar existência da atividade, existência da dor, impacto econômico e intenção de compra;
7. materializar somente nichos que ultrapassem um gate mínimo auditável;
8. distinguir falha de mercado, falta de evidência, erro de validação e falha de infraestrutura;
9. preservar idempotência, retomada e rastreabilidade por estágio;
10. reduzir custo de busca e uso de IA sem sacrificar qualidade.

---

## 3. Diagnóstico executivo

A arquitetura atual apresenta boa auditabilidade e uma decomposição conceitual adequada do CNAE. Entretanto, ainda não é confiável para materialização automática de nichos.

### 3.1 Avaliação atual

| Camada | Nota estimada | Diagnóstico |
|---|---:|---|
| Auditabilidade dos relatórios | 8/10 | Há boa visibilidade de prompts, queries, resultados, custos e erros. |
| Geração inicial de candidatos | 7/10 | Os recortes são frequentemente plausíveis e comercialmente interessantes. |
| Planejamento de queries | 5/10 | Há cobertura ampla, porém excesso de queries longas e pouco naturais. |
| Seleção de fontes | 2/10 | Fontes diretas relevantes são frequentemente ignoradas em favor de páginas adjacentes. |
| Extração de evidências | 2/10 | O extrator gera rótulos genéricos e aceita relações semânticas frágeis. |
| Síntese da rotina | 3/10 | A síntese herda afirmações não sustentadas e repete categorias abstratas. |
| Gates e máquina de estados | 3/10 | Existem transições incoerentes, falsos negativos e reprocessamentos desnecessários. |
| Prontidão para produção | 4/10 | Ainda requer supervisão humana para impedir materializações incorretas. |

### 3.2 Conclusão principal

O gargalo prioritário não é a criatividade do `niche-research-seed-builder`. O maior ganho virá de corrigir:

- seleção e reranking de fontes;
- correspondência entre ator e contexto;
- extração baseada em trecho exato;
- validação de entailment;
- deduplicação e corroboração;
- contratos semânticos;
- gates de materialização;
- máquina de estados e tolerância a falhas.

Trocar apenas o modelo ou aumentar o prompt não resolverá esses problemas estruturais.

---

## 4. Evidências observadas nos ciclos 68–75

### 4.1 Eficiência de busca

| Ciclo | Queries executadas | Queries sem resultado | Páginas coletadas | Evidências únicas efetivas | Resultado final |
|---|---:|---:|---:|---:|---|
| 68 | 45 | 22 | 2 | 2 | Sem evidência mínima de dor prática |
| 70 | 51 | 5 | 5, com duplicação de URLs | 3 | Sem evidência mínima de dor prática |
| 72 | 47 | 8 | 7 | 7 | Sem evidência mínima de dor prática |
| 74 | 57 | 36 | 7 | 6 | Falha de infraestrutura em etapa posterior |
| 75 | 40 | 16 | 5 | 5 | Sem evidência mínima de dor prática |

Os resultados mostram dois padrões:

1. ciclos com excesso de queries específicas e artificiais, gerando muitos resultados vazios;
2. ciclos com muitos resultados disponíveis, mas baixa qualidade de seleção e extração.

Portanto, aumentar a quantidade de queries não é suficiente. O sistema precisa melhorar a qualidade das decisões entre busca, seleção, coleta e extração.

### 4.2 Tipos recorrentes de erro

- escolha antecipada de um subnicho sem pesquisa comparativa;
- dor, canal e resultado já embutidos no nome antes de serem provados;
- inversão do sujeito da evidência;
- uso de ocupações homônimas ou adjacentes;
- uso de modelos de negócio adjacentes como prova direta;
- fontes institucionais ou comerciais tratadas como relato do executor;
- repetição da mesma URL como se fossem evidências independentes;
- classificação baseada em proximidade lexical;
- confiança fixa por tipo de sinal;
- queries pouco naturais;
- campos do schema com significado variável;
- reexecução de pesquisa quando o próximo movimento já era materializar;
- erro técnico registrado como reprovação do nicho;
- contaminação entre CNAEs e subnichos proibidos;
- resultados de domínios inadequados ou de conteúdo adulto não bloqueados na origem.

---

## 5. Problemas estruturais e correções

## 5.1 Escolha do vencedor antes da evidência

### Problema

O modelo gera candidatos, atribui notas de 1 a 5 e escolhe um vencedor antes de pesquisar o mercado. Essas notas são inferências do modelo, mas são usadas como se fossem scores observados.

### Risco

- viés de confirmação;
- descarte precoce de candidatos com melhor evidência pública;
- falsa segurança comercial;
- geração de muitas queries para um recorte fraco.

### Correção

Substituir a escolha direta por um torneio de candidatos:

1. gerar de 4 a 6 candidatos neutros;
2. executar uma pesquisa exploratória curta para todos;
3. medir densidade e qualidade de evidências;
4. selecionar no máximo dois finalistas;
5. executar pesquisa profunda apenas nos finalistas;
6. materializar somente após o gate final.

O pipeline deve aceitar explicitamente:

```json
{
  "decision": "NO_VIABLE_SUBNICHE",
  "reason": "Nenhum candidato apresentou evidência pública suficiente"
}
```

Nenhuma etapa deve obrigar o modelo a escolher um vencedor quando todos são fracos.

---

## 5.2 Identidade do nicho contaminada por hipóteses

### Problema

O nome do nicho já afirma canais e dores ainda não validados, por exemplo:

```text
Motorista autônomo de transfer para aeroporto com agenda via WhatsApp e dor de no-show, cancelamento e negociação de preço
```

### Risco

As etapas seguintes passam a procurar confirmação de afirmações já cristalizadas no nome.

### Correção

Separar:

- identidade do executor;
- job operacional;
- compradores prováveis;
- contexto de execução;
- hipóteses de canal;
- hipóteses de dor;
- hipóteses de resultado.

Exemplo:

```json
{
  "audience": "Motoristas autônomos de transfer aeroportuário",
  "jobContext": "Corridas previamente agendadas para passageiros e pequenas empresas",
  "buyerTypes": ["B2C", "B2B"],
  "channelHypotheses": ["WHATSAPP", "INDICATION"],
  "painHypotheses": [
    "LATE_CANCELLATION",
    "FLIGHT_DELAY_WAITING",
    "WAITING_AND_TOLL_PRICING"
  ]
}
```

Nome neutro recomendado:

```text
Motoristas autônomos de transfer aeroportuário agendado
```

O nome enriquecido só deve ser criado após a aprovação das evidências.

---

## 5.3 Alinhamento incorreto entre ator, contexto e afirmação

### Problema

O pipeline aceita conteúdos com palavras semelhantes, mesmo quando o ator ou a relação causal são diferentes.

Casos observados:

- cancelamento de voo pela companhia aérea usado como prova de no-show contra motorista;
- degustador de grãos e bebidas usado como promotora de degustação em supermercado;
- entregador de aplicativo usado como motoboy de documentos empresariais;
- personal shopper usado como revendedora autônoma plus size;
- conteúdo genérico de consumidor usado como evidência do executor profissional.

### Correção

Adicionar um `SourceClaimSemanticJudge` antes da persistência de qualquer evidência.

Contrato mínimo:

```json
{
  "targetActorMatch": 0.96,
  "jobContextMatch": 0.91,
  "businessModelMatch": 0.89,
  "claimEntailment": 0.94,
  "sourceDirectness": "DIRECT",
  "decision": "ACCEPT",
  "rejectionReasons": []
}
```

Hard gates recomendados:

```text
Se targetActorMatch < 0,75: rejeitar.
Se jobContextMatch < 0,70: rejeitar.
Se businessModelMatch < 0,70: rejeitar.
Se claimEntailment < 0,80: rejeitar.
Se sourceDirectness = ANALOGY_ONLY: rejeitar como prova principal.
```

Nenhuma média global deve compensar falha grave de ator ou contexto.

---

## 5.4 Extração genérica sem trecho comprobatório

### Problema

Textos como os seguintes são classificações genéricas, não evidências:

```text
Executar rotina diária de atendimento, agenda, materiais e entrega do serviço.
```

```text
Conseguir, atender, fidelizar ou recuperar clientes na rotina autônoma.
```

### Correção

Toda evidência deve conter um trecho exato e um claim específico:

```json
{
  "claimType": "REWORK",
  "claim": "Quando o responsável não está no endereço, o motoboy precisa retornar com o documento ou realizar uma segunda tentativa",
  "actor": "MOTOBOY_SELF_EMPLOYED",
  "jobContext": "BUSINESS_DOCUMENT_DELIVERY",
  "exactEvidenceSpan": "Trecho textual exato da fonte",
  "sourceId": 123,
  "sourceType": "OPERATIONAL_DOCUMENT",
  "directness": "DIRECT",
  "entailmentScore": 0.93,
  "actorMatchScore": 0.91,
  "contextMatchScore": 0.90,
  "sourceQualityScore": 0.72,
  "corroboratingSourceCount": 2
}
```

Regras:

- sem `exactEvidenceSpan`, a evidência não pode ser aprovada;
- o trecho deve existir no snapshot persistido;
- uma fonte só pode sustentar claims realmente presentes no conteúdo;
- o mesmo trecho não pode gerar múltiplas categorias incompatíveis sem justificativa;
- resumo gerado por IA nunca substitui o trecho da fonte.

---

## 5.5 Confiança fixa ou artificial

### Problema

Há indícios de scores constantes por categoria, como se a confiança viesse do tipo do sinal, não da qualidade da fonte.

### Correção

Calcular a confiança com base nos componentes reais:

```text
claimConfidence =
  entailmentScore
  × actorMatchScore
  × contextMatchScore
  × businessModelMatchScore
  × sourceQualityScore
  × directnessFactor
  × corroborationFactor
```

Fatores sugeridos:

```text
DIRECT = 1,00
STRONG_INFERENCE = 0,80
WEAK_INFERENCE = 0,55
ANALOGY_ONLY = 0,25
```

A confiança final do nicho deve considerar quantidade, diversidade e independência das fontes, não apenas a soma de claims.

---

## 5.6 Seleção de fontes e orçamento de coleta

### Problema

O pipeline frequentemente ignora fontes operacionais fortes e coleta páginas genéricas, comerciais, jurídicas ou adjacentes.

### Correção

Criar reranking por objetivo de evidência, não apenas um ranking global.

Score sugerido:

| Critério | Peso |
|---|---:|
| Correspondência com o executor | 30% |
| Correspondência com o contexto operacional | 25% |
| Adequação ao objetivo da query | 20% |
| Evidência direta | 15% |
| Qualidade e rastreabilidade da fonte | 10% |

Aplicar cotas mínimas de coleta:

- 2 fontes de rotina concreta;
- 2 fontes de dor ou falha operacional;
- 2 fontes de comportamento comercial;
- 1 fonte de preço, cobrança ou transação;
- 1 fonte de linguagem real do executor;
- 1 fonte oficial ou institucional de apoio.

Uma mesma URL só pode ocupar uma cota. URLs duplicadas, canônicas equivalentes ou conteúdos espelhados devem ser consolidados.

---

## 5.7 Queries longas e artificiais

### Problema

Queries com muitos qualificadores não correspondem à forma como conteúdos reais são publicados.

### Correção

Usar uma escada adaptativa de busca.

Exemplo para transfer aeroportuário:

```text
motorista transfer aeroporto rotina
```

```text
"trabalho com transfer" aeroporto
```

```text
passageiro cancelou transfer motorista
```

```text
site:youtube.com motorista transfer aeroporto rotina
```

```text
motorista transfer atraso voo espera -direitos -indenização -companhia
```

Estratégia:

1. iniciar com 10 a 14 queries curtas;
2. medir cobertura por objetivo;
3. gerar novas queries apenas para gaps;
4. quando houver zero resultado:
   - remover qualificadores;
   - substituir sinônimos;
   - retirar termos como `Brasil`, `relato` e `autônomo` temporariamente;
   - aplicar filtro de domínio;
   - abandonar a formulação após limite de tentativas;
5. encerrar cedo quando o gate estiver atingido.

Não gerar antecipadamente 40 a 60 queries para um único candidato.

---

## 5.8 Schema com semântica instável

### Problema

Campos atuais recebem valores de naturezas diferentes.

Exemplos:

- `businessType`: serviço, MEI, autônomo;
- `operationType`: serviço, autônomo, materialização ou pesquisa;
- `customerType`: B2C, B2B, B2B2C ou MEI;
- `sourceGroup`: canal de busca ou intenção da query;
- `createdBy`: múltiplas grafias.

### Correção

Separar dimensões e usar enums:

```json
{
  "operatorType": "SELF_EMPLOYED",
  "businessModel": "SERVICE",
  "buyerTypes": ["B2C", "B2B"],
  "salesContext": "LOCAL",
  "researchIntent": "OPERATIONAL_PAIN",
  "searchChannel": "GOOGLE",
  "createdBy": "OPENAI"
}
```

Enums recomendados:

```text
OperatorType:
- SELF_EMPLOYED
- MEI_OWNER_OPERATOR
- FREELANCER
- SMALL_TEAM_OWNER_OPERATOR

BusinessModel:
- SERVICE
- RETAIL
- RESALE
- LOCAL_DELIVERY
- EVENT_DAILY_WORK

BuyerType:
- B2C
- B2B
- B2B2C
- PUBLIC_SECTOR

SearchChannel:
- GOOGLE
- YOUTUBE
- REDDIT
- PUBLIC_SOCIAL
- OFFICIAL_DATABASE

ResearchIntent:
- EXECUTOR_EXISTENCE
- ROUTINE
- OPERATIONAL_PAIN
- ECONOMIC_IMPACT
- CUSTOMER_ACQUISITION
- RECURRENCE
- PRICING_AND_COLLECTION
- LANGUAGE
- PURCHASE_INTENT
```

---

## 5.9 Máquina de estados e idempotência

### Problema

Há ciclos que repetem pesquisa mesmo quando o próximo movimento indicado é materializar. Erros técnicos também encerram ciclos como se fossem reprovações de mercado.

### Correção

Definir transições explícitas:

```text
CANDIDATES_GENERATED
  → EXPLORATORY_RESEARCH
  → CANDIDATE_TOURNAMENT
  → DEEP_RESEARCH
  → EVIDENCE_VALIDATION
  → SYNTHESIS
  → QUALITY_GATE
  → MATERIALIZE
```

Estados de saída:

```text
MATERIALIZED
NO_VIABLE_SUBNICHE
NEEDS_MORE_RESEARCH
RESEARCH_FAILED
VALIDATION_FAILED
INFRASTRUCTURE_FAILED
MANUAL_REVIEW_REQUIRED
```

Regras:

- `MEI_AUDIENCE_READY + MATERIALIZAR_NICHO` deve ir diretamente para materialização;
- cada estágio deve possuir chave idempotente;
- reprocessamento deve começar no estágio que falhou;
- resultados aprovados anteriores devem ser reutilizados;
- erros de rede, SSL, broken pipe e timeout devem ser `INFRASTRUCTURE_FAILED`;
- validação de nome deve retornar erro de domínio 422, não erro 500;
- retry com backoff exponencial e jitter;
- fallback de modelo quando permitido;
- limites de tentativas por estágio;
- registro separado de falha técnica e decisão de mercado.

---

## 5.10 Validação do nome do nicho

### Problema

Nomes claramente específicos foram rejeitados como se fossem o CNAE amplo.

### Correção

Substituir o validador binário por validação estruturada:

```json
{
  "broadCnaeSimilarity": 0.24,
  "containsExecutor": true,
  "containsOperationalContext": true,
  "containsUnprovenPain": true,
  "isSpecificSubniche": true,
  "decision": "ACCEPT_WITH_REPAIR",
  "suggestedNeutralName": "Motoristas autônomos de transfer aeroportuário agendado"
}
```

O validador deve:

1. detectar se o nome é realmente amplo;
2. detectar se o nome está específico demais ou contém hipóteses não validadas;
3. reparar automaticamente o nome quando possível;
4. rejeitar apenas quando não houver executor/contexto suficientes;
5. produzir mensagem de erro acionável.

---

## 5.11 Integridade entre CNAEs

### Problema

Foi observado subnicho materializado de outro setor dentro da lista de proibições de um CNAE de vestuário, além de divergência no volume MEI do mesmo CNAE entre ciclos.

### Correção

- validar joins por `cnae_code` e `source_niche_candidate_id`;
- incluir `research_cycle_id` em logs de cache;
- tornar cache key composta por CNAE, versão do pipeline e estágio;
- bloquear carregamento de nicho materializado quando o CNAE não coincidir;
- registrar origem e data do volume MEI;
- criar verificação automática de consistência antes do seed;
- adicionar alerta quando o volume variar além de limite configurado entre ciclos próximos.

---

## 5.12 Segurança e adequação dos resultados de busca

### Problema

Os resultados podem incluir domínios de conteúdo adulto, páginas irrelevantes, golpes ou conteúdo incompatível com o objetivo da pesquisa.

### Correção P0

Antes do reranking semântico:

1. aplicar SafeSearch quando disponível;
2. manter hard blocklist de domínios e categorias proibidas;
3. classificar conteúdo adulto, malware, spam, pirataria e páginas de baixa confiança;
4. impedir persistência de snippet ou URL bloqueada;
5. registrar apenas a categoria de rejeição, sem armazenar conteúdo desnecessário;
6. adicionar testes de regressão para termos ambíguos como `prova de roupa`, `degustação`, `massagem` e similares;
7. aplicar allowlist preferencial para fontes oficiais e setoriais em buscas sensíveis.

Contrato:

```json
{
  "safetyCategory": "ADULT_CONTENT",
  "decision": "HARD_REJECT",
  "persistContent": false
}
```

---

## 6. Arquitetura-alvo do pipeline

## 6.1 Estágio A — Candidate Generator

### Entrada

- CNAE;
- descrição;
- volume MEI e sua origem;
- score OPRM;
- subnichos já materializados do mesmo CNAE;
- aprendizado anterior válido.

### Saída

De 4 a 6 candidatos neutros:

```json
{
  "candidateId": "C1",
  "operator": "MOTORISTA_SELF_EMPLOYED",
  "job": "AIRPORT_TRANSFER",
  "buyerTypes": ["B2C", "B2B"],
  "operationalContext": "PRE_SCHEDULED_RIDES",
  "painHypotheses": [],
  "priorConfidence": "LOW"
}
```

### Regra

O estágio não escolhe vencedor e não declara dor validada.

---

## 6.2 Estágio B — Exploratory Query Planner

Gerar apenas 4 ou 5 queries por candidato para verificar:

- existência do executor;
- rotina concreta;
- comportamento do comprador;
- problema operacional;
- preço, contratação ou recorrência.

A saída deve declarar o objetivo de cada query e termos de exclusão.

---

## 6.3 Estágio C — Candidate Tournament

Calcular por candidato:

- quantidade de fontes potencialmente diretas;
- diversidade de domínios;
- correspondência média de ator;
- evidência de rotina;
- evidência de dor;
- impacto econômico provável;
- comportamento de contratação;
- risco de contaminação;
- custo estimado para pesquisa profunda.

Selecionar no máximo dois finalistas.

Exemplo:

```json
{
  "candidateId": "C1",
  "evidenceDensity": 0.78,
  "actorMatch": 0.91,
  "operationalEvidence": 0.82,
  "economicEvidence": 0.64,
  "contaminationRisk": 0.11,
  "decision": "FINALIST"
}
```

---

## 6.4 Estágio D — Adaptive Deep Research Planner

O planejador recebe gaps de evidência:

```json
{
  "evidenceGaps": [
    "ECONOMIC_IMPACT",
    "CUSTOMER_ACQUISITION_CHANNEL",
    "EXISTING_WORKAROUND"
  ]
}
```

Gera novas queries apenas para esses gaps.

Critérios de parada:

- gate mínimo atingido;
- orçamento de busca atingido;
- ausência persistente de resultados;
- risco de contaminação acima do limite;
- evidência contraditória suficiente para reprovar.

---

## 6.5 Estágio E — Search Result Normalizer and Safety Filter

Responsabilidades:

- canonicalizar URLs;
- deduplicar domínio e página;
- remover tracking parameters;
- detectar idioma e país;
- aplicar safety filter;
- classificar tipo inicial da fonte;
- evitar coleta repetida.

---

## 6.6 Estágio F — Source Judge

Avaliar cada resultado antes do fetch completo:

```json
{
  "actorMatch": 0.94,
  "contextMatch": 0.87,
  "businessModelMatch": 0.84,
  "sourceType": "FIRST_PERSON",
  "supportedGoals": ["ROUTINE", "PAIN"],
  "fetchDecision": true,
  "rejectionReasons": []
}
```

O orçamento de fetch deve ser reservado para as fontes com maior valor marginal.

---

## 6.7 Estágio G — Source Fetcher

Responsabilidades:

- coletar snapshot curto e suficiente;
- preservar título, URL canônica, data, autor e domínio;
- registrar status HTTP e erro técnico;
- extrair conteúdo principal sem menus e ruído;
- não armazenar HTML completo quando desnecessário;
- manter hash do conteúdo para deduplicação.

---

## 6.8 Estágio H — Claim Extractor

Extrair somente claims atômicos, cada um vinculado a trecho exato.

Tipos recomendados:

```text
ROUTINE_TASK
OPERATIONAL_FAILURE
REWORK
WAITING_TIME
DIRECT_COST
OPPORTUNITY_COST
CUSTOMER_ACQUISITION
RECURRENCE
PRICING
COLLECTION
WORKAROUND
EMOTIONAL_PAIN
DESIRED_OUTCOME
PURCHASE_SIGNAL
```

---

## 6.9 Estágio I — Claim Entailment Validator

Revalidar:

- o trecho sustenta a afirmação?
- o ator é o executor pesquisado?
- o contexto é o mesmo?
- a direção causal foi preservada?
- a fonte é direta ou apenas adjacente?

Resultado:

```json
{
  "claimId": 321,
  "decision": "ACCEPT",
  "entailment": 0.92,
  "actorMatch": 0.95,
  "contextMatch": 0.88,
  "directness": "DIRECT"
}
```

---

## 6.10 Estágio J — Corroboration and Deduplication

Agrupar claims semanticamente equivalentes e contar apenas fontes independentes.

Uma URL duplicada, conteúdo espelhado ou reprodução da mesma matéria não aumenta corroboração.

Saída:

```json
{
  "canonicalClaim": "Cancelamentos tardios geram perda do horário reservado",
  "independentSourceCount": 3,
  "sourceDiversity": 0.81,
  "contradictingSourceCount": 1,
  "confidence": 0.76
}
```

---

## 6.11 Estágio K — Evidence-grounded Synthesizer

A síntese só pode usar claims aprovados.

Cada seção deve manter IDs de evidência:

```json
{
  "operationalPainSummary": "Cancelamentos tardios podem deixar o horário sem reposição e desperdiçar preparação ou deslocamento.",
  "supportingClaimIds": [321, 404, 455]
}
```

Se não houver evidência suficiente, o campo deve ser `null` ou declarar explicitamente o gap. Não é permitido preencher com texto genérico.

---

## 6.12 Estágio L — Commercial Evidence Gate

Separar os níveis:

| Nível | Evidência validada |
|---|---|
| E0 | Hipótese criada pelo modelo |
| E1 | Público e atividade existem |
| E2 | Rotina e dor recorrente existem |
| E3 | A dor possui impacto econômico ou workaround observável |
| E4 | Há busca por solução, gasto existente ou comportamento de contratação |
| E5 | Uma oferta real recebeu compra |

Recomendação:

- perfil exploratório pode existir em E2;
- materialização comercial deve exigir E3;
- priorização para hipótese de produto deve preferir E4;
- E5 pertence ao pipeline de pré-venda/MVP.

---

## 6.13 Estágio M — Materializer

Exemplo de decisão:

```json
{
  "decision": "MATERIALIZE",
  "validationLevel": "E3_ECONOMIC_PAIN",
  "confidence": 0.78,
  "supportedClaimCount": 8,
  "independentDomainCount": 5,
  "missingEvidence": ["DIRECT_PURCHASE_INTENT"]
}
```

Ou:

```json
{
  "decision": "NEEDS_MORE_RESEARCH",
  "missingEvidence": [
    "NO_DIRECT_ECONOMIC_PAIN",
    "NO_EXECUTOR_PERSPECTIVE_SOURCE"
  ],
  "nextMove": "SEARCH_FIRST_PERSON_PAIN"
}
```

---

## 7. Gate mínimo para materialização

Um nicho deve ser materializado somente quando houver:

- pelo menos 3 tarefas concretas;
- pelo menos 2 fontes sustentando o conjunto de tarefas;
- pelo menos 2 dores práticas distintas;
- pelo menos 2 fontes independentes sustentando as dores;
- pelo menos 1 comportamento de aquisição, contratação ou recorrência;
- pelo menos 1 sinal de impacto financeiro, custo de oportunidade ou workaround;
- pelo menos 3 domínios independentes;
- pelo menos 1 fonte diretamente ligada ao executor;
- nenhuma dor principal baseada somente em ocupação ou modelo adjacente;
- taxa de duplicação abaixo de 20%;
- `actorMatch` e `contextMatch` acima dos hard gates;
- nenhuma fonte com safety hard reject persistida;
- ausência de falha técnica pendente em estágio obrigatório.

Campos com texto como `sem evidência suficiente` não contam como evidência positiva.

---

## 8. Contratos de dados recomendados

## 8.1 Candidate

```json
{
  "id": 1,
  "researchCycleId": 75,
  "cnaeCode": "4923002",
  "neutralName": "Motoristas autônomos de transfer aeroportuário agendado",
  "operatorType": "SELF_EMPLOYED",
  "businessModel": "SERVICE",
  "buyerTypes": ["B2C", "B2B"],
  "operationalContext": "PRE_SCHEDULED_RIDES",
  "status": "EXPLORATORY_RESEARCH",
  "priorConfidence": 0.30
}
```

## 8.2 Source evaluation

```json
{
  "sourceId": 10,
  "candidateId": 1,
  "canonicalUrl": "https://example.com/page",
  "sourceType": "FIRST_PERSON",
  "searchChannel": "YOUTUBE",
  "brazilRelevance": 0.90,
  "freshnessScore": 0.84,
  "actorMatch": 0.95,
  "contextMatch": 0.88,
  "businessModelMatch": 0.86,
  "qualityScore": 0.72,
  "safetyDecision": "ALLOW",
  "fetchDecision": true
}
```

## 8.3 Evidence claim

```json
{
  "claimId": 321,
  "candidateId": 1,
  "sourceId": 10,
  "claimType": "DIRECT_COST",
  "claimText": "O cancelamento tardio deixa o horário sem reposição e pode gerar deslocamento desperdiçado",
  "exactEvidenceSpan": "Trecho exato da fonte",
  "actor": "TRANSFER_DRIVER_SELF_EMPLOYED",
  "jobContext": "AIRPORT_TRANSFER",
  "entailmentScore": 0.92,
  "actorMatchScore": 0.95,
  "contextMatchScore": 0.88,
  "directness": "DIRECT",
  "status": "ACCEPTED"
}
```

## 8.4 Gate result

```json
{
  "candidateId": 1,
  "validationLevel": "E3_ECONOMIC_PAIN",
  "routineTaskCount": 4,
  "practicalPainCount": 3,
  "economicImpactCount": 2,
  "independentDomainCount": 5,
  "executorSourceCount": 2,
  "duplicateRate": 0.12,
  "decision": "MATERIALIZE",
  "nextMove": "CREATE_NEUTRAL_NICHE"
}
```

---

## 9. Plano de implementação por prioridade

## P0 — Integridade e confiança

### P0.1 Source safety filter

- SafeSearch;
- hard blocklist;
- rejeição de conteúdo adulto, malware e spam;
- testes com termos ambíguos.

### P0.2 Juiz semântico de ator e contexto

- novo contrato de avaliação;
- hard gates;
- razões de rejeição persistidas;
- testes unitários e integração.

### P0.3 Trecho exato obrigatório

- alterar `signal-extractor`;
- persistir `exactEvidenceSpan`;
- bloquear claim sem trecho;
- validar presença no snapshot.

### P0.4 Correção da máquina de estados

- transições explícitas;
- idempotência;
- retomada por estágio;
- classificação correta de falhas técnicas;
- retry e backoff.

### P0.5 Validador de nome

- validação estruturada;
- reparo automático;
- erro 422;
- testes dos ciclos 71 e 73.

### P0.6 Isolamento entre CNAEs

- revisar joins, caches e filtros;
- validar volume MEI e origem;
- impedir subnicho de outro CNAE.

### Critério de conclusão P0

O pipeline não pode mais:

- inverter ator;
- persistir fonte bloqueada;
- aceitar claim sem trecho;
- recomeçar desde o seed após falha posterior;
- rejeitar nome específico como CNAE amplo sem explicação;
- misturar subnichos entre CNAEs.

---

## P1 — Eficiência e cobertura

### P1.1 Candidate tournament

- gerar vários candidatos;
- executar busca exploratória curta;
- selecionar dois finalistas.

### P1.2 Query planner adaptativo

- reduzir geração inicial;
- pesquisar por gaps;
- fallback de termos;
- early stopping.

### P1.3 Reranking por objetivo

- score multidimensional;
- cotas por tipo de evidência;
- orçamento de fetch por valor marginal.

### P1.4 Deduplicação

- URL canônica;
- hash de conteúdo;
- detecção de espelhos;
- fontes independentes.

### P1.5 Normalização do schema

- enums;
- migração dos campos ambíguos;
- compatibilidade temporária de leitura.

### Critério de conclusão P1

- redução mínima de 40% no número médio de queries por candidato;
- taxa de queries sem resultado abaixo de 25%;
- ausência de fetch duplicado;
- aumento da proporção de fontes diretas;
- custo médio por ciclo menor ou igual ao atual.

---

## P2 — Inteligência comercial e calibração

### P2.1 Evidence levels E0–E5

- persistir nível de validação;
- separar dor de intenção de compra;
- mostrar gaps explícitos.

### P2.2 Confiança calculada

- fórmula baseada em evidência;
- diversidade de domínios;
- corroboração e contradição;
- calibração com amostra humana.

### P2.3 Benchmark humano

Criar dataset anotado com pelo menos:

- 100 pares fonte/claim;
- 30 casos de inversão de ator;
- 30 casos de contexto adjacente;
- 20 casos de evidência direta;
- 20 casos de fonte inadequada ou insegura.

Métricas:

- precisão de aprovação;
- recall de evidências relevantes;
- precisão de ator;
- precisão de contexto;
- taxa de claims sem suporte;
- taxa de materialização incorreta.

### P2.4 Human review seletivo

Enviar para revisão apenas quando:

- confiança estiver em faixa intermediária;
- houver contradição relevante;
- fontes diretas forem insuficientes;
- candidato tiver alto valor comercial, mas baixa evidência pública;
- custo de erro de materialização for alto.

---

## 10. Testes de regressão baseados nos ciclos reais

## Ciclo 68

```text
Fontes sobre motorista de aplicativo ou transporte de carga não podem provar rotina de motorista executivo autônomo.
```

```text
Conteúdo sobre cancelamento iniciado pelo motorista não pode provar cancelamento do passageiro contra o motorista.
```

## Ciclo 69

```text
Erro de handshake SSL deve gerar INFRASTRUCTURE_FAILED e permitir retry, sem reprovar o nicho.
```

## Ciclo 70

```text
CBO ou página de degustador de bebidas não pode representar promotora de degustação em supermercado.
```

```text
Conteúdo genérico sobre cobrança ao consumidor não pode provar atraso de diária da promotora.
```

## Ciclo 71

```text
Nome específico de motorista executivo deve passar pela validação ou ser reparado, nunca retornar erro 500.
```

## Ciclo 72

```text
Entregador de aplicativo não pode provar rotina de entrega de malote empresarial.
```

```text
Fontes operacionais de coleta, protocolo, assinatura e entrega devem superar páginas gerais da profissão no reranking.
```

## Ciclo 73

```text
Nome específico de revendedora plus size deve passar ou ser neutralizado automaticamente.
```

## Ciclo 74

```text
Status anterior MATERIALIZAR_NICHO não pode reexecutar seed e pesquisa.
```

```text
Personal shopper não pode provar rotina de revendedora plus size.
```

```text
Domínios de conteúdo adulto devem ser rejeitados antes da persistência e do fetch.
```

```text
Broken pipe deve retomar a etapa que falhou sem repetir toda a pesquisa.
```

## Ciclo 75

```text
Direitos do passageiro por cancelamento de voo não podem provar no-show do passageiro contra motorista de transfer.
```

```text
A direção causal da fonte deve ser preservada no claim.
```

## Todos os ciclos

```text
A mesma URL canônica não pode ser coletada duas vezes.
```

```text
Nenhum claim pode ser aceito sem trecho exato.
```

```text
Texto genérico gerado pela taxonomia não conta como evidência.
```

---

## 11. Observabilidade e métricas

Persistir por estágio:

- duração;
- custo de IA;
- quantidade de tokens;
- número de queries;
- queries com zero resultado;
- resultados bloqueados por safety;
- resultados rejeitados por ator;
- resultados rejeitados por contexto;
- URLs duplicadas;
- páginas coletadas;
- claims extraídos;
- claims aceitos;
- claims rejeitados por entailment;
- fontes independentes;
- taxa de contradição;
- nível E0–E5;
- decisão final;
- motivo da decisão;
- tentativas e retries.

Métricas operacionais recomendadas:

```text
query_zero_result_rate
source_fetch_duplicate_rate
source_actor_rejection_rate
source_context_rejection_rate
claim_entailment_accept_rate
claim_without_exact_span_count
independent_domain_count
materialization_rate
manual_review_rate
infrastructure_failure_rate
average_cost_per_materialized_niche
```

Alertas:

- duplicação acima de 20%;
- mais de 40% de queries sem resultado;
- nenhuma fonte direta do executor;
- materialização em nível abaixo de E3;
- erro técnico repetido no mesmo estágio;
- divergência relevante no volume MEI;
- conteúdo safety hard reject encontrado;
- confiança alta com poucas fontes independentes.

---

## 12. Estratégia de migração

1. manter leitura dos campos antigos durante uma versão de compatibilidade;
2. introduzir novos enums e contratos sem remover imediatamente os anteriores;
3. executar novos estágios inicialmente em shadow mode;
4. comparar decisão antiga e nova nos ciclos históricos;
5. não materializar automaticamente durante a fase de calibração;
6. liberar P0 para produção com feature flag;
7. liberar torneio de candidatos e query adaptativa depois da validação P0;
8. migrar scores antigos para `legacyModelScore`, sem tratá-los como evidência;
9. reprocessar amostra de nichos já materializados para verificar falso positivo;
10. remover contratos legados apenas após estabilidade e cobertura de testes.

---

## 13. Critérios globais de aceite

O plano será considerado implementado quando:

- nenhuma afirmação materializada existir sem trecho exato;
- nenhuma evidência principal depender de ator ou contexto adjacente;
- os ciclos 71 e 73 não falharem por validação incorreta de nome;
- falhas dos ciclos 69 e 74 forem retomáveis;
- a busca do ciclo 74 não persistir conteúdo bloqueado;
- o ciclo 72 priorizar fontes de protocolo e operação real;
- o ciclo 75 rejeitar fontes sobre direitos do passageiro como prova de no-show;
- a taxa média de queries sem resultado ficar abaixo de 25%;
- o fetch duplicado ficar próximo de zero;
- cada materialização informar nível E0–E5 e gaps remanescentes;
- materialização automática exigir pelo menos E3;
- o custo médio por nicho aprovado não aumentar em relação ao baseline;
- uma revisão humana de amostra mostrar precisão mínima de 90% para ator e contexto.

---

## 14. Fora de escopo nesta fase

- criação de produto digital;
- geração de oferta, preço ou checkout;
- validação E5 por venda real;
- campanhas de mídia;
- landing pages;
- automação comercial do nicho encontrado;
- substituição completa dos provedores de busca;
- scraping não autorizado de redes sociais;
- armazenamento integral de páginas públicas.

---

## 15. Ordem recomendada de execução

1. safety filter e canonicalização de URLs;
2. `SourceClaimSemanticJudge` com hard gates;
3. trecho exato obrigatório no extrator;
4. correção da máquina de estados e retries;
5. correção do validador de nome;
6. isolamento entre CNAEs e validação de volume;
7. deduplicação de fonte e conteúdo;
8. torneio de candidatos;
9. query planner adaptativo;
10. reranking por objetivo e cotas;
11. normalização dos contratos e enums;
12. níveis E0–E5;
13. confiança calculada e benchmark humano;
14. liberação gradual da materialização automática.

---

## 16. Checklist para cada pull request

- [ ] A mudança preserva o vínculo entre claim e fonte?
- [ ] Existe trecho exato auditável?
- [ ] Ator, contexto e modelo de negócio foram validados?
- [ ] Há teste para inversão de sujeito?
- [ ] Há teste para ocupação adjacente?
- [ ] Há teste de segurança de domínio/conteúdo?
- [ ] O estágio é idempotente?
- [ ] A falha técnica pode ser retomada?
- [ ] O schema usa enums coerentes?
- [ ] A decisão informa nível E0–E5?
- [ ] Métricas e logs foram adicionados?
- [ ] O custo e a quantidade de queries foram avaliados?
- [ ] A documentação canônica foi atualizada quando necessário?

---

## 17. Resultado esperado

Ao final deste plano, o OPRM NichoCNAE deve deixar de operar como um classificador de textos relacionados e passar a operar como um pipeline de validação de mercado baseado em evidências.

A mudança central é:

> De buscar conteúdos relacionados e classificá-los, para provar afirmações específicas sobre um executor específico, com trechos auditáveis, fontes independentes e gates comerciais explícitos.

Essa evolução deve melhorar simultaneamente:

- precisão dos nichos materializados;
- qualidade das hipóteses posteriores;
- confiabilidade dos scores;
- redução de falsos positivos;
- redução de custos de busca e IA;
- capacidade de auditoria;
- segurança operacional;
- previsibilidade da máquina de estados;
- confiança para automatizar materializações no futuro.
