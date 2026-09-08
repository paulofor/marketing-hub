# Vega: posição na cadeia e sequência para vender

Análise de 08/09/2026 UTC, com consultas entre 02:20 e 02:32 UTC — noite de 07/09 no Brasil.
Produto `4`, Vega/Método MUSA; experimento pago `91`; plano comercial `3`.
Esta é uma recomendação estratégica documentada, sem alteração da operação comercial.

**Recomendação:** manter o Vega em validação comercial inicial. O próximo avanço deve comprovar
um ajuste que a cliente consegue aplicar, uma compra reconciliada e uma entrega útil. A sequência
recomendada é corrigir a leitura operacional, fortalecer a primeira entrega, facilitar sua descoberta
no celular, validar com pessoas reais e então executar outro teste pago limitado. Instagram permanece
a tese principal do plano; o piloto direto ajuda a entender o uso e mantém seus resultados separados.

**Posição na cadeia de valor**

O backend e a tela identificam o Vega como `ATIVO`, na **etapa 6 de 6: Venda, entrega e aprendizado do
PDE**, da cadeia v12. O subprocesso atual é **Operação e otimização de experimento**; o próximo é
**Venda, entrega e satisfação da cliente**. O objetivo comercial da etapa atual ainda não foi atingido.

O produto já tem forma e preço: programa guiado de sete dias, pagamento único de **R$ 67**, acesso
individual por 90 dias, primeiro ajuste e Dia 1 gratuitos. Seu trabalho comercial é ajudar a mulher
a decidir o que ajustar antes de sair, usando principalmente o que possui. A experiência organizada,
a aplicação fácil e a continuidade são o valor vendido. Aprovação técnica e revisão de agentes
qualificam a experiência para teste; compra, aplicação e satisfação precisam de pessoas reais.

Não há motivo demonstrado para reiniciar toda a descoberta ou reconstruir o produto inteiro.
Cabe um retorno dirigido à construção para melhorar a devolutiva e à comunicação para facilitar
a entrada, seguido de homologação da versão resultante e retorno à operação.

O histórico da cadeia contém lacunas e datas incompatíveis: descoberta e construção aparecem sem
período registrado; comunicação apresenta saída anterior à entrada. Isso limita o uso desse histórico
para avaliar duração ou completude das etapas anteriores. Não comprova que o trabalho nunca existiu:
há contratos e homologações anteriores da v7 no repositório.

**Experimento 91: fotografia conciliada**

| Indicador | Evidência atual | Leitura comercial |
| --- | --- | --- |
| Experimento | `USER_STOPPED` | Foi interrompido pelo usuário |
| Campanha Meta | `PAUSED` | A pausa foi confirmada diretamente na Graph API |
| Conjunto e anúncio | Configurados `ACTIVE`, efetivamente `CAMPAIGN_PAUSED` | Não estão entregando enquanto a campanha está pausada |
| Objetivo e otimização | `OUTCOME_SALES`, `OFFSITE_CONVERSIONS`, `PURCHASE` | A configuração busca conversão de compra |
| Mídia Meta, consulta direta | R$ 27,45; 121 impressões; alcance 113 | O Hub ainda mostra R$ 27,25 e 120 impressões na última sincronização |
| Cliques | 5 totais; **4 de saída** | O quinto clique não deve ser contado como acesso ao destino |
| Sessões atribuídas | **4**, classificadas `HUMAN`, todas mobile | Coincidem numericamente com os quatro cliques de saída, sem provar correspondência individual |
| Degustação | **2 iniciadas** | Há sinal inicial de interação |
| Primeiro resultado | **1 devolutiva renderizada** | Ainda não comprova aplicação ou valor percebido |
| Captura | **1 e-mail preenchido e login iniciado** | Não houve login concluído atribuído |
| Oferta | 1 evento `PAYWALL_VIEWED` | Dispara junto da devolutiva; não prova leitura da oferta |
| Checkout e compra | **0 e 0** | Nenhuma receita ou CAC realizado pode ser demonstrado |

O banco PDE contém 86 eventos atribuídos às quatro sessões e nenhuma transação do produto na
auditoria de pagamentos. Há um acesso gratuito por link mágico, sem pagamento. Um cadastro criado
antes do envio do e-mail não comprova que a mensagem foi entregue, aberta ou que a pessoa entrou.
A causa do abandono após solicitar acesso permanece indeterminada: há somente um caso e não foi
executado envio de e-mail nem pagamento real nesta análise.

