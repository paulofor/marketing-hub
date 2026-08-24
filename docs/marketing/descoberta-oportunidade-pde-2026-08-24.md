# Descoberta de oportunidade PDE — ciclo de 2026-08-24

## Decisão

O processo `pde-opportunity-discovery` v4 terminou em **PESQUISAR MAIS**. Nenhuma nova oportunidade
foi aprovada como produto porque nenhuma atingiu, de forma estável, o benchmark de Rigel em 82/100.
A nota é priorização interna; não representa venda, receita ou validação comercial.

Não foi criado cadastro de produto, nome interno, experimento, oferta, checkout, campanha ou ativo
público. O melhor sinal foi preservado como hipótese de pesquisa, sem reservar uma estrela.

## Comparação final

| Oportunidade | Rodada final 1 | Rodada final 2 | Conclusão |
| --- | ---: | ---: | --- |
| Pedido no Azul | 73 | 70 | melhor sinal; pesquisar mais |
| Venda Líquida | 66 | 63 | substitutos próximos e distribuição fraca |
| Escopo Pago | 65 | 61 | sobreposição funcional e risco de interpretação jurídica |
| Rigel | 82 | 82 | benchmark não alcançado |

Antes da comparação final, o ciclo também investigou reputação local ética, cobrança sem desgaste,
orçamento de serviço, decisão de encomenda, fotografia de produto e retomada de estudos. Uma
amostragem isolada chegou ao benchmark, mas a repetição conservadora não confirmou o resultado; ela
foi rejeitada para impedir seleção oportunista de nota.

## Melhor sinal: Pedido no Azul

**Público:** pequenos restaurantes, dark kitchens e deliveries que já vendem pelo iFood.

**Dor:** o valor bruto do pedido não deixa claro quanto restou depois de comissão, pagamento online,
promoção custeada pela loja, embalagem e custo da receita. O próprio iFood documenta planos e taxas
distintos e fornece relatório com taxas, incentivos e valor líquido por pedido
([planos](https://blog-parceiros.ifood.com.br/planos-ifood/comment-page-1/),
[relatório de pedidos](https://blog-parceiros.ifood.com.br/relatorio-de-pedidos/)). A Abrasel
registrou pressão real no setor: 23% dos negócios operaram no prejuízo em janeiro de 2026 e 35% não
reajustaram preços nos 12 meses anteriores
([situação financeira](https://odp.abrasel.com.br/noticias/noticias/situacao-financeira-de-bares-e-restaurantes-volta-a-piorar-em-janeiro260306014352/),
[pressão de custos](https://vertentes.abrasel.com.br/noticias/noticias/bares-restaurantes-seguem-segurando-precos-mesmo-tendencia-alta-insumos260413052128/)).
Esses dados sustentam a consequência econômica, mas não provam que a microexperiência proposta será
comprada.

**Mecanismo:** auditoria privada e determinística de um pedido liquidado, sem login ou integração.
Ela separaria valor observado, custo declarado e campo desconhecido; mostraria a cascata até a
contribuição do pedido e permitiria simular uma única mudança reversível.

**Microvalor:** explicar “para onde foi o dinheiro deste pedido” em poucos minutos, preservando a
decisão humana e sem se apresentar como auditoria do iFood, lucro total ou recomendação financeira.

## Por que não avançou

- A intenção de compra permaneceu em 8–11/20: existem seis ofertas pagas adjacentes, mas não há
  comportamento verificável de compra da auditoria pontual.
- A calculadora gratuita do próprio iFood já cobre precificação antes da venda
  ([calculadora](https://blog-parceiros.ifood.com.br/materiais-gratuitos/calculadora-de-precificacao-ifood/)).
  A diferença pós-liquidação é plausível, mas ainda não foi percebida em uso por operadores reais.
- Distribuição ficou em 4–5/10: busca, conteúdo e comunidades são rotas futuras, não um canal próprio
  ou aquisição histórica.
- Psique manteve valor percebido em 72/100: o alívio é concreto, porém preenchimento manual, custos
  ausentes e risco de interpretar um pedido como lucro total ainda superam a vantagem comprovada.
- Sem custo real, margem observada, retenção ou venda atribuída, economia não pode receber nota
  máxima.

## Decisão entre alternativas

1. **Reduzir o benchmark ou escolher a amostragem mais alta:** menor esforço, mas transformaria
   variação do modelo em prova comercial. Rejeitado.
2. **Cadastrar Pedido no Azul como produto agora:** aceleraria a construção, mas consumiria a cadeia
   com intenção de compra e canal ainda não demonstrados. Rejeitado.
3. **Preservar Pedido no Azul como sinal prioritário:** mantém a dor e o mecanismo auditáveis e exige
   evidência própria antes de criar produto. Escolhido.

## Gate para uma pesquisa futura

Uma nova rodada só deve ser aberta quando houver autorização para validação consentida e evidência
nova capaz de responder, sem mídia paga:

- operadores concluem a auditoria de um pedido real sem assistência;
- distinguem contribuição por pedido de lucro total;
- identificam pelo menos uma premissa ausente ou divergente;
- preferem materialmente a auditoria pós-liquidação à calculadora pré-venda gratuita;
- demonstram intenção de pagar ou compromisso observável, sem tratar resposta favorável como venda;
- uma rota orgânica atribuível produz uso real suficiente para avaliar aquisição.

Se a diferença gratuita não for percebida, o sinal deve ser rejeitado. Se houver uso e intenção, o
ciclo volta a comparar a oportunidade com duas alternativas novas; não recebe aprovação automática.

## Execução e auditoria

- 42 evidências ativas e 20 ofertas pagas deduplicadas na comparação final.
- Argos, Hermes, Dédalo e Psique executados com prompt e schema versionados, `gpt-5.6-terra`, Flex e
  armazenamento desabilitado.
- Request, response bruto, resultado funcional, modelo, status, tokens e custo preservados por
  correlação local.
- 51 respostas de agente no ciclo completo: 834.816 tokens de entrada, 77.881 em cache e 174.413 de
  saída; custo Flex estimado em US$ 3,62240220 pelas tarifas versionadas consultadas em 2026-08-24.
- Duas rodadas finais completas e consecutivas após a última correção, ambas com 26 testes de
  contrato aprovados e efeitos comerciais zerados.
- Contatos, compras, vendas, receita, mídia e publicações: zero.
