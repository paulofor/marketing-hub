# Marketing Hub — System Governance Canon v2

> Changelog v2
> - adiciona regra de precedência canônica
> - adiciona critérios explícitos para criar novos cânones de domínio
> - adiciona matriz curta de ownership
> - vincula mudanças cross-domain a ADRs

## 1. Propósito

- Estabelecer um documento-mãe curto que orienta como o Marketing Hub deve preservar a unidade de regras entre backend (`backend/ads-service`), frontend, workers (por exemplo `ai-worker`, `facebook-ads-worker`, `video-management-service`) e serviços satélites (`lead-portal`, `email-service`, `image-watermark-service`, `image-zipper-service`, `lead-portal-payments-service`).
- Criar uma "constituição" de governança para reduzir o drift observado nas evoluções independentes de módulos.
- Definir o roteiro para cânones específicos por domínio, mantendo este arquivo como referência global mínima.

## 2. Escopo

| Cobre agora | Ainda não cobre |
| --- | --- |
| Princípios que determinam onde mora a fonte da verdade, como contratos de domínio, modelos do backend, schemas canônicos e decisões operacionais relevantes. | Regras detalhadas de cada fluxo, como publicação de anúncios, validações campo a campo de formulários, sequências completas do Lead Portal ou contratos externos específicos. |
| Critérios para detectar divergências entre frontend, backend, workers e serviços auxiliares. | Diagramas de arquitetura completos, design de infraestrutura, listas de endpoints ou tabelas completas. |
| Estrutura futura da família de cânones e como evoluir versões. | Decisões irrevogáveis sobre topologias, ferramentas ou refactors abrangentes. |

## 3. Princípios Canônicos Globais

1. **Fonte de verdade explícita.** Cada regra operacional relevante deve apontar para um contrato oficial e identificável.
2. **Backend e domínio decidem; interfaces consomem.** Frontend, workers e integrações não devem reinventar regra de negócio já decidida no domínio.
3. **Workers e integrações emitem fatos.** Serviços assíncronos e conectores externos devem produzir fatos, resultados e eventos; a interpretação final da regra pertence ao domínio.
4. **Flags derivadas não são verdade primária.** Rótulos de UI, atalhos de leitura, campos calculados e indicadores transitórios são projeções, não a regra original.
5. **Contratos entre módulos são explícitos e versionados.** Nenhum módulo pode depender de campos implícitos, estados informais ou convenções não registradas.
6. **Mudanças relevantes exigem teste e documentação correspondente.** Regra operacional sem teste e sem contrato atualizado é candidata imediata a drift.
7. **LHM é determinístico por definição canônica.** O Landing HTML Module (LHM) é responsável pela renderização previsível do HTML final de landing pages a partir de artefatos/contratos canônicos; não é camada de ideação livre de copy.
8. **Evolução visual via artefato canônico.** Ajustes de estética/tema para landing pages devem ser modelados em artefato canônico estruturado (ex.: `landingPageDesignPreset`) consumido pelo LHM; evitar etapa de geração livre de HTML/JS fora do contrato.
8.1. **Paridade obrigatória em geração paralela de landing.** Quando houver geração de múltiplas variantes públicas de HTML para o mesmo experimento (ex.: `deterministic` e `ai`), todas devem usar o mesmo snapshot de entrada canônica e o mesmo conjunto de validações obrigatórias.
9. **Arquitetura robusta orientada a vendas.** Robustez técnica, validações e contratos existem para sustentar resultado comercial: melhorar conversão, reduzir fricção e manter continuidade de mensagem entre criativo, landing e oferta. Toda decisão arquitetural deve explicitar seu impacto em receita (CVR, CPL/CPA e avanço de funil), não apenas conformidade técnica.
10. **Stack mandatória para módulos de apoio + deploy rastreável.** Módulos de apoio/satélites devem adotar baseline **Spring Boot + Java + Maven** e manter workflow CI/CD dedicado com teste, build e deploy por **disparo manual (`workflow_dispatch`)** além dos gatilhos automáticos necessários.
11. **Exceções capturadas preservam ponto de falha.** Todo tratamento de exceção que captura, converte ou relança erro deve registrar log com contexto operacional e a exceção completa para manter stack trace e permitir diagnóstico de causa-raiz. Mensagens resumidas de causa raiz podem ser retornadas ao usuário/API, mas não substituem o log completo no ponto da captura.

## 3.1 Regra global de exclusividade de artefatos (todo o sistema)

