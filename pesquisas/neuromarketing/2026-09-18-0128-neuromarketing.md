# Radar de Neuromarketing e Desejos Digitais — 2026-09-18 01:28

**Data/hora:** 2026-09-18 01:28 — America/Sao_Paulo

## Resumo executivo

A rodada trouxe três sinais úteis para o Marketing Hub:

1. **O problema não é simplesmente “usar IA no criativo”; é parecer sintético, genérico ou pouco fiel ao produto.** Grandes pesquisas de consumo mostram perda de confiança quando imagens/anúncios parecem feitos por IA, enquanto outro teste mostra que consumidores frequentemente nem conseguem identificar corretamente a origem do anúncio e priorizam relevância, informação e utilidade.
2. **Repetição percebida está virando um problema de atenção.** Consumidores relatam pular/bloquear mais anúncios e encontrar o mesmo criativo em várias plataformas; isso favorece diversidade funcional entre creative variants, e não apenas pequenas alterações cosméticas.
3. **Na compra mediada por IA, usuários querem que a recomendação seja sustentada por prova humana real.** Pesquisa Bazaarvoice/Bluefish indica que reviews e fotos reais são o principal elemento citado para confiar em recomendações de IA.

Nenhum desses achados prova aumento de venda no Marketing Hub. São sinais externos para formular testes controlados e guardrails.

---

## 1. Criativos com IA: utilidade e autenticidade percebida importam mais que esconder a origem

### Evidência encontrada

O **Future Shopper 2026**, da VML, baseado em 28.000 consumidores de 17 países, informa que **49% perdem confiança quando imagens de produto parecem geradas por IA** e **48% pulam conteúdo que suspeitam ser sintético**. O mesmo estudo relata que 58% conferem recomendações de IA em outras fontes antes de comprar.

A terceira onda do **MORE Intelligence Consumer Pulse**, da Net Conversion, pesquisou 1.500 adultos dos EUA com compra não essencial recente. **54% disseram confiar menos em uma marca quando seus anúncios parecem produzidos por IA**. Ao mesmo tempo, 54% usam IA pelo menos algumas vezes para pesquisar compras, e 45% dos usuários de IA dizem que uma recomendação de IA aumenta sua confiança em uma marca.

O **COOL AI Challenge**, seguido por pesquisa com mais de 500 consumidores, encontrou que apenas **17% identificaram corretamente o anúncio gerado por IA**. Na mesma pesquisa, **73% disseram que relevância importa mais que a autoria humana ou por IA**, 80% querem publicidade mais informativa e 40% disseram que saber que um anúncio foi feito com IA reduziria sua confiança na marca.

### Comportamento/desejo revelado

O consumidor não parece rejeitar IA de forma uniforme. A tensão é mais específica:

- aceita IA quando ela reduz esforço ou ajuda a decidir;
- rejeita com mais facilidade publicidade que parece artificial, genérica ou pouco útil;
- nem sempre consegue identificar a origem sintética com precisão;
- valoriza relevância e informação concreta.

### Hipótese interpretativa

A variável mais útil para o Marketing Hub provavelmente não é `AI=true/false`, mas algo como **autenticidade percebida + fidelidade ao produto + utilidade informacional**.

Isso é hipótese. Os levantamentos são majoritariamente auto-relatados e não demonstram causalidade comercial.

### Aplicação possível no Marketing Hub

Nos `creative_variants`, usar IA para escala, mas preservar:

- produto/entregável real;
- evidência verificável;
- preço e condições verdadeiros;
- linguagem menos genérica;
- composição e cenas não excessivamente padronizadas.

Uma revisão automática poderia sinalizar criativos em que o produto aparece de forma excessivamente idealizada ou em que toda a peça poderia ser aplicada indistintamente a dezenas de ofertas.

### Experimento

**A:** criativo sintético genérico e altamente polido.  
**B:** mesma oferta e promessa, mas com âncora real/verificável do produto, demonstração ou tela real e informação concreta.

