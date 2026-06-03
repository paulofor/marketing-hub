# Plano de pipeline para biblioteca de páginas de venda

> Documento gerado por ChatGPT para o projeto Marketing Hub.

## 1. Objetivo

Transformar a base de aproximadamente 400 páginas de vendas bem-sucedidas em um insumo estruturado para ajudar o Marketing Hub a criar novas páginas de vendas com mais consistência comercial, visual e estratégica.

A ideia não é copiar HTML, textos ou designs dessas páginas. A ideia é extrair padrões reutilizáveis, como sequência de seções, tipos de prova, formas de CTA, estrutura de oferta, mecanismos de persuasão, padrões visuais e decisões de formulário.

Esses padrões devem alimentar as etapas atuais do pipeline de geração de landing pages:

```text
referências vencedoras
  -> análise estruturada
  -> biblioteca de padrões
  -> seleção de padrões relevantes
  -> geração do wireframe
  -> geração da copy
  -> geração do planejamento de imagens
  -> geração do design preset
  -> assembler determinístico
  -> validação final
```

## 2. Princípio central

A base de páginas deve virar uma biblioteca de inteligência comercial, não uma biblioteca de templates para clonagem.

O sistema deve usar as páginas para responder perguntas como:

- Quais seções aparecem com mais frequência em páginas de alta conversão?
- Em que ordem essas seções aparecem?
- Onde os CTAs costumam entrar?
- Que tipos de prova são usados antes do formulário ou antes da compra?
- Quais padrões de hero funcionam melhor para cada tipo de oferta?
- Quais objeções são tratadas no FAQ?
- Que tipos de imagens ou mockups ajudam a reduzir ceticismo?
- Que estrutura de formulário parece ter menor fricção?

O resultado deve ser usado como contexto para gerar páginas novas, sem reproduzir textos, imagens, marcas, depoimentos ou design específico das páginas originais.

## 3. Visão geral da arquitetura

A arquitetura recomendada combina três camadas:

1. **Banco estruturado** para metadados, filtros e análises objetivas.
2. **Vector store** para busca semântica de páginas e padrões parecidos.
3. **Biblioteca de padrões comerciais** para alimentar as etapas de geração.

Fluxo recomendado:

```text
HTML bruto das páginas
  -> limpeza e normalização
  -> extração de estrutura
  -> análise comercial por IA
  -> geração de pageAnalysis.json
  -> geração/atualização de patternLibrary.json
  -> embeddings dos resumos e padrões
  -> indexação em vector store
  -> busca semântica por nova oferta
  -> seleção de padrões úteis
  -> reference-patterns.json
  -> geração da nova landing
```

## 4. Entrada do pipeline

Cada página da base deve ser cadastrada com um registro inicial.

Exemplo:

```json
{
  "pageId": "sales-page-0001",
  "sourceUrl": "",
  "htmlStoragePath": "",
  "nicho": "fitness",
  "tipoOferta": "lead magnet",
  "ticket": "baixo",
  "idioma": "pt-BR",
  "observacoes": "Página considerada bem-sucedida por histórico interno."
}
```

Campos recomendados:

- `pageId`
- `sourceUrl`
- `htmlStoragePath`
- `nicho`
- `subnicho`
- `tipoOferta`
- `ticket`
- `idioma`
- `pais`
- `origem`
- `observacoes`
- `statusAnalise`

## 5. Etapa 1 — Limpeza e normalização do HTML

Objetivo: reduzir ruído antes de mandar a página para análise.

Remover ou ignorar:

- scripts de tracking;
- pixels;
- comentários;
- CSS não essencial para análise comercial;
- menus repetitivos;
- rodapés legais longos;
- blocos de cookie;
- popups;
- código minificado que não ajuda a entender a página.

Preservar:

- ordem visual das seções;
- headings;
- textos principais;
- CTAs;
- formulários;
- imagens e `alt`;
- cards;
- listas;
- blocos de prova;
- FAQ;
- estrutura de preço/oferta/garantia quando existir.

Saída sugerida:

```json
{
  "pageId": "sales-page-0001",
  "cleanText": "...",
  "sectionCandidates": [],
  "ctaCandidates": [],
  "formCandidates": [],
  "imageCandidates": []
}
```

## 6. Etapa 2 — Extração estrutural

Objetivo: transformar a página em uma representação navegável e comparável.

Extrair:

