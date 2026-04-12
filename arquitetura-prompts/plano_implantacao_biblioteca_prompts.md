# Plano de Implantação — Biblioteca de Prompts Orientada a Artefatos

## Objetivo

Implantar, em etapas, uma **Biblioteca de Prompts** para o Marketing Hub que trate prompts como **artefatos versionados de produção**, com contrato, schema, invariantes, evals, promoção por ambiente e observabilidade.

Este plano foi escrito para ser lido e executado futuramente pelo **codex-gpt**.

A implantação deve:

- reduzir prompts espalhados no código;
- evitar regressões já vistas no pipeline;
- preservar o conceito de **Workflow Orientado a Artefatos**;
- permitir crescimento forte de volume de prompts para:
  - pesquisa de mercado;
  - modelagem de mecanismos;
  - introdução de conceitos de neurociência;
  - criação de produtos baseada em teorias científicas;
  - geração de anúncios, landing pages, ofertas e ativos derivados;
- manter separação clara entre:
  - **conteúdo gerado por IA**;
  - **contratos de artefato**;
  - **runtime de integração**;
  - **tracking/funil/telemetria**.

---

## Contexto

O repositório já possui um modelo canônico de artefatos do pipeline em:

- `docs/modelo-canonico-artefatos-pipeline-experimento.md`

Esse modelo já estabelece uma direção correta: dividir o fluxo em artefatos como `campaignAngle`, `landingPageCopy`, `landingPageWireframe`, `landingPageImagePlanning` e `landingPageHtml`.

O problema atual é que os prompts ainda estão muito espalhados pelo código, o que dificulta:

- manutenção;
- versionamento;
- rollback;
- comparação entre versões;
- avaliação de regressão;
- reuso entre etapas;
- auditoria do que exatamente foi enviado ao modelo em cada execução.

Além disso, o sistema já apresentou falhas concretas que mostram a necessidade de uma camada mais madura de PromptOps.

---

## Falhas já observadas que motivam esta implantação

### 1. Drift entre artefatos

Já houve casos em que o artefato gerado em uma etapa não respeitou o contrato da etapa anterior.

Exemplos já observados:

- HTML final divergindo do `formSpec` canônico;
- HTML final divergindo do `surfaceSpec` esperado;
- HTML final divergindo do `landingPageImagePlanning`;
- geração de imagem extra fora do plano canônico.

### 2. HTML com responsabilidade demais

O HTML gerado por IA vinha acumulando responsabilidades que não deveriam morar nele:

- lógica de submissão;
- integração de endpoint;
- parte do comportamento de tracking;
- decisões de runtime.

Isso aumenta fragilidade e dificulta evolução segura.

### 3. Prompt grande, repetitivo e difícil de auditar

O prompt de etapas como `landing-page-html` já cresceu muito e mistura:

- política global;
- contexto do experimento;
- múltiplos artefatos anteriores;
- regras de implementação;
- bindings canônicos;
- schema de saída.

Isso funciona no curto prazo, mas aumenta custo, latência e chance de contradição interna.

### 4. Ausência de governança formal de prompts

Hoje o prompt tende a ser tratado como string de código.

Consequências:

- difícil saber qual versão rodou em produção;
- difícil comparar mudança de prompt com mudança de código;
- difícil promover uma versão validada para produção;
- difícil reproduzir bugs antigos.

### 5. Crescimento iminente do escopo

O sistema vai crescer para incluir ainda mais prompts com alto acoplamento a conhecimento especializado, como:

- mercado;
- comportamento de compra;
- neurociência;
- teorias científicas;
- mecanismos de produto;
- racional de oferta;
- provas e evidências.

Sem uma biblioteca organizada, o custo de manutenção tende a crescer de forma desordenada.

---

## Princípios arquiteturais obrigatórios

### 1. Prompt é artefato de produção

Prompt não deve ser tratado como texto solto.

Cada prompt deve ter:

- identificador estável;
- versão;
- dono;
- contrato de entrada;
- contrato de saída;
- invariantes;
- evals associadas;
- histórico de mudanças.

### 2. O modelo gera conteúdo; o sistema integra comportamento

O modelo deve ser responsável principalmente por:

- redação;
- composição;
- estrutura de narrativa;
- seleção e transformação de conteúdo ambíguo.

O sistema deve continuar responsável por:

- submissão de formulário;
- tracking;
- eventos de funil;
- integração com Pixel/CAPI;
- persistência;
- validação determinística;
- bindings canônicos;
- publicação.

### 3. Schema-first

Toda etapa crítica deve ter schema de saída explícito e validado.

### 4. Artefatos antes de strings gigantes

Sempre que possível, preferir:

- artefatos menores;
- contratos intermediários;
- campos estruturados;

em vez de colocar responsabilidade demais em uma string de HTML ou em um prompt monolítico.

