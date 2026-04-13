# Especificação do Módulo `PromptResolver`

## Objetivo

Definir claramente as responsabilidades, limites, contratos e interfaces do módulo **PromptResolver**, que será implementado como **serviço independente** do ecossistema Marketing Hub, com **container próprio** e **imagem própria**, separado do Worker AI e dos backends de domínio.

Este documento existe para orientar a implementação futura pelo **codex-gpt** em etapas, sem misturar esse módulo com urgências operacionais atuais.

---

## 1. Papel do módulo no ecossistema

O `PromptResolver` é um **serviço interno de infraestrutura de IA**.

Ele **não executa o workflow do experimento**, **não chama o modelo diretamente** e **não publica artefatos finais**.

Sua função é transformar uma intenção do tipo:

- “quero gerar o artefato `landingPageHtml`”
- “na versão `v1`”
- “no ambiente `staging` ou `prod`”
- “com estes artefatos e knowledge packs de entrada”

em um resultado resolvido e auditável contendo:

- template de prompt final
- metadados de versão
- schema de saída
- invariantes obrigatórios
- knowledge packs anexados
- identidade auditável da resolução

Em termos simples:

> O PromptResolver é a camada que entrega **prompt pronto para uso com contrato**, e não apenas um texto.

---

## 2. Posicionamento arquitetural

### Decisão principal

O `PromptResolver` deve nascer como **módulo novo e independente**, semelhante ao Worker AI em termos de isolamento operacional:

- projeto próprio dentro do monorepo ou repo atual
- build próprio
- imagem Docker própria
- container próprio
- API interna própria
- configuração própria por ambiente

### Tecnologia preferencial

**Java + Spring Boot**, para manter coerência com o ecossistema atual, reaproveitar práticas já dominadas pelo projeto e facilitar:

- API REST interna
- configuração externa por ambiente
- health checks
- métricas
- observabilidade
- empacotamento e operação em container

### Regra de cautela

Esse serviço só se justifica como módulo independente se ele nascer já com responsabilidades reais de plataforma.

Ele **não deve** ser criado como microserviço separado se for apenas:

- leitor de arquivos Markdown/YAML
- resolvedor trivial de string
- sem versionamento
- sem schema
- sem knowledge packs
- sem cache
- sem auditoria
- sem observabilidade

Se o escopo ficar pequeno demais, ele vira custo operacional sem ganho real.

---

## 3. Responsabilidades principais

## 3.1 Resolver prompts por identidade

O módulo deve ser capaz de localizar um prompt a partir de:

- `promptId`
- `artifactType`
- `version`
- `label` (`draft`, `staging`, `prod`, `deprecated`)

### Exemplos
- `landing-page-html + prod`
- `landing-page-image-planning + v1`
- `campaign-angle + staging`

### Resultado esperado
Devolver a versão correta do prompt sem depender de strings espalhadas no código do Worker AI ou do backend.

---

## 3.2 Montar o prompt final

O PromptResolver deve compor o prompt final a partir de camadas.

### Camadas mínimas
1. **policy/global instructions**
2. **template do artefato**
3. **invariantes do sistema**
4. **artefatos de entrada**
5. **knowledge packs**
6. **schema/response format**
7. **metadados de resolução**

### Objetivo
Evitar prompts inline gigantes, duplicação de regras e montagem manual em classes Java do Worker AI.

---

## 3.3 Entregar o contrato de saída

O PromptResolver deve devolver, junto com o prompt final:

- `responseFormat`
- `outputSchema`
- `schemaVersion`
- `requiredChecks`
- `invariants`

### Exemplo de invariantes
- `formSpec` é a fonte única da verdade do formulário
- não pode haver `<img>` fora de `landingPageImagePlanning.images[]`
- CTA deve ser literal e idêntico ao anúncio
- HTML não é dono da submissão nem do tracking

### Objetivo
Tirar do código chamador a responsabilidade de adivinhar ou reconstruir o contrato de saída.

---

## 3.4 Resolver knowledge packs

O módulo deve resolver blocos de conhecimento versionados, anexáveis aos prompts.

### Casos previstos
- pesquisa de mercado
- neurociência
- teoria científica
- boas práticas de UX/conversão
- compliance e restrições comerciais
- packs setoriais por nicho

