# Uso de Páginas de Vendas de Sucesso em Pequenos Passos

## Objetivo

Este documento define uma abordagem incremental para usar páginas de vendas de sucesso como referência na criação de novas páginas de vendas dentro do Marketing Hub.

A ideia central é não tentar criar um sistema que “entende páginas de vendas de sucesso e gera tudo” de uma vez.

Em vez disso, o sistema deve evoluir em pequenos passos, aprendendo gradualmente a:

1. coletar páginas de referência;
2. organizar essas páginas em uma biblioteca;
3. extrair padrões estruturados;
4. transformar esses padrões em insumos úteis;
5. usar esses insumos nas etapas de geração de novas páginas.

O objetivo é copiar **fórmulas comerciais vencedoras**, não páginas existentes de forma literal. Na prática, o sistema deve transformar referências em inteligência estruturada para apoiar a geração de:

- wireframes;
- copy;
- prompts de imagem;
- presets de design.

---

## Princípio central

> Referências devem ser usadas para copiar a fórmula estrutural que vende, não para plagiar conteúdo, marca, identidade visual ou ativos.

O sistema não deve reproduzir literalmente headlines, layouts proprietários ou visuais de páginas existentes.

A leitura correta da biblioteca é: identificar qual sequência de Dor → Resultado → Mecanismo → Prova → Oferta está funcionando, entender como ela reduz dor/esforço e aumenta facilidade/prazer, e adaptar essa fórmula para produtos próprios do Marketing Hub.
Ele deve extrair padrões, estratégias e orientações reutilizáveis.

A lógica recomendada é:

```text
biblioteca de referências
        ↓
análise estruturada
        ↓
padrões reutilizáveis
        ↓
insumos por etapa de geração
```

## Processo recorrente para produtos quentes

A partir de 2026-07-03, produtos Hotmart com temperatura `>= 80` são o primeiro critério operacional para alimentar o aprendizado comercial recorrente.

O processo não deve recapturar ou reanalisar páginas sem necessidade. A captura já existe como HTML bruto e a análise comercial já existe na Biblioteca de Páginas de Vendas. O novo passo é promover, de forma recorrente, os produtos quentes já capturados e analisados para o pipeline `dossieproduto.v1`.

Fluxo canônico:

1. Listar candidatos em `GET /api/mois/sales-library/hot-products/dossier-candidates`.
2. Exigir Hotmart, temperatura mínima, HTML útil, análise concluída e ausência de dossiê ativo/concluído.
3. Enfileirar lote em `POST /api/mois/sales-library/hot-products/dossier-candidates:enqueue`.
4. O backend apenas marca a página como `INICIADO/intake` e registra auditoria em `pipeline_dossieproduto`.
5. O `mois-sales-library-worker` consome o endpoint `pending` do `dossieproduto.v1` e executa IA, pesquisa externa, síntese e validação.
6. O resultado deve virar padrões abstratos para melhorar páginas próprias, nunca cópia literal de páginas externas.

Essa separação mantém o backend como fonte de verdade de leitura/escrita e o worker como executor da inteligência principal.

---

# As 4 camadas

## Camada 1 — Biblioteca de páginas

### Objetivo

Guardar e organizar páginas de vendas de referência.

Essa camada responde perguntas como:

- quais páginas temos?
- de que nicho são?
- qual promessa principal?
- qual mecanismo?
- qual tipo de oferta?
- qual estilo visual?
- quais blocos existem?
- qual é o status da coleta?

### Primeiro passo mínimo

Nesta fase, não é necessário ter análise avançada por IA.

O primeiro passo pode guardar apenas:

- URL;
- título;
- nicho;
- subtipo;
- promessa principal;
- status da coleta;
- screenshot;
- HTML bruto;
- data de captura.

### Exemplo de estrutura inicial