- Todo artefato gerado direta ou indiretamente por fluxos oficiais do sistema é **exclusivo do contexto de origem** (por exemplo `experimentId`, `leadId`, `campaignId` ou equivalente canônico do domínio).
- Para um artefato ser classificado como **não exclusivo**, ele não pode ter sido produzido em nenhuma etapa de pipeline/fluxo oficial vinculada a um contexto específico.
- Reuso de artefatos entre contextos distintos só é permitido quando houver contrato canônico explícito de compartilhamento no domínio correspondente.

## 4. Precedência Canônica

Em caso de conflito, a precedência deve ser:

1. **Schema ou contrato canônico publicado**
2. **Cânone de domínio correspondente**
3. **System Governance Canon**
4. **Implementação atual no código**
5. **Comportamento observado em frontend, worker ou integração externa**

Regra prática:

- Implementação divergente não redefine a regra; ela sinaliza drift.
- Ausência de contrato explícito não autoriza cada módulo a decidir por conta própria.
- Quando dois documentos canônicos conflitarem, o conflito deve ser registrado explicitamente até ser resolvido.

## 5. Critérios para identificar risco de drift

- **Mesma regra repetida em múltiplos módulos.** Quando a mesma elegibilidade, bloqueio ou transformação aparece em backend, frontend e worker, há risco alto.
- **Bloqueios condicionais diferentes entre camadas.** Se UI e backend calculam permissões, prontidão ou status de formas diferentes, existe drift em andamento.
- **Workers inferindo elegibilidade.** Quando um worker passa a aprovar, reprovar ou liberar ações com heurísticas locais, ele virou dono informal da regra.
- **Ausência de contrato único para estados encadeados.** Fluxos que atravessam vários módulos precisam de estados e transições formais.
- **Flags múltiplas para o mesmo conceito.** Dois nomes, dois campos ou duas flags para representar a mesma ideia são sinal de modelo mal consolidado.
- **Cópias locais de modelo.** Toda vez que um módulo redefine localmente entidades ou estados já existentes em outro lugar, o risco de drift cresce.

## 6. Quando criar um novo cânone de domínio

Criar um novo cânone de domínio quando um assunto:

- possui estado próprio
- possui regras próprias
- envolve mais de um módulo
- já gerou drift ou tem alto risco de gerar drift
- precisa de contrato, tabela de decisão, transições ou testes específicos

Regra prática:

- Se o tema cabe apenas como princípio global, ele fica neste documento.
- Se o tema exige estados, comandos, invariantes ou contratos próprios, ele deve sair deste documento e ganhar cânone próprio.

## 7. Ownership canônico

| Tema | Dono da regra | Consumidores típicos |
| --- | --- | --- |
| Regras operacionais de domínio | backend / domínio correspondente | frontend, workers, integrações |
| Regras de negócio do domínio **MOIS** | **módulo MOIS (`/mois`)** | backend principal (gateway/contrato), frontend, workers |
| Schemas e contratos de decisão | domínio + backend responsável | todos os consumidores do contrato |
| Artefatos canônicos de landing (`landingPageCopy`, `landingPageWireframe`, `landingPageDesignPreset`) | backend / domínio de experimentos | ai-worker, frontend administrativo, LHM |
| Contrato visual runtime do LHM (`landingPageDesignPreset.lhmRuntime.baseCss`, `sectionPresets.surfaceStyle`, `sectionPresets.contrastMode`) | backend / domínio de experimentos | LHM, ai-worker, frontend administrativo |
| Matriz de conversão visual e confiança (hierarquia, prova, acessibilidade, sinais legais) | cânone de experimentos + backend validador | ai-worker, LHM, frontend |
| Projeções de UI | frontend | usuário final |
| Fatos externos e resultados assíncronos | workers / integrações | backend / domínio |
| Governança global do sistema | `system-governance-canon` + ADRs relevantes | todo o projeto |

### 7.1 Regra arquitetural mandatória para MOIS

- O **backend principal** não deve conter regra de negócio específica do MOIS.
- Para MOIS, o backend principal atua como:
  - gateway HTTP;
  - camada de contrato/validação;
  - camada de leitura/escrita de dados quando aplicável.
- A orquestração, decisões, cálculo de score, transições e políticas de domínio devem residir no **módulo MOIS**.

## 8. Áreas candidatas a futuros cânones específicos

| Domínio sugerido | Justificativa breve |
| --- | --- |
| **Experiments & Activation** | Abrange nicho → hipótese → targeting → ad sets → creatives → métricas. Exige estados e contratos próprios. |
| **Lead Capture & Portal / Payments** | Lead Portal e pagamentos compartilham eventos, submissions, pacotes, compras e reenvios. |
| **Media Asset Lifecycle** | Watermark, zipper, distribuição, miniaturas e entrega exigem lifecycle e contratos claros. |
| **Ads Delivery & Channel Integrations** | Integrações com Meta e canais pagos têm dependências externas, estados e contratos próprios. |
| **AI Prompt & Worker Governance** | Workers de IA compartilham obrigações de prompt, modelo, auditoria, orçamento e versionamento. |
| **Vitrines & Content Entitlements** | Regras de acesso, plano, papel, magic links e visibilidade de conteúdo merecem modelo próprio. |

