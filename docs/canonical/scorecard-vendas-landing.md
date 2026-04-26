# Scorecard de Vendas — Landing Page (v2)

## Objetivo
Padronizar a avaliação comercial de cada landing antes de publicação, mantendo coerência com o eixo canônico **Dor → Resultado → Mecanismo → Prova → Oferta** e garantindo que a arquitetura robusta sirva a vendas reais (não só conformidade técnica).

---

## Como pontuar

- Escala por dimensão: **0 a 5**
- Peso por dimensão: conforme tabela abaixo
- Fórmula:

```text
score_final (0-100) = Σ( nota_dimensão/5 * peso_dimensão )
```

---

## Dimensões, pesos e critérios (gate comercial)

| Dimensão | Peso | O que mede | Critério de nota 5 |
|---|---:|---|---|
| 1) Clareza da Promessa | 25 | Clareza da dor, do resultado e do próximo passo | Headline + promessa + CTA estão cristalinos para o ICP, sem ambiguidade e com alta continuidade de mensagem. |
| 2) Força da Prova | 25 | Evidência que sustenta a promessa | Prova concreta, específica e contextualizada (não genérica), com risco percebido baixo de “promessa vazia”. |
| 3) Fricção do Formulário | 15 | Esforço para conversão | Formulário com mínimo atrito, microcopy anti-ansiedade e clareza de próximos passos. |
| 4) Urgência e Oferta | 20 | Intensidade de decisão | Oferta com escassez/urgência legítima, ancoragem de valor e risco reverso explícito. |
| 5) Objeções Cobertas | 15 | Capacidade de remover travas de compra | FAQ e seções de prova neutralizam objeções principais do nicho com respostas específicas. |

---

## Rubrica prática (0 a 5) por dimensão

- **0**: ausente ou contraditório
- **1**: muito fraco; confuso; sem evidência
- **2**: básico; parcialmente claro; ainda gera objeções críticas
- **3**: aceitável; converte em cenário favorável
- **4**: forte; consistente; reduz objeções principais
- **5**: excelente; específico; convincente para tráfego frio

---

## Gate automático pré-publicação

### Gate mínimo global
- **Aprova para publicação controlada**: `score_final >= 75`
- **Aprova para escala**: `score_final >= 85`

### Gates mínimos por dimensão (hard-fail)
Se qualquer condição abaixo falhar, a landing não escala mesmo com score global alto:

- Clareza da Promessa **>= 3.5**
- Força da Prova **>= 3.0**
- Fricção do Formulário **>= 3.0**
- Urgência e Oferta **>= 3.0**
- Objeções Cobertas **>= 3.0**

### Regra de regressão para ajuste automático
- Se `score_final < 75` **ou** qualquer hard-fail ocorrer, a variante deve voltar para ajuste de `landingPageCopy`/`landingPageWireframe`/`landingPageDesignPreset` antes de nova tentativa de publicação.

---

## Critérios mínimos de oferta (mandatórios)

Toda landing aprovada deve explicitar:

1. **Risco reverso**: garantia, teste, política clara ou mecanismo equivalente.
2. **Escassez/urgência legítima**: justificativa real, sem manipulação artificial.
3. **Ancoragem de valor**: contraste explícito entre custo e valor percebido.
4. **Prova específica**: dados/casos contextuais, evitando afirmações genéricas.

---

## Biblioteca canônica de mecanismos de prova por nicho

As variantes devem priorizar combinações de prova de alto impacto:

- **Antes/depois** com contexto de aplicação.
- **Benchmark comparativo** com alternativa comum do mercado.
- **Micro-caso com número** (resultado mensurável + janela temporal).
- **Evidência técnica simplificada** (como o mecanismo funciona em linguagem acessível).

> Regra prática: pelo menos **2 tipos de prova** por landing, sendo ao menos 1 prova numérica ou comparativa.

---

## Checklist operacional por dimensão

### 1) Clareza da Promessa (25)
- A dor principal aparece em até 5 segundos de leitura.
- O resultado prometido é específico (o quê, para quem e em qual contexto).
- CTA principal está semanticamente alinhado à promessa do anúncio.

### 2) Força da Prova (25)
- Existe ao menos 1 prova principal com contexto (quem, quando, cenário).
- Há mecanismo explicando **como** o resultado ocorre.
- Limitações/condições estão explícitas para reduzir risco jurídico e de confiança.

### 3) Fricção do Formulário (15)
- Campos estritamente necessários para a etapa do funil.
- Microcopy reduz ansiedade (privacidade, tempo de resposta, próximos passos).
- CTA é claro, visível e consistente em pontos críticos da página.

### 4) Urgência e Oferta (20)
- Entregáveis e formato estão claros.
- Risco reverso está explícito.
- Escassez/urgência é legítima e verificável.
- Ancoragem de valor foi comunicada.

### 5) Objeções Cobertas (15)
- FAQ cobre objeções principais do público-alvo.
- Respostas usam prova e não só opinião.
- Objeções de preço, tempo, confiança e aplicabilidade possuem tratamento claro.

---

## Loop de aprendizado pós-publicação (obrigatório)

Após publicar, reavaliar semanalmente com dados reais para retroalimentar copy/wireframe/preset:

- CTR anúncio → landing
- Scroll por seção
- Submit rate da landing
- Abandono por campo de formulário
- CPL / CPA

Regra: variações com piora estatisticamente relevante devem regressar para ajuste automático e nova rodada de scorecard.

---

## Governança

- Este scorecard é canônico para avaliação comercial de landing pages no pipeline de experimento.
- O scorecard valida não apenas “está correto”, mas “está vendendo”.
- Alterações de pesos, gates ou rubrica exigem atualização versionada deste documento.