### 5. Evolução incremental

A implantação deve ser feita sem interromper as partes urgentes do sistema.

Nada de reescrever tudo de uma vez.

---

## Resultado final esperado

Ao final da implantação, o sistema deverá ter:

1. uma **Biblioteca de Prompts** no repositório;
2. prompts organizados por artefato e por versão;
3. um **Prompt Resolver** para carregar prompts por ID + versão/label;
4. schemas separados do código de chamada;
5. evals de regressão para os principais artefatos;
6. observabilidade por execução (`promptId`, `promptVersion`, `promptHash`, `schemaVersion`, `model`, `evals`);
7. mecanismo de promoção por ambiente (`draft`, `staging`, `prod`);
8. separação clara entre:
   - prompt;
   - contrato;
   - conhecimento;
   - runtime;
   - tracking.

---

## Estratégia recomendada de implantação

### Recomendação principal

**Começar em uma branch no repositório atual.**

### Motivo

No estágio atual, a Biblioteca de Prompts ainda depende fortemente de contratos e fluxos do próprio `marketing-hub`:

- artefatos canônicos já existentes;
- worker atual;
- backend atual;
- lead portal;
- validações do pipeline.

Criar um repositório separado agora pode introduzir fricção cedo demais:

- sincronização de versões;
- duplicação de schemas;
- dependência de publicação interna;
- risco de afastar prompt do contexto real do código que o usa.

### Quando considerar novo repositório

Só considerar extração para novo repositório quando houver, ao mesmo tempo:

1. contrato estável de prompt;
2. schemas mais maduros;
3. necessidade real de reuso por múltiplos serviços/repos;
4. runtime de carregamento já desacoplado;
5. fluxo claro de versionamento/publicação.

### Decisão recomendada agora

- **Agora:** branch no repo atual.
- **Depois:** avaliar extração para novo repo ou pacote interno compartilhado.

---

## Estrutura proposta no repositório

```text
/prompts
  /shared
    global-policy.md
    marketing-hub-style.md
    artifact-rules.md
    scientific-content-rules.md
    market-research-rules.md

  /artifacts
    /campaign-angle
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /landing-page-copy
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /landing-page-wireframe
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /landing-page-image-planning
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /landing-page-html
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /market-research-report
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /mechanism-spec
      /v1
        prompt.md
        meta.yaml
        evals.yaml
    /offer-spec
      /v1
        prompt.md
        meta.yaml
        evals.yaml

/schemas
  /artifacts
    campaign-angle.schema.json
    landing-page-copy.schema.json
    landing-page-wireframe.schema.json
    landing-page-image-planning.schema.json
    landing-page-html.schema.json
    market-research-report.schema.json
    mechanism-spec.schema.json
    offer-spec.schema.json

/evals
  /datasets
  /goldens
  /fixtures
  /reports

/docs
  modelo-canonico-artefatos-pipeline-experimento.md
  plano-implantacao-biblioteca-prompts.md
```

---

## Estrutura mínima de cada prompt

Cada diretório de prompt deve conter pelo menos:

### `prompt.md`
Texto principal do prompt da etapa.

### `meta.yaml`
Metadados operacionais do prompt.

Exemplo de campos esperados:

```yaml
prompt_id: landing-page-html
artifact_type: landingPageHtml
version: v1
status: draft
owner: marketing-hub
model_family: gpt-5
response_format: json_schema
output_schema_ref: /schemas/artifacts/landing-page-html.schema.json

depends_on:
  - campaignAngle
  - adCopy
  - landingPageCopy
  - landingPageWireframe
  - landingPageImagePlanning

invariants:
  - CTA literal deve ser idêntico ao anúncio
  - formSpec é fonte única da verdade do formulário
  - só pode haver imagens listadas em landingPageImagePlanning.images[]
  - cada <img> deve reproduzir binding canônico sectionId + imageBindingKey
  - HTML não é dono de submissão nem tracking

eval_suite:
  - cta_match
  - form_spec_binding
  - surface_spec_binding
  - image_plan_binding
  - no_extra_images
```

### `evals.yaml`
Lista de evals obrigatórias para promoção da versão.

---

## Nova camada recomendada: biblioteca de conhecimento controlado

Como o volume de prompts vai crescer muito com **pesquisa de mercado**, **neurociência** e **teorias científicas**, não é suficiente ter só biblioteca de prompts.

Também será necessário ter uma camada de **knowledge packs** ou **blocos de conhecimento versionados**.

### Objetivo

Evitar que conhecimento científico/mercadológico fique:

- copiado dentro de vários prompts;
- difícil de atualizar;
- sem origem clara;
- sem distinção entre evidência forte e hipótese fraca.

### Estrutura sugerida

