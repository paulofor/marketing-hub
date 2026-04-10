# Inconsistências do Modelo Canônico de Artefatos do Pipeline de Experimento

Documento de análise do arquivo `docs/modelo-canonico-artefatos-pipeline-experimento.md`, consolidando inconsistências de contrato, nomenclatura, cardinalidade e rastreabilidade entre todos os artefatos do pipeline.

## Tabela única de inconsistências (visão cross-artefatos)

| ID | Artefato(s) impactado(s) | Regra/Contrato esperado | Inconsistência encontrada | Impacto | Ajuste recomendado |
|---|---|---|---|---|---|
| INC-01 | `landingCodeBundle` / `landingPageHtml` | Deve existir **um único nome canônico** para o artefato final de HTML/publicação | O documento usa `landingCodeBundle` na tabela canônica e dependência lógica, mas descreve o item operacional como `landingPageHtml` em seção dedicada | Ambiguidade de contrato, risco de integração quebrar por nome divergente | Unificar o nome canônico do artefato final (sugestão: manter `landingPageHtml` ou formalizar alias explícito entre os dois) |
| INC-02 | `landingPageHtml` | `consistencyChecks[]` tem mínimo e checks obrigatórios coerentes | O mínimo declarado é **3 itens**, porém os checks obrigatórios listados são **6** (`CTA_MATCH`, `PROMISE_MATCH`, `IMAGE_PLAN_BINDING`, `SURFACE_SPEC_BINDING`, `FORM_SPEC_BINDING`, `FORM_USABILITY`) | Regra impossível de cumprir literalmente sem interpretação implícita | Ajustar mínimo para 6, ou declarar quais checks são mandatórios sempre e quais são opcionais |
| INC-03 | `landingPageCopy` | Cardinalidade mínima de `consistencyChecks[]` coerente com checks obrigatórios | O mínimo declarado é **2 itens**, porém a regra crítica exige inclusão explícita de **3 checks** (`CTA_MATCH`, `PROMISE_MATCH`, `GOOGLE_LANDING_BEST_PRACTICES`) | Validação contraditória para payloads limítrofes | Ajustar mínimo para 3 ou reduzir/checks obrigatórios |
| INC-04 | `landingPageImagePlanning` | Separação clara entre campos de raiz e campos por item de `images[]` | Na tabela canônica inicial, o artefato lista `sectionId`, `imageBindingKey`, `imageRole`, `conversionRole`, `layoutBinding` como se fossem campos de raiz; nas seções detalhadas, esses campos pertencem a `images[]` | Interpretação incorreta de schema e geração inválida por implementações automáticas | Corrigir tabela canônica para explicitar `images[].<campo>` |
| INC-05 | `landingPageCopy` ↔ `landingPageWireframe` ↔ `landingPageImagePlanning` | Compartilhar `sectionId` entre artefatos | O exemplo JSON usa `sectionId` em `landingPageCopy.bodySections` como `pain-01`, `mechanism-01`, `proof-01`, `cta-01`, enquanto wireframe/imagens usam `hero` | Quebra de rastreabilidade e impossibilidade de validação cross-artefato | Padronizar IDs de seção no exemplo e no contrato (ou definir mapeamento formal quando IDs diferirem) |
| INC-06 | `landingPageWireframe` | `sectionOrder[]` mínimo 4 itens | Exemplo JSON possui apenas 1 item em `sectionOrder[]` | Exemplo inválido frente à própria regra; induz implementação errada | Expandir exemplo para no mínimo 4 seções |
| INC-07 | `landingPageImagePlanning` | `images[]` mínimo 4 itens | Exemplo JSON possui apenas 1 imagem | Exemplo inválido frente à própria regra; induz implementação errada | Expandir exemplo para no mínimo 4 imagens |
| INC-08 | Fluxo `LANDING_PAGE_IMAGE_PLANNING` → `LANDING_PAGE_HTML` | Consistência de nomenclatura no encadeamento final | Regras de consistência gerais ainda referenciam validação com `landingCodeBundle`, enquanto seção HTML operacional trabalha com `landingPageHtml` | Pipeline de validação pode divergir por etapa/serviço | Alinhar todas as referências para o mesmo artefato final |
| INC-09 | Artefatos gerados por IA (geral) | Todo registro IA deve conter metadados `model` e `prompt` | Regra geral cita auditoria (`model` e `prompt`), mas os contratos detalhados/envelopes não padronizam onde esses campos vivem (envelope vs content) e o exemplo não os apresenta de forma consistente por artefato | Auditoria incompleta e inconsistência de persistência entre serviços | Definir padrão único (`artifact.audit.model`, `artifact.audit.prompt`) e refletir em todos os artefatos canônicos |
| INC-10 | `landingPageWireframe` ↔ `landingPageCopy` | Preservar CTA oficial de anúncio com igualdade explícita | Em copy a regra exige igualdade entre `hero.ctaLabel`, `primaryCTA`, `ctaBlocks[].ctaLabel`, `ctaBlocks[].matchAdCta`; no wireframe o `ctaSlot.matchAdCta` é opcional por seção com CTA (recomendado, não obrigatório) | Lacuna de validação de CTA em seções com CTA no wireframe | Tornar `ctaSlot.matchAdCta` obrigatório quando `ctaSlot.hasCta = true` |

## Observações de consolidação

- O documento está sólido na definição de objetivo de cada artefato, porém mistura **schema canônico**, **regras operacionais** e **exemplo** sem separar formalmente o que é obrigatório de produção e o que é apenas ilustrativo.
- As principais quebras vêm de três frentes:
  1. Nomenclatura dual do artefato final (`landingCodeBundle` vs `landingPageHtml`);
  2. Contradições de cardinalidade mínima vs checks obrigatórios;
  3. Exemplo JSON que viola regras críticas do próprio contrato.

## Proposta de padronização rápida

1. Escolher um único nome de artefato final e atualizar tabela, fluxo, regras e exemplo.
2. Harmonizar todos os `mínimos` com os `checks obrigatórios`.
3. Reescrever o exemplo JSON para ser 100% válido (cardinalidade e `sectionId` cruzado).
4. Formalizar bloco único de auditoria IA (`model` e `prompt`) no envelope canônico.
5. Publicar uma matriz de validação automática por artefato (campos, cardinalidade, enums e dependências cruzadas).