```json
{
  "url": "https://exemplo.com/pagina-de-vendas",
  "titulo": "Página de vendas exemplo",
  "nicho": "emagrecimento",
  "subtipo": "curso digital",
  "promessa_principal": "perder peso com método simples",
  "status_coleta": "capturada",
  "screenshot_url": "...",
  "html_snapshot_id": "...",
  "capturado_em": "2026-05-18T10:00:00"
}
```

### Valor gerado

Mesmo sem análise profunda, essa camada já cria uma base útil de referências consultáveis.

O sistema passa a ter um acervo organizado de páginas reais que podem ser usadas como inspiração e comparação.

### Fora do escopo inicial

Nesta camada, evitar implementar:

- análise profunda por IA;
- score de qualidade;
- benchmarking automático;
- clusterização;
- geração automática de páginas;
- classificação visual avançada.

---

## Camada 2 — Análise estruturada

### Objetivo

Transformar cada página de venda em dados estruturados e reutilizáveis.

Aqui a pergunta deixa de ser apenas:

> “qual página foi coletada?”

E passa a ser:

> “o que essa página contém?”

### Tipos de análise

#### Estrutura da página

Identificar blocos como:

- hero;
- dor;
- promessa;
- mecanismo;
- prova;
- benefícios;
- oferta;
- bônus;
- garantia;
- FAQ;
- CTA;
- depoimentos;
- fechamento.

#### Copy

Extrair ou resumir:

- headline principal;
- subheadline;
- ângulo da promessa;
- tipo de dor trabalhada;
- tipo de resultado prometido;
- mecanismo apresentado;
- objeções tratadas;
- estilo de linguagem;
- intensidade emocional;
- padrão de CTA;
- construção de autoridade;
- estrutura da oferta.

#### Visual

Identificar características como:

- estilo geral;
- densidade visual;
- tipo de layout;
- uso de contraste;
- tipo de imagem;
- presença de mockups;
- sensação visual predominante;
- nível de sofisticação percebida.

#### Imagens

Classificar padrões como:

- cenas com especialista;
- imagens de lifestyle;
- mockups de produto;
- prints de prova;
- antes e depois;
- metáforas visuais;
- demonstrações;
- imagens aspiracionais;
- imagens de autoridade.

### Primeiro passo mínimo

Não tentar extrair tudo de uma vez.

Começar com um JSON simples por página:

```json
{
  "headline": "",
  "promessa": "",
  "dor": "",
  "resultado": "",
  "mecanismo": "",
  "oferta": "",
  "secoes": [],
  "estilo_visual": "",
  "tipo_imagens": []
}
```

### Valor gerado

Essa camada transforma páginas de referência em matéria-prima para o restante do sistema.

A partir dela, o Marketing Hub consegue começar a comparar páginas, identificar padrões e preparar insumos para geração.

### Fora do escopo inicial

Nesta camada, evitar implementar:

- scoring complexo;
- análise preditiva de conversão;
- comparação automática entre páginas;
- interpretação avançada de vídeo;
- classificação excessivamente detalhada;
- modelos genéricos demais.

---

## Camada 3 — Biblioteca de padrões reutilizáveis

### Objetivo

Sair da análise individual de cada página e começar a identificar padrões entre páginas parecidas.

Aqui a pergunta muda de:

> “o que essa página tem?”

Para:

> “quais padrões aparecem nas páginas de referência?”

Essa camada é onde a biblioteca começa a virar inteligência reutilizável.

---

## Padrões para wireframe

A partir das páginas analisadas, o sistema pode identificar:

- páginas curtas, médias ou longas;
- seções mais recorrentes por nicho;
- ordem comum dos blocos;
- onde a prova costuma aparecer;
- onde o mecanismo costuma ser explicado;
- onde a oferta aparece;
- quantidade média de CTAs;
- presença ou ausência de FAQ;
- uso de hero mais direto ou mais narrativo.

### Exemplo de saída

