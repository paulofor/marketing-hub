# Plano de evolução do funil para buscar a primeira venda

Data: 2026-06-29

## 1. Diagnóstico

A dúvida atual não é apenas escolher entre Instant Form, página com formulário ou página de venda. O problema principal é que o funil evoluiu a partir de uma limitação operacional antiga: como era difícil criar Instant Forms, a página própria virou o caminho padrão. Depois, mesmo quando os produtos deixaram de exigir personalização forte, o formulário continuou sendo usado como etapa intermediária para medir interesse.

Com o problema dos Instant Forms resolvido, a decisão precisa voltar para o objetivo de negócio:

> gerar a primeira venda de um produto digital que resolva uma dor real com baixo esforço de compra.

A evidência dos relatórios mostra que o topo do funil não é o maior problema em todos os casos. Nos experimentos 37, 39 e 40 houve CTR bom e clique barato, mas não houve envio de formulário. O relatório `docs/relatorios/experimentos/analises/analise-campanhas-resultados-fracos.md` registra 575 visualizações de formulário somadas e 0 envios. Isso indica quebra depois do clique: página, recompensa, formulário, velocidade, oferta ou falta de intenção de compra.

## 2. Leitura estratégica

O funil inicial fazia sentido quando o produto era uma imagem personalizada:

1. anúncio;
2. Instant Form ou página;
3. coleta de dados;
4. geração de amostra personalizada;
5. entrega por e-mail;
6. convite para comprar pacote maior.

Esse modelo é bom para produtos em que a personalização é a prova principal. Porém ele cria atrito antes da venda. Para buscar a primeira venda, nem todo produto deve passar por esse caminho.

A evolução recomendada é trabalhar com três funis em paralelo, cada um com papel claro:

| Funil | Quando usar | Objetivo |
|---|---|---|
| Venda direta | Produto simples, pronto, low-ticket e fácil de entender | Medir compra real |
| Página com captura + checkout | Oferta precisa de mais explicação ou prova | Medir intenção e venda |
| Instant Form + amostra | Produto depende de personalização ou diagnóstico | Medir demanda por personalização |

O erro a evitar é usar formulário em todos os casos só porque ele mede interesse. Para primeira venda, o melhor sinal é pagamento ou tentativa de checkout.

## 3. Prioridade para a primeira venda

### Prioridade 1 — Produto low-ticket sem personalização obrigatória

Criar uma oferta simples, pronta para comprar, com preço baixo e entrega imediata.

Recomendação inicial:

- preço de entrada: R$ 9,90 a R$ 29,90;
- entrega: kit, checklist, mensagens prontas, planilha, roteiro ou mini-biblioteca;
- checkout direto visível na primeira dobra;
- página curta, com prova visual grande do que a pessoa recebe;
- sem exigir briefing antes da compra.

Motivo: a primeira venda precisa provar disposição de pagamento. Se o usuário precisa preencher formulário antes de entender o valor, o funil pode estar medindo curiosidade, não compra.

### Prioridade 2 — Usar os ângulos com melhor sinal de clique

Os melhores candidatos imediatos são os experimentos 39 e 40, porque o relatório de campanhas fracas aponta bons CTRs e CPC baixo:

- experimento 39: manicure em domicílio, 4.297 impressões, 179 cliques, CTR 4,17%, CPC R$ 0,19;
- experimento 40: alongamento de unhas, 2.757 impressões, 113 cliques, CTR 4,10%, CPC R$ 0,15.

Leitura: existe atenção para a dor. O gargalo está depois do clique.

Oferta recomendada para relançar:

**Kit Manutenção Guiada para Alongamento em Domicílio**

Promessa:

> Organize o pós-atendimento e pare de improvisar mensagens quando a cliente some, atrasa manutenção, quebra ou pergunta de novo sobre cuidados.

Entrega low-ticket:

