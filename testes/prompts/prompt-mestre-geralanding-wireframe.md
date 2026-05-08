# Prompt mestre — geração de wireframe HTML (única rodada)

Você vai implementar **em uma única entrega** a geração de HTML provisório para GeraLanding, com separação de responsabilidades, integração no backend e validação final.

## Objetivo
Gerar um HTML provisório a partir de um JSON com `sectionOrder`, usando `uiTags` e `uiSizes`, preenchendo placeholders e aplicando estilos visuais de preview.

## Entradas/Saídas obrigatórias
- **Entrada JSON:** `testes/entradas/geralanding-wirefram.json`
- **Saída HTML:** `testes/saídas/wireframe.html`

## Requisitos de implementação

### 1) Estrutura de classes (testes)
1. Criar `testes/WireframeHtmlGenerator.java` com método público:
   - `String generateFromJson(String json)`
   - Esse método recebe o JSON e retorna o HTML completo como string.
2. Criar `testes/WireframeMain.java` com `main()` que:
   - lê o arquivo de entrada,
   - instancia `WireframeHtmlGenerator`,
   - chama `generateFromJson(json)`,
   - grava a saída em `testes/saídas/wireframe.html`.

### 2) Regras de renderização HTML
- Gerar documento HTML válido com:
  - `<!doctype html>`
  - `<html lang="pt-BR">`
  - `<meta charset="UTF-8">`
  - `<meta name="viewport" content="width=device-width, initial-scale=1">`
  - `<style>` com CSS convertido de `uiSizes`.
- Usar `sectionOrder[].uiTags` para montar o corpo.
- Usar `sectionOrder[].uiSizes` para gerar CSS (incluindo `@media`).

### 3) Placeholders de conteúdo
Preencher elementos vazios com texto estilo Lorem Ipsum:
- `h1`, `h2`, `h3`, `p`, `li`, `span`, `summary`, `a`, `button`.
- Para `img`, usar `src` placeholder e `alt` descritivo.

### 4) Estilo de preview visual
- Aplicar cores alternadas nas seções (fundo/texto) **quando não quebrar contratos existentes**.
- Aplicar cor diferenciada no formulário (`#s1-form` / `#lead-form`).
- Aplicar cores diferenciadas nas áreas de imagem (placeholder de slots de imagem).

### 5) Integração backend (GeraLanding)
- Copiar/adaptar o gerador para:
  - `backend/ads-service/src/main/java/com/marketinghub/geralanding/WireframeHtmlGenerator.java`
- Atualizar:
  - `backend/ads-service/src/main/java/com/marketinghub/geralanding/WireframeProvisionalHtmlAssembler.java`
- Fluxo esperado do assembler:
  1. receber `modelResponse`,
  2. identificar `landingPageWireframe` (ou raiz),
  3. pré-processar `uiTags` (sanitizar wrappers html/head/body/doctype),
  4. manter comportamento de preview lorem por `uiSizeTexts` (ex.: `data-wireframe-lorem-slot`) se já houver contrato/teste,
  5. delegar ao `WireframeHtmlGenerator` backend para montar HTML final.

### 6) Compatibilidade e segurança de mudança
- Não quebrar testes existentes de `WireframeProvisionalHtmlAssembler`.
- Se algum comportamento visual novo conflitar com testes/contratos, priorizar compatibilidade do backend e ajustar apenas no gerador de testes local.

### 7) Validação obrigatória
Executar e reportar:
1. `javac testes/WireframeMain.java testes/WireframeHtmlGenerator.java`
2. `java -cp testes WireframeMain`
3. validações básicas do HTML gerado (doctype, html/body, número de sections, presença de lead-form).
4. no backend:
   - `cd backend/ads-service && mvn -q -Dtest=WireframeProvisionalHtmlAssemblerTest test`

### 8) Entrega final
Na resposta final, incluir:
- resumo das mudanças por arquivo,
- comandos executados e status (pass/fail),
- observações de compatibilidade tomadas no backend.