- sequência de seções;
- títulos de seção;
- tipo de cada seção;
- CTAs e posições;
- imagens principais;
- formulários;
- listas;
- FAQ;
- prova social;
- stack de oferta;
- preço, bônus e garantia quando existirem;
- densidade de texto por seção;
- presença de mockup, vídeo ou depoimento.

Exemplo de saída:

```json
{
  "pageId": "sales-page-0001",
  "sectionSequence": [
    {
      "index": 1,
      "sectionType": "hero",
      "commercialRole": "promessa-prova-acao",
      "hasPrimaryCta": true,
      "hasProductMockup": true,
      "hasForm": false
    },
    {
      "index": 2,
      "sectionType": "dor",
      "commercialRole": "identificacao-amplificacao"
    }
  ],
  "ctaMap": [
    {
      "text": "Começar agora",
      "position": "hero",
      "target": "form"
    }
  ],
  "forms": [
    {
      "position": "middle",
      "fieldCount": 2,
      "fields": ["nome", "email"]
    }
  ]
}
```

## 7. Etapa 3 — Análise comercial por IA

Objetivo: entender por que a página funciona como página de venda.

A análise deve responder:

- Qual é a promessa central?
- Qual dor principal é explorada?
- Qual mecanismo é usado?
- Qual prova é apresentada?
- Qual objeção cada seção remove?
- Onde a página cria desejo?
- Onde a página reduz risco?
- Onde a página pede ação?
- O formulário parece leve ou pesado?
- Que padrão de oferta aparece?
- Que riscos de compliance existem?

Saída sugerida:

```json
{
  "pageId": "sales-page-0001",
  "commercialAnalysis": {
    "primaryPromise": "",
    "primaryPain": "",
    "mechanism": "",
    "proofTypes": [],
    "objectionMap": [],
    "ctaStrategy": {},
    "formFriction": "baixa",
    "riskReducers": [],
    "conversionPattern": "lead-magnet-preview-before-form"
  }
}
```

## 8. Etapa 4 — Extração de padrões reutilizáveis

Objetivo: transformar observações específicas em padrões genéricos.

Exemplo:

Página específica:

```text
Hero com promessa de ver uma prévia antes de comprar, mockup ao lado e CTA para formulário.
```

Padrão reutilizável:

```json
{
  "patternId": "hero-preview-before-action",
  "category": "hero",
  "description": "Hero que reduz risco mostrando uma prévia visual antes da ação principal.",
  "whenToUse": [
    "ofertas com ceticismo alto",
    "produtos digitais com entregável visual",
    "lead magnets que prometem amostra rápida"
  ],
  "recommendedSlots": [
    "headline",
    "subheadline",
    "3 bullets",
    "primary CTA",
    "visual mockup",
    "microcopy de confiança"
  ],
  "risks": [
    "não prometer resultado garantido",
    "não exagerar no tempo de entrega"
  ]
}
```

Categorias de padrão recomendadas:

- `hero`
- `dor`
- `mecanismo`
- `prova`
- `oferta`
- `bonus`
- `preco`
- `garantia`
- `formulario`
- `faq`
- `cta`
- `design`
- `imagem`
- `mobile`

## 9. Etapa 5 — Biblioteca de padrões

A biblioteca deve agrupar padrões que aparecem em várias páginas.

Exemplo de item:

```json
{
  "patternId": "form-low-friction-preview",
  "category": "formulario",
  "description": "Formulário curto usado para entregar uma prévia ou material gratuito.",
  "whenToUse": [
    "captura de lead",
    "preview personalizado",
    "amostra antes da compra"
  ],
  "recommendedFields": ["nome", "email"],
  "maxVisibleFields": 3,
  "recommendedMicrocopy": [
    "sem call",
    "sem compromisso",
    "tempo estimado",
    "privacidade"
  ],
  "relatedSections": ["hero", "prova", "faq"],
  "examples": ["sales-page-0001", "sales-page-0028"]
}
```

A biblioteca não deve guardar copy literal. Deve guardar padrões, papéis comerciais e recomendações abstratas.

## 10. Etapa 6 — Indexação em vector store

Objetivo: permitir busca semântica por páginas e padrões parecidos.

Não indexar apenas HTML bruto. Indexar principalmente:

- resumo comercial da página;
- análise estruturada;
- padrões extraídos;
- sequência de seções;
- objeções removidas;
- tipos de prova;
- perfil de oferta;
- perfil visual;
- perfil de formulário.

Exemplo de texto para embedding:

```text
Página de captura para produto digital com promessa de preview rápido antes da compra. Usa hero com dor + promessa, mockup do entregável, CTA para formulário curto, seção de antes/depois, mecanismo em 3 passos, prova visual e FAQ para remover objeções de tempo, personalização e risco.
```

