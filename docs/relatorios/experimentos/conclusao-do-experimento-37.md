# Conclusão do experimento 37 — Agenda Cheia Sem Desconto

- **Experimento:** 37
- **Status analisado:** INVALIDATED
- **Plataforma:** Facebook Ads
- **Etapa:** AD
- **Nicho:** Profissionais da Área de Educação Física — Personal Trainer
- **Hipótese:** Agenda Cheia Sem Desconto (8 Semanas)
- **Documento-base:** `docs/relatorios/experimentos/experimento-37-relatorio-completo.md`

## 1. Resumo executivo

O experimento 37 não deve ser lido como uma reprovação definitiva da hipótese estratégica. A leitura mais segura é que **esta materialização específica do experimento foi invalidada**: combinação de público amplo, criativos pouco diferenciados, landing com problemas de qualidade/conversão e possível falha funcional no formulário.

O topo do funil mostrou sinal positivo: houve atenção e clique barato. O problema principal apareceu depois do clique, especialmente na passagem de visualização do formulário para envio.

**Conclusão objetiva:** preservar a hipótese, mas reconstruir o experimento com uma rota comercial mais específica, uma isca mais direta e uma landing/formulário tecnicamente confiáveis antes de investir novamente em mídia.

## 2. Principais evidências

| Ponto analisado | Evidência | Leitura |
| --- | --- | --- |
| Impressões | 2.524 | A campanha teve volume suficiente para leitura inicial. |
| Cliques | 114 | O anúncio gerou curiosidade/interesse. |
| Gasto | R$ 25,11 | CPC baixo para validação inicial. |
| CPC | R$ 0,22 | O topo do funil não foi o principal gargalo. |
| Leads | 0 | A conversão morreu na captura. |
| Visualizações do formulário | 100 | Houve tráfego suficiente até a área de captura. |
| Envios do formulário | 0 | Gargalo crítico entre formulário visto e formulário enviado. |
| Motivo de parada | `FORM_ZERO_CONVERSION_RULE_OF_THREE` | A materialização foi reprovada por 100 acessos sem envio. |

## 3. O que funcionou

### 3.1 Dor comercial relevante

A dor escolhida é forte: personal trainer que depende de indicação, sofre com leads perguntando apenas preço e perde alunos nas primeiras semanas. Essa dor tem ligação direta com vendas, previsibilidade de agenda, percepção de valor e retenção.

### 3.2 Mecanismo plausível

O mecanismo do “Ciclo de Evolução de 8 semanas” é comercialmente interessante porque muda a comparação de “preço por sessão” para “caminho de evolução”. Isso pode aumentar valor percebido, facilitar a conversa no WhatsApp e organizar o início do aluno.

### 3.3 Sinal de interesse no anúncio

O volume de cliques com baixo custo indica que o mercado demonstrou curiosidade inicial. Portanto, a hipótese não deve ser descartada apenas porque não houve leads.

## 4. O que não funcionou

### 4.1 Conversão do formulário

O maior problema foi a passagem de **100 visualizações de formulário para 0 envios**. Esse comportamento sugere uma ou mais causas:

1. formulário com fricção excessiva;
2. promessa pouco clara no momento da captura;
3. baixa confiança na amostra oferecida;
4. problema técnico no botão/envio;
5. desalinhamento entre clique no anúncio e expectativa da landing.

A revisão de qualidade da landing também apontou um problema crítico: o botão do formulário estava como `type="button"`, sem ação visível ou integração clara. Antes de rodar novo tráfego, esse ponto precisa ser tratado como causa-raiz operacional.

### 4.2 Landing abaixo do padrão para tráfego pago

A landing teve score 76 e recomendação de regeneração antes da publicação. Os problemas apontados foram relevantes para conversão:

- desktop empilhado e pouco premium;
- primeira dobra sem nomear claramente personal trainer;
- prova visual pouco legível;
- hierarquia visual fraca;
- formulário sem ação funcional evidente.

Uma landing nesse estado enfraquece a conclusão do teste, porque o experimento pode ter medido uma falha de página, e não uma falta de desejo do mercado.

### 4.3 Público amplo demais

O conjunto de anúncios segmentou Brasil de forma ampla, sem interesses, comportamentos ou recortes de maturidade profissional. Isso pode gerar clique barato, mas atrair pessoas fora do perfil de compra.

### 4.4 Persona insuficiente

A persona aparece como “teste”. Isso enfraquece a geração dos ativos, porque não orienta corretamente:

- nível de maturidade do personal;
- se atua presencialmente, online ou híbrido;
- principal público atendido;
- ticket médio;
- principal canal de venda;
- objeção dominante.

