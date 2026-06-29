# Plano para buscar a primeira venda com funis de produtos digitais

## Objetivo

Definir um caminho simples, mensurável e acionável para sair da fase de interesse/lead e chegar na primeira venda real do Marketing Hub.

O problema atual não é falta de capacidade de gerar ativos. O histórico mostra que já existem criativos, páginas, formulários, geração de imagens, pagamentos e métricas de funil. A dúvida principal é estratégica: qual rota comercial usar em cada tipo de produto sem transformar todo experimento em um formulário longo antes de saber se alguém compraria.

Este plano prioriza venda, não volume de leads.

## Evidências analisadas

Arquivos usados como base:

- `docs/implementacao/experimentos/analise-historico-experimentos-sem-sucesso.md`
- `docs/implementacao/experimentos/plano-mestre-evolucao-funis-produtos-personalizacao.md`
- `docs/implementacao/experimentos/especificacao-centro-de-decisao-frontend.md`
- `docs/relatorios/experimentos/analises/analise-campanhas-resultados-fracos.md`
- `docs/relatorios/experimentos/experimento-37-relatorio-completo (1).md`
- `docs/relatorios/experimentos/experimento-38-relatorio-completo (1).md`
- `docs/relatorios/experimentos/experimento-39-relatorio-completo.md`
- `docs/relatorios/experimentos/experimento-40-relatorio-completo.md`
- `docs/relatorios/experimentos/experimento-41-relatorio-completo.md`
- `docs/relatorios/experimentos/experimento-47-relatorio-completo.md`
- `docs/relatorios/experimentos/experimento-50-relatorio-completo.md`
- `docs/manual-usuario/aihub/funil-vendas-experimento.md`
- `docs/manual-usuario/aihub/formularios-simples-sem-imagem.md`
- `docs/manual-usuario/aihub/lead-portal-pagamentos.md`
- `docs/gera-landing/melhorias-qualidade-paginas-venda-produtos-digitais.md`

## Diagnóstico direto

O histórico não prova que o mercado rejeitou os produtos. Ele mostra principalmente quebra depois do clique.

Na análise consolidada de campanhas fracas, os experimentos 37, 38, 39, 40, 41, 47 e 50 somaram:

- 28.038 impressões;
- 551 cliques;
- 575 visualizações de formulário;
- 0 envios de formulário.

Esse dado muda a prioridade. Antes de tentar escalar mídia ou criar mais produtos, o sistema precisa provar que uma pessoa consegue:

1. entender a promessa;
2. acreditar no mecanismo;
3. ver valor concreto;
4. avançar com pouco esforço;
5. comprar.

O formulário foi útil para medir interesse, mas virou uma etapa obrigatória mesmo quando o produto já poderia ser vendido diretamente. Isso aumenta fricção e pode estar impedindo o aprendizado mais importante: alguém paga por isso?

## Decisão estratégica recomendada

Para buscar a primeira venda, a rota principal deve ser:

```text
Anuncio -> pagina propria de venda curta -> checkout -> entrega simples
```

Use formulario apenas quando ele for essencial para entregar valor antes da compra.

A primeira venda deve vir de um produto digital low-ticket, com entrega simples, pouca ou nenhuma personalizacao obrigatoria e preco baixo o suficiente para compra de impulso racional.

Faixa recomendada inicial:

- B2C/MEI/autonomo: R$ 19 a R$ 47;
- B2B pequeno/operacional: R$ 37 a R$ 97;
- produto personalizado com custo de IA relevante: testar depois, quando ja houver compra em produto simples.

## Alternativas comparadas

### Alternativa 1: Instant Form + amostra personalizada + venda posterior

Fluxo:

```text
Anuncio -> Instant Form -> email com amostra -> oferta do pacote maior -> checkout
```

Beneficios:

- baixa friccao inicial dentro da Meta;
- bom para capturar contato rapidamente;
- permite personalizacao e follow-up;
- agora voltou a ser viavel porque o problema dos Instant Forms foi resolvido.

Riscos:

- ainda posterga a venda;
- depende de abertura de email;
- exige fluxo de amostra, entrega, tracking e follow-up funcionando muito bem;
- pode gerar lead curioso que queria apenas a amostra gratuita.

