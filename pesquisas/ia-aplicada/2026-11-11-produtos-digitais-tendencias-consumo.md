# Radar de produtos digitais e tendências de consumo — 11/11/2026

**Janela de verificação:** 11/11/2026, com informações publicadas de 04 a 10/11 e medições datadas. **Critério:** fatos novos ou atualizações quantitativas materialmente relevantes; evitar replicar os relatórios anteriores. **Síntese:** quatro sinais, dois com contagens de uso ou de pagamentos, um lançamento de precificação ainda sem renovação e uma funcionalidade B2B em beta sem adoção externa mensurada.

## Resumo executivo

1. **Postiz: monetizar a operação gerenciada de um produto aberto e acessível a agentes.** Subiu de aproximadamente US$ 352 mil de MRR (02/11) para US$ 390 mil (09/11), com cerca de 11,5 mil assinaturas. O canal declarado é comunidade de código aberto/Reddit, não CAC auditado.
2. **Uber: uso real de agentes para fechar pedidos continua marginal em tarefas cotidianas.** A empresa relatou cerca de 500 pedidos Uber Eats por agentes na semana do Halloween e somente quatro corridas intermediadas por agentes durante todo o terceiro trimestre: **períodos diferentes**, que não devem ser misturados. Trata-se de atualização quantitativa mais precisa do que os relatos qualitativos anteriores.
3. **Grindr: experimentar um patamar de assinatura muito mais alto.** A empresa já monetiza 1,4 milhão de usuários pagantes em média no Q3 e iniciou em 10/11 o acesso antecipado ao Edge, anunciado por US$ 120/mês. Quase 400 mil registros de interesse em 72 horas foram declarados, mas **não são assinaturas pagas**.
4. **Claude Meetings: vender a execução durante o trabalho, e não apenas o resumo após ele.** Anunciado em 10/11 como beta para empresas no Google Meet, sem divulgação de clientes adicionais ou receita incremental.

## 1. Postiz — código aberto como aquisição, infraestrutura gerenciada como produto

**Medições:** AI Receipts/TrustMRR, dados vinculados ao processador de pagamento; pontos de observação em 02/11 e 09/11.

| Indicador | 02/11/2026 | 09/11/2026 |
| --- | ---: | ---: |
| MRR estimado | US$ 352.049 | US$ 390.019 |
| Assinaturas ativas | 10.324 | 11.468 |

A mudança é de cerca de **+10,8% de MRR em sete dias**, calculada a partir de duas leituras, **não** crescimento consolidado de receita realizada, margem ou retenção. A receita acumulada indicada na última leitura é próxima de US$ 1,81 milhão, indicador diferente de MRR. O produto funciona como agendador/publicador para mais de 30 canais e fornece interface, API, CLI e MCP para agentes. Os planos em nuvem anunciados custam aproximadamente US$ 29 a US$ 99 por mês, com teste de sete dias. A versão autogerenciável do código aberto convive com o serviço hospedado.

- **Público pagante:** gestores de mídias sociais, equipes de marketing, desenvolvedores de automações e agências.
- **Monetização observada:** planos de hospedagem e conveniência operacional por assinatura, não apenas cobrança por acesso a um LLM.
- **Aquisição declarada pelo fundador:** comunidade open source, GitHub, Reddit, conteúdo técnico e integrações com agentes; a parcela de novas assinaturas atribuída a cada canal não foi medida publicamente.
- **Mecanismo proposto, não causalmente provado:** ferramentas abertas podem ampliar distribuição e confiança; integrações passam a criar uma necessidade operacional recorrente para quem não quer manter a infraestrutura.
- **Riscos:** alto custo de suporte, dependência de APIs sociais, políticas de publicação automatizada e manutenção do projeto aberto. Não inferir lucro nem atribuir todo o crescimento ao MCP.

**Experimento para o Marketing Hub:** abrir uma capacidade estreita (ex.: relatório de diagnóstico de campanhas) com CLI, API ou versão local limitada, e cobrar pelo serviço gerenciado e integração segura com contas de clientes. Medir ativação do software gratuito → assinatura, CAC técnico, tickets, churn, margem operacional, ações efetivamente aprovadas. Não copiar a escala do Postiz como projeção.