### Objetivo
Separar:
- **template do prompt**
- **conteúdo de conhecimento**
- **artefatos do experimento**

Esse ponto é crítico porque o volume de prompts e de contexto vai crescer bastante.

---

## 3.5 Validar pré-condições mínimas de resolução

Antes de devolver um prompt final, o PromptResolver deve verificar se os insumos mínimos existem.

### Exemplo
`landing-page-html` não deve ser resolvido sem:
- `landingPageCopy`
- `landingPageWireframe`
- `landingPageImagePlanning`

### Resultado
Se faltar dependência mínima, o serviço deve devolver erro claro de contrato, não permitir que o Worker AI monte prompt incompleto.

---

## 3.6 Gerar identidade auditável da resolução

Toda resolução deve gerar metadados auditáveis, no mínimo:

- `promptId`
- `artifactType`
- `version`
- `label`
- `promptHash`
- `schemaVersion`
- `knowledgePackVersions`
- `resolvedAt`

### Objetivo
Permitir rastrear exatamente qual prompt foi usado em cada geração de artefato.

---

## 3.7 Aplicar cache de resolução

O serviço deve cachear resoluções repetidas quando seguro.

### Candidatos a cache
- prompts de mesma versão/label
- schemas
- invariantes
- knowledge packs estáveis
- composição final quando o contexto for idêntico

### Objetivo
Reduzir custo, latência e variabilidade operacional.

---

## 3.8 Expor preview técnico para debug

O módulo deve oferecer um modo de preview/dry-run para inspeção.

### Deve permitir visualizar
- prompt final montado
- schema aplicado
- invariantes
- knowledge packs anexados
- hash
- warnings de contrato

### Objetivo
Facilitar depuração antes de chamar o modelo.

---

## 3.9 Associar prompt a evals

O serviço deve ser capaz de informar quais suites de avaliação pertencem a cada prompt.

### Exemplo
`landing-page-html`
- `cta_match`
- `form_spec_binding`
- `surface_spec_binding`
- `image_plan_binding`
- `no_extra_images`

### Objetivo
Fazer o prompt nascer ligado ao seu contrato de qualidade, e não como texto desacoplado de validação.

---

## 4. Responsabilidades secundárias recomendadas

Estas funções não precisam entrar no primeiro ciclo, mas combinam com a evolução do módulo.

### 4.1 Resolver por label de ambiente
Permitir:
- `draft`
- `staging`
- `prod`

### 4.2 Suportar promoção controlada
Promover versões entre ambientes sem alterar código de aplicação.

### 4.3 Expor diffs entre versões
Comparar:
- template
- invariantes
- schema
- knowledge packs

### 4.4 Emitir eventos internos de auditoria
Exemplos:
- prompt resolvido
- versão promovida
- dependência ausente
- knowledge pack incompatível

### 4.5 Suportar rollout gradual
Exemplo:
- 90% `landing-page-html:v1`
- 10% `landing-page-html:v2`

---

## 5. Não-responsabilidades explícitas

Para evitar expansão indevida do módulo, o PromptResolver **não deve**:

- chamar a OpenAI
- executar o pipeline do experimento
- publicar HTML no Lead Portal
- validar HTML final de negócio
- ser dono do submit do formulário
- disparar Pixel da Meta
- abastecer funil de vendas
- persistir submissões de lead
- sincronizar tracking browser/server
- substituir o backend de domínio
- substituir o Worker AI

### Regra simples
O PromptResolver é dono de **prompt + schema + contexto resolvido**.
Ele não é dono da execução final do artefato.

---

## 6. Interfaces internas recomendadas

## 6.1 API REST interna

### `POST /internal/prompt-resolver/resolve`
Resolve um prompt final para uso imediato.

#### Entrada
- `promptId`
- `artifactType`
- `label` ou `version`
- `inputArtifacts`
- `knowledgePackRefs`
- `options`

#### Saída
- `resolvedPrompt`
- `responseFormat`
- `outputSchema`
- `metadata`
- `invariants`
- `requiredChecks`
- `warnings`

---

### `GET /internal/prompt-resolver/prompts/{promptId}`
Retorna metadados de um prompt.