### 4.5 Criativos pouco diferenciados

Os criativos publicados ficaram similares entre si e com textos truncados. Isso reduz o aprendizado do experimento, porque não testa rotas comerciais realmente diferentes.

## 5. Diagnóstico de causa-raiz

A causa-raiz provável não é “personal trainer não quer esse produto”.

A causa-raiz mais provável é:

> O experimento levou tráfego interessado para uma experiência de captura fraca/insegura, com público amplo, persona genérica, criativos pouco diferenciados e landing/formulário abaixo do nível necessário para converter tráfego frio.

Isso significa que o próximo passo não deve ser abandonar o nicho ou a hipótese. O próximo passo deve ser **reduzir fricção, aumentar especificidade e garantir funcionamento técnico da captura**.

## 6. Decisão recomendada

**Decisão:** não escalar e não repetir igual.

**Ação recomendada:** criar um novo experimento derivado, preservando a hipótese estratégica, mas mudando a materialização:

- nova dor de entrada;
- nova isca;
- nova landing;
- nova segmentação;
- nova métrica primária;
- validação técnica obrigatória do formulário antes de publicar.

## 7. Plano de melhoria

### Fase 1 — Correção operacional obrigatória

Antes de qualquer nova campanha:

1. validar que o botão do formulário envia corretamente;
2. confirmar criação do lead no backend;
3. confirmar registro do evento de envio no funil;
4. confirmar disparo da amostra/e-mail, se aplicável;
5. confirmar que a tela do experimento mostra o envio corretamente;
6. não publicar landing com recomendação `REGENERATE_BEFORE_PUBLICATION`.

**Critério de pronto:** formulário testado ponta a ponta com pelo menos um envio real de teste registrado no funil.

### Fase 2 — Reposicionamento da isca

Trocar a isca genérica “prévia em PDF + mini-kit” por uma promessa mais imediata e próxima da dor do WhatsApp.

Sugestões de isca:

1. **Roteiro anti-preço de WhatsApp**
   - Promessa: “Receba uma resposta pronta para quando o lead perguntar ‘quanto custa?’ e conduza para o próximo passo sem dar desconto.”

2. **Sequência D0–D7 para novo aluno**
   - Promessa: “Receba uma sequência pronta de boas-vindas e check-ins para reduzir sumiço nas primeiras semanas.”

3. **Mapa de evolução de 8 semanas**
   - Promessa: “Mostre ao aluno um caminho claro de evolução antes dele comparar você por preço.”

**Recomendação inicial:** começar pelo **Roteiro anti-preço de WhatsApp**, porque é a dor mais próxima da venda imediata.

### Fase 3 — Nova landing curta e específica

A landing precisa ser reconstruída com foco em conversão, não em explicar tudo.

Estrutura recomendada:

1. **Hero direto**
   - “Personal trainer: pare de perder leads quando eles perguntam ‘quanto custa?’”

2. **Prova imediata**
   - mostrar um trecho real do roteiro anti-preço;
   - mostrar antes/depois da resposta no WhatsApp;
   - deixar claro que é prático e copiável.

3. **Mecanismo simples**
   - “Você não vende sessão. Você conduz o lead para entender o objetivo, o caminho e o próximo passo.”

4. **Oferta de entrada**
   - “Receba o roteiro + modelo de follow-up inicial.”

5. **Formulário mínimo**
   - nome;
   - e-mail;
   - WhatsApp opcional.

6. **CTA orientado ao benefício**
   - substituir “Preencher briefing” por “Receber meu roteiro anti-preço”.

### Fase 4 — Segmentação mais qualificada

Evitar rodar apenas Brasil amplo.

Segmentações sugeridas para teste:

1. personal trainer autônomo;
2. professor de educação física;
3. consultoria fitness online;
4. musculação/academia/treinamento funcional;
5. públicos por comportamento empreendedor, quando disponível;
6. separar, em campanhas futuras, personal presencial e consultoria online.

### Fase 5 — Criativos realmente diferentes

Criar ao menos 3 rotas de criativo:

#### Criativo A — Dor preço

- Headline: “Quando o lead pergunta preço, ele some?”
- Promessa: roteiro pronto para responder sem desconto.
- CTA: receber roteiro.

#### Criativo B — Dor agenda

- Headline: “Sua agenda depende de indicação?”
- Promessa: transformar o acompanhamento em um caminho claro de 8 semanas.
- CTA: ver modelo.

#### Criativo C — Dor retenção

- Headline: “Aluno novo some na segunda semana?”
- Promessa: sequência de boas-vindas e check-ins D0–D7.
- CTA: receber sequência.