```text
/knowledge
  /market
    personal-trainer-retention-v1.md
    onboarding-first-30-days-v1.md
  /science
    behavior-change-habits-v1.md
    adherence-exercise-v1.md
    motivation-self-determination-v1.md
  /frameworks
    value-perception-v1.md
    price-objection-handling-v1.md
```

### Regra importante

Prompt não deve incorporar livremente “teorias científicas” como texto decorativo.

Toda base científica ou de mercado usada em produção deve idealmente ter:

- `knowledgeId`;
- fonte;
- versão;
- resumo canônico;
- grau de confiança;
- escopo de uso.

### Campo recomendado para uso futuro

Adicionar ao metadata do prompt algo como:

```yaml
knowledge_dependencies:
  - onboarding-first-30-days-v1
  - behavior-change-habits-v1
```

---

## Fases de implantação

# Fase 0 — Preparação e congelamento do escopo

## Objetivo

Preparar o terreno sem mexer no comportamento urgente do sistema.

## Tarefas

1. Criar branch dedicada para este trabalho.
2. Consolidar o documento canônico atual de artefatos como referência oficial.
3. Listar todas as chamadas ao modelo existentes no código.
4. Mapear para cada chamada:
   - onde o prompt é montado;
   - qual artefato produz;
   - quais artefatos consome;
   - qual schema usa hoje;
   - quais validações locais existem;
   - onde ocorre persistência/publicação.
5. Classificar prompts por prioridade.

## Entregáveis

- inventário de prompts existentes;
- mapa de dependências prompt -> artefato -> persistência;
- lista de prioridades de migração.

## Critério de pronto

Nenhum prompt novo migra antes de o inventário existir.

---

# Fase 1 — Estrutura mínima da biblioteca

## Objetivo

Criar a estrutura física da Biblioteca de Prompts dentro do repo.

## Tarefas

1. Criar diretórios `/prompts`, `/schemas`, `/evals`, `/knowledge`.
2. Criar convenção mínima de `prompt.md`, `meta.yaml`, `evals.yaml`.
3. Criar documento de convenções da biblioteca.
4. Criar um resolvedor local simples para ler prompts do filesystem.

## Entregáveis

- estrutura inicial no repositório;
- documentação da convenção;
- resolver básico por `prompt_id + version`.

## Critério de pronto

É possível carregar um prompt a partir do filesystem sem montar string inline no código.

---

# Fase 2 — Migrar prompts críticos do pipeline atual

## Objetivo

Extrair primeiro os prompts mais frágeis e mais importantes.

## Ordem sugerida

1. `landing-page-html`
2. `landing-page-image-planning`
3. `landing-page-wireframe`
4. `landing-page-copy`
5. `campaign-angle`

## Motivo da ordem

Esses foram os pontos onde já houve mais risco de drift e bugs estruturais.

## Tarefas

1. Extrair prompt atual para `prompt.md`.
2. Extrair schema de saída para arquivo próprio.
3. Criar `meta.yaml` com invariantes.
4. Ajustar o código para carregar via resolver.
5. Registrar `promptVersion` e `promptHash` na execução.

## Entregáveis

- prompts críticos fora das classes Java;
- schemas separados;
- execução já apontando versão/hash do prompt.

## Critério de pronto

A execução em produção/staging já não depende de prompt inline para essas etapas.

---

# Fase 3 — Evals e testes de regressão

## Objetivo

Impedir que mudanças em prompt quebrem comportamento sem percepção.

## Tarefas

1. Criar fixtures reais e sintéticas por artefato.
2. Criar goldens para casos críticos.
3. Criar evals obrigatórias por artefato.
4. Integrar evals no CI.
5. Definir thresholds mínimos para promoção.

## Evals mínimas sugeridas

### `landing-page-html`
- `CTA_MATCH`
- `FORM_SPEC_BINDING`
- `SURFACE_SPEC_BINDING`
- `IMAGE_PLAN_BINDING`
- `NO_EXTRA_IMAGES`
- `NO_RUNTIME_SUBMIT_LOGIC`

### `landing-page-image-planning`
- `IMAGE_BINDING_KEYS_PRESENT`
- `SECTION_ID_COVERAGE`
- `NO_DUPLICATE_BINDING_KEYS`

### `market-research-report` (futuro)
- `HAS_EVIDENCE`
- `NO_UNSUPPORTED_CLAIMS`
- `SEGMENT_CLARITY`

### `mechanism-spec` (futuro)
- `CAUSAL_MODEL_PRESENT`
- `LIMITATIONS_PRESENT`
- `NO_ABSOLUTE_CLAIMS`

## Entregáveis

- suíte inicial de evals;
- execução automática em CI;
- relatório básico por prompt/versão.

## Critério de pronto

Nenhuma mudança de prompt crítico sobe sem eval.

---

# Fase 4 — Promoção por ambiente

## Objetivo