Custo/esforco:

- medio/alto, porque exige integracao de formulario, identidade do lead, email, amostra e oferta posterior.

Aderencia ao objetivo de primeira venda:

- media. E bom para produtos realmente personalizados, mas nao e o caminho mais curto para validar compra.

### Alternativa 2: Landing propria com formulario e amostra

Fluxo:

```text
Anuncio -> landing propria -> formulario -> amostra -> oferta -> checkout
```

Beneficios:

- explica melhor a promessa do que o Instant Form;
- permite medir acesso, scroll, view de secao e comportamento;
- permite coletar dados mais ricos quando isso for necessario;
- e boa para ofertas que precisam de contexto antes da compra.

Riscos:

- historicamente, o gargalo apareceu exatamente no formulario;
- pode virar uma barreira antes do valor ser percebido;
- se a recompensa gratuita nao for muito forte, o lead abandona.

Custo/esforco:

- medio, porque a infraestrutura ja existe, mas exige qualidade alta de pagina, velocidade, formulario e follow-up.

Aderencia ao objetivo de primeira venda:

- media/baixa para produtos simples; alta apenas quando a personalizacao e a prova antes da compra forem parte central do valor.

### Alternativa 3: Pagina propria de venda direta low-ticket

Fluxo:

```text
Anuncio -> pagina de venda -> checkout -> entrega
```

Beneficios:

- mede o que importa agora: compra;
- reduz etapas e friccao;
- evita confundir lead com demanda pagante;
- permite usar a pagina propria para analytics;
- combina com produtos que ja nao dependem de nome, foto ou dados pessoais.

Riscos:

- menos leads para remarketing;
- exige uma oferta muito concreta e uma pagina mais vendedora;
- se o checkout ou a entrega falharem, a leitura fica contaminada.

Custo/esforco:

- baixo/medio, porque reaproveita GeraLanding e pagamento, mas precisa foco comercial na pagina e na oferta.

Aderencia ao objetivo de primeira venda:

- alta. E a melhor rota para obter a primeira compra com menor complexidade.

### Alternativa 4: Hibrido com venda direta e captura secundaria

Fluxo:

```text
Anuncio -> pagina de venda -> checkout
                      -> captura secundaria para quem nao compra
```

Beneficios:

- mantem o objetivo de venda no centro;
- captura interessados que ainda nao compraram;
- permite remarketing e follow-up sem bloquear a compra;
- reduz o risco de perder aprendizado de visitantes qualificados.

Riscos:

- exige pagina bem organizada para nao dividir demais a atencao;
- pode diluir o CTA se a captura secundaria ficar forte demais.

Custo/esforco:

- medio.

Aderencia ao objetivo de primeira venda:

- alta, mas deve vir depois da versao direta simples para nao complicar o primeiro teste.

## Escolha recomendada

Escolha a Alternativa 3 como primeiro movimento:

```text
Pagina propria de venda direta low-ticket
```

Justificativa objetiva:

- o historico mostra clique e visualizacao, mas zero envio de formulario;
- a primeira venda exige testar disposicao de pagamento, nao apenas interesse;
- muitos produtos recentes ja nao precisam de personalizacao no primeiro contato;
- a pagina propria preserva metricas de acesso que o Instant Form nao entrega;
- a complexidade operacional e menor que personalizacao + email + pacote final.

Depois da primeira venda, evoluir para a Alternativa 4. Depois disso, testar Instant Form novamente em produtos onde a personalizacao seja o principal diferencial.

## Produto inicial recomendado

Priorizar um produto com estas caracteristicas:

- resolve uma dor operacional concreta e frequente;
- entrega uma vitoria em ate 24 horas;
- nao depende de suporte humano;
- nao exige imagem, nome ou briefing longo antes da compra;
- pode ser mostrado com preview visual na pagina;
- tem entregaveis claros e copiaveis/editaveis;
- possui limite honesto, sem promessa exagerada.

Entre os produtos analisados, os melhores candidatos sao:

1. **Kit Manutencao Guiada para Alongamento em Domicilio**
   - Melhor aderencia a MEI/autonoma.
   - Dor concreta: cliente some, atrasa manutencao, volta com urgencia, gera buraco na agenda.
   - Entrega simples: mensagens, checklist, ficha, calculadora simples e plano de 7 dias.
   - Preco sugerido: R$ 27 ou R$ 37.

