# Occupation Persona Routine Mapper (OPRM) — Plano Geral de Implementação

## 1. Visão geral

O **Occupation Persona Routine Mapper (OPRM)** é um módulo interno do Marketing Hub, mantido **dentro do mesmo repositório** e **dentro do mesmo sistema**, porém com **diretórios próprios** e **containers Docker próprios**.

O papel do OPRM é mapear, por pesquisa automatizada na internet e por fontes ocupacionais estruturadas, o **dia a dia operacional de ocupações/personas de nicho**.

Exemplos de ocupações-alvo:
- personal trainer
- pastor
- agricultor
- manicure
- cabeleireiro
- dono de loja de celulares

O foco do módulo é reconstruir a rotina prática dessa persona ocupacional:
- o que ela faz ao longo do dia
- quais tarefas repete
- quais ferramentas usa
- onde perde tempo
- onde improvisa
- quais gargalos e fricções enfrenta
- onde existem oportunidades reais de melhoria, simplificação ou facilitação

O módulo deve operar de forma **100% automatizada**, sem entrevistas, sem diários de participante e sem coleta direta com pessoas.

---

## 2. Missão

**Mapear o dia a dia operacional de ocupações/personas de nicho a partir de pesquisa automatizada na internet e fontes ocupacionais estruturadas, transformando tarefas, contexto, fricções e oportunidades em artefatos reutilizáveis para o Marketing Hub.**

---

## 3. Objetivo de negócio

O objetivo do OPRM é alimentar o Marketing Hub com inteligência prática sobre a rotina da persona, para melhorar a capacidade do sistema de:

- identificar dores reais
- formular hipóteses melhores
- propor mecanismos mais aderentes
- gerar ofertas mais úteis
- criar landing pages e criativos mais conectados com a vida real da persona
- priorizar melhorias com relevância operacional

---

## 4. Princípio central

A unidade semântica central do módulo é:

- uma **ocupação específica**
- em um **nicho específico**
- com uma **rotina operacional inferida**
- com **tarefas, fricções, workarounds e oportunidades**
- ligadas a **evidências observáveis**

O OPRM não deve trabalhar com “mercado genérico” como unidade principal.
Ele deve trabalhar com **persona ocupacional**.

---

## 5. Referências canônicas comuns do Marketing Hub

Este módulo deve obedecer às referências canônicas comuns do Marketing Hub.

### 5.1 Governança global
- `docs/canonical/system-governance-canon.v2.md`

Esse documento governa:
- precedência entre fontes
- ownership das regras
- critérios de drift
- regras de evolução
- relação entre módulos e contratos

### 5.2 Implicação prática
O OPRM deve ser desenvolvido de forma compatível com o `system-governance-canon.v2.md`, especialmente nos pontos abaixo:
- artefatos e contratos explícitos
- separação entre domínio e projeções derivadas
- integração entre módulos por contrato
- manutenção de ownership claro
- tratamento de divergência como drift, não como nova verdade implícita

---

## 6. Papel dentro do ecossistema do Marketing Hub

### 6.1 Características arquiteturais
- fica **dentro do mesmo repositório**
- possui **diretório próprio**
- possui **containers Docker próprios**
- possui **pipeline próprio**
- publica artefatos no esquema canônico compartilhado
- integra-se ao restante do sistema por contratos explícitos

### 6.2 O que ele não deve ser
- não é um sistema externo isolado
- não é apenas uma pasta de crawler
- não é um módulo de pesquisa qualitativa manual
- não é um substituto do MDS, do MOIS ou do framework dor-resultado-oferta-mecanismo-prova
- não é um front-end novo obrigatório no MVP

---

## 7. Integração com o framework dor → resultado → oferta → mecanismo → prova

O OPRM deve ser tratado como um **fornecedor de insumos estruturados** para o framework já existente no Marketing Hub.

### 7.1 Papel do OPRM no framework