O antigo diagnóstico de ausência total de tráfego não descreve mais o #91. Também seria precipitado
declarar rejeição do produto, preço inadequado ou criativo vencedor com quatro sessões.

**Três problemas concretos que prejudicam a próxima decisão**

1. **O planejamento repete um bloqueio vencido.** O plano #3 ainda afirma que o #91 está `PLANNED`,
   sem campanha e aguardando HLS do vídeo #38. As execuções automáticas #914, #915 e #917 de Hermes
   repetem essa orientação. Os snapshots carregam `experimentId=90`, apesar do objetivo textual ser
   vender pelo #91. O código monta o contexto a partir do experimento principal do plano e copia
   seus campos narrativos. Assim, repetir a análise não atualiza a premissa. A correção recomendada
   é reconciliar referências e evidências atuais antes de uma nova decisão, preservando as tentativas
   antigas e impedindo que um bloqueio histórico prevaleça sobre a campanha observada.
2. **O monitor perde informação ao filtrar por campanha.** A conversão para o resumo atribuído fixa
   `FIELD_FILLED` e page views em zero, omite jornadas e soma apenas alguns eventos para apresentar
   o total. Por isso recomenda ajustar a captura mesmo existindo preenchimento no banco. O tempo
   agrega `SCREEN_TIME` e `PAGE_VISIBLE_TIME`, que se sobrepõem; não usar os 1min49s da tela como
   tempo médio confiável. A correção recomendada é obter eventos e jornadas já filtrados na origem,
   separar clique total de clique de saída e definir um único relógio de tempo visível.
3. **A primeira entrega ainda exige interpretação da cliente.** A resposta realmente associada à
   sessão que concluiu a degustação inclui: “Use postura e presença para aproximar o sinal de
   elegância discreta.” A implementação concatena categorias escolhidas. Ela organiza intenção,
   mas não explica uma ação concreta. A devolutiva também expõe “regras locais”, um detalhe interno
   que não ajuda a cliente a executar o ajuste. Isso fundamenta uma melhoria de utilidade; não prova
   que foi a causa do abandono observado.

Na navegação pública, a primeira resposta fica abaixo da dobra: aproximadamente y=964 em viewport
iPhone 393×659 e y=980 em Pixel 412×839. A imagem vem antes da escolha e o CTA final só é habilitado
depois das quatro respostas. Não houve erro de JavaScript nem overflow nas observações. A prioridade
de experiência é permitir iniciar imediatamente e reduzir o esforço até uma ação útil.

**Alternativas de aquisição e continuidade**

| Rota | Benefício | Risco e custo/esforço | Decisão para o Vega |
| --- | --- | --- | --- |
| Instagram Ads para degustação e compra | Canal principal já configurado; permite medir aquisição paga | Custo atual por acesso incompatível com o cenário de conversão; exige verba e medição íntegra | Preservar como tese principal; retomar teste limitado somente após os gates abaixo |
| Creators/parceiros de estilo por performance | Demonstração em contexto aderente e confiança do parceiro | Comissão, produção, atribuição e recrutamento; canal ainda sem validação própria | Testar depois de existir prova real de uso e margem para comissão |
| Utilidade gratuita e piloto direto consentido, via #90 | Observação do uso, aprendizado rápido e construção de relacionamento consentido | Custo humano; disponibilidade de público ainda não demonstrada; amostra pode ser mais receptiva que tráfego frio | Usar como apoio qualitativo antes de novo gasto, sem substituir o teste pago |

A combinação escolhida preserva o investimento no produto e no canal já definidos, enquanto reduz
o risco de comprar tráfego para uma experiência ainda difícil de interpretar. O #90 está `RUNNING`,
mas possui **zero contatos consentidos registrados**. Não presumir base disponível, não chamar teste
de agente de cliente real e não transferir suas futuras vendas ao #91. Sua amostra contratual de
100 contatos permanece; uma observação inicial com cinco pessoas não conclui essa atividade.

**Sequência recomendada, com responsáveis e critérios**