2. **Kit Relatorio Promocional por Evidencias**
   - Melhor para B2B operacional.
   - Dor concreta, mas publico mais frio e clique historicamente mais fraco.
   - Preco sugerido: R$ 47 ou R$ 67.

3. **Agenda Cheia Sem Desconto para Personal Trainer**
   - Oferta rica, mas complexa.
   - Melhor para uma segunda fase com prova/amostra, porque a promessa e maior e pode exigir mais confianca.

Recomendacao: comecar pelo **Kit Manutencao Guiada para Alongamento em Domicilio**.

Motivo: e o melhor equilibrio entre dor frequente, promessa simples, entrega rapida, preco baixo e baixa dependencia de personalizacao.

## Plano de execucao

### Fase 1: Preparar uma oferta compravel

Objetivo: transformar o produto em algo que alguem consiga comprar sem precisar falar com ninguem.

Tarefas:

- definir uma unica promessa de entrada;
- definir preco inicial de R$ 27 ou R$ 37;
- montar uma entrega simples em PDF/Google Doc/planilha;
- criar preview visual real dos entregaveis;
- escrever garantia simples: se nao conseguir aplicar em 7 dias, recebe uma versao revisada ou reembolso;
- remover qualquer dependencia de formulario antes do checkout.

Oferta sugerida:

```text
Kit Manutencao Guiada para Alongamento em Domicilio

Organize o pos-atendimento para a cliente sair sabendo quando voltar, quais cuidados seguir e o que fazer se quebrar, atrasar ou pedir encaixe.
```

CTA principal:

```text
Comprar o kit por R$ 27
```

CTA secundario apenas depois:

```text
Ver uma amostra gratuita
```

### Fase 2: Criar pagina de venda curta

Estrutura recomendada:

1. Primeira dobra:
   - dor: cliente some depois do alongamento;
   - resultado: manutencao mais organizada e menos conversa improvisada;
   - mecanismo: roteiro + ficha + lembretes + mensagens prontas;
   - CTA: comprar o kit.

2. Prova visual:
   - mostrar print/preview da ficha, da regua de lembretes e de 3 mensagens prontas.

3. Como funciona:
   - preencher o mapa dos ultimos atendimentos;
   - escolher janela de manutencao;
   - usar roteiro de fechamento;
   - enviar lembretes e respostas prontas.

4. O que recebe:
   - cada entregavel ligado a beneficio pratico.

5. Para quem e / para quem nao e:
   - filtrar profissionais de alongamento que atendem em domicilio ou agenda propria;
   - excluir quem busca curso tecnico de alongamento ou automacao completa de WhatsApp.

6. Garantia e limites:
   - nao promete agenda cheia;
   - promete organizacao aplicada do pos-atendimento.

7. Checkout:
   - botao repetido com preco claro.

### Fase 3: Rodar campanha com pergunta de negocio correta

Pergunta do teste:

```text
Profissionais de alongamento em domicilio compram um kit simples de organizacao de pos-atendimento por R$ 27/R$ 37 sem passar por formulario?
```

Variavel primaria:

```text
rota de conversao: venda direta sem formulario
```

Metrica primaria:

```text
compra aprovada
```

Metricas de guarda:

- CTR;
- CPC;
- view da pagina;
- clique no checkout;
- taxa checkout/view;
- custo por checkout;
- custo por compra;
- tempo de carregamento mobile;
- eventos de erro no checkout.

Orcamento inicial:

- R$ 30 a R$ 50 por dia por 3 dias, ou ate atingir limite de perda definido;
- nao encerrar antes de validar que pagina, checkout e tracking funcionam.

Criterios de decisao:

- 1 compra: validar rota minima e repetir com melhoria de criativo/pagina;
- 0 compra, mas checkout clicado: melhorar oferta, preco, prova e checkout;
- 0 checkout com bom CTR: problema provavel na pagina/oferta;
- CTR baixo: problema provavel no criativo, publico ou angulo;
- carregamento lento/erro: execucao invalida, nao interpretar como rejeicao comercial.

