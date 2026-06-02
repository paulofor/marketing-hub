{prompt-regras-globais}

# Etapa: Gera Prompt Imagens (landing-page-image-planning)

template_id: landing-page-image-planning
template_version: v2
artifact_target: landingPageImagePlanning

Objetivo:
- Ler o wireframe salvo no experimento e gerar prompts finais, específicos e executáveis para cada elemento visual (`tag: img`) da landing.
- Fazer as imagens aumentarem a conversão: prova visual, demonstração do produto, redução de objeção e percepção de valor.
- Evitar prompts genéricos, abstratos ou decorativos que gerem imagens bonitas mas inúteis para vender.

## Regras de contrato
- Use como base SOMENTE os elementos de imagem definidos no wireframe (`tag: img`).
- Não altere estrutura do wireframe nem da copy; apenas planeje imagens.
- Não invente novas imagens, não remova imagens e não renomeie `sectionId` ou `elementId`.
- A resposta DEVE ser um objeto com o atributo `images` (array) dentro de `landingPageImagePlanning`.
- Cada item deve possuir somente: `sectionId`, `elementId`, `imageGoal` e `imagePrompt`.
- É proibido retornar array na raiz ou concatenar múltiplos JSONs; retorne exatamente um único objeto JSON com `landingPageImagePlanning.images`.
- Responda em JSON válido e estritamente aderente ao schema da etapa.

## Contexto disponível
- NICHO: {{NICHE_NAME}}
- Ângulo de campanha: {dados-campaignAngle}
- Copy do anúncio: {dados-adCopy}
- Briefing de imagem do anúncio: {dados-adImageBriefing}
- Wireframe da landing: {dados-landingPageWireframe}

## Fonte de verdade visual
Para cada `tag: img`, use obrigatoriamente os dados do próprio elemento no wireframe:
- `sectionId`: seção onde a imagem aparece.
- `elementId`: id do elemento de imagem.
- `briefingVisual.tipoVisualEsperado`: tipo concreto de imagem esperado.
- `briefingVisual.funcaoComercial`: função da imagem na conversão.
- `briefingVisual.objecaoQueRemove`: objeção que a imagem precisa reduzir.
- `briefingVisual.classificacaoVisual`: mockup, foto, ilustração, diagrama ou print conceitual.
- `briefingVisual.aspectRatio`, `maxVisualHeight`, `layoutRole`, `posicaoDesejada` e `relacaoComCta`.
- `asset.alt`: use como pista de acessibilidade e intenção, mas não gere campo `altText` no output.

## Qualidade comercial obrigatória
- Toda imagem deve responder visualmente a uma pergunta do usuário: “o que eu vou receber?”, “isso parece útil?”, “isso serve para mim?”, “isso é confiável?”, “por que isso vale meu tempo?”.
- Priorize tangibilidade: mockups de páginas, cards de conteúdo, telas conceituais, mapas de progresso, checklists, antes/depois visual ou demonstrações do mecanismo.
- Evite imagens abstratas, ícones genéricos, pessoas sorrindo sem contexto, gráficos decorativos, objetos aleatórios ou ilustrações que não provem a entrega.
- A imagem do hero deve aumentar desejo e confiança em poucos segundos: mostrar o produto/resultado de forma premium, organizada e com aparência real.
- Imagens de prova devem mostrar a entrega como algo concreto e visualmente valioso, não apenas um documento branco com marca d’água gigante.
- Quando o produto for digital, prefira mockup premium de tela/documento/card com camadas, sombras, bordas e elementos visuais legíveis.
- Quando houver dor/resultado, mostre contraste visual ético e plausível, sem exagero e sem prometer resultado garantido.

## Direção de arte dos prompts
Cada `imagePrompt` deve ser autossuficiente e incluir, de forma natural:
1. O tipo de visual: mockup, print conceitual, diagrama, foto contextual ou ilustração funcional.
2. O nicho e o contexto comercial.
3. O que deve aparecer na composição principal.
4. A função da imagem na landing e a objeção que ela remove.
5. Estilo visual compatível com landing moderna/premium: composição limpa, contraste bom, profundidade, sombras suaves, bordas arredondadas, aparência profissional.
6. Orientação de enquadramento/proporção coerente com `aspectRatio` e posição na seção.
7. Restrições negativas dentro do próprio prompt: sem logos reais, sem marcas registradas, sem texto pequeno ilegível, sem elementos poluídos, sem aparência amadora.

## Regras específicas para texto dentro da imagem
- Não dependa de textos longos dentro da imagem para comunicar a ideia; modelos de imagem podem errar texto pequeno.
- Se for necessário texto, use no máximo 1 a 4 palavras grandes e simples em português, como rótulos visuais: “AMOSTRA”, “Semana 1”, “Progresso”, “Convite”.
- Para mockups de PDF, página, dashboard ou chat, represente o conteúdo com blocos, cards, linhas e rótulos curtos. Não peça parágrafos legíveis.
- Marca d’água pode aparecer quando fizer sentido, mas não deve destruir a percepção de valor do mockup. Ela deve ser sutil, diagonal ou translúcida.
- Não peça screenshots reais de apps conhecidos. Se precisar representar conversa, descreva como “interface conceitual de mensagens, sem logotipo real”.

## Regras específicas por classificação visual
- `mockup`: peça uma composição de produto digital premium, com moldura, camadas, sombras, cartões e detalhes suficientes para parecer entrega real.
- `print conceitual`: peça uma interface fictícia/ilustrativa, sem marcas reais, com blocos legíveis e foco no fluxo/resultado.
- `diagrama`: peça fluxo simples com 3 passos ou antes/depois, usando formas limpas e hierarquia clara.
- `foto`: peça cena plausível do nicho, humana e contextual, sem pose de banco de imagem; conecte a dor/resultado.
- `ilustração`: use apenas se for funcional para explicar mecanismo; evite ilustração decorativa genérica.

## Consistência com a landing
- As imagens precisam conversar entre si como uma mesma campanha: mesma sensação de qualidade, mesma paleta aproximada, mesma linguagem visual e mesmo nível de acabamento.
- Não misture estilos incompatíveis, como foto realista no hero e ilustração cartoon em prova, salvo se o wireframe pedir claramente.
- A imagem não pode competir com o CTA; deve preparar o clique, reforçar a promessa ou reduzir risco.
- O prompt deve mencionar que a imagem será usada em uma landing page, para o modelo privilegiar composição limpa e comercial.

## Quality gate interno antes de responder
Antes de devolver o JSON, verifique:
- Cada imagem corresponde a um `tag: img` real do wireframe.
- Cada `sectionId` e `elementId` está exatamente igual ao wireframe.
- Cada prompt é específico o suficiente para gerar uma imagem útil sem depender de explicação externa.
- Nenhum prompt é genérico como “imagem moderna de marketing digital”.
- Nenhum prompt pede logos reais, marcas registradas, prints reais ou texto longo.
- Pelo menos a imagem principal tangibiliza o produto/entrega e não apenas decora a página.
- O conjunto de imagens melhora a percepção de valor da landing.

OUTPUT_CONTRACT
Retorne somente JSON válido conforme schema da etapa:
{
  "landingPageImagePlanning": {
    "images": [
      {
        "sectionId": "id-da-secao-do-wireframe",
        "elementId": "id-do-elemento-img-do-wireframe",
        "imageGoal": "função comercial objetiva da imagem",
        "imagePrompt": "prompt final autossuficiente para gerar a imagem"
      }
    ]
  }
}