---

### `GET /internal/prompt-resolver/prompts/{promptId}/versions`
Lista versões disponíveis.

---

### `POST /internal/prompt-resolver/preview`
Gera preview completo da resolução sem executar workflow.

---

### `GET /internal/prompt-resolver/knowledge-packs/{packId}`
Retorna metadados de um knowledge pack.

---

### `GET /internal/prompt-resolver/actuator/health`
Healthcheck operacional.

---

## 6.2 Resposta mínima de resolução

```json
{
  "promptId": "landing-page-html",
  "artifactType": "landingPageHtml",
  "version": "v1",
  "label": "prod",
  "promptHash": "sha256:...",
  "resolvedPrompt": "string",
  "responseFormat": "json_schema",
  "schemaVersion": "v1",
  "outputSchema": {},
  "invariants": [],
  "requiredChecks": [],
  "knowledgePacks": [],
  "warnings": []
}
```

---

## 7. Estrutura de arquivos esperada

```text
/prompt-resolver
  /src/main/java/...
  /src/main/resources
  /Dockerfile
  /build.gradle ou pom.xml

/prompts
  /shared
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

/schemas
  /artifacts
    landing-page-html.schema.json
    landing-page-image-planning.schema.json

/knowledge
  /market-research
  /neuroscience
  /scientific-theories
  /compliance
```

---

## 8. Observabilidade mínima

Como serviço Spring Boot, o módulo deve nascer com observabilidade básica.

### Requisitos mínimos
- healthcheck
- métricas de resolução
- contagem por prompt/version
- latência por endpoint
- cache hits/misses
- logs estruturados
- correlação por requestId/traceId

### Campos mínimos em log
- `promptId`
- `artifactType`
- `version`
- `label`
- `promptHash`
- `schemaVersion`
- `knowledgePacks`
- `durationMs`
- `cacheHit`

---

## 9. Segurança e isolamento

Como é um serviço interno, a primeira versão pode nascer sem exposição pública, mas com isolamento claro.

### Regras mínimas
- somente rede interna
- endpoints internos autenticados quando necessário
- sem exposição pública direta
- sem secrets no prompt em texto plano
- sem knowledge packs sensíveis retornados a qualquer consumidor sem política

---

## 10. Dependências com outros módulos

## Consome
- biblioteca de prompts versionados
- schemas versionados
- knowledge packs versionados
- configuração de ambiente

## É consumido por
- Worker AI
- eventualmente ferramentas internas de preview/debug
- futuramente pipeline de evals ou painel de administração de prompts

## Não substitui
- Worker AI
- backend de experimento
- Lead Portal
- tracking/funnel runtime

---

## 11. Critérios de pronto da primeira versão

A primeira versão do módulo só deve ser considerada pronta quando conseguir:

1. resolver prompt por `promptId + label/version`
2. montar prompt final com camadas estáveis + dinâmicas
3. devolver schema de saída e metadados auditáveis
4. validar dependências mínimas do artefato
5. resolver knowledge packs básicos
6. oferecer preview técnico
7. operar em container próprio
8. expor healthcheck e logs estruturados
9. ser consumida pelo Worker AI sem prompt inline gigante
10. manter compatibilidade com o modelo canônico de artefatos

---

## 12. Critérios do que fica para depois

Não entram na primeira versão, salvo necessidade comprovada:

- UI de edição de prompt
- banco de dados próprio
- promoção automática de versões
- rollout percentual
- multi-tenant
- editor de knowledge packs
- integrações com ferramentas externas de prompt registry
- política avançada de acesso por equipe

---

## 13. Resumo executivo

O `PromptResolver` deve ser um módulo independente de infraestrutura interna com foco em:

- **resolver prompt**
- **entregar schema**
- **anexar contexto e knowledge packs**
- **forçar invariantes**
- **fornecer identidade auditável**
- **preparar o sistema para crescer sem espalhar prompt pelo código**

Ele **não** deve absorver responsabilidades de runtime, submit, tracking ou publicação.

Sua missão é organizar a camada de prompts do Marketing Hub para que o restante do sistema trabalhe com contratos claros, versionamento, preview, cache e observabilidade.
