# Sinais de microconteúdo, continuidade e IA controlada — 2026-08-21

> STATUS: HISTÓRICO
> FONTE CANÔNICA: `docs/canonical/audio-video-studio-canon.v1.md`,
> `docs/canonical/social-media-worker-canon.v1.md`,
> `docs/canonical/solucoes-prontas-ia-trabalho-canon.v1.md` e
> `docs/canonical/cadeia-produtos-pde-canon.v1.md`
> SUBSTITUÍDO POR: —
> ÚLTIMA VALIDAÇÃO: 2026-08-21

## Pergunta comercial

Quais sinais publicados em agosto de 2026 ajudam o Marketing Hub a chegar mais rápido a vendas sem
criar uma plataforma, um agente ou uma frente de produto antes de existir evidência própria?

## Evidências e limites

### Microdramas e consumo seriado

- A Reuters descreveu microdramas verticais de até dois minutos e reportou estimativa da Omdia de
  US$ 1,5 bilhão em receita nos Estados Unidos em 2026, com 66 milhões de usuários ativos mensais.
- A Sensor Tower estimou mais de 850 milhões de downloads globais no primeiro trimestre de 2026,
  alta anual de 140%, e aproximadamente US$ 750 milhões de receita dentro dos aplicativos. A América
  Latina respondeu por 23% dos downloads.
- O sinal comprova escala de entretenimento serial mobile. Não comprova, sozinho, que uma sequência
  vende melhor cursos, PDEs ou outras ofertas do Marketing Hub.

Fontes: [Reuters republicada pela MarketScreener](https://au.marketscreener.com/news/microdramas-boom-in-a-shrinking-hollywood-studios-chase-a-tiktok-audience-ce7859dcd180f026),
[Sensor Tower](https://sensortower.com/blog/state-of-short-drama-apps-2026-report).

### Redução de atrito e relacionamento identificado

- Um caso publicado pela fornecedora e-Plus atribuiu à nova experiência móvel da Lojas Torra ganho de
  velocidade de 3 vezes, aumento de 60% na conversão móvel e crescimento de 20% na participação do
  aplicativo nas vendas digitais.
- É evidência direcional de fornecedor, sem auditoria independente apresentada. O aprendizado seguro
  é reduzir atrito e preservar continuidade com a cliente; não é construir aplicativo nativo por
  padrão nem reutilizar os percentuais como previsão.

Fonte: [Agência e-Plus](https://agenciaeplus.com.br/en/case-lojas-torra-conversao-app-commerce/).

### IA de compra com controle humano

- O estudo PYMNTS/Visa ouviu 2.273 consumidores e 501 comerciantes brasileiros em março de 2026.
  Relatou uso intenso de celular durante compras e maior atividade digital entre usuários de IA.
- O aprofundamento global com 5.241 consumidores indicou preferência por aprovação antes da compra,
  acesso a dados caso a caso, teto de gasto, desligamento, cancelamento em 24 horas e suporte humano.
- O sinal favorece ofertas que vendem economia e confiança e começa por `IA pesquisa/compara ->
  pessoa decide`, em vez de autonomia ampla.

Fontes: [Brazil Playbook](https://www.pymnts.com/consumer-insights/2026/70percent-of-brazilians-expect-to-shop-with-ai-agents-by-2028/),
[Agentic Commerce Deep Dive](https://www.pymnts.com/wp-content/uploads/2026/07/PYMNTS-Intelligence-Global-Digital-Shopping-Index-Agentic-Deep-Dive-July-2026.pdf).

### IA dentro de uma tarefa conhecida

- A Baidu informou crescimento anual de 27,4% na penetração diária das funções de IA dentro do Wenku
  e do Drive em junho de 2026. A receita trimestral de aplicações de IA foi RMB 2,5 bilhões, alta de
  apenas 3%.
- Os números são internos e não auditados. Eles sustentam a hipótese de adoção de IA incorporada a
  fluxos conhecidos, mas não demonstram causalidade entre integração, receita e retenção.

Fonte: [Baidu Q2 2026](https://ir.baidu.com/news-releases/news-release-details/baidu-announces-second-quarter-2026-results).

## Alternativas comparadas

| Alternativa | Benefício | Risco | Custo/esforço | Aderência a vendas | Decisão |
| --- | --- | --- | --- | --- | --- |
| Criar plataforma ou agente de microdramas | Captura integralmente a tendência | Nova frente sem prova própria; alto custo de conteúdo e aquisição | Alto | Baixa no gargalo atual | Rejeitada |
| Tornar todo criativo e PDE seriado | Padronização rápida | Contamina testes e força formato inadequado a ofertas simples | Médio | Média | Rejeitada |
| Adicionar sequência curta como hipótese opcional aos agentes atuais | Testa o mecanismo com ativos, canais e métricas existentes | Exige disciplina de instrumentação e comparação | Baixo | Alta | Escolhida |

## Implementação decidida

1. Apolo e o Estúdio passam a reconhecer microconteúdo seriado como hipótese opcional dentro do
   mesmo `strategyGroupKey`.
2. O Social Media Worker usa uma sequência inicial de três funções: conflito, microrecompensa e
   demonstração com CTA.
3. O gerador de roteiro recebe regra condicional de continuidade quando o contexto indicar série ou
   microsérie; roteiros avulsos preservam o comportamento atual.
4. Soluções prontas de IA passam a exigir envelope visível de controle humano para ações financeiras,
   externas ou irreversíveis.
5. A cadeia PDE reforça continuidade simples pós-compra, sem exigir aplicativo nativo antes de
   recorrência comprovada.

## Métricas e gate

- aquisição: início, retenção em 3 segundos, 50% e 95% por episódio;
- hábito: continuidade para o próximo episódio e retorno em 24 horas e sete dias;
- venda: CTA, checkout, compra, receita e custo da sequência completa;
- entrega: primeiro uso, retorno, conclusão, satisfação, reembolso e nova compra pertinente;
- confiança de IA: recomendações aceitas, aprovações, cancelamentos, correções, incidentes e suporte.

**Continuar:** sequência melhora checkout, compra ou receita contra uma peça única equivalente.

**Ajustar:** retenção cresce, mas a pessoa não avança ao CTA ou à compra.

**Parar:** instrumentação não reconstrói a jornada, custo ultrapassa o teto, valor é retido de forma
enganosa ou automação executa ação sensível fora do envelope aprovado.

Para o experimento #88, esta hipótese não deve substituir a linha de base enquanto o funil comercial
não receber eventos confiáveis. O primeiro uso é em próximo experimento ou nova versão explicitamente
aprovada, com uma variável por vez.