### Fase 4: So depois testar captura

Se a venda direta gerar clique mas nao compra, testar captura secundaria sem abandonar a venda:

```text
Nao quer comprar agora? Baixe 3 mensagens prontas de manutencao.
```

Essa captura deve ficar abaixo da oferta principal ou em exit/segunda dobra, nunca substituir o CTA de compra no primeiro teste.

Se a captura secundaria gerar leads baratos, usar follow-up com:

- email 1: entregar 3 mensagens;
- email 2: mostrar erro comum no pos-atendimento;
- email 3: apresentar kit completo;
- email 4: lembrar garantia e preco baixo.

## Quando usar cada funil daqui para frente

### Use venda direta quando:

- produto e generico ou semi-generico;
- a entrega pode ser entendida por preview;
- o preco e baixo;
- o objetivo e validar compra;
- os dados do usuario nao sao necessarios para produzir a primeira versao.

### Use landing com formulario quando:

- precisa explicar melhor a promessa;
- precisa coletar uma informacao para gerar valor real;
- quer medir comportamento detalhado;
- o lead magnet e forte o suficiente para justificar o preenchimento.

### Use Instant Form quando:

- a captura precisa ser muito rapida;
- o dado necessario e minimo;
- o produto personalizado depende de follow-up;
- a campanha esta em fase de volume de leads depois que a oferta ja mostrou potencial.

### Use personalizacao por imagem quando:

- a imagem personalizada e a prova central do valor;
- a amostra aumenta muito o desejo de compra;
- o custo de gerar amostra cabe na margem;
- o email/WhatsApp de follow-up esta medido e funcionando.

## Regras praticas para nao repetir o problema

- Nao usar formulario como padrao automatico.
- Nao medir sucesso por lead quando a meta e venda.
- Nao publicar campanha sem checkout testado ponta a ponta.
- Nao interpretar 0 venda como rejeicao se pagina, checkout, tracking ou velocidade falharam.
- Nao mudar produto, publico, criativo, pagina e preco ao mesmo tempo sem registrar a variavel primaria.
- Nao pedir dados que nao serao usados imediatamente para aumentar valor percebido.
- Nao vender "PDF", "planilha" ou "kit"; vender reducao de dor e facilidade pratica.

## Proximo experimento recomendado

Nome:

```text
Venda direta - Kit Manutencao Guiada
```

Nicho:

```text
Profissional de alongamento de unhas que atende em domicilio ou com agenda propria
```

Oferta:

```text
Kit Manutencao Guiada para Alongamento em Domicilio
```

Promessa:

```text
Organize o pos-atendimento para reduzir conversa improvisada, lembrar manutencao no prazo e responder quebra, atraso, falta e encaixe com mais seguranca.
```

Preco inicial:

```text
R$ 27
```

Funil:

```text
Anuncio -> pagina de venda -> checkout -> entrega
```

Meta do primeiro ciclo:

```text
Conseguir a primeira compra aprovada ou diagnosticar exatamente em qual etapa a compra travou.
```

Indicador de aprendizado minimo:

```text
Pelo menos 1 compra aprovada ou, se nao houver compra, dados suficientes para classificar o gargalo entre criativo, pagina, checkout, preco ou oferta.
```

## Sequencia apos a primeira venda

1. Entregar manualmente se necessario, para nao atrasar aprendizado.
2. Pedir feedback simples ao comprador:
   - o que fez comprar;
   - o que quase impediu;
   - qual entregavel pareceu mais util;
   - se o preco pareceu justo.
3. Transformar o feedback em prova da pagina.
4. Criar segunda versao da pagina com prova real.
5. Testar preco R$ 37.
6. Adicionar captura secundaria para nao compradores.
7. So entao testar Instant Form como topo de funil para volume.

## Conclusao

A melhor decisao agora e reduzir a complexidade e colocar compra no centro.

O funil original de personalizacao por imagem continua valido, mas ele e mais adequado para uma fase posterior, quando ja houver oferta validada, follow-up confiavel e clareza de margem. Para a primeira venda, o caminho mais forte e um produto digital simples, barato, com pagina propria de venda direta e checkout testado.

Primeiro vender. Depois sofisticar o funil.