Parar de depender do “arquivo atual no branch atual” como verdade operacional.

## Tarefas

1. Introduzir labels de ambiente:
   - `draft`
   - `staging`
   - `prod`
2. Fazer runtime resolver por label e não só por versão hardcoded.
3. Criar fluxo de promoção documentado.
4. Registrar changelog por versão.

## Entregáveis

- convenção de promoção;
- labels operacionais;
- rollback simples por troca de label.

## Critério de pronto

É possível mudar a versão ativa de um prompt sem mexer no código de negócio.

---

# Fase 5 — Knowledge Packs e prompts de pesquisa/ciência

## Objetivo

Escalar o sistema para o novo volume de prompts especializados.

## Tarefas

1. Criar padrão para blocos de conhecimento versionados.
2. Separar claramente:
   - prompt de transformação;
   - conteúdo de evidência;
   - política editorial/comercial.
3. Introduzir artefatos novos, quando necessário, por exemplo:
   - `marketResearchReport`
   - `scientificEvidencePack`
   - `mechanismSpec`
   - `offerSpec`
4. Adicionar regras para citações, limitações e grau de confiança.

## Entregáveis

- primeira biblioteca de knowledge packs;
- primeiros prompts de pesquisa e ciência desacoplados;
- contratos de evidência.

## Critério de pronto

Conceitos de mercado/neurociência/teorias científicas deixam de ficar espalhados em prompts gigantes e passam a ser dependências explícitas.

---

# Fase 6 — Extração opcional para novo repositório

## Objetivo

Avaliar se a Biblioteca de Prompts já merece virar um módulo independente.

## Só executar se

1. houver reuso real por vários serviços;
2. resolver atual estiver estável;
3. schemas estiverem maduros;
4. evals estiverem funcionando;
5. versionamento/promoção já estiverem operacionais.

## Possíveis formatos

### Opção A — manter no mesmo repo
Mais simples, menor atrito, melhor para evolução rápida.

### Opção B — extrair para monorepo interno de prompt assets
Boa opção se vários serviços começarem a consumir a mesma biblioteca.

### Opção C — extrair para pacote/versioned artifact interno
Boa opção quando houver runtime mais formal e necessidade de distribuição controlada.

## Critério de decisão

Só extrair se a extração reduzir atrito operacional. Nunca extrair “por organização estética”.

---

## Regras para o Codex durante a implantação

1. Não refatorar o sistema inteiro de uma vez.
2. Trabalhar em fases pequenas e reversíveis.
3. Cada fase deve preservar compatibilidade com o fluxo atual.
4. Cada migração de prompt deve incluir:
   - extração do template;
   - schema separado;
   - metadados;
   - eval mínima;
   - logging de versão/hash.
5. O Codex não deve mover lógica de runtime crítico para o prompt.
6. O Codex deve tratar prompts como contrato operacional, não como simples texto.
7. Toda mudança deve atualizar documentação e registro.

---

## Decisões arquiteturais já fixadas por este plano

### Decisão 1

**HTML gerado por IA não será o dono de submissão, tracking ou funil.**

### Decisão 2

**Bindings canônicos continuarão sendo validados deterministicamente no backend.**

### Decisão 3

**Prompts devem sair das classes Java e ir para biblioteca versionada no repositório.**

### Decisão 4

**Conhecimento científico e de mercado deve evoluir para dependência explícita versionada.**

### Decisão 5

**A implantação começa no repo atual, em branch dedicada.**

---

## Primeiros entregáveis concretos recomendados

Quando chegar a hora de começar, a ordem recomendada é:

1. criar estrutura `/prompts`, `/schemas`, `/evals`, `/knowledge`;
2. migrar `landing-page-html` para a biblioteca;
3. migrar `landing-page-image-planning`;
4. criar eval `NO_EXTRA_IMAGES`;
5. registrar `promptVersion` e `promptHash` na execução do worker;
6. documentar fluxo de promoção `draft -> staging -> prod`;
7. criar primeiro `knowledge pack` de pesquisa de mercado;
8. criar primeiro `knowledge pack` científico com escopo controlado.

---

## Critério global de sucesso

Este plano será considerado bem-sucedido quando o Marketing Hub conseguir:

- escalar muito o número de prompts;
- manter governança e auditabilidade;
- reduzir regressões por drift de prompt;
- separar melhor conteúdo, contrato e runtime;
- evoluir para novos domínios (mercado, ciência, produto) sem explodir complexidade no código.

---

## Observação final

A Biblioteca de Prompts não deve ser tratada como projeto estético ou organizacional.

Ela é uma **camada operacional** para tornar o sistema mais previsível, observável e escalável.

A implantação deve acontecer **depois que os pontos urgentes estiverem estáveis**, mas o desenho precisa estar claro desde já para evitar novos remendos arquiteturais.