| Ordem | Entrega | Responsável na cadeia | Critério para avançar |
| --- | --- | --- | --- |
| 1 | Fechamento factual do #91 e contexto atualizado do plano #3 | Hermes, com suporte de dados do backend; Atena define o próximo teste | Pausa, R$ 27,45, quatro saídas, quatro sessões e ausência de compra coerentes; #90 separado; nenhuma tarefa nova para o vídeo #38 baseada apenas no texto antigo |
| 2 | Primeiro ajuste executável e salvável | Dédalo | Cada combinação atendida devolve o que fazer, como fazer, em qual ocasião e como a cliente verifica se lhe serviu; caminho neutro funcional |
| 3 | Entrada mobile e comunicação que demonstram essa utilidade | Íris; Apolo somente se um ativo audiovisual for necessário | Primeira escolha visível sem rolagem; benefício concreto; demonstração do produto real; transição clara entre grátis e R$ 67 por sete dias de jornada e 90 dias de acesso |
| 4 | Homologação e observação de uso | Homologação técnica, Psique, Têmis e acompanhamento humano consentido | Fluxo local de resultado → e-mail → retomada → checkout de teste → acesso → missão funciona com dados segregados; pessoas aderentes conseguem aplicar o ajuste sem instrução oculta |
| 5 | Novo ciclo pago versionado | Atena define hipótese; Plutus valida economia; autorização humana; Hermes opera | Uma hipótese comercial explícita, preço preservado inicialmente, orçamento autorizado, aquisição e compra atribuíveis, regras de parada persistidas |
| 6 | Primeiras vendas, entrega e aprendizado | Subprocesso Venda, entrega e satisfação da cliente | Cinco vendas líquidas do canal em avaliação, conciliadas e entregues; primeiro uso comprovado, satisfação acompanhada e contribuição positiva |
| 7 | Ampliação gradual e continuidade | Atena, Plutus e Hermes | Repetição de resultado em novas coortes, CAC sustentável e entrega íntegra; aumento de verba exige autorização |

Para a devolutiva, a alternativa de menor risco é um catálogo curado de microações concretas,
combinado às escolhas já existentes e a exemplos aprovados. Preserva custo previsível e evita exigir
mais dados ou conhecimento de IA da cliente. Vídeo personalizado e reconstrução total do aplicativo
não têm justificativa comercial demonstrada neste momento.

Exemplo ilustrativo de direção editorial, ainda não implementado: **“Seu primeiro ajuste para sair
com o que você já tem.”** Apoio: “Faça quatro escolhas e receba uma ação prática para experimentar
hoje. O primeiro ajuste é gratuito.” A prova deve mostrar a ação e o resultado real do produto.
A continuação paga precisa explicar concretamente o que os Dias 2 a 7 acrescentam ao ajuste inicial.

A rodada de observação com cinco pessoas é qualitativa. Registrar se conseguiram começar, entender,
aplicar, salvar e retomar, além do motivo de continuar ou não. Uma devolutiva exibida não basta.
Revisão de Psique ajuda a antecipar problemas; não substitui essa evidência humana nem intenção de
compra. A conexão e o retorno pelo link de acesso merecem atenção, sem atribuir prematuramente a
queda a falha de e-mail.

Os ajustes de qualidade formam uma nova versão do produto. A comparação histórica com #91 não será
um A/B causal se produto e entrada mudarem juntos. Depois de estabilizar essa versão, cada teste
deve alterar uma variável definida: mensagem, público ou oferta, com as demais condições preservadas.
Não dividir o teto pequeno entre vários públicos, preços e criativos ao mesmo tempo.

**Economia do próximo teste**

O custo observado por saída/acesso foi **R$ 27,45 ÷ 4 = R$ 6,86**. Usando apenas como cenário o alvo
de conversão de 5% do experimento, o CAC seria **R$ 137,25**, acima do preço de R$ 67 antes de taxas,
suporte e reembolsos. Não é um CAC realizado: ainda não houve venda.

O plano registra CAC esperado de R$ 25, custo variável de R$ 20 e reembolso de 12%. São premissas,
não resultados validados. Para CAC de R$ 25 com conversão de 5%, o custo máximo por acesso seria
R$ 1,25. Aos R$ 6,86 observados, seriam necessários 27,45% de conversão para esse CAC — uma hipótese
sem comprovação. Comprar 100 acessos ao mesmo custo exigiria aproximadamente R$ 686,25, excedendo
o teto de mídia de R$ 100. O tamanho da amostra desejado não autoriza ampliar o orçamento.

Antes de novo gasto, Plutus deve verificar taxas, tributos aplicáveis, entrega, suporte, comissão,
reembolso e margem de contribuição. O teto de R$ 200 do plano e o de R$ 100 de mídia são limites
distintos, não somáveis. Os R$ 72,55 remanescentes no conjunto são saldo aritmético do orçamento
anterior; não representam autorização para reativar a campanha interrompida.

