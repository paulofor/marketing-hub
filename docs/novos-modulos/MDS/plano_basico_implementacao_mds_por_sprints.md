# Plano Básico de Implementação — MDS (Mechanism Discovery Service)

## Objetivo

Este documento define um plano básico de implementação do **MDS (Mechanism Discovery Service)** dentro do **Marketing Hub**, dividido por sprints curtas e incrementais.

O plano foi desenhado para ser compatível com o padrão já adotado no repositório:

- o **MDS deve existir como módulo próprio**, no mesmo estilo dos demais módulos internos;
- **toda integração com base de dados deve acontecer através do backend principal**;
- o **MDS não acessa MySQL diretamente**;
- contratos, persistência, versionamento e lineage ficam centralizados no backend;
- o módulo MDS conversa com o backend por APIs explícitas.

---

## Regra arquitetural obrigatória

### Regra 1 — Banco sempre via backend

Assim como nos demais módulos do projeto:

- somente o **backend principal** acessa o banco MySQL;
- o MDS **não cria acesso direto ao banco**;
- toda leitura e escrita de artefatos, requests, lineage, status e histórico deve ocorrer via **endpoints do backend**;
- qualquer necessidade nova de persistência exige:
  1. contrato no backend;
  2. migration no backend;
  3. endpoint no backend;
  4. documentação do contrato.

### Regra 2 — Implementação incremental

Cada sprint deve produzir um incremento verificável.

Não avançar para uma sprint posterior deixando indefinido:

- contrato essencial;
- forma de persistência;
- ownership de responsabilidade entre backend e MDS.

### Regra 3 — Handoff explícito entre sprints

Ao final de cada sprint, o Codex deve registrar:

- o que foi concluído;
- o que ficou parcial;
- o que ficou pendente;
- riscos conhecidos;
- próximos passos recomendados.

---

## Estrutura padrão de cada sprint

Cada sprint deste plano deve ser executada e atualizada usando sempre esta estrutura.

### Sprint X — <nome>

**Objetivo da sprint**

<descrever o alvo principal>

**Escopo desta sprint**

- item
- item
- item

**Entregáveis esperados**

- artefato
- endpoint
- migration
- documentação

**Critérios mínimos para considerar concluída**

- critério 1
- critério 2
- critério 3

**Fora do escopo nesta sprint**

- item
- item

**Registro do Codex ao final da sprint**

**Status:** `<NAO_INICIADA | EM_ANDAMENTO | PARCIAL | CONCLUIDA | BLOQUEADA>`

**O que foi concluído:**

- item
- item

**O que ficou pendente para a próxima sprint:**

- item
- item

**Riscos / observações:**

- item
- item

**Arquivos alterados/criados:**

- caminho/arquivo
- caminho/arquivo

---

## Sprint 1 — Fundação documental e contratos iniciais

**Objetivo da sprint**

Criar a base canônica do MDS no repositório, deixando clara sua responsabilidade, seu limite arquitetural e seu contrato inicial com o backend.

**Escopo desta sprint**

- criar a documentação principal do MDS;
- definir papel do MDS dentro do Marketing Hub;
- definir que o backend é o ponto central de persistência;
- definir artefatos iniciais do MDS;
- definir contrato inicial backend ↔ MDS em nível conceitual.

**Entregáveis esperados**

- documento de responsabilidades do MDS;
- documento de alinhamento com o esquema canônico do sistema;
- protocolo de histórico de implantação do MDS;
- plano por sprints do MDS;
- definição inicial dos artefatos MDS.

**Critérios mínimos para considerar concluída**

- papel do MDS está claro e não conflita com outros módulos;
- regra de banco via backend está documentada explicitamente;
- artefatos iniciais do MDS estão nomeados;
- existe base documental suficiente para implementação.

**Fora do escopo nesta sprint**

- DDL final do banco;
- endpoints implementados;
- worker funcional;
- integração real com fontes externas.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Sprint 2 — Persistência inicial no backend (MySQL + migrations + contratos)

**Objetivo da sprint**

Preparar a camada inicial de persistência do MDS no backend principal, usando MySQL e migrations compatíveis com o padrão do projeto.

**Escopo desta sprint**

- modelar tabelas iniciais do MDS no backend;
- criar migrations/changelogs;
- definir entidades e repositórios do backend;
- definir endpoints internos para criação e leitura inicial de artefatos MDS;
- garantir compatibilidade com a regra de banco centralizado.

**Entregáveis esperados**

- DDL inicial das tabelas do MDS;
- migrations no backend;
- endpoints backend para persistência inicial;
- documentação do contrato de payload entre MDS e backend.

**Critérios mínimos para considerar concluída**

- backend consegue persistir um artifact record do MDS;
- backend consegue registrar lineage básico;
- migrations estão aplicáveis no ambiente compatível do projeto;
- não existe acesso direto do MDS ao banco.

**Fora do escopo nesta sprint**

- discovery real em fontes científicas;
- scoring sofisticado de evidência;
- busca semântica;
- UI do MDS.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Sprint 3 — Estrutura do módulo MDS e ciclo básico de job

**Objetivo da sprint**

Criar o módulo MDS no repositório, no padrão dos módulos internos, com ciclo básico de execução e integração inicial com o backend.

**Escopo desta sprint**

