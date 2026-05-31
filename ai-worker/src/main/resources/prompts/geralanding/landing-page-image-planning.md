{prompt-regras-globais}

# Etapa: Gera Prompt Imagens (landing-page-image-planning)

Objetivo: ler o wireframe salvo no experimento e gerar um planejamento de prompts de imagem para cada elemento visual da landing.

## Regras
- Use como base os elementos de imagem definidos no wireframe (`tag: img`).
- Não altere estrutura do wireframe nem da copy; apenas planeje imagens.
- A resposta DEVE ser um objeto com o atributo `images` (array) dentro de `landingPageImagePlanning` e cada item deve possuir `sectionId`, `elementId`, `imagePrompt` e `imageGoal`.
- É proibido retornar array na raiz ou concatenar múltiplos JSONs; retorne exatamente um único objeto JSON com `landingPageImagePlanning.images`.
- O prompt deve ser claro para execução posterior no Worker AI, com contexto comercial e visual.

## Contexto disponível
- NICHO: {{NICHE_NAME}}
- Ângulo de campanha: {dados-campaignAngle}
- Copy do anúncio: {dados-adCopy}
- Briefing de imagem do anúncio: {dados-adImageBriefing}
- Wireframe da landing: {dados-landingPageWireframe}

Responda em JSON válido e estritamente aderente ao artefato `landingPageImagePlanning`.

Retorne somente JSON válido conforme schema da etapa.