- 6 mensagens prontas de WhatsApp para manutenção;
- checklist simples de pós-atendimento;
- mini-calculadora de janela de manutenção;
- ficha rápida de acompanhamento;
- respostas prontas para quebra, descolamento, falta e preço;
- plano de aplicação em 7 dias.

CTA principal:

> Comprar o kit por R$ 19,90

CTA secundário:

> Ver uma amostra gratuita

## 4. Arquitetura de funis recomendada

### Funil A — Venda direta

Fluxo:

1. Anúncio com dor concreta.
2. Página de venda curta.
3. Botão de checkout.
4. Compra.
5. Entrega imediata.

Uso:

- primeira venda;
- produto pronto;
- oferta de baixo preço;
- dor fácil de reconhecer.

Métrica principal:

- compra aprovada.

Métricas secundárias:

- CTR;
- CPC;
- visualização da página;
- clique no checkout;
- taxa checkout/page view.

Regra de decisão:

- se houver clique no checkout e nenhuma compra, melhorar preço, prova, garantia e checkout;
- se não houver clique no checkout, melhorar oferta e primeira dobra;
- se não houver clique no anúncio, trocar criativo ou dor.

### Funil B — Página com captura + checkout

Fluxo:

1. Anúncio.
2. Página explicativa.
3. CTA principal para compra.
4. CTA secundário para amostra gratuita.
5. Sequência de e-mail ou WhatsApp para quem pediu amostra.

Uso:

- produto ainda precisa de educação;
- oferta tem mecanismo que precisa ser explicado;
- há valor em recuperar interessados.

Métrica principal:

- clique no checkout ou compra.

Métrica secundária:

- lead capturado.

Regra importante:

O formulário não pode competir com a venda. Ele deve ser rota de recuperação, não o caminho principal.

### Funil C — Instant Form com amostra personalizada

Fluxo:

1. Anúncio prometendo amostra personalizada.
2. Instant Form com poucos campos.
3. Geração de amostra.
4. Entrega por e-mail ou WhatsApp.
5. Oferta do pacote completo.

Uso:

- quando a personalização é o diferencial percebido;
- quando o usuário precisa ver o resultado aplicado ao caso dele;
- quando a amostra é forte o suficiente para gerar desejo.

Métrica principal:

- compra após amostra.

Métricas secundárias:

- taxa de envio do Instant Form;
- abertura da entrega;
- clique para checkout;
- resposta ao WhatsApp/e-mail.

Regra importante:

Não usar Instant Form apenas porque ficou tecnicamente possível. Usar quando a personalização aumenta a conversão mais do que o atrito reduz.

## 5. Plano de execução em 30 dias

### Semana 1 — Preparar a primeira oferta comprável

Objetivo: sair de teste de interesse para teste de compra.

Ações:

- escolher 1 nicho inicial: alongamento/manicure em domicílio;
- montar 1 produto low-ticket pronto, sem personalização obrigatória;
- criar página curta com checkout como CTA principal;
- incluir prova visual grande do kit;
- criar amostra gratuita como CTA secundário;
- garantir tracking de page view, clique no checkout, compra e lead.

Critério de pronto:

- a pessoa entende em até 5 segundos o que compra, para quem é e qual dor reduz;
- o botão de compra aparece na primeira dobra;
- a entrega parece concreta, não apenas "PDF genérico".

### Semana 2 — Rodar teste de venda direta

Objetivo: descobrir se existe intenção de compra.

Campanhas:

- 2 criativos com dor;
- 1 criativo com prova visual do kit;
- 1 criativo com oferta direta.

Orçamento sugerido:

- baixo e controlado, suficiente para gerar sinais iniciais;
- não escalar antes de ter clique no checkout ou venda.

Métrica de decisão:

- compra aprovada;
- se ainda não houver compra, clique no checkout como sinal intermediário.

Interpretação:

- CTR bom + sem checkout: problema de página/oferta;
- checkout bom + sem venda: problema de preço, confiança, checkout ou prova;
- CTR ruim: problema de criativo, dor ou público.

### Semana 3 — Testar captura como recuperação, não como centro

