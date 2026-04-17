# MDS Canonical Artifact Alignment
## Alinhamento do Mechanism Discovery Service com o esquema canônico de artefatos do ecossistema

## Objetivo

Definir como o **Mechanism Discovery Service (MDS)** deve se alinhar ao esquema canônico de artefatos já existente no ecossistema do Marketing Hub, sem gerar acoplamento excessivo entre domínios distintos.

Este documento foi escrito para orientar implementação futura pelo **codex-gpt**, em etapas, preservando:
- coerência arquitetural
- integração entre módulos
- evolução segura de schema
- autonomia do domínio do MDS

---

## 1. Decisão arquitetural principal

### Decisão
O **MDS deve compartilhar o mesmo esquema canônico de artefatos com o restante do sistema no nível do envelope, da governança e da semântica de ciclo de vida**.

Ao mesmo tempo, o MDS **não deve** ser obrigado a compartilhar rigidamente:
- o mesmo schema interno de `content`
- a mesma modelagem física de banco
- a mesma estrutura de blobs
- a mesma lógica interna de persistência
- a mesma granularidade de subtipos do domínio

### Em termos simples

O MDS deve compartilhar com o ecossistema:

- o **protocolo de artefato**
- a **disciplina de versionamento**
- a **semântica de status**
- a **identidade do artefato**
- a **rastreabilidade entre artefatos**

Mas o MDS deve manter autonomia sobre:

- o **conteúdo interno dos artefatos do seu domínio**
- a **estrutura detalhada do `content`**
- a **persistência física interna**
- a **lógica de busca, evidência e descoberta de mecanismos**

---

## 2. Por que essa é a decisão correta

Essa decisão equilibra dois princípios que precisam coexistir:

### 2.1 Integração sistêmica
O Marketing Hub já possui um modelo canônico de artefatos, com envelope e semântica compartilhada entre módulos. Isso é importante para:
- interoperabilidade
- lineage entre módulos
- observabilidade
- versionamento previsível
- consumo por outros serviços como PromptResolver e Worker AI

### 2.2 Boundaries de domínio
O MDS é um **bounded context próprio**, com lógica especializada em:
- busca e análise de evidência
- descoberta de mecanismos
- avaliação de confiança
- tradução de ciência em conhecimento prático

Forçar o MDS a compartilhar o mesmo schema interno de todos os outros módulos geraria:
- acoplamento excessivo
- perda de autonomia
- dificuldade de evolução
- contaminação do domínio científico por contratos pensados para outros tipos de artefato

---

## 3. Fundamento arquitetural

A arquitetura recomendada segue a ideia moderna de **bounded context**:
cada serviço ou subsistema deve possuir autonomia sobre o seu modelo interno, mas expor contratos de integração estáveis para o ecossistema.

### Regra
O MDS deve compartilhar **contrato externo**, não necessariamente **modelo interno idêntico**.

### Consequência prática
- mesmo envelope externo
- schemas de conteúdo específicos do MDS
- mesma linguagem de governança
- persistência e implementação próprias

---

## 4. O que deve ser compartilhado

## 4.1 Envelope canônico de artefato

O MDS deve publicar artefatos aderentes ao mesmo envelope base do ecossistema.

### Envelope base recomendado
```json
{
  "artifactId": "uuid",
  "artifactType": "string",
  "artifactVersion": "v1",
  "schemaVersion": "v1",
  "status": "DRAFT | VALIDATED | APPROVED",
  "parentArtifactIds": [],
  "createdAt": "timestamp",
  "createdByModule": "string",
  "content": {}
}
```

### Objetivo
Permitir que qualquer módulo do sistema consiga:
- identificar artefato
- entender sua versão
- saber seu status
- navegar lineage básico
- consumir o conteúdo conforme o tipo

---

## 4.2 Convenções de status

O MDS deve respeitar a mesma semântica de status do restante do sistema, por exemplo:

- `DRAFT`
- `VALIDATED`
- `APPROVED`

### Objetivo
Garantir coerência operacional entre módulos.

---

## 4.3 Convenções de versionamento