- criar diretório do módulo `mds/`;
- estruturar projeto Spring Boot do MDS;
- criar configuração base, Dockerfile e README;
- criar cliente para consumir APIs do backend;
- implementar fluxo mínimo: buscar request → processar de forma simples → publicar artefatos no backend.

**Entregáveis esperados**

- módulo `mds/` criado no repositório;
- aplicação sobe localmente;
- cliente HTTP para backend configurado;
- job mínimo funcional de ponta a ponta.

**Critérios mínimos para considerar concluída**

- módulo compila;
- módulo consegue consultar o backend;
- módulo consegue publicar pelo menos um artefato MDS no backend;
- fluxo básico consegue ser executado manualmente ou por loop simples.

**Fora do escopo nesta sprint**

- avaliação robusta de evidência;
- retries sofisticados;
- observabilidade completa;
- UI do módulo.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Sprint 4 — Pipeline mínimo de discovery e publicação de artefatos

**Objetivo da sprint**

Implementar o pipeline mínimo do MDS para transformar uma requisição de discovery em artefatos úteis e persistidos via backend.

**Escopo desta sprint**

- definir fluxo básico de `mechanismDiscoveryRequest`;
- implementar busca inicial/ingestão controlada de fontes permitidas;
- normalizar resultados iniciais;
- gerar `sourceDocument`, `evidenceItem` e `mechanismCandidate`;
- publicar os artefatos no backend com lineage.

**Entregáveis esperados**

- pipeline mínimo funcional;
- geração de artefatos encadeados;
- persistência via backend;
- documentação do fluxo ponta a ponta.

**Critérios mínimos para considerar concluída**

- uma request gera ao menos um conjunto coerente de artefatos;
- o backend recebe e persiste os artefatos com relacionamento rastreável;
- erros básicos do pipeline são tratados;
- logs mínimos permitem entender a execução.

**Fora do escopo nesta sprint**

- ranking avançado de evidência;
- policy engine sofisticada;
- UI;
- automação operacional avançada.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Sprint 5 — mechanismSpec, practicalKnowledgePack e contrato downstream

**Objetivo da sprint**

Fechar a primeira versão útil do MDS para o restante do Marketing Hub, produzindo os artefatos que interessam ao pipeline comercial e de produto.

**Escopo desta sprint**

- transformar candidates em `mechanismSpec`;
- gerar `practicalKnowledgePack`;
- definir payload de consumo downstream;
- documentar como outros módulos usarão os artefatos do MDS;
- refinar status, versionamento e lineage.

**Entregáveis esperados**

- geração inicial de `mechanismSpec`;
- geração inicial de `practicalKnowledgePack`;
- contrato de leitura/consumo via backend;
- documentação de integração com pipeline downstream.

**Critérios mínimos para considerar concluída**

- backend expõe leitura dos artefatos principais do MDS;
- os artefatos têm shape estável o suficiente para consumo por outros módulos;
- lineage entre evidence → candidate → mechanismSpec está claro;
- existe documentação operacional de consumo.

**Fora do escopo nesta sprint**

- UX completa;
- painel administrativo completo;
- motor avançado de decisão científica;
- observabilidade distribuída completa.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Sprint 6 — Hardening operacional, testes e estabilização

**Objetivo da sprint**

Endurecer a primeira versão do MDS para reduzir risco operacional e preparar continuidade.

**Escopo desta sprint**

- adicionar testes de contrato backend ↔ MDS;
- adicionar testes de integração principais;
- melhorar tratamento de erro;
- melhorar logs, métricas e observabilidade mínima;
- revisar documentação e histórico;
- consolidar pendências para próxima fase.

**Entregáveis esperados**

- suíte mínima de testes;
- tratamento básico de falhas e reprocessamento;
- documentação consolidada;
- backlog da fase seguinte.

**Critérios mínimos para considerar concluída**

- contratos principais estão testados;
- execução falha de forma mais previsível;
- histórico do MDS está atualizado;
- existe clareza do que entra na fase 2.

**Fora do escopo nesta sprint**

- otimizações prematuras;
- UI avançada;
- features experimentais de fase 2.

**Registro do Codex ao final da sprint**

**Status:** `NAO_INICIADA`

**O que foi concluído:**

- 

**O que ficou pendente para a próxima sprint:**

- 

**Riscos / observações:**

- 

**Arquivos alterados/criados:**

- 

---

## Backlog provável de Fase 2

Itens prováveis para depois da fase básica:

- object storage para blobs/documentos grandes;
- embeddings e busca semântica dedicada;
- política de acesso/licença mais sofisticada;
- ranking mais robusto de evidência;
- UI do MDS no Marketing Hub;
- dashboards operacionais;
- reprocessamento avançado;
- integração mais profunda com outros módulos de criação de produto.

---

## Regra final para uso pelo Codex

Ao final de cada sprint, o Codex deve:

1. atualizar o bloco da sprint correspondente neste documento;
2. registrar o que foi entregue de forma factual;
3. deixar explícito o que ficou pendente para a sprint seguinte;
4. registrar arquivos alterados e riscos;
5. atualizar também o documento de histórico de implantação do MDS quando houver mudança relevante.

Este documento funciona como plano vivo de execução, e o histórico de implantação funciona como verdade operacional acumulada do que realmente foi feito.