Cinco vendas a R$ 67 representam R$ 335 de receita bruta, antes de custos e reembolsos. São um marco
inicial de validação, não prova estatística de escala. Para avaliar aquisição paga, as cinco vendas
devem vir do ciclo pago correspondente; vendas do piloto direto têm análise própria.

**Público, criativo e próximos aprendizados**

O anúncio real promete o primeiro ajuste com o que a cliente já tem e usa o perfil
`@produtividade360_`. A audiência configurada é Brasil, 18–65+, interesse “Estilista”, sem restrição
explícita a mulheres. Há uma oportunidade de melhorar a coerência entre identidade pública,
promessa e público B2C. Isso deve ser uma hipótese posterior, sem concluir que homens ou uma faixa
etária causaram o resultado. Na pequena amostra, dois cliques de saída vieram de mulheres 18–24,
um de mulher 55–64 e um de homem 55–64; não há base para eleger segmento vencedor.

Na Meta, o vídeo teve 119 reproduções iniciadas, 51 eventos `video_view` e oito reproduções de 100%.
Essas medidas não são equivalentes a visitantes ou intenção de compra. Priorizar demonstração clara
do ajuste e continuidade da mensagem até a página. Em futura peça para Reels, preservar vídeo
vertical, áudio e mensagens dentro da área segura, conforme a
[orientação da Meta para Reels](https://www.facebook.com/business/ads/facebook-instagram-reels-ads).
Não aplicar benchmarks agregados da plataforma como previsão para o Vega.

Depois de demonstrar utilidade e conclusão dos sete dias, a sequência de produto deve começar pela
retomada do plano pessoal durante os 90 dias já comprados. Uma extensão para ocasiões recorrentes
pode ser testada se clientes reais pedirem ajuda adicional e aceitarem pagar. Assinatura só deve
ser avaliada quando houver necessidade e uso recorrentes; não entra no cálculo atual como receita
futura garantida.

**Evidências e limites**

- [Snapshot agregado, cálculos e origens das consultas](evidencias/vega-experimento-91-2026-09-08.json).
- [Primeira dobra observada em iPhone](evidencias/vega-v7-primeira-dobra-iphone-2026-09-08.png).
- [Experimento 91 no Hub](http://191.252.181.168:5173/experiments/91), abas Saúde da campanha e Comportamento PDE.
- [Cadeia do Vega](http://191.252.181.168:5173/products/4/value-chain-history) e [Plano comercial 3](http://191.252.181.168:5173/planning/3).
- Contratos: [cadeia PDE](../canonical/cadeia-produtos-pde-canon.v1.md), [construção MUSA v7](musa-v7-construction-contract.md), [governança de dados](musa-v7-data-governance.md) e [homologação anterior do #91](../homologacao/experimento-91-abas-operacionais-v1.md).
- Causas verificadas no código: `toAttributedPdeSummary` e `buildKeepMonitoringRecommendation` em [PostDeployMonitorService](../../backend/ads-service/src/main/java/com/marketinghub/experiment/monitoring/PostDeployMonitorService.java); `TrafficSourceMetricBuilder` em [AccessService](../../pde-platform/backend/src/main/java/com/marketinghub/pde/service/AccessService.java); `buildEvidenceSnapshot` em [GrowthOperatorService](../../backend/ads-service/src/main/java/com/marketinghub/growthoperator/service/GrowthOperatorService.java); `localActionsForMission` em [AiGuidanceService](../../pde-platform/backend/src/main/java/com/marketinghub/pde/service/AiGuidanceService.java).

Foram consultados o monitor obrigatório, as telas administrativas, a v7 pública em desktop e mobile,
APIs oficiais do Hub, banco e logs via MCP e a Graph API. Os logs consultados mostram recuperação da
conexão do worker com o backend após uma recusa transitória; não sustentam um bloqueio atual de
publicação. A leitura financeira global agrega por nicho e tem custos incompletos, portanto não foi
usada como lucro histórico reconciliado do Vega.

A observação de navegador usou analytics desligado e bloqueou métodos HTTP de escrita. Não foi
criado diagnóstico, contato, e-mail, compra ou evento de teste em produção. Nenhum código executável,
preço, campanha, orçamento ou configuração do produto foi alterado. Esta análise não substitui a
homologação do fluxo autenticado e de pagamento exigida antes de publicar uma futura correção.
