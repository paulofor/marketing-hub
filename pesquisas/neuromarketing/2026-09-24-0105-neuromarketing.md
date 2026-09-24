# Radar de Neuromarketing e Desejos Digitais — 24/09/2026 01:05

## Resumo executivo

Nesta rodada, três achados merecem atenção para o Marketing Hub.

1. **IA cria mais valor quando executa uma tarefa clara para o usuário do que quando apenas escala conteúdo ou personalização.** Pesquisa global da Havas mostra escores mais altos para educação/suporte e comparação de produtos com IA do que para publicidade, formatos shoppable e conteúdo de creators com IA.
2. **Compradores estão usando IA mesmo sem confiar plenamente nela e querem recomendações auditáveis.** Pesquisa da SmartCustomer mostra alta verificação externa, arrependimento relevante e demanda explícita por justificativa, fontes, reviews e alertas de incerteza.
3. **Eye-tracking pode detectar emoção dentro do mesmo indivíduo muito melhor do que generalizar entre pessoas.** Um novo artigo de 23/09 mostra forte queda de desempenho entre modelos intra-sujeito e inter-sujeito, reforçando que métricas fisiológicas exigem calibração e não devem ser tratadas como leitura universal de emoção.

Dois achados viraram cards: um novo card sobre valor de IA antes de escala e uma nova versão de `ia-gatekeeper-de-compra`. O achado de eye-tracking foi mantido apenas no relatório por ser um estudo pequeno, em VR e ainda distante de uma aplicação comercial validada.

---

## 1. IA de marca: utilidade e credibilidade antes de escala

### Evidência encontrada

A Havas divulgou o estudo **Avoiding the AI Efficiency Trap: Setting a New Standard for How Brands Create Valuable AI Experiences**, baseado em mais de 21 mil avaliações de experiências com IA em sete mercados; cobertura do lançamento também descreve 10.558 respondentes quantitativos via YouGov e 200 entrevistas em profundidade.

Os escores reportados foram mais altos quando a IA tinha uma tarefa funcional clara:

- educação/suporte assistidos por IA: **56,7/100**;
- comparação de produtos: **55,4/100**;
- presença de marca em chat tools: **54,9/100**;
- formatos dinâmicos/shoppable: **49,6/100**;
- publicidade de marca: **48,4/100**;
- conteúdo de creator/influencer com IA: **45,9/100**.

Também foram reportados sinais de resistência: **50%** disseram que IA usada por marcas pode fazer experiências parecerem iguais, **56%** disseram que pode parecer enganosa, apenas **29%** disseram que o uso de IA pelas marcas parece humano, **74%** consideram importante poder sair da personalização por IA e **78%** consideram importante disclosure claro.

### Desejo/comportamento revelado

O usuário parece aceitar melhor IA quando percebe **utilidade concreta, credibilidade e controle**, e não apenas quando a marca consegue produzir mais conteúdo, personalizar mais ou automatizar mais.

### Hipótese interpretativa

A pergunta correta para o Marketing Hub não é “onde podemos colocar IA?”, mas **“qual tarefa humana esta IA melhora?”**. Comparar, explicar, ensinar, resumir e resolver uma fricção parecem funções mais promissoras do que usar IA apenas como novidade ou multiplicador de conteúdo.

### Aplicação possível no Marketing Hub

Criar um `AIValueGate` antes de aprovar uma feature ou experiência com IA. O gate poderia exigir:

- tarefa humana explícita que será melhorada;
- benefício esperado para o usuário;
- evidência utilizada;
- grau de autonomia;
- opção de controle/saída quando relevante;
- risco de parecer manipulativo ou genérico;
- métrica humana e comercial a ser observada.

Isso pode ser aplicado a landing pages, Click-to-WhatsApp, agentes, personalização e creative variants.

### Experimento

Para a mesma oferta:

- **A:** IA apresentada principalmente como novidade/personalização;
- **B:** IA dedicada a uma tarefa concreta, por exemplo comparar duas opções e explicar a recomendação.

Medir conclusão da tarefa, confiança percebida, abandono, CTA, lead, checkout, pagamento reconciliado e pedido de humano.

### Impacto potencial

