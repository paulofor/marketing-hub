# Plano de melhoria do Marketing Hub a partir do experimento 37

- **Experimento analisado:** 37 — Agenda Cheia Sem Desconto
- **Relatórios de origem:**
  - `docs/relatorios/experimentos/experimento-37-relatorio-completo.md`
  - `docs/relatorios/experimentos/conclusao-do-experimento-37.md`
- **Objetivo da análise:** transformar o aprendizado do experimento 37 em melhorias de produto, marketing e operação para aumentar a chance de gerar vendas com menos desperdício de mídia.

## 1. Leitura executiva

O experimento 37 mostrou um padrão importante: o sistema conseguiu atrair atenção, mas não conseguiu converter essa atenção em lead.

A conclusão estratégica é que o Marketing Hub não deve tratar esse caso como rejeição definitiva da hipótese ou do nicho. O aprendizado mais valioso é operacional e comercial: **antes de gastar mídia, o sistema precisa garantir que anúncio, landing, formulário, isca e persona estejam suficientemente específicos, coerentes e funcionais**.

Em termos de negócio, o problema não foi apenas “zero lead”. O problema foi que o Marketing Hub quase invalidou uma hipótese promissora por uma combinação de:

1. captura sem validação ponta a ponta;
2. persona genérica;
3. público amplo demais para aprendizado inicial;
4. landing abaixo do padrão ideal para tráfego pago;
5. isca ampla demais para uma dor que acontece no WhatsApp;
6. criativos pouco diferentes entre si;
7. relatório de decisão ainda pouco separado entre falha técnica e falha comercial.

## 2. O que o experimento 37 ensinou sobre marketing digital

### 2.1 Clique barato não valida oferta

O experimento teve impressão e clique suficientes para indicar curiosidade, mas a ausência de envio mostrou que o clique não era o gargalo principal. Para tráfego frio, o Marketing Hub precisa dar mais peso à taxa de passagem pós-clique do que ao CPC isolado.

**Melhoria de produto:** o painel de experimento deve destacar automaticamente o principal gargalo do funil e alertar quando o topo está saudável, mas a captura está quebrada ou fraca.

### 2.2 Uma promessa ampla gera curiosidade, mas uma isca específica gera ação

A promessa “Ciclo de Evolução de 8 semanas” é estratégica, mas pode estar distante demais do momento de dor mais quente do personal trainer. O momento mais próximo da venda é quando o lead pergunta “quanto custa?” e o personal perde controle da conversa.

**Melhoria de marketing:** o gerador de ofertas deve priorizar uma isca de entrada que resolva uma microdor imediata em poucos minutos, antes de apresentar o mecanismo completo.

### 2.3 Persona fraca enfraquece todos os ativos

Uma persona preenchida como “teste” não dá insumo suficiente para boas decisões de promessa, criativo, segmentação, CTA e prova visual.

**Melhoria de produto:** bloquear geração ou publicação quando a persona não tiver o mínimo necessário para orientar venda: maturidade, canal de venda, objeção dominante, contexto de atuação e resultado financeiro desejado.

### 2.4 Landing ruim contamina a leitura da hipótese

Se a landing tem problema de hierarquia, prova, legibilidade ou formulário, o experimento mede a falha da página — não necessariamente a falta de desejo do mercado.

**Melhoria de operação:** criar um gate de pré-publicação com validação comercial e funcional da landing antes de liberar mídia.

### 2.5 Criativos parecidos reduzem aprendizado

Quando os criativos testam a mesma promessa com variações pequenas, o sistema aprende pouco. O correto é testar rotas comerciais diferentes: dor, resultado e mecanismo/prova.

**Melhoria de marketing:** exigir pelo menos três rotas criativas realmente distintas em experimentos exploratórios.

## 3. Melhorias recomendadas para o Marketing Hub

### 3.1 Gate obrigatório antes de publicar mídia

**Problema observado:** houve tráfego pago para uma experiência de captura que não estava comprovadamente pronta para converter.

**Melhoria:** criar um checklist bloqueante de publicação com cinco confirmações:

1. formulário envia corretamente;
2. lead é criado no backend;
3. evento de envio aparece no funil;
4. amostra, e-mail ou próximo passo é entregue;
5. landing não possui recomendação de regeneração antes da publicação.

**Impacto esperado:** menos dinheiro gasto em páginas que não conseguem capturar lead e menos invalidação falsa de hipóteses boas.

### 3.2 Classificação da invalidação: técnica ou comercial

**Problema observado:** zero lead pode significar falta de desejo, mas também pode significar falha funcional, excesso de fricção ou baixa confiança na captura.

**Melhoria:** toda conclusão de experimento deve classificar a causa principal em uma das categorias:

- falha técnica;
- falha de landing;
- falha de promessa/oferta;
- falha de público;
- falha de criativo;
- falha de preço/checkout;
- hipótese realmente fraca.

**Impacto esperado:** decisões melhores: preservar, derivar, repetir corrigido ou descartar.

### 3.3 Campo obrigatório de “momento de dor”

**Problema observado:** a hipótese era boa, mas a isca ficou ampla. O sistema precisa conectar a oferta ao momento em que o cliente sente a dor com mais urgência.

**Melhoria:** antes de gerar landing e anúncio, obrigar o preenchimento ou geração validada de:

- onde a dor acontece;
- qual frase o prospect fala nesse momento;
- qual microresultado ele quer agora;
- qual ativo simples resolve uma parte do problema em até 5 minutos;
- qual CTA traduz esse benefício imediato.

**Impacto esperado:** ofertas de entrada mais claras, mais conversão no formulário e leads mais qualificados.

### 3.4 Persona mínima vendável

**Problema observado:** a persona genérica contaminou segmentação e copy.

**Melhoria:** transformar persona em um gate com campos mínimos:

1. papel profissional;
2. maturidade do negócio;
3. canal de aquisição/venda;
4. principal objeção recebida;
5. ticket ou faixa de preço;
6. forma de entrega do serviço;
7. maior perda financeira percebida.

**Impacto esperado:** anúncios mais específicos, landing mais direta e melhor coerência entre Dor → Resultado → Mecanismo → Prova → Oferta.

### 3.5 Gerador de criativos por rotas comerciais

**Problema observado:** os criativos não diferenciaram suficientemente os ângulos de teste.

**Melhoria:** o sistema deve gerar, nomear e medir rotas de criativo separadamente:

- **Rota Dor:** “Quando perguntam preço, o lead some?”
- **Rota Resultado:** “Agenda mais previsível sem depender só de indicação.”
- **Rota Mecanismo:** “Transforme seu acompanhamento em um caminho claro de 8 semanas.”
- **Rota Prova:** “Veja um modelo pronto de resposta anti-preço.”

**Impacto esperado:** aprendizado real sobre qual argumento move o público para ação.

### 3.6 Painel de gargalos por etapa

**Problema observado:** o aprendizado mais importante estava na queda entre visualização do formulário e envio.

**Melhoria:** criar visão gerencial com:

- taxa de passagem entre etapas;
- custo por etapa;
- etapa do maior gargalo;
- suspeita de falha técnica versus comercial;
- recomendação automática do próximo passo.

**Impacto esperado:** o Marketing Hub passa a operar como fábrica de melhoria contínua, não apenas como gerador de campanhas.

### 3.7 Derivação automática do próximo experimento

**Problema observado:** a conclusão já aponta um 37B, mas esse aprendizado ainda precisa virar insumo operacional reaproveitável.

**Melhoria:** criar um fluxo “Criar experimento derivado” que reaproveite:

1. hipótese preservada;
2. causa-raiz do fracasso;
3. nova dor de entrada;
4. nova isca;
5. nova segmentação;
6. nova métrica primária;
7. gates obrigatórios antes da publicação.

**Impacto esperado:** menos repetição de erro e evolução mais rápida de ofertas até chegar em venda.

## 4. Próximo experimento recomendado

### 4.1 Nome

**37B — Roteiro Anti-Preço para Personal Trainer**

### 4.2 Promessa

“Personal trainer: receba um roteiro pronto para responder ‘quanto custa?’ e conduzir o lead para o próximo passo sem dar desconto.”

### 4.3 Por que essa rota é melhor

Ela é mais próxima do dinheiro. O personal não precisa entender todo o mecanismo de 8 semanas antes de agir. Ele reconhece imediatamente a situação: alguém chama no WhatsApp, pergunta preço, compara, some ou pede desconto.

### 4.4 Isca

Roteiro anti-preço + modelo curto de follow-up.

### 4.5 Landing

Landing curta, com foco em:

1. dor direta no WhatsApp;
2. exemplo visual do roteiro;
3. promessa de uso imediato;
4. formulário mínimo;
5. CTA orientado ao benefício.

### 4.6 Métrica primária

Taxa de envio do formulário por visualização do formulário/landing.

### 4.7 Regra de parada

Parar após 100 visualizações reais do formulário sem envio **somente se** o formulário tiver sido testado ponta a ponta antes da publicação.

## 5. Priorização recomendada

| Prioridade | Melhoria | Por quê |
| --- | --- | --- |
| P0 | Gate funcional de formulário antes de mídia | Evita gastar com captura quebrada. |
| P0 | Bloqueio de landing com recomendação de regeneração | Evita testar página abaixo do padrão. |
| P1 | Classificação técnica vs comercial da invalidação | Evita descartar hipótese boa por falha operacional. |
| P1 | Persona mínima vendável | Aumenta especificidade de oferta, CTA e criativo. |
| P1 | Campo “momento de dor” | Aproxima a isca do momento real de compra. |
| P2 | Rotas criativas distintas | Melhora aprendizado de ângulo comercial. |
| P2 | Painel de gargalos por etapa | Ajuda gestão do portfólio de experimentos. |
| P2 | Fluxo de experimento derivado | Acelera ciclos de melhoria até venda. |

## 6. Decisão recomendada

Não repetir o experimento 37 como estava e não descartar a hipótese de personal trainer.

A decisão mais forte para o Marketing Hub é transformar o aprendizado em travas e automações de qualidade. O sistema deve impedir que uma campanha vá para tráfego pago quando ainda não está pronta para vender ou aprender com segurança.

**Direção objetiva:** implementar primeiro o gate funcional de formulário + gate de qualidade da landing + persona mínima. Depois, rodar o 37B com uma isca mais direta: roteiro anti-preço de WhatsApp.