## 9. Regras de evolução

- Este documento deve continuar enxuto e estável.
- Detalhes operacionais devem migrar para cânones de domínio assim que surgirem estados e regras próprias.
- Mudanças que alterem princípios, escopo, precedência, ownership ou estrutura da família de cânones exigem nova versão.
- Mudanças cross-domain ou arquiteturalmente significativas devem gerar ADR correspondente.
- Conflitos entre documentos devem ser registrados explicitamente, nunca escondidos.
- Toda alteração que muda comportamento em produção deve alinhar: código, testes, contrato e cânone relevante.
- Todo novo módulo de apoio deve nascer com workflow CI/CD próprio contendo, no mínimo, etapa de testes, build de imagem e deploy com acionamento manual (`workflow_dispatch`).

## 10. ADRs

Usar ADR quando a decisão:

- afeta mais de um domínio
- muda precedência entre fontes de verdade
- altera ownership de uma regra importante
- introduz ou remove um mecanismo estrutural relevante
- cria exceções permanentes ao cânone atual

Regra prática:

- cânone descreve a regra estável
- ADR explica por que a decisão estrutural foi tomada e quais consequências ela traz

## 11. Estrutura-alvo da família de cânones

```text
docs/canonical/
├─ system-governance-canon.v2.md              # documento-mãe
├─ experiments-canon.v1.md                    # estados e contratos do pipeline de experimentos
├─ experiments-automation-flow-canon.v1.md    # ordem e estados da fila automática do pipeline
├─ experiments-decision-schema.v1.json        # schema machine-readable para validações automáticas
├─ lead-capture-canon.v1.md                   # fluxos do Lead Portal, packages e payments
├─ media-packages-canon.v1.md                 # lifecycle watermark → zip → email → entrega
├─ ads-integrations-canon.v1.md               # contratos Meta / canais pagos
├─ facebook-campaign-publication-canon.v1.md   # prontidão, liberação e funil do Facebook Ads Worker
├─ mois-worker-canon.v1.md                    # cânone único do worker MOIS (fluxo, contratos e OpenAI)
└─ <domínio>-decision-schema.v1.json          # schemas específicos quando necessário
```

> Cada novo documento deve declarar propósito, limites, contratos oficiais e tabela de estados, referenciando explicitamente os módulos que implementam as regras descritas.

## 14) Regra canônica de finalização de importação OPRM CNPJ/CNAE (market size)

### Objetivo
Evitar fechamento prematuro de `import run` que destrói a totalização de market size por CNAE.

### Regra obrigatória
- É proibido finalizar (`completeRun`/`finalize-latest-started`) uma run OPRM CNPJ/CNAE quando existir ao menos um arquivo de dataset `ESTABELECIMENTOS` em `STARTED`.
- Nessa condição, o backend deve bloquear a finalização com erro de conflito (`HTTP 409`) e mensagem explícita de causa-raiz operacional.
- A run deve permanecer aberta para permitir conclusão correta da consolidação de `marketSizes`.

### Critério de efetividade
- Só é permitido fechamento quando não houver `ESTABELECIMENTOS` em `STARTED`.
- Se houver falha real em arquivos de estabelecimentos, ela deve estar explícita como `FAILED` por evento de arquivo, com erro rastreável, e não por fechamento automático sem execução.

### Regra obrigatória de identificação de CNAE em `ESTABELECIMENTOS`
- A identificação do CNAE para totalização de market size deve usar o campo de **CNAE principal** da linha de `Estabelecimentos*.zip` (posição `11`, índice zero-based no split por `;`).
- O valor do CNAE deve ser normalizado antes da consolidação (remoção de caracteres não numéricos), preservando somente dígitos para chave de agregação.
- Linhas sem colunas mínimas esperadas ou com CNAE principal vazio devem ser contabilizadas como ignoradas e registradas em logs de observabilidade.

### Critério de efetividade (identificação de CNAE)
- Cada arquivo `ESTABELECIMENTOS` deve gerar log de início, progresso periódico e resumo final com: `linhasLidas`, `linhasValidas`, `linhasIgnoradas` e quantidade de CNAEs consolidados.
- A ausência desses logs invalida a rastreabilidade operacional da totalização e deve ser tratada como não conformidade canônica.