Cada criativo deve apontar para uma landing coerente com sua dor principal. Não misturar todas as dores na primeira dobra.

### Fase 6 — Métrica e regra de parada

Para o próximo experimento, a métrica primária deve ser:

- **taxa de envio do formulário por visualização da landing/formulário**.

Métricas secundárias:

- CTR do anúncio;
- custo por visualização do formulário;
- custo por lead;
- abertura do e-mail da amostra;
- clique para checkout, se houver oferta paga imediata.

Regra de parada recomendada:

- parar após 100 visualizações reais do formulário sem envio, **somente se o formulário já tiver sido testado ponta a ponta**;
- se houver cliques baratos e nenhuma submissão, investigar formulário/landing antes de invalidar hipótese.

## 8. Novo experimento recomendado

### Nome sugerido

**Experimento 37B — Roteiro Anti-Preço para Personal Trainer**

### Hipótese derivada

Personal trainers que perdem leads no WhatsApp quando a conversa vira preço têm interesse em receber um roteiro prático para reposicionar a conversa e conduzir o lead para um próximo passo sem dar desconto.

### Público inicial

Personal trainer, professor de educação física e profissionais de consultoria fitness que vendem acompanhamento pelo WhatsApp/Instagram.

### Oferta de entrada

Roteiro anti-preço + modelo curto de follow-up.

### Promessa da landing

“Receba um roteiro pronto para responder ‘quanto custa?’ e levar o lead para o próximo passo sem entrar em guerra de preço.”

### CTA

“Receber meu roteiro anti-preço”

### Resultado esperado

Validar se a dor de preço no WhatsApp converte melhor do que a promessa mais ampla de “Ciclo de Evolução de 8 semanas”.

## 9. Checklist antes de publicar o 37B

- [ ] Persona preenchida corretamente, não como “teste”.
- [ ] Público com segmentação mínima de personal/educação física/fitness.
- [ ] Landing aprovada sem recomendação de regeneração.
- [ ] Formulário com submit funcional validado.
- [ ] Evento de envio registrado no funil.
- [ ] Criativos sem truncamento crítico de promessa/headline.
- [ ] Cada criativo com uma dor principal clara.
- [ ] CTA da landing orientado ao benefício.
- [ ] Prova visual legível antes do formulário.
- [ ] Métrica primária definida antes da publicação.


## 10. Conclusão final

O experimento 37 mostrou que existe atenção inicial para a dor, mas não provou rejeição do mercado ao produto. O aprendizado principal é que a próxima rodada precisa ser mais simples e mais próxima da venda real do personal: **WhatsApp, objeção de preço e próximo passo**.

A melhor melhoria é sair de uma oferta ampla (“prévia de ciclo de 8 semanas”) para uma entrada mais imediata:

> “Personal trainer, receba um roteiro pronto para responder ‘quanto custa?’ sem dar desconto.”

Essa rota reduz esforço mental, aumenta clareza do benefício e conversa diretamente com o momento em que o personal sente a dor e perde dinheiro.

## 11. Melhorias sistêmicas para o Marketing Hub

A leitura do experimento 37 expõe melhorias que devem virar capacidade geral do sistema, não apenas correção de um único teste. O objetivo é proteger a verba de mídia, aumentar a qualidade do aprendizado e acelerar a criação de ofertas que realmente vendem.

### 11.1 Bloqueio de publicação por qualidade funcional da captura

O sistema não deve permitir publicação paga quando a captura ainda não foi validada ponta a ponta.

**Melhoria geral recomendada:** criar um gate obrigatório de pré-publicação que só libera mídia quando houver:

1. envio real de teste do formulário;
2. criação do lead no backend;
3. registro do evento de envio no funil;
4. confirmação de entrega da amostra, e-mail ou próximo passo;
5. ausência de recomendação `REGENERATE_BEFORE_PUBLICATION` na revisão de landing.

**Impacto esperado:** evita que o sistema invalide hipóteses promissoras por falha técnica de landing/formulário.

### 11.2 Diagnóstico separado entre falha comercial e falha técnica

O funil atual consegue indicar zero envio após volume mínimo, mas a decisão operacional precisa separar melhor duas situações diferentes:

- **falha comercial:** a promessa, público ou oferta não gerou desejo suficiente;
- **falha técnica:** o usuário tentou ou chegou ao ponto de conversão, mas a captura não funcionou ou gerou fricção indevida.

**Melhoria geral recomendada:** todo relatório de conclusão deve classificar a invalidação em uma dessas categorias antes de recomendar abandono, repetição ou derivação da hipótese.

**Impacto esperado:** melhora a qualidade da decisão e reduz descarte indevido de nichos, dores e mecanismos.