Medir CTR, CTA, lead, checkout, pagamento reconciliado, comentários de desconfiança, ocultação e reclamações.

### Impacto potencial

Alto para Meta Ads, vídeos de venda e páginas com material gerado por IA, porque afeta confiança antes mesmo do clique.

### Limites

Não há prova causal de que conteúdo gerado por IA reduz conversão. A aparência dos modelos sintéticos muda rapidamente e os percentuais vêm de mercados/amostras diferentes.

### Fontes

- https://www.vml.com/insight/the-future-shopper-2026
- https://www.businesswire.com/news/home/20260916786372/en/Your-Best-Customers-Are-Using-AI-to-Find-You.-Your-AI-Made-Ads-Are-Losing-Them.
- https://cool.co/press/the-cool-companys-ai-challenge-finds-that-83-of-consumers-fail-to-identify-ai-generated-ads-as-ai-reshapes-advertising/

---

## 2. Fadiga criativa: variar detalhes pode não ser suficiente

### Evidência encontrada

Na pesquisa da Net Conversion:

- **52%** disseram estar ignorando, pulando ou bloqueando mais anúncios digitais do que um ano antes;
- **48%** encontram frequentemente o mesmo anúncio em várias plataformas;
- **34%** encontram com frequência anúncios pouco relacionados a sua localização, idade ou interesses.

Na pesquisa da COOL Company:

- **72%** querem publicidade menos repetitiva;
- **80%** querem publicidade mais informativa;
- **73%** dizem que relevância importa mais que a origem humana ou sintética.

### Comportamento/desejo revelado

O usuário quer novidade **percebida**, não apenas uma alteração técnica no arquivo. Trocar cor, enquadramento ou frase mantendo essencialmente a mesma experiência pode continuar sendo percebido como “o mesmo anúncio”.

### Hipótese interpretativa

A saturação deve ser acompanhada pela **distância funcional entre criativos**, não só pela quantidade de arquivos produzidos.

### Aplicação possível no Marketing Hub

Organizar `creative_variants` por função:

- demonstração;
- prova;
- transformação/resultado;
- situação emocional;
- objeção;
- comparação;
- oferta;
- descoberta/curiosidade.

Uma feature futura poderia calcular um `creativeDiversityScore` com base em semelhança de roteiro, cena, benefício, prova e ritmo. O score não deveria bloquear sozinho; serviria como sinal para revisão.

### Experimento

**A:** rotação de variantes superficiais do mesmo conceito.  
**B:** rotação de conceitos funcionalmente distintos.

Manter oferta, público, janela e orçamento comparáveis. Acompanhar frequência, CTR por exposição, custo por CTA/lead, queda de performance ao longo do tempo, ocultações/comentários negativos e pagamentos reconciliados.

### Impacto potencial

Alto para campanhas Meta que usam geração em escala. A IA reduz custo de produzir peças, mas também facilita produzir dezenas de peças percebidas como iguais.

### Limites

As fontes não estabelecem uma frequência universal de fadiga nem medem Meta Ads diretamente. Não tratar 2, 3 ou qualquer outro número de impressões como limite causal.

### Fontes

- https://www.businesswire.com/news/home/20260916786372/en/Your-Best-Customers-Are-Using-AI-to-Find-You.-Your-AI-Made-Ads-Are-Losing-Them.
- https://cool.co/press/the-cool-companys-ai-challenge-finds-that-83-of-consumers-fail-to-identify-ai-generated-ads-as-ai-reshapes-advertising/

---

## 3. IA de compra: recomendação confiável precisa mostrar “recibos”

### Evidência encontrada

A Bazaarvoice e a Bluefish divulgaram em 17 de setembro pesquisa com mais de 1.700 usuários ativos de IA em EUA, APAC e EMEA, complementada por dados anteriores de mais de 1.300 adultos nos EUA.

Entre os respondentes:

