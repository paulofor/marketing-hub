# Matriz de responsabilidades — pipeline de landing

## Objetivo
Documento operacional para leitura rápida, separando **quem é responsável por cada item** no pipeline e quais validações de fechamento existem em cada etapa.

> Para visão campo-a-campo com dono único, consulte também: `docs/canonical/matriz-responsaveis-unicos-itens-artefato.md`.

## Mapa por etapa

| Etapa | Responsabilidade principal | Itens canônicos sob responsabilidade | Validação pesada no fechamento (`complete`) |
|---|---|---|---|
| `landingPageCopy` | Narrativa comercial e continuidade de mensagem | `hero` (headline/promise/ctaLabel), `bodySections`, `consistencyChecks` (`CTA_MATCH`, `PROMISE_MATCH`) | Rejeita payload sem `hero` estruturado (ou fallback legado mínimo), sem `bodySections` válidos e sem checks obrigatórios `CTA_MATCH` + `PROMISE_MATCH`. |
| `landingPageWireframe` | Estrutura da página (ordem/hierarquia) e estrutura canônica de imagem | `sectionOrder`, `sectionId`, `surfaceSpec.surfaceToken`, `formSpec`, bindings estruturais de imagem por seção | Rejeita sem `sectionOrder`, sem `sectionId`, sem `surfaceSpec.surfaceToken` por seção ou sem estrutura de imagem por seção quando obrigatória. |
| `landingPageImagePlanning` | Geração do prompt final para o modelo de imagem | `generationPrompt` | Rejeita payload sem `generationPrompt` textual válido; não pode redefinir ownership estrutural herdado do wireframe. |
| `landingPageDesignPreset` | Detalhe visual por seção e por elemento | `sectionPresets` com `surfaceStyle`/`contrastMode`; `componentPresets.primitives` para `<p>`, `<h1>`, `<h2>`, `<h3>`, `<ul>/<li>`, `<button>`, `<form>`, `<label>`, `<input>`, `<img>` | Rejeita sem `sectionPresets`; rejeita sem cobertura total de `sectionId`; rejeita sem declaração explícita de primitives/tokens por elemento. |
| `landingPageHtml` | Implementação final e aderência total de contrato | `htmlDocument` + binding de formulário, superfície e imagens | Valida runtime de form, binding de superfície e binding canônico de imagem antes de aceitar publicação. |

## Regra de ownership visual x estrutural

- **Estrutural (`wireframe`)**: `surfaceToken` + organização da página.
- **Visual (`designPreset`)**: `surfaceStyle` + `contrastMode` por `sectionId`.
- **Namespace CSS canônico do LHM**: classes emitidas no HTML determinístico devem seguir prefixo `lhm-` e corresponder aos seletores presentes em `landingPageDesignPreset.lhmRuntime.baseCss`.
- **HTML final (`LHM`)**: combina os dois contratos e publica os `data-*` aderentes.
- **Contrato de saída por etapa de IA**: quando uma etapa do pipeline solicitar resolução ao modelo, a resposta deve ser pedida e validada por **JSON Schema canônico da etapa** (sem texto livre fora do envelope do artefato).

### Regra anti-duplicidade (fonte de verdade única por campo)

- Não há duplicidade de responsabilidade quando os campos são separados por natureza:
  - `surfaceToken` → **exclusivo** do `landingPageWireframe.sectionOrder[*].surfaceSpec`.
  - `surfaceStyle` e `contrastMode` → **exclusivos** do `landingPageDesignPreset.sectionPresets[*]`.
- Se `style/contrastMode` aparecerem no wireframe por legado, devem ser tratados apenas como fallback transitório; a fonte de verdade vigente continua sendo o design preset.
- Em conflito entre valores, prevalece `landingPageDesignPreset` para `style/contrastMode` e prevalece o wireframe para `surfaceToken` e `sectionId`.

## Gate antecipado antes de HTML

Além do fechamento de cada etapa, o backend executa pré-validação antes de gerar `landingPageHtml` para bloquear inconsistências de artefatos cedo (sem esperar o erro final de publicação).

## Score comercial (vendas)

Além das validações de contrato, a qualidade comercial da landing deve ser avaliada pelo **Scorecard de Vendas — Landing Page (v2)** em `docs/canonical/scorecard-vendas-landing.md`, incluindo gate automático de regressão para ajuste quando o score ficar abaixo do limiar.