Metadados a salvar junto do embedding:

```json
{
  "pageId": "sales-page-0001",
  "nicho": "fitness",
  "tipoOferta": "lead magnet",
  "objetivo": "captura",
  "hasForm": true,
  "fieldCount": 2,
  "hasMockup": true,
  "hasFaq": true,
  "hasBeforeAfter": true,
  "patternIds": [
    "hero-preview-before-action",
    "form-low-friction-preview"
  ]
}
```

## 11. Etapa 7 — Busca de referências para nova landing

Quando uma nova página for gerada, criar uma query semântica a partir da oferta.

Exemplo:

```json
{
  "nicho": "personal trainer",
  "tipoOferta": "preview personalizado em PDF",
  "objetivo": "captura de email",
  "dorPrincipal": "conversa vira preço cedo demais",
  "mecanismo": "amostra em PDF + mini-kit",
  "restricoes": [
    "sem promessa de resultado garantido",
    "sem call",
    "briefing rápido"
  ]
}
```

A query semântica poderia ser:

```text
Landing page de captura para produto digital com preview visual rápido, formulário curto, dor de objeção de preço, prova por mockup do entregável e mecanismo em três passos.
```

A busca deve retornar:

- páginas semelhantes;
- padrões semelhantes;
- sequências de seções recomendadas;
- riscos a evitar;
- sugestões de prova/CTA/formulário.

## 12. Etapa 8 — Seleção de padrões

Após recuperar referências, uma etapa com IA deve escolher os padrões mais úteis.

Saída sugerida:

```json
{
  "referenceSelection": {
    "query": "...",
    "selectedPages": [
      {
        "pageId": "sales-page-0001",
        "whySelected": "Oferta também usa preview antes da captura."
      }
    ],
    "selectedPatterns": [
      {
        "patternId": "hero-preview-before-action",
        "applyTo": "wireframe",
        "reason": "A oferta precisa reduzir risco antes do formulário."
      },
      {
        "patternId": "form-low-friction-preview",
        "applyTo": "wireframe",
        "reason": "A promessa é briefing rápido."
      },
      {
        "patternId": "faq-objection-time-personalization-risk",
        "applyTo": "copy",
        "reason": "Remove dúvidas sobre tempo, personalização e dados."
      }
    ],
    "rejectedPatterns": [
      {
        "patternId": "long-vsl-sales-page",
        "reason": "Oferta atual é captura rápida, não VSL longa."
      }
    ]
  }
}
```

## 13. Etapa 9 — `reference-patterns.json`

Esse arquivo vira entrada oficial para as etapas gerativas.

Exemplo:

```json
{
  "jobId": "...",
  "offerContext": {},
  "patternsToUse": [],
  "sectionSequenceRecommendation": [],
  "copyMoves": [],
  "visualPatterns": [],
  "formPattern": {},
  "designDirection": {},
  "risksToAvoid": []
}
```

Cada etapa usa uma parte:

- `wireframe` usa `sectionSequenceRecommendation`, `formPattern`, `visualPatterns`.
- `copy` usa `copyMoves`, `objectionMap`, `risksToAvoid`.
- `imagePlanning` usa `visualPatterns`.
- `designPreset` usa `designDirection`.
- `assembler` não usa diretamente padrões; ele apenas monta o resultado final já produzido.

## 14. Integração com o pipeline atual

Pipeline final recomendado:

```text
1. Receber contexto da oferta
2. Criar query semântica da oferta
3. Buscar no vector store
4. Selecionar padrões relevantes
5. Gerar reference-patterns.json
6. Gerar wireframe.json
7. Validar wireframe schema
8. Gerar copy.json
9. Validar copy schema
10. Gerar image-planning.json
11. Validar image schema
12. Gerar design-preset.json
13. Validar design schema
14. Assembler determinístico
15. Validação HTML
16. Validação visual/mobile/desktop
17. Etapa futura: script de formulário
```

## 15. Validações recomendadas

### Validação de referência

Antes de usar padrões recuperados:

- O padrão é compatível com o tipo de oferta?
- O padrão é compatível com o nível de fricção desejado?
- O padrão respeita compliance?
- O padrão depende de prova que não temos?
- O padrão exige preço/garantia/VSL quando a página é só captura?

### Validação do wireframe

- Tem CTA no hero?
- Tem formulário ou âncora para formulário?
- Tem prova antes do formulário?
- Tem seção para objeções?
- Tem imagens com papel comercial claro?
- Tem responsividade mobile/desktop explícita?