Objetivo: recuperar quem não comprou sem atrapalhar a venda.

Ações:

- adicionar pop/box discreto de amostra gratuita após rolagem ou intenção de saída;
- testar "baixar 3 mensagens prontas" em vez de "preencher diagnóstico";
- enviar sequência curta:
  1. entrega da amostra;
  2. exemplo de uso;
  3. convite para comprar o kit completo;
  4. lembrete com objeções.

Métrica de decisão:

- lead-to-checkout;
- lead-to-purchase.

### Semana 4 — Comparar com Instant Form

Objetivo: descobrir se o Instant Form volta a ser útil neste contexto.

Teste:

- mesma promessa;
- mesmo criativo ou variação próxima;
- rota A: anúncio para página de venda;
- rota B: anúncio para Instant Form com amostra.

Decisão:

- se Instant Form gera leads, mas não gera checkout, ele serve para pesquisa, não para venda;
- se página gera menos leads, mas mais checkout, priorizar página;
- se Instant Form gera checkout depois da amostra, manter como funil de personalização.

## 6. Critérios de publicação de novos experimentos

Antes de publicar, cada experimento precisa ter:

- uma hipótese clara;
- uma variável principal;
- métrica primária;
- meta mínima;
- stop loss;
- página testada no celular;
- formulário ou checkout testado ponta a ponta;
- prova visual concreta da entrega;
- CTA compatível com o que realmente acontece.

Não publicar experimento se:

- o objetivo for apenas "medir interesse" sem definir o que será feito com esse interesse;
- o formulário pedir esforço antes de provar valor;
- o produto ainda não puder ser comprado;
- a página prometer personalização, mas coletar dados insuficientes;
- o relatório tratar CPL como sucesso quando há 0 leads.

## 7. Decisão prática sobre formulário vs venda

Regra simples:

- se o produto é pronto e barato, venda direta;
- se o produto é complexo, página com venda principal e lead secundário;
- se o produto depende da imagem, briefing ou personalização, Instant Form ou formulário próprio;
- se a personalização é só um detalhe, não coloque ela antes da compra.

Para a primeira venda, o funil recomendado é:

> anúncio → página curta de venda → checkout → entrega imediata.

Com rota secundária:

> amostra gratuita → sequência curta → checkout.

## 8. Próximo experimento recomendado

### Produto

Kit Manutenção Guiada para Alongamento em Domicílio.

### Público

Nail designer, manicure ou profissional de alongamento que atende em domicílio e depende de WhatsApp para retorno, manutenção e encaixes.

### Dor

Cliente sai satisfeita, mas some da manutenção, volta atrasada ou chama só quando quebra/descola.

### Oferta

Kit digital pronto para organizar pós-atendimento e manutenção.

### Preço

R$ 19,90 no primeiro teste.

### Página

Página curta com:

1. headline da dor;
2. mockup grande do kit;
3. lista de entregáveis por benefício;
4. exemplo real de mensagem pronta;
5. preço;
6. garantia simples;
7. checkout.

### Anúncios

Variação 1:

> Cliente some da manutenção e só chama quando quebra?

Variação 2:

> Pare de improvisar mensagem de manutenção no WhatsApp.

Variação 3:

> 6 mensagens prontas para conduzir manutenção sem parecer cobrança chata.

### Métrica principal

Compra aprovada.

### Métrica intermediária

Clique no checkout.

### Aprendizado esperado

Descobrir se a dor que já gerou clique barato também gera intenção de pagamento quando a oferta é direta, simples e comprável.

## 9. O que muda na mentalidade do funil

Antes:

> capturar dados para gerar amostra e depois tentar vender.

Agora:

> oferecer uma compra simples primeiro e usar captura/amostra para recuperar quem ainda não está pronto.

Essa mudança é importante porque o sistema ainda não conseguiu a primeira venda. Neste momento, a prioridade não é maximizar leads. A prioridade é encontrar a menor oferta comprável que prove que alguém aceita pagar para remover uma dor real.