### 11.3 Persona mínima obrigatória antes de gerar ativos

Uma persona preenchida como “teste” enfraquece todos os ativos gerados: anúncio, landing, isca, promessa, CTA e segmentação.

**Melhoria geral recomendada:** bloquear a geração/publicação quando a persona não tiver pelo menos:

1. papel profissional claro;
2. maturidade do negócio;
3. canal principal de venda;
4. objeção dominante;
5. situação de dor concreta;
6. resultado financeiro desejado.

**Impacto esperado:** melhora a especificidade da oferta e aumenta a chance de o criativo falar com uma dor real de compra.

### 11.4 Isca alinhada ao momento de dor mais próximo da venda

O experimento indicou que uma oferta ampla pode gerar curiosidade, mas não necessariamente ação. Para o sistema como um todo, a isca deve nascer do momento em que o cliente sente a dor com mais urgência.

**Melhoria geral recomendada:** adicionar uma validação de “momento de dor” antes de publicar a landing:

- onde a dor acontece?
- o que a pessoa está tentando fazer nesse momento?
- qual frase ela diria se estivesse pedindo ajuda?
- qual ativo resolve uma parte do problema em menos de 5 minutos?

**Impacto esperado:** mais leads qualificados, menos curiosos e maior ligação entre clique, landing e formulário.

### 11.5 Criativos com rotas comerciais realmente diferentes

Criativos muito parecidos reduzem o aprendizado. O sistema precisa testar ângulos diferentes, não apenas variações superficiais de texto.

**Melhoria geral recomendada:** exigir pelo menos três rotas comerciais antes de publicar campanhas exploratórias:

1. rota da dor imediata;
2. rota do resultado desejado;
3. rota do mecanismo/prova.

Cada rota deve ter headline, promessa, imagem e CTA coerentes com uma única dor principal.

**Impacto esperado:** o experimento passa a descobrir qual argumento vende melhor, e não apenas se uma peça isolada performa.

### 11.6 Segmentação mínima por maturidade e contexto

Campanhas amplas podem ser úteis em escala, mas são ruins para aprender quando a persona ainda está pouco definida.

**Melhoria geral recomendada:** exigir uma segmentação mínima de contexto para testes iniciais, com separação por maturidade quando fizer sentido:

- iniciante versus profissional estabelecido;
- presencial versus online/híbrido;
- autônomo versus equipe/empresa;
- venda por WhatsApp versus checkout direto.

**Impacto esperado:** leitura mais limpa dos resultados e menor risco de clique barato sem intenção real.

### 11.7 Relatório de conclusão como entrada automática do próximo experimento

O relatório não deve ficar apenas como documento pós-morte. Ele deve alimentar automaticamente o próximo ciclo.

**Melhoria geral recomendada:** transformar conclusões recorrentes em campos reaproveitáveis no próximo experimento derivado:

1. causa-raiz provável;
2. hipótese preservada ou descartada;
3. nova dor de entrada;
4. nova isca;
5. bloqueios técnicos obrigatórios;
6. métrica primária;
7. regra de parada ajustada.

**Impacto esperado:** reduz repetição de erros e acelera a evolução de Dor → Resultado → Mecanismo → Prova → Oferta.

### 11.8 Painel geral de gargalos por etapa

O aprendizado do experimento 37 é valioso porque mostrou que o topo do funil não era o gargalo principal. Esse tipo de leitura deve ser visível em nível de portfólio.

**Melhoria geral recomendada:** criar uma visão consolidada que mostre, por nicho e por hipótese:

- gargalo mais frequente;
- taxa de passagem por etapa;
- custo por etapa;
- motivos de invalidação;
- suspeitas técnicas versus falhas comerciais;
- hipóteses que merecem derivação em vez de descarte.

**Impacto esperado:** o Marketing Hub deixa de olhar experimentos isolados e passa a aprender como fábrica de vendas.

### 11.9 Prevenção de recorrência

A causa-raiz sistêmica é a ausência de uma trava operacional que impeça tráfego pago antes de comprovar que a promessa, a página e a captura estão tecnicamente prontas e comercialmente específicas.

**Correção preventiva recomendada:** tratar o próximo desenvolvimento como melhoria de plataforma:

1. gate funcional de formulário;
2. gate de qualidade da landing;
3. gate de persona mínima;
4. gate de coerência anúncio → landing → CTA;
5. relatório de conclusão categorizando falha técnica ou comercial;
6. reaproveitamento obrigatório do aprendizado no experimento derivado.

Essa melhoria protege o objetivo principal do sistema: gerar vendas com menos desperdício de mídia e com aprendizado acumulado a cada ciclo.