Alto para desenho de agentes e experiências com IA, porque evita otimizar apenas custo/velocidade de produção e cria um critério reaproveitável de valor ao usuário.

### Limites

A pesquisa é proprietária e os resultados disponíveis são avaliações/associações, não experimentos causais de vendas. Os mercados agregados podem não representar o público brasileiro. Os escores não equivalem a receita, retenção ou conversão do Marketing Hub.

### Fontes

- https://lbbonline.com/news/ai-is-making-brands-more-efficient-but-is-it-creating-value-for-consumers
- https://marketingmind.in/fifty-six-percent-of-consumers-say-brand-ai-can-feel-misleading-havas-report-finds/
- https://havasvaluableai.com/

---

## 2. Recomendação de IA: uso cresce antes da confiança, e o comprador quer verificar

### Evidência encontrada

A SmartCustomer publicou em 21/09 uma pesquisa online com **1.181 consumidores dos EUA**, conduzida em agosto de 2026 com painel balanceado.

Os principais resultados:

- **76%** usaram IA para ajudar em compras no último ano;
- **89%** não confiam completamente nas recomendações de IA;
- **33%** relataram uma compra recomendada por IA da qual depois se arrependeram;
- **75%** verificam recomendações de IA pelo menos algumas vezes antes de agir;
- em caso de conflito, **54%** recorrem a feedback/reviews online, **46%** fazem pesquisa adicional sem IA e **39%** checam diretamente com o varejista;
- para aumentar confiança, **57%** querem explicação clara do porquê da recomendação, **48%** querem lista de fontes, **45%** querem confirmação de uso de reviews e **41%** querem alertas quando a qualidade das fontes for incerta;
- **98%** atribuem pelo menos alguma responsabilidade às empresas de IA pela integridade das fontes e verificação da legitimidade do negócio.

### Desejo/comportamento revelado

O usuário está disposto a usar IA **antes de confiar plenamente nela**, mas trata a recomendação como algo a ser verificado. Isso cria uma necessidade latente de **rastreabilidade e justificativa**, não apenas de respostas mais convincentes.

### Hipótese interpretativa

A recomendação de IA que funciona melhor na jornada comercial pode não ser a mais assertiva, e sim a que permite ao comprador entender **por que**, **com base em quê** e **com qual grau de incerteza** aquela opção foi sugerida.

### Aplicação possível no Marketing Hub

Evoluir o `AgentReadableOfferAudit` e agentes próprios com um `RecommendationEvidenceBundle`:

- motivo curto da recomendação;
- 2–3 evidências relevantes;
- origem/link da evidência quando disponível;
- data ou atualidade da informação;
- reviews legítimos quando aplicáveis;
- alerta explícito de incerteza ou evidência insuficiente.

Isso também aumenta a chance de ofertas do Marketing Hub sobreviverem à verificação feita por ChatGPT, Gemini, buscas, reviews e pelo próprio site.

### Experimento

Comparar:

- **A:** recomendação direta sem justificativa;
- **B:** recomendação + motivo + evidências/fontes + indicação de incerteza quando necessária.

Medir abertura de fonte, continuidade da conversa, correção pelo usuário, abandono, CTA, lead, checkout, pagamento, reembolso e reclamação.

### Impacto potencial

Alto para agentic commerce e para agentes próprios, porque ataca um problema já observado: a adoção da IA está avançando mais rápido do que a confiança.

### Limites

Pesquisa autorrelatada e dos EUA. “Arrependimento” não prova erro factual da IA. Preferência declarada por explicação/fontes não comprova aumento de conversão. Fontes e reviews também podem ser manipulados.

### Fontes

- https://www.smartcustomer.com/resources/ai-shopping-survey-2026
- https://www.retaildive.com/news/shoppers-burned-bad-ai-purchase-recommendations/831013/

---

## 3. Eye-tracking para emoção: forte dentro da pessoa, fraco para generalizar entre pessoas

### Evidência encontrada

Artigo publicado em 23/09/2026 em *Applied Computing and Informatics* apresentou o dataset DET-VRET e avaliou classificação de quatro estados emocionais em VR usando apenas sinais de eye-tracking.