```json
{
  "wireframe_patterns": {
    "secoes_recomendadas": [
      "hero",
      "dor",
      "mecanismo",
      "beneficios",
      "prova",
      "oferta",
      "faq",
      "cta_final"
    ],
    "observacoes": [
      "As páginas de referência apresentam prova logo após o hero.",
      "O mecanismo costuma ser apresentado antes da oferta.",
      "O FAQ é curto e orientado a objeções."
    ]
  }
}
```

---

## Padrões para copy

A biblioteca pode ajudar a identificar:

- tipos de headline mais comuns;
- ângulos de promessa;
- formas de apresentar o mecanismo;
- objeções recorrentes;
- padrões de CTA;
- formatos de oferta;
- formas de construir autoridade;
- nível de agressividade da comunicação;
- linguagem predominante do nicho.

### Exemplo de saída

```json
{
  "copy_patterns": {
    "angulos_de_promessa": [
      "resultado rápido com método simples",
      "solução sem depender de força de vontade",
      "transformação guiada passo a passo"
    ],
    "objeções_frequentes": [
      "falta de tempo",
      "medo de não conseguir aplicar",
      "tentativas anteriores que falharam"
    ],
    "tom_recomendado": "direto, emocional e orientado à transformação"
  }
}
```

---

## Padrões para imagem

A partir das referências, o sistema pode identificar:

- tipos de imagem mais usados;
- metáforas visuais recorrentes;
- cenas comuns por nicho;
- presença de especialistas;
- uso de mockups;
- imagens de transformação;
- imagens de prova;
- imagens aspiracionais;
- estilo fotográfico ou ilustrado.

### Exemplo de saída

```json
{
  "image_patterns": {
    "tipos_de_imagem": [
      "mockup do produto",
      "cena lifestyle aspiracional",
      "visual de transformação"
    ],
    "elementos_recorrentes": [
      "smartphone",
      "laptop",
      "gráficos",
      "ambiente moderno"
    ],
    "evitar": [
      "visual genérico de banco de imagens",
      "cenas sem relação direta com a promessa"
    ]
  }
}
```

---

## Padrões para design

A biblioteca pode ajudar a observar:

- uso de fundo claro ou escuro;
- nível de contraste;
- densidade visual;
- estilo premium, clínico, energético ou minimalista;
- uso de cards;
- uso de faixas;
- hierarquia visual;
- estilo dos CTAs;
- sensação geral da página.

### Exemplo de saída

```json
{
  "design_patterns": {
    "mood": "premium e confiável",
    "densidade": "média",
    "contraste": "alto",
    "superficies": "cards suaves com destaque para CTA",
    "hierarquia": "hero forte, seções bem separadas e provas destacadas",
    "direcao_visual": "clean com pontos de energia"
  }
}
```

### Primeiro passo mínimo da camada 3

Em vez de tentar descobrir automaticamente “os padrões vencedores”, começar com agrupamentos simples:

- nicho;
- tipo de oferta;
- estilo visual;
- estrutura de seções;
- tipo de promessa;
- mecanismo;
- presença ou ausência de prova forte.

### Fora do escopo inicial

Evitar nesta camada:

- ranking automático de conversão;
- inferência de sucesso sem dados confiáveis;
- machine learning complexo;
- clusterização sofisticada antes de ter volume de dados;
- recomendações automáticas sem explicação.

---

## Camada 4 — Uso na geração

### Objetivo

Usar as referências analisadas para melhorar cada etapa do pipeline de criação de páginas de vendas.

Aqui a biblioteca começa a alimentar diretamente os geradores do Marketing Hub.

As referências não devem gerar a página final de forma automática.  
Elas devem gerar insumos intermediários para cada etapa.

---

# Uso por etapa do pipeline

## 1. Gerar wireframe

O gerador de wireframe pode receber:

- nicho;
- promessa;
- dor;
- resultado;
- mecanismo;
- tipo de oferta;
- referências estruturais encontradas na biblioteca.

### Saída esperada

Um guidance de wireframe:

```json
{
  "wireframe_guidance": {
    "secoes_recomendadas": [
      "hero",
      "dor",
      "mecanismo",
      "beneficios",
      "prova",
      "oferta",
      "faq",
      "cta_final"
    ],
    "ordem_recomendada": [
      "hero",
      "prova_inicial",
      "dor",
      "mecanismo",
      "beneficios",
      "oferta",
      "faq",
      "cta_final"
    ],
    "observacoes": [
      "As referências semelhantes usam prova logo no início.",
      "O mecanismo aparece antes da oferta.",
      "A página deve evitar listas muito longas no início."
    ]
  }
}
```

### Primeiro passo mínimo

- buscar 3 páginas semelhantes;
- extrair a lista de seções;
- sugerir uma ordem de seções.

---

## 2. Gerar copy

A biblioteca pode alimentar a copy com:

- ângulos de promessa;
- objeções recorrentes;
- tom de linguagem;
- padrões de headline;
- padrões de CTA;
- formas de apresentar o mecanismo;
- formas de construir autoridade.

### Saída esperada

Um brief de copy:

```json
{
  "copy_guidance": {
    "angulos_de_promessa": [
      "resultado desejado sem depender de esforço extremo",
      "método simples baseado em mecanismo específico"
    ],
    "objeções_relevantes": [
      "falta de tempo",
      "medo de não conseguir aplicar",
      "tentativas anteriores frustradas"
    ],
    "estilo_de_tom": "claro, emocional e confiante",
    "padrao_de_headline": "dor + promessa + mecanismo",
    "padrao_de_oferta": "oferta principal + bônus + garantia + urgência moderada"
  }
}
```

### Primeiro passo mínimo

- resumir headline;
- promessa;
- objeções;
- tom de linguagem.

---

## 3. Gerar prompt de imagem

A biblioteca pode orientar a geração de imagens com:

- tipos de imagem comuns no nicho;
- metáforas visuais recorrentes;
- cenas que comunicam a promessa;
- objetos recorrentes;
- estilo visual;
- tipo de composição;
- imagens que devem ser evitadas.

### Saída esperada

Um guidance para prompt de imagem:

```json
{
  "image_prompt_guidance": {
    "tipos_de_imagem": [
      "mockup do produto",
      "cena de transformação",
      "especialista em ambiente profissional"
    ],
    "elementos_recorrentes": [
      "laptop",
      "celular",
      "gráficos",
      "ambiente limpo"
    ],
    "estilo_visual": "premium, limpo e contrastado",
    "evitar": [
      "imagens genéricas",
      "cenas sem relação com a promessa",
      "visual excessivamente artificial"
    ]
  }
}
```

### Primeiro passo mínimo

- classificar o tipo de imagem principal de cada página;
- gerar um resumo de orientação visual para imagens.

---

## 4. Gerar preset de design

A biblioteca pode orientar o preset de design com:

- mood visual;
- contraste;
- densidade;
- tipo de superfícies;
- estilo de CTA;
- sensação de autoridade;
- estilo de seções;
- nível de sofisticação.

### Saída esperada

Um guidance para design:

```json
{
  "design_preset_guidance": {
    "mood": "premium e confiável",
    "densidade": "média",
    "contraste": "alto",
    "superficies": "cards suaves com áreas de destaque",
    "hierarquia": "hero forte, seções separadas e CTA muito visível",
    "direcao_visual": "clean com detalhes de energia"
  }
}
```

### Primeiro passo mínimo

Classificar apenas:

- claro ou escuro;
- clean, agressivo, premium, clínico ou vibrante;
- leve, médio ou denso;
- baixo, médio ou alto contraste.

---

# Artefatos intermediários recomendados

Para manter o sistema controlável, cada página de referência deve gerar artefatos intermediários.

## Por página

```json
{
  "snapshot_bruto": {},
  "analise_estrategica": {},
  "analise_estrutural": {},
  "analise_visual": {},
  "insumos_derivados": {
    "wireframe_guidance": {},
    "copy_guidance": {},
    "image_guidance": {},
    "design_guidance": {}
  }
}
```

## Para um novo projeto

Dado um novo projeto com:

- nicho;
- persona;
- dor;
- resultado;
- mecanismo;
- oferta;

o sistema deve:

1. buscar referências parecidas;
2. extrair padrões em comum;
3. montar briefs de geração por etapa.

### Exemplo de saída

```json
{
  "referencias_encontradas": [],
  "brief_wireframe": {},
  "brief_copy": {},
  "brief_imagem": {},
  "brief_design": {}
}
```

---

# Ordem recomendada de implementação

## Sprint 1 — Biblioteca mínima

### Objetivo

Criar a base inicial de páginas de referência.

### Implementar

- cadastrar URLs;
- salvar screenshot;
- salvar HTML;
- salvar título;
- listar páginas.

### Fora do escopo

- análise profunda;
- IA;
- score;
- busca semântica;
- recomendações.

---

## Sprint 2 — Análise mínima estruturada

### Objetivo

Extrair os campos essenciais de cada página.

### Implementar

- headline;
- promessa;
- dor;
- resultado;
- mecanismo;
- oferta;
- lista de seções.

### Fora do escopo

- score de conversão;
- análise visual avançada;
- comparação automática.

---

## Sprint 3 — Guidance para wireframe

### Objetivo

Usar referências para orientar a estrutura de novas páginas.

### Implementar

- buscar páginas semelhantes;
- listar seções comuns;
- sugerir ordem de seções;
- gerar observações para o wireframe.

### Fora do escopo

- gerar landing completa;
- decidir copy;
- decidir design final.

---

## Sprint 4 — Guidance para copy

### Objetivo

Usar referências para orientar a copy.

### Implementar

- resumir tom;
- extrair ângulos;
- listar objeções;
- sugerir padrão de headline;
- sugerir padrão de CTA.

### Fora do escopo

- gerar copy final completa;
- copiar textos de páginas reais;
- criar campanha completa.

---

## Sprint 5 — Guidance para imagem

### Objetivo

Usar referências para orientar prompts de imagem.

### Implementar

- classificar tipos de imagem;
- identificar elementos visuais recorrentes;
- sugerir estilo visual;
- listar o que evitar.

### Fora do escopo

- gerar imagens automaticamente;
- analisar vídeo frame a frame;
- criar banco complexo de assets.

---

## Sprint 6 — Guidance para design

### Objetivo

Usar referências para orientar presets de design.

### Implementar

- classificar mood visual;
- classificar densidade;
- classificar contraste;
- sugerir direção de superfícies;
- sugerir hierarquia visual.

### Fora do escopo

- criar design system completo;
- alterar renderer de landing;
- aplicar CSS automaticamente sem revisão.

---

## Sprint 7 — Uso na geração assistida

### Objetivo

Integrar os guidances ao pipeline de criação de página.

### Implementar

- dado um novo projeto, buscar referências relevantes;
- gerar brief para wireframe;
- gerar brief para copy;
- gerar brief para imagem;
- gerar brief para design.

### Fora do escopo

- publicação automática;
- geração totalmente autônoma sem revisão;
- inferir taxa real de conversão sem dados confiáveis.

---

# Regras para agentes

Ao trabalhar neste módulo, o agente deve seguir estas regras:

1. Este documento descreve uma visão incremental, não uma autorização para implementar tudo de uma vez.
2. Sempre começar pela menor fatia funcional possível.
3. Antes de implementar, declarar o que será feito e o que ficará fora do escopo.
4. Não criar arquitetura complexa sem necessidade.
5. Não copiar páginas existentes.
6. Não inferir sucesso comercial sem dados confiáveis.
7. Transformar referências em padrões, não em duplicações.
8. Priorizar artefatos intermediários verificáveis.
9. Preferir telas, wireframes e dados mockados antes de backend complexo.
10. Implementar uma camada por vez.

---

# Frase-guia

> O sistema não deve tentar gerar diretamente a página final a partir de referências.  
> Ele deve primeiro transformar referências em insumos estruturados para cada etapa do pipeline.