O MDS deve seguir a mesma lógica de:
- `artifactVersion`
- `schemaVersion`
- evolução compatível
- nova versão para mudança material

### Regra
Artefato publicado não deve ser sobrescrito silenciosamente.

---

## 4.4 Lineage básico

O MDS deve usar a mesma linguagem geral de rastreabilidade:
- `parentArtifactIds`
- relações de derivação
- vínculo entre artefatos

### Exemplo
Um `mechanismSpec` pode derivar de:
- vários `evidenceItem`
- um `mechanismDiscoveryReport`
- um conjunto de `sourceDocument`

---

## 4.5 Identidade e auditabilidade

O MDS deve seguir a mesma disciplina de:
- `artifactId`
- `contentHash`
- timestamps
- origem do módulo
- autoria da produção
- referência de schema

---

## 5. O que NÃO deve ser compartilhado rigidamente

## 5.1 Schema interno de `content`

O MDS não deve ser forçado a usar o mesmo `content` dos outros módulos.

### Motivo
O domínio do MDS tem estruturas próprias, por exemplo:
- `sourceDocument`
- `evidenceItem`
- `mechanismCandidate`
- `mechanismSpec`
- `practicalKnowledgePack`

Esses artefatos exigem campos e semântica diferentes de artefatos de campanha, landing ou image planning.

---

## 5.2 Schema físico de persistência

O MDS não precisa compartilhar:
- as mesmas tabelas físicas
- os mesmos blobs
- a mesma estratégia de armazenamento
- a mesma organização de índices

### Motivo
Persistência é detalhe interno do bounded context, desde que o contrato externo permaneça estável.

---

## 5.3 Regras internas de transformação

O MDS não deve herdar regras operacionais de módulos que não pertencem ao seu domínio, como:
- regras de landing page
- wiring de formulário
- tracking de funil
- integração do Lead Portal

---

## 6. Estratégia recomendada de schema

## 6.1 Camada 1 — Base compartilhada do ecossistema

Criar ou assumir um schema-base comum, por exemplo:

- `ArtifactBaseSchema`

Esse schema define:
- envelope
- status
- versionamento
- lineage básico
- identidade

---

## 6.2 Camada 2 — Base do domínio MDS

Criar uma base própria do MDS, derivada do schema-base do ecossistema.

### Exemplo
- `MdsArtifactBaseSchema`

Esse schema pode acrescentar campos comuns ao domínio científico, como:
- `accessLevel`
- `permissionState`
- `confidenceLevel`
- `sourceProvider`
- `evidenceScope`
- `mechanismFamily`

---

## 6.3 Camada 3 — Schemas específicos por artefato

Cada tipo de artefato do MDS deve possuir schema próprio.

### Exemplos
- `MdsSourceDocumentSchema`
- `MdsEvidenceItemSchema`
- `MdsMechanismCandidateSchema`
- `MdsMechanismSpecSchema`
- `MdsPracticalKnowledgePackSchema`

### Regra
Todos herdam o envelope base, mas definem o `content` específico do domínio.

---

## 7. Convenção de naming recomendada

Para evitar colisão e facilitar governança, os tipos do MDS devem ser publicados com namespace explícito.

### Formato sugerido
- `mds.sourceDocument.v1`
- `mds.evidenceItem.v1`
- `mds.mechanismCandidate.v1`
- `mds.mechanismSpec.v1`
- `mds.practicalKnowledgePack.v1`

### Objetivo
Distinguir claramente:
- artefatos do domínio científico/mecanístico
- artefatos do pipeline de marketing
- artefatos de apresentação/execução

---

## 8. Compatibilidade entre módulos

## 8.1 O que outros módulos precisam entender
Outros módulos não precisam conhecer a lógica interna do MDS.
Eles precisam conhecer apenas:
- o envelope
- o `artifactType`
- a versão
- o schema publicado
- os campos do conteúdo que de fato consomem

### Exemplo
O PromptResolver talvez precise consumir:
- `mechanismSpec`
- `practicalKnowledgePack`

Mas não precisa conhecer o pipeline inteiro de busca e triagem do MDS.

---