Foram **30 participantes**, de 20 a 29 anos. Os sinais incluíram diâmetro pupilar, posição da pupila e fixação. Com fusão das características e Random Forest, o melhor resultado intra-sujeito chegou a **92,58%**, mas o desempenho inter-sujeito ficou em apenas **54,21% (±3,12%)**. Os próprios autores destacam que essa queda mostra a dificuldade de generalização entre indivíduos e recomendam calibração pessoal, normalização de baseline e adaptação de domínio.

### Desejo/comportamento revelado

Este estudo não revela diretamente um desejo do consumidor. O valor para o radar é técnico: **resposta ocular não é uma assinatura emocional universal simples**.

### Hipótese interpretativa

Se o Marketing Hub algum dia incorporar eye-tracking, webcam gaze ou biometria para avaliar criativos, uma predição genérica de “emoção” pode ser muito menos confiável do que parece. Modelos calibrados por indivíduo podem funcionar melhor, mas isso aumenta custo, fricção e sensibilidade de dados.

### Aplicação possível no Marketing Hub

Não usar score de emoção por eye-tracking como verdade de aprovação de criativo. Se houver protótipo futuro:

- separar atenção visual de emoção inferida;
- exigir validação inter-sujeito;
- reportar incerteza;
- não inferir atributos sensíveis;
- comparar a inferência com autorrelato e eventos reais;
- tratar gaze/biometria como dado sensível operacionalmente, com minimização e consentimento apropriados.

### Experimento/feature

Antes de qualquer feature comercial, executar um benchmark offline com dados consentidos:

1. treinar/avaliar por indivíduo;
2. avaliar em pessoas nunca vistas;
3. comparar contra baseline simples;
4. verificar se a previsão acrescenta algo além de métricas comuns como retenção de vídeo, CTA e resposta declarada.

### Impacto potencial

Médio no curto prazo, alto como guardrail técnico. Evita construir uma feature de “leitura emocional” com precisão aparente obtida apenas por calibração individual ou overfitting.

### Limites

Amostra pequena (**N=30**), faixa etária estreita, ambiente de VR, estímulos específicos e tarefa de laboratório. O estudo não mede anúncios, compra, conversão ou comportamento em Meta/WhatsApp. O desempenho intra-sujeito não deve ser extrapolado para usuários novos.

### Fontes

- https://www.emerald.com/aci/article/doi/10.1108/ACI-01-2026-0037/1398467/DET-VRET-a-dataset-for-emotion-detection-in
- https://doi.org/10.1108/ACI-01-2026-0037

---

## Cards criados/atualizados

### `ia-utilidade-credibilidade-antes-de-escala`

**Novo card.** Merece existir porque transforma um achado global diretamente ligado a experiências de marca com IA em uma regra operacional reutilizável: IA precisa melhorar uma tarefa do usuário e preservar credibilidade/controle; escala de conteúdo não é valor por si só.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-24-ia-utilidade-credibilidade-antes-de-escala.md`

SHA-256:
`70ee27a6fe84c952ecfe6ae982a3b372973da038d989f6e4daa46fafb4dc9f64`

Card:
`pesquisas/neuromarketing/cards/2026-09-24-ia-utilidade-credibilidade-antes-de-escala.json`

### `ia-gatekeeper-de-compra`

**Nova versão do card existente.** A evidência nova acrescenta algo material ao conceito: compradores não apenas usam IA para descobrir ou filtrar ofertas; eles querem **justificativa, fontes, reviews e sinalização de incerteza** para verificar a recomendação.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-24-ia-gatekeeper-explicacao-fontes.md`

SHA-256:
`6b9f84e66343b66164ccb54e309d5396c32d434e571d3f7f8f4541bdf1ca8909`

Card:
`pesquisas/neuromarketing/cards/2026-09-24-ia-gatekeeper-de-compra.json`

O estudo DET-VRET **não virou card** nesta rodada porque ainda é laboratório/VR, com N pequeno e baixa generalização inter-sujeito. Ele foi registrado como sinal técnico e guardrail para qualquer futura feature de emoção/eye-tracking.

## Observação editorial

A coleção `neuromarketing` permanece aceita pelo guia `harness-library-api/docs/guia-uso-api-cards.md`. Nenhum POST manual foi feito para `https://mkthub.api.br/v1/cards`; os JSONs ficaram no repositório para o fluxo automático de `DRAFT`.