#### Dor
O OPRM ajuda a identificar:
- tarefas desgastantes
- perdas de tempo
- atritos recorrentes
- ineficiências
- dependências problemáticas
- improvisos e gambiarras

#### Resultado
O OPRM ajuda a transformar dor em:
- melhoria concreta
- ganho de tempo
- previsibilidade
- organização
- facilidade
- redução de retrabalho
- aumento de controle

#### Oferta
O OPRM ajuda a apontar:
- que tipo de solução faz sentido para aquela rotina
- qual formato de entrega tende a ser melhor aceito
- qual promessa parece mais aderente ao cotidiano

#### Mecanismo
O OPRM ajuda a orientar:
- que tipo de mecanismo operacional é plausível
- o que precisa ser automatizado, simplificado, ensinado, padronizado ou delegado

#### Prova
O OPRM ajuda a organizar:
- evidências que conectam dor e rotina
- linguagem de credibilidade ligada ao contexto real da persona
- sinais observáveis que reforçam aderência do mecanismo proposto

### 7.2 Saída esperada para o framework
O módulo deve publicar artefatos utilizáveis por etapas posteriores, por exemplo:
- `painSignal`
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`
- `occupationPersonaRoutineCard`
- `dorResultadoOfertaMecanismoProvaInput`

---

## 8. Papel do backend em relação ao OPRM

O backend principal do Marketing Hub **não é a fonte de verdade conceitual do OPRM**.

A fonte de verdade do módulo é composta por:
- este plano do módulo
- os artefatos canônicos do OPRM
- os contratos de integração
- as regras do framework dor → resultado → oferta → mecanismo → prova
- a governança arquitetural do Marketing Hub
- o `docs/canonical/system-governance-canon.v2.md`

O backend atua como:
- ponto de entrada de dados para processamento
- ponto de saída para recebimento de resultados
- camada de persistência dos dados publicados pelo módulo

Consequentemente:
- o OPRM não define seu domínio a partir do backend
- o backend não governa a modelagem interna do OPRM
- o backend apenas transporta e persiste dados operacionais do fluxo

---

## 9. Modelo de execução

O OPRM será implementado como um módulo **Java Spring Boot** com diretório próprio e containers Docker próprios dentro do repositório do Marketing Hub.

Ele executará em **loop controlado/agendado**, buscando trabalho no backend, processando internamente suas etapas e devolvendo os resultados ao backend.

### Fluxo de execução
1. o OPRM consulta o backend para obter jobs, seeds, ocupações e contexto
2. o OPRM processa internamente suas etapas
3. o OPRM envia os artefatos e resultados ao backend
4. o backend persiste os dados

### Regras do modelo
- o OPRM é um módulo de processamento
- o OPRM não usa o backend como definição do seu domínio
- o backend é o canal operacional de entrada e saída
- o backend é responsável pela persistência dos dados publicados pelo módulo
- o OPRM não deve se tornar um segundo sistema de registro do domínio

---

## 10. Escopo

### 10.1 O que o módulo deve fazer
1. Resolver uma ocupação/persona-alvo.
2. Coletar fontes estruturadas e públicas relacionadas à ocupação.
3. Inferir a rotina operacional dessa ocupação.
4. Extrair tarefas, contexto, ferramentas, fricções e workarounds.
5. Transformar isso em sinais estruturados de dor e oportunidade.
6. Publicar artefatos reutilizáveis no ecossistema do Marketing Hub.

### 10.2 O que o módulo não deve fazer
- entrevistar pessoas
- manter pesquisa participante
- operar formulários de pesquisa manual
- depender de input humano recorrente para funcionar
- virar um crawler irrestrito
- virar um sistema de “copiar concorrente”
- decidir sozinho a oferta final

---

## 11. Fontes de dados

O módulo deve combinar fontes de três tipos.

### 11.1 Fontes ocupacionais estruturadas
Servem como esqueleto da rotina da ocupação.

Exemplos:
- bases ocupacionais estruturadas
- taxonomias de ocupação
- descrições formais de tarefas
- contexto de trabalho
- ferramentas e skills associadas

### 11.2 Fontes públicas de enriquecimento
Servem para aproximar a ocupação da prática real.

Exemplos:
- associações profissionais
- FAQs públicos
- help centers
- blogs e conteúdos do nicho
- job descriptions públicas
- descrições de cursos
- páginas de software usado pela ocupação
- reviews públicas
- vídeos, textos e páginas públicas relacionadas à rotina

### 11.3 Fontes próprias do Marketing Hub
Servem para reforçar o que o sistema já observa em seus próprios experimentos e ativos.

Exemplos:
- dados agregados de funil
- termos de busca e headlines vencedoras
- sinais de conversão
- comportamento em landing pages
- performance de hipóteses e ofertas derivadas

---

## 12. Arquitetura lógica

O OPRM deve ser dividido em camadas claras.

### 12.1 Occupation Resolver
Responsável por:
- receber um rótulo como “manicure” ou “pastor”
- resolver aliases e variantes
- mapear isso para um perfil ocupacional estruturado
- montar um `occupationSeed`

### 12.2 Structured Intake
Responsável por:
- puxar dados estruturados sobre a ocupação
- organizar tarefas, skills, tools e contexto
- gerar um snapshot ocupacional inicial

### 12.3 Web Enrichment
Responsável por:
- pesquisar fontes públicas complementares
- capturar páginas relevantes
- extrair blocos semânticos
- identificar vocabulário prático da ocupação
- enriquecer a rotina com sinais do mundo real

### 12.4 Routine Inference
Responsável por:
- inferir a sequência do dia a dia
- separar tarefas principais e secundárias
- identificar sobrecarga, urgência, repetição e improviso
- produzir rotina operacional inferida

### 12.5 Pain & Opportunity Mining
Responsável por:
- transformar a rotina em sinais de dor
- identificar gargalos
- gerar oportunidades priorizadas

### 12.6 Artifact Publishing
Responsável por:
- publicar a saída no esquema canônico do Marketing Hub
- disponibilizar artefatos para módulos seguintes

---

## 13. Fluxo geral

1. usuário ou pipeline informa a ocupação-alvo
2. `Occupation Resolver` resolve a ocupação
3. `Structured Intake` cria o perfil ocupacional base
4. `Web Enrichment` coleta enriquecimento público
5. `Routine Inference` sintetiza o dia a dia
6. `Pain & Opportunity Mining` extrai dores e oportunidades
7. `Artifact Publishing` gera artefatos consumíveis
8. módulos downstream usam os artefatos no framework dor-resultado-oferta-mecanismo-prova

---

## 14. Artefatos canônicos sugeridos

### 14.1 Artefatos de entrada
- `occupationSeed`
- `occupationAliasResolution`
- `occupationSourcePolicyProfile`

### 14.2 Artefatos de observação
- `occupationProfileSnapshot`
- `occupationWebSourceSnapshot`
- `occupationContextSignal`
- `occupationTaskEvidence`

### 14.3 Artefatos analíticos
- `routineTaskPattern`
- `routineConstraintSignal`
- `routinePainSignal`
- `routineWorkaroundSignal`
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`