Fontes: [AI Receipts — Postiz, medições e histórico](https://theaireceipts.com/case-studies/postiz/); [Postiz — integração com agentes](https://postiz.com/agent); [Postiz — preços e limites](https://postiz.com/kimi).

## 2. Uber e DoorDash — intenção de compra por agente versus transação concluída

**Evento:** apresentação de resultados da Uber em 04/11; cobertura inicial The Verge na mesma data, com repercussão em 10/11. DoorDash forneceu comentário executivo em reunião de resultados no final de outubro.

- **Uber Eats:** aproximadamente **500 pedidos via agentes externos durante a semana do Halloween de 2026**. No mesmo período houve cerca de **320 milhões de transações combinadas da Uber** (corridas + pedidos); o denominador não é somente pedidos de refeições.
- **Uber corridas:** apenas **quatro corridas intermediadas por agentes externos durante todo o terceiro trimestre**, não na mesma semana do Halloween.
- **DoorDash:** CEO afirmou haver apenas algumas centenas de pedidos acumulados por agentes de terceiros, frente ao volume muito maior do serviço convencional. A duração desse acumulado difere da janela da Uber.

**O que funciona, hoje:** os apps próprios das plataformas continuam sendo o canal de volume para pedidos padronizados; os números não demonstram que um agente independente aumente as compras. A interpretação plausível é que uma jornada já otimizada para repetir pedidos pode oferecer menos ganho marginal com um assistente, mas o estudo **não isolou esse mecanismo**. A adoção de bots também depende de integrações e acesso de plataformas terceirizadas.

- **Público:** consumidores de mobilidade e entrega; parceiros comerciais que pretendem vender via agentes.
- **Monetização existente:** taxas de intermediação, entrega e outras receitas próprias das plataformas, não receita por agente explicitada.
- **Aquisição:** apps existentes, marca e distribuição das próprias plataformas; a origem de pedidos agênticos não foi detalhada por plataforma de IA.
- **Oportunidade:** usar agentes para tarefas em que verificar múltiplas condições e fornecedores economiza esforço, sem substituir o checkout direto nas compras muito simples. Testar comparação assistida, recomendações e aprovação final humana, conciliando pedidos e custo de inferência.

**Evidência e limites:** fala de executivos sobre transações internas, relatada pela imprensa; não é uma amostra geral do comércio. Há diferença entre agente que **faz o pedido**, ferramenta de IA que **recomenda** e chatbot que **remete tráfego**. Este achado **atualiza quantitativamente** o tema já tratado no card de 01/11, sem justificar um card duplicado.

Fontes: [The Verge — Uber, 04/11](https://www.theverge.com/transportation/1026306/uber-ai-agents-q3-earnings-disintermediation); [Business Insider — Uber/DoorDash, 10/11](https://www.businessinsider.com/ai-agents-drive-few-uber-doordash-orders-despite-hype-2026-11).

## 3. Grindr — assinatura de alto tíquete e qualificação de interesse antes de vender

**Resultado econômico:** comunicado de 05/11, referente ao terceiro trimestre encerrado em 30/09/2026: receita total de aproximadamente **US$ 141,4 milhões (+22% a/a)**, cerca de **1,4 milhão de usuários pagantes médios (+11%)** e receita média mensal por pagante **US$ 27,15 (+10%)**. A empresa atribui parte do desempenho dos planos existentes a otimização da oferta e do paywall, mas não publicou os resultados de cada teste individual.

**Lançamento:** acesso antecipado ao **Edge** em 10/11, com lançamento mundial previsto para 17/11 e preço introdutório de **US$ 120/mês**, segundo teleconferência de 05/11. O CEO relatou **quase 400 mil registros de interesse em 72 horas** após abrir a manifestação de interesse. Registro de interesse não é venda, renovação nem retenção; o produto completo ainda não havia sido lançado globalmente em 11/11.

- **Público:** parcela de usuários muito engajados do aplicativo de encontros que valoriza funcionalidades premium personalizadas.
- **Monetização:** planos de assinatura existentes, compras no app, publicidade; Edge adiciona nível premium de alto tíquete.
- **Canais:** distribuição no app e sua base instalada são conhecidos, mas não foi apresentado CAC por canal, e o número de manifestações de interesse não prova conversão.
- **Mecanismo hipotético:** segmentar usuários de alta disposição a pagar, em vez de elevar preço para toda a base. A exclusividade deve ser avaliada junto a riscos de segmentação desigual, privacidade e valor real da entrega.

**Ideia comercial:** testar planos premium por utilização intensiva e valor adicional demonstrável para um micro-SaaS, mantendo uma opção básica viável. Medir proporção de interessados → checkout → primeira cobrança → três renovações, reclamações, margem e cancelamentos; não extrapolar o ticket de uma rede social madura para produto novo sem marca e base instalada.

Fontes: [Grindr — comunicado de resultados, 05/11](https://investors.grindr.com/news/news-details/2026/Grindr-Inc--Reports-Third-Quarter-2026-Revenue-Growth-of-22-Raises-2026-Guidance-and-Provides-Initial-2027-Outlook/default.aspx); [teleconferência, 05/11, transcrição](https://www.fool.com/earnings/call-transcripts/2026/11/06/grindr-grnd-q3-2026-earnings-call-transcript/); [registro financeiro SEC, números do trimestre](https://financialfilings.com/filings/grindr-inc/interim-quarterly-report/2026/75990832/).

## 4. Claude Meetings — IA atuando enquanto a conversa ocorre

**Anunciado em 10/11/2026:** Claude Meetings, beta da Anthropic inicialmente no Google Meet para planos Team e Enterprise. Participa explicitamente da reunião, responde a solicitações, consulta dados conectados e prepara planos, prazos, quadros e outros materiais durante a discussão. A empresa também apresentou Claude Whiteboards e Claude Sheets. A disponibilidade e o produto foram demonstrados; **não há adoção externa, pagamentos incrementais, produtividade verificada ou CAC publicados**.

- **Público-alvo:** equipes de conhecimento, gestão de projetos e empresas que fazem reuniões operacionais.
- **Modelo de monetização:** oferta dentro dos planos corporativos da Anthropic; não há preço avulso ou contribuição isolada da funcionalidade.
- **Aquisição:** distribuição para clientes empresariais da plataforma, material de lançamento e beta; conversões não informadas.
- **Hipótese:** produzir tarefas e artefatos ainda durante uma interação reduz a etapa manual de transcrição de decisões em tarefas; **falta evidência externa** de tempo poupado e adoção.
- **Ideia pouco explorada:** construir assistentes especializados em transformar um evento de trabalho em entregas verificáveis (por exemplo, orçamento aprovado, plano de obra, ata estruturada, checklist de conformidade), cobrando por evento ou por equipe, com permissões e consentimento explícitos.

**Experimento:** medir eventos concluídos/aceitos, horas humanas revisadas, taxa de correções, reaberturas, custo computacional por reunião e disponibilidade de dados autorizados. Evitar gravação/transcrição sem consentimento.

Fontes: [Reuters — Claude Meetings, 10/11](https://www.reuters.com/technology/anthropic-brings-claude-google-meet-tackle-tasks-live-2026-11-10/); [TechCrunch — anúncio, 10/11](https://techcrunch.com/2026/11/10/anthropic-launches-claude-meetings-to-turn-workplace-discussions-into-plans-and-tasks/).

## Priorização aplicada ao Marketing Hub

| Experimento possível | Evidência que inspira | Medição indispensável | Prioridade |
| --- | --- | --- | --- |
| Serviço gerenciado com API/MCP para capacidade específica | Postiz possui clientes pagantes e crescimento recente | Conversão de uso gratuito, margem, churn e custo de suporte | Alta |
| Assistência à decisão com checkout convencional | Baixo volume real de pedidos por agentes em Uber/DoorDash | Pedidos pagos, CAC, abandono e custo de execução | Alta |
| Plano premium restrito a casos de valor claro | Grindr tem base pagante e teste de demanda por Edge | Pagamento e renovação, não cadastro de interesse | Média |
| Assistente que transforma reuniões em tarefas | Produto beta sem tração externa divulgada | Ações aceitas, economia revisada e consentimento | Exploratória |

**Interpretação geral:** receitas recorrentes aparecem ao redor da integração e operação confiável de IA, enquanto o uso comercial autônomo por consumidores ainda tem fricções. Nenhum caso prova, isoladamente, qual mecanismo causou crescimento, ou que um produto semelhante teria tração no mercado brasileiro.

## Avaliação de cards — nenhum criado

Conferido o guia `harness-library-api/docs/guia-uso-api-cards.md` na branch `main` em 11/11/2026: coleções permitidas `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

**Decisão editorial:** não criar card nesta rodada. Postiz, Grindr e Claude Meetings apresentam observações de produto e/ou monetização, sem mecanismo comportamental ou audiovisual isolado suficientemente demonstrado. O caso Uber reforça a tese do card `agentes-compra-friccao-checkout` publicado em 01/11, e geraria duplicação sem uma hipótese nova que justifique outra versão agora. Nenhuma fonte de card e nenhum JSON foram gerados; não foi executado cadastro de DRAFT via API.

**Rastreabilidade:** distinguir emissão da notícia, data da medição e janela econômica. Referências externas acima são evidências daquilo que declaram e não resultados experimentais do Marketing Hub.