### Validação da copy

- Copy bate com a promessa do anúncio?
- Não promete resultado garantido?
- Não inventa benefícios não suportados?
- Remove objeções principais?
- CTAs são claros?

### Validação do design preset

- Tokens usados existem?
- Há tokens mobile e desktop quando necessário?
- Imagens têm comportamento responsivo?
- Botões têm aparência de botão?
- Formulário tem boa legibilidade?

### Validação do assembler

- Não inventou estilo.
- Não inventou href.
- Não inventou texto.
- Não inventou imagem.
- Montou apenas o que veio dos JSONs.
- Gerou HTML válido.

## 16. Cuidados legais e éticos

A base deve ser usada para aprender padrões, não para copiar páginas.

Evitar:

- copiar headlines;
- copiar blocos de copy;
- copiar depoimentos;
- copiar imagens;
- copiar identidade visual;
- copiar promessas específicas;
- copiar estrutura visual de forma muito literal.

Preferir:

- extrair princípios;
- identificar padrões de persuasão;
- entender sequência comercial;
- aprender tipos de prova;
- mapear objeções;
- construir componentes próprios.

## 17. MVP recomendado

Para não tentar fazer tudo de uma vez, implementar em quatro passos.

### MVP 1 — Ingestão e análise de 20 páginas

- Selecionar 20 páginas da base.
- Limpar HTML.
- Gerar `pageAnalysis.json` para cada uma.
- Criar primeiros padrões manualmente/semiautomaticamente.

### MVP 2 — Vector store e busca

- Gerar embeddings dos resumos/análises.
- Criar busca semântica.
- Recuperar páginas parecidas a partir de uma nova oferta.

### MVP 3 — Seleção de padrões

- Criar etapa `reference-patterns.json`.
- Alimentar o gerador de wireframe com esses padrões.
- Comparar páginas geradas com e sem referências.

### MVP 4 — Validação automática

- Criar checks para mobile/desktop.
- Criar checks de CTA, formulário, prova e FAQ.
- Salvar padrões usados em cada página gerada.

## 18. Métricas de sucesso

Métricas técnicas:

- Percentual de páginas analisadas com sucesso.
- Percentual de padrões reutilizáveis extraídos.
- Tempo médio de busca no vector store.
- Número de warnings por página gerada.
- Falhas de schema por etapa.

Métricas comerciais:

- Aumento de taxa de clique no CTA.
- Aumento de taxa de envio do formulário.
- Redução de páginas rejeitadas visualmente.
- Tempo menor para gerar página publicável.
- Número de variações geradas por oferta.

Métricas qualitativas:

- Clareza do hero.
- Força da prova.
- Baixa fricção do formulário.
- Coerência visual.
- Aderência ao anúncio.

## 19. Conclusão

A base de 400 páginas pode se tornar um ativo estratégico do Marketing Hub se for tratada como fonte de padrões comerciais e não como coleção de templates.

A recomendação é usar uma arquitetura híbrida:

```text
banco estruturado + vector store + biblioteca de padrões + validação forte
```

O vector store ajuda a encontrar referências semanticamente parecidas. O banco estruturado ajuda a filtrar por características objetivas. A biblioteca de padrões transforma as referências em conhecimento reutilizável. O assembler continua determinístico e não inventa nada.

Essa combinação deve melhorar a qualidade das páginas geradas, reduzir retrabalho e criar uma vantagem acumulativa: quanto mais páginas analisadas e quanto mais páginas próprias forem geradas e medidas, melhor a inteligência comercial do sistema fica.

## 20. Execução inicial no Marketing Hub — lote Hotmart de 400 produtos

Primeiro passo implementado para começar pelo lote Hotmart já coletado:

- endpoint operacional: `POST /api/mois/sales-library/hotmart-products:ingest`;
- entrada mínima: `{ "workspaceId": "workspace-001", "limit": 400 }`;
- quando `jobId` não é informado, o backend escolhe o job Hotmart mais recente persistido em `mois_collected_reference`;
- origem dos dados: `mois_collected_reference` com `source = 'HOTMART'`;
- URL priorizada: `sales_page_url`, com fallback em `product_url` e `url`;
- destino: `mois_sales_library_url_ingest`, com criação de job `PENDING` em `mois_sales_library_processing_job` apenas quando a URL ainda não existia;
- objetivo deste passo: transformar o lote bruto de produtos Hotmart em fila deduplicada de páginas para análise do MVP 1.