### 14.4 Artefato principal
- `occupationPersonaRoutineCard`

### 14.5 Artefatos de integração com o framework
- `dorResultadoOfertaMecanismoProvaInput`
- `hypothesisDraftInput`

---

## 15. Artefato principal: occupationPersonaRoutineCard

O `occupationPersonaRoutineCard` deve ser a unidade sintética central do módulo.

Campos sugeridos:
- `persona_label`
- `occupation_name`
- `occupation_aliases`
- `niche_name`
- `routine_summary`
- `top_tasks`
- `top_tools`
- `top_constraints`
- `top_work_contexts`
- `customer_interaction_pattern`
- `revenue_dependency_pattern`
- `admin_burden_pattern`
- `workaround_patterns`
- `pain_signals`
- `desired_outcome_signals`
- `mechanism_opportunity_signals`
- `evidence_refs`
- `confidence_score`
- `source_mix`
- `generated_at`

---

## 16. Scoring

O módulo deve trabalhar com score em múltiplas dimensões.

### 16.1 Scores mínimos
- `routine_confidence_score`
- `pain_intensity_score`
- `pain_recurrence_score`
- `opportunity_relevance_score`
- `mechanism_fit_score`

### 16.2 Fórmula inicial de oportunidade
`OpportunityScore = (reach * pain_intensity * confidence * commercial_fit) / max(1, implementation_effort)`