## 8.2 Contrato de consumo
O consumo entre módulos deve ser baseado em:
- schema versionado
- tipagem explícita
- compatibilidade controlada

### Regra
Acoplamento entre módulos deve ocorrer por contrato publicado, não por dependência direta no modelo interno de classes.

---

## 9. Política de evolução de schema

## 9.1 Compatibilidade
Mudanças em schemas de artefatos do MDS devem seguir política explícita de compatibilidade.

### Regra recomendada
Preferir evolução compatível para consumidores existentes.

### Exemplo
Mudanças aceitáveis sem quebrar consumidores:
- adicionar campos opcionais
- expandir metadata não obrigatória

Mudanças perigosas:
- remover campos usados por consumidores
- alterar significado de campo sem nova versão
- mudar estrutura central do `content` sem bump de versão

---

## 9.2 Nova versão
Sempre criar nova versão quando houver:
- mudança material no significado
- mudança incompatível de estrutura
- mudança de contrato entre módulos

---

## 10. Relação com o armazenamento

Mesmo compartilhando o mesmo envelope canônico, o MDS pode manter seu armazenamento interno conforme a arquitetura definida para ele:

- PostgreSQL como catálogo transacional
- JSONB para `content`
- pgvector para busca semântica
- object storage para blobs
- lineage em tabela própria

### Regra
Compartilhar esquema canônico **não implica** compartilhar a mesma infraestrutura física de persistência com os outros módulos.

---

## 11. Relação com o PromptResolver

O alinhamento entre MDS e PromptResolver deve ocorrer assim:

### O MDS publica
- `mechanismSpec`
- `practicalKnowledgePack`
- `evidencePack` ou equivalentes

### O PromptResolver consome
- artefatos aprovados
- schemas publicados
- knowledge packs derivados

### Regra
O PromptResolver não deve depender do modelo interno do MDS.
Ele deve depender dos artefatos canônicos publicados por ele.

---

## 12. Relação com o Worker AI

O Worker AI não precisa conhecer toda a complexidade do MDS.
Ele deve consumir somente os artefatos canônicos aprovados que forem relevantes para a etapa do pipeline.

---

## 13. Critérios de pronto dessa decisão arquitetural

Essa diretriz só deve ser considerada corretamente implementada quando:

1. o MDS publicar artefatos com o mesmo envelope canônico do ecossistema
2. os tipos de artefato do MDS tiverem namespace próprio
3. existir schema-base compartilhado + schema-base do MDS + schemas específicos
4. o MDS puder evoluir seu conteúdo sem quebrar os demais módulos
5. PromptResolver e demais consumidores dependerem do contrato publicado, não do modelo interno do MDS
6. o lineage entre artefatos do MDS e artefatos do restante do sistema estiver preservado

---

## 14. Anti-padrões a evitar

### Anti-padrão 1
Forçar o MDS a usar exatamente o mesmo schema interno de todos os módulos.

### Anti-padrão 2
Criar artefatos do MDS fora do envelope canônico do ecossistema.

### Anti-padrão 3
Acoplar PromptResolver ou Worker AI às classes internas do MDS.

### Anti-padrão 4
Misturar no mesmo schema conceitos de domínios muito diferentes sem namespace.

### Anti-padrão 5
Mudar significado de artefato do MDS sem nova versão.

---

## 15. Resumo executivo

### Resposta curta
**Sim, o MDS deve compartilhar o mesmo esquema canônico de artefatos com o restante do sistema no nível do envelope e da governança.**

### Mas com a seguinte ressalva
**Não, o MDS não deve compartilhar rigidamente o mesmo schema interno de conteúdo.**
Ele deve:
- herdar o envelope canônico
- estender esse contrato com schemas próprios do seu domínio
- manter autonomia interna de modelagem e persistência

### Fórmula arquitetural recomendada
- **mesmo protocolo**
- **mesma governança**
- **mesmo envelope**
- **schemas específicos por bounded context**
- **baixo acoplamento entre serviços**

Essa é a forma mais moderna, segura e evolutiva de integrar o MDS ao restante do Marketing Hub.