- **57%** apontaram reviews autênticos e estrelas como o elemento que mais aumenta confiança em uma recomendação de produto feita por IA;
- **57%** consideram “muito importante” saber que a recomendação de IA usou reviews/fotos reais de clientes;
- outros **33%** consideram isso “um pouco importante”;
- 62% disseram confiar mais em recomendações de IA do que seis meses antes.

### Comportamento/desejo revelado

O usuário não quer apenas uma resposta de IA; ele quer saber **em que evidência humana a recomendação se apoia**.

### Hipótese interpretativa

No comércio mediado por agentes, “fonte da recomendação” pode se tornar parte da experiência de confiança, assim como preço, prazo e política de devolução.

### Aplicação possível no Marketing Hub

Para ofertas e produtos que possuam prova real:

- expor reviews/depoimentos verificáveis em formato legível por humanos e máquinas;
- permitir que agentes indiquem a origem da prova;
- diferenciar claramente review real, exemplo produzido pela marca e hipótese gerada por IA.

Isso complementa o `AgentReadableOfferAudit` já sugerido nas rodadas anteriores.

### Experimento

Em uma página/WhatsApp que possua reviews reais:

**A:** recomendação do agente sem referência à prova.  
**B:** recomendação + “por que estou sugerindo” + 1–2 evidências reais verificáveis.

Medir continuidade da conversa, clique na prova, CTA, lead, pagamento e perguntas de confiança.

### Impacto potencial

Moderado a alto para ofertas que acumularem prova social real. É especialmente relevante para o futuro funil `agente → shortlist → validação → compra`.

### Limites

A Bazaarvoice vende infraestrutura de UGC e a Bluefish trabalha com visibilidade em IA, portanto existe interesse comercial. Os dados são de pesquisa de percepção e não provam que citar reviews eleva vendas.

### Fonte

- https://www.bazaarvoice.com/press/bazaarvoice-and-bluefish-partnership/

---

## Cards desta rodada

### 1. `autenticidade-criativo-ia` — nova versão

Atualizado porque a evidência nova acrescenta **confiança e comportamento declarados em contexto publicitário**, complementando a evidência anterior sobre discernimento de conteúdo sintético. A versão atual enfatiza que o objetivo não é “esconder IA”, mas preservar fidelidade, utilidade, prova e autenticidade percebida.

Fonte revisada:

`pesquisas/neuromarketing/cards/fontes/2026-09-18-autenticidade-criativos-ia.md`

SHA-256:

`0e6d01a2359b8418ccb04a9b0ab7d4d41e7b6161e3ea129fd4e6dc24932c67e3`

Card:

`pesquisas/neuromarketing/cards/2026-09-18-autenticidade-criativo-ia.json`

### 2. `rotacao-criativa-repeticao-percebida` — novo card

Criado porque dois levantamentos recentes convergem para um problema diretamente acionável no Marketing Hub: **escala de geração não equivale a diversidade percebida**. O card orienta Meta Ads e `creative_variants` a testar diversidade funcional em vez de apenas multiplicar versões cosméticas.

Fonte revisada:

`pesquisas/neuromarketing/cards/fontes/2026-09-18-repeticao-fadiga-criativa.md`

SHA-256:

`31b3f81120e76da59ed93d1d9c66f6a1a3d692ee8ba46e6c8354dd455611d791`

Card:

`pesquisas/neuromarketing/cards/2026-09-18-rotacao-criativa-repeticao-percebida.json`

### Sem card novo para reviews em recomendações de IA

O achado Bazaarvoice/Bluefish é útil, mas sobrepõe a regra já coberta por `ia-gatekeeper-de-compra`: preparar oferta e evidência para jornadas mediadas por agentes. Registrei a evidência nesta rodada sem criar outra chave para não aumentar o catálogo com ideias redundantes.

---

## Observação editorial

A coleção `neuromarketing` permanece aceita pelo guia atual `harness-library-api/docs/guia-uso-api-cards.md`. Os arquivos JSON foram versionados no repositório para o fluxo automático de `DRAFT`. Não foi feito POST manual para `https://mkthub.api.br/v1/cards`.