Onde:
- `reach` = quantas evidências e contextos apontam para o mesmo padrão
- `pain_intensity` = quão severa a dor parece
- `confidence` = quão forte é a evidência
- `commercial_fit` = quão bem isso se conecta ao framework do Marketing Hub
- `implementation_effort` = quão difícil é transformar isso em solução

---

## 17. Estrutura de diretórios sugerida

Dentro do mesmo repositório do Marketing Hub, o módulo deve ter diretório próprio.

Exemplo:

```text
oprm/
  README.md
  docker/
    Dockerfile
    Dockerfile.worker
  docs/
    oprm_plano_geral_implementacao.md
    oprm_artifacts.md
    oprm_source_policy.md
  src/
    resolver/
    intake/
    enrichment/
    inference/
    mining/
    publishing/
  tests/
  config/
```

### 17.1 Módulos internos sugeridos
- `resolver`
- `intake`
- `enrichment`
- `inference`
- `mining`
- `publishing`

---

## 18. Containers Docker sugeridos

### 18.1 `oprm-worker`
Responsável por:
- executar o loop/agendamento
- buscar trabalho no backend
- processar ocupações e fontes
- chamar modelos de IA quando necessário
- devolver artefatos e resultados ao backend

### 18.2 `oprm-api` (opcional no MVP)
Responsável por:
- healthcheck
- métricas
- inspeção de status
- endpoints operacionais internos do módulo

### 18.3 `oprm-scheduler` (opcional)
Responsável por:
- agendar ciclos de execução
- controlar janelas de coleta
- acionar reprocessamentos

### 18.4 Persistência
- o OPRM não é o sistema de persistência principal
- o backend do Marketing Hub persiste:
  - jobs
  - snapshots
  - artefatos
  - status
  - lineage

---

## 19. Integração com o restante do sistema

### 19.1 Integrações necessárias
- framework dor-resultado-oferta-mecanismo-prova
- hypothesis engine
- MDS
- MOIS
- landing generator
- creative generator
- experiment pipeline

### 19.2 Padrão de integração
O módulo não deve injetar lógica diretamente nos outros módulos.
Ele deve publicar artefatos claros, versionados e com lineage.

---

## 20. Roadmap de implementação

### Fase 0 — fundação
Objetivo:
- criar a base arquitetural e documental do módulo

Entregas:
- diretório do módulo no repo
- containers Docker iniciais
- README inicial
- documento de responsabilidades
- política de fontes
- artefatos base
- contrato inicial de integração

### Fase 1 — resolução ocupacional e intake estruturado
Objetivo:
- conseguir montar um perfil inicial de ocupações-alvo

Entregas:
- `Occupation Resolver`
- ingestão de fontes ocupacionais estruturadas
- `occupationProfileSnapshot`
- suporte inicial às 6 ocupações do MVP

### Fase 2 — enriquecimento web
Objetivo:
- aproximar a ocupação do cotidiano real

Entregas:
- crawler com allowlist
- captura de páginas públicas
- classificação de fonte
- `occupationWebSourceSnapshot`
- enriquecimento semântico da rotina

### Fase 3 — inferência de rotina
Objetivo:
- transformar sinais em rotina operacional inferida

Entregas:
- `routineTaskPattern`
- `routineConstraintSignal`
- `routinePainSignal`
- `routineWorkaroundSignal`
- `occupationPersonaRoutineCard`

### Fase 4 — integração com o framework
Objetivo:
- transformar rotina em insumo direto para dor-resultado-oferta-mecanismo-prova

Entregas:
- `desiredOutcomeSignal`
- `mechanismOpportunitySignal`
- `dorResultadoOfertaMecanismoProvaInput`

### Fase 5 — feedback loop
Objetivo:
- usar resultados do próprio Marketing Hub para recalibrar os mapas

Entregas:
- reponderação de scores
- melhoria de confiança
- histórico por ocupação
- comparação entre rotina inferida e performance de hipóteses

---

## 21. MVP inicial

### 21.1 Ocupações do MVP
- personal trainer
- pastor
- agricultor
- manicure
- cabeleireiro
- dono de loja de celulares

### 21.2 Escopo do MVP
- rotina operacional básica por ocupação
- top tarefas
- top fricções
- top workarounds
- top oportunidades
- card principal por ocupação

### 21.3 O que fica fora do MVP
- UI complexa própria
- múltiplos idiomas
- monitoramento contínuo em larga escala
- inferência avançada por sazonalidade
- orquestração complexa multi-tenant

---

## 22. Regras de governança

1. O módulo deve permanecer dentro do repo do Marketing Hub.
2. O módulo deve ter diretórios e containers próprios.
3. O módulo deve ser implementado em Java Spring Boot.
4. O módulo deve executar em loop/agendamento e usar o backend como canal operacional de entrada e saída.
5. O módulo deve respeitar o `docs/canonical/system-governance-canon.v2.md`.
6. O módulo deve publicar artefatos canônicos, não apenas relatórios livres.
7. O módulo deve se integrar ao framework dor-resultado-oferta-mecanismo-prova por contrato explícito.
8. O módulo deve manter separação entre evidência, inferência e hipótese.
9. O módulo deve operar com policy de fontes e allowlist.
10. O módulo deve nascer automatizado desde a primeira versão.

---

## 23. Riscos principais

### 23.1 Risco semântico
Confundir ocupação genérica com nicho específico.

### 23.2 Risco de inferência excessiva
Transformar observação fraca em “verdade” sobre a rotina.

### 23.3 Risco de scraping inadequado
Coletar fontes públicas sem governança suficiente.

### 23.4 Risco de drift entre módulos
O OPRM gerar artefatos que não encaixem bem no framework canônico do Marketing Hub.

### 23.5 Risco de pouca utilidade
Gerar descrição bonita da ocupação, mas com pouca utilidade para hipóteses e ofertas.

---

## 24. Medidas de mitigação

- score de confiança obrigatório
- evidências ligadas a cada sinal
- allowlist por fonte
- versionamento de artefatos
- validação com resultados downstream
- integração explícita com dor-resultado-oferta-mecanismo-prova
- foco no uso operacional, não em descrição genérica

---

## 25. Questões em aberto

- qual stack interna exata será usada além do Spring Boot
- como o OPRM vai persistir cache técnico transitório, se necessário
- como a allowlist será governada
- qual será o formato canônico do `dorResultadoOfertaMecanismoProvaInput`
- quais APIs internas do Marketing Hub consumirão o módulo primeiro
- como versionar cards por ocupação ao longo do tempo
- qual estratégia de reprocessamento periódico será adotada

---

## 26. Definição final

> **Occupation Persona Routine Mapper (OPRM)**
>
> Módulo interno do Marketing Hub, mantido no mesmo repositório e no mesmo sistema, com diretórios e containers Docker próprios, implementado em Java Spring Boot e executado em loop/agendamento, responsável por mapear o dia a dia operacional de ocupações/personas de nicho via pesquisa automatizada na internet e fontes estruturadas, transformando tarefas, contexto, fricções e oportunidades em artefatos reutilizáveis integrados ao framework dor-resultado-oferta-mecanismo-prova.
