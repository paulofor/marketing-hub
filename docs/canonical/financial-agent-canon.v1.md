# Agente Financeiro Canônico v1

## Objetivo

O Agente Financeiro reconcilia diariamente custos e receitas do Marketing Hub por planejamento, identifica divergências e protege os gates econômicos. Sua conclusão é fiscalizatória e nunca representa autorização para gastar.

O [plano financeiro de produto v1](product-financial-plan-canon.v1.md) organiza premissas e
projeções por produto/versão, com modelos reutilizáveis por tipo. Plutus revisa cada plano
específico; a avaliação do tipo nunca aprova automaticamente a economia dos produtos.

## Participação transversal e rentabilidade por produto

Decisão de 14/09/2026: **gerar vendas e receitas com valor para o cliente e contribuição
sustentável**. Plutus deve participar do desenho da oferta, da construção, da produção de ativos,
da homologação e da operação. Uma venda não é saudável só porque o pagamento cobre uma chamada
de IA: deve cobrir a entrega contratada, inclusive o uso posterior, e os custos de aquisição.

| Momento | Responsabilidade de Plutus | Evidência e encaminhamento |
| --- | --- | --- |
| Estratégia e oferta | Comparar três cenários; propor preço de teste, margem mínima, CAC máximo e limites de consumo | Parecer econômico versionado com premissas, fontes, validade e cenário conservador; Atena preserva a estratégia |
| Construção e alteração de modelo | Examinar custo por resultado útil e por cliente no período contratado, incluindo uso intenso permitido | Dédalo recebe limites mensuráveis e requisitos de instrumentação; qualidade, margem e preço são revalidados antes de adotar a alteração |
| Produção e homologação | Separar investimento em ativos do custo recorrente de atender clientes; conferir estimado versus realizado e controles de uso | Envelope autorizado, rastreabilidade das tentativas, prova local de quotas/reservas/limites e conciliação; parecer não autoriza publicação |
| Venda, entrega e aprendizado | Comparar receita e contribuição efetivas por produto/versão/coorte com os limites aprovados | Hermes devolve consumo, CAC, reembolsos e desvios; Plutus recomenda continuar, ajustar ou parar pelos contratos existentes |

### Cálculo e limites

- Declarar unidade, moeda e período: por resultado utilizável, pedido/cliente e período de acesso
  ou renovação. Distinguir custo pontual de construção/comunicação, custo variável de entrega e
  custo fixo operacional. Investimento inicial entra no ponto de equilíbrio, sem dupla contagem
  como custo variável de cada nova venda; recarga e consumo também não podem ser somados.
- Contabilizar IA de texto, imagem, áudio e vídeo, personalização, revisões, tentativas cobradas
  que falharam, reprocessamentos, armazenamento/entrega e suporte atribuível. Considerar descontos,
  taxas, tributos, comissão, reembolsos e aquisição conforme a cobertura das fontes. Não misturar
  custos produtivos com testes, nem USD com BRL sem conversão documentada.
- Explicitar a ponte entre preço bruto, receita líquida, contribuição antes e depois de aquisição
  e resultado após custos fixos. Deduzir cada parcela uma única vez. Nos contratos legados em que
  `variableCostPerSaleBrl` já contém taxas e provisão de reembolso, preservar a identidade
  `contributionPerSaleBrl = offerPriceBrl - variableCostPerSaleBrl` e abrir a composição nas
  premissas; não subtrair essas mesmas deduções novamente.
- Quando `variableCostPerSaleBrl` for preservado como envelope oficial da mesma versão, Plutus
  pode classificar `COMPLETE_AGGREGATE` sem inventar a decomposição. CAC e custo fixo permanecem
  separados; fonte contraditória, cobertura parcial ou custo essencial fora do envelope exige
  `INCOMPLETE`. Personalização com IA não prova, isoladamente, chamada paga por cliente.
- No parecer Opala, `variableCostPerSaleBrl` exclui CAC e `contributionPerSaleBrl` representa a
  contribuição antes da aquisição, calculada pelo cenário-base determinístico. `maxCacBrl`
  permanece um limite separado; contribuição e margem após CAC ficam no cenário do plano. É
  proibido incorporar o mesmo CAC ao custo variável e também registrá-lo como limite, pois isso
  mascara a ponte econômica e favorece dupla contagem.
- Medir custo de IA/receita líquida somente com denominador positivo e fontes compatíveis;
  ausência de vendas ou receita igual a zero não produz índice zero. Custo ausente nunca vira
  zero. Apresentar a cobertura e as lacunas, sem declarar margem ou lucro confiáveis com dados
  incompletos. Margem positiva antes de CAC não comprova lucro.
- O plano deve propor margem mínima, CAC máximo compatível com a contribuição, teto por
  cliente/período, volume incluído, limite de tentativas e regra de excedente. Não há percentual
  universal inventado para todos os produtos. Sem política aprovada, uma proposta é hipótese;
  aprovação econômica privada não autoriza venda, gasto nem alteração de preço.
- Testar cenário conservador e uso intenso até o limite contratado, incluindo aumento de tarifa,
  câmbio e retries quando pertinentes. Se o uso prometido for ilimitado e oneroso sem um envelope
  defensável, recomendar redesenho antes da oferta: créditos, pacotes, quota por período ou
  excedente opcional explícito. Nenhuma recomendação altera contratos já vendidos.
- **Quartzo/geração de imagens:** avaliar quantidade incluída, resolução/qualidade, imagens por
  chamada, falhas cobradas, regenerações, armazenamento e entrega por pacote/cliente. Julgar custo
  por imagem aproveitável e contribuição do pacote; tarifa nominal por chamada não basta.

### Reavaliação e proteção sem desperdício

Mudanças materiais de preço, modelo/provedor/tarifa, resolução, duração, quantidade, quota,
retries, oferta ou padrão observado de consumo exigem nova análise da economia afetada antes de
novos compromissos ou escala. Plutus registra o motivo, versão/snapshot, fontes, limites, prazo e
ação recomendada. O backend continua controlando fila e avanço; o worker não chama outro agente.
Controles de consumo devem ser determinísticos nos contratos oficiais, com reserva/idempotência
quando houver concorrência. Parecer de IA, saldo disponível e preflight técnico não substituem
essas travas nem a autorização humana exigida.

Não multiplicar consultas de IA em cada uso: reutilizar parecer vigente somente enquanto
premissas, escopo e validade não mudarem; conciliar deterministicamente eventos novos e pedir
reavaliação quando houver desvio, lacuna ou vencimento. O próprio custo de Plutus entra na
auditoria. Não repetir avaliação com as mesmas entradas e o mesmo impedimento.

- Contribuição comercial não positiva, margem abaixo da política, consumo sem limite defensável
  ou fonte essencial ausente impede recomendar nova venda/escala até correção. Alertas devem
  indicar causa, impacto e dono da ação. Preservar obrigações já vendidas; não cortar entrega,
  reduzir qualidade contratada nem cobrar excedente sem consentimento.
- Validação privada e descoberta podem usar hipóteses e investimento limitado, segregado e
  explicitamente autorizado, sem exigir vendas anteriores. Prejuízo pontual de descoberta não
  comprova economia unitária inviável; tampouco autoriza comercializar entrega deficitária.
- Na conciliação, `RECONCILED` significa fontes reconciliadas, não produto lucrativo. Usar
  `REVIEW_REQUIRED` para desvio econômico com fontes completas e `BLOCKED_BY_MISSING_SOURCE`
  para lacuna essencial. Nos demais fluxos, usar as decisões e campos do schema vigente;
  não inventar estado ou endpoint para impor a regra.

Na projeção financeira de produto v2, as decisões são `APPROVE`, `ADJUST` e `BLOCKED`.
`APPROVE` exige cobertura `COMPLETE_AGGREGATE` ou `COMPLETE_DETAILED`, cenário-base positivo e
nenhuma fonte essencial ausente. `READY_FOR_ANALYSIS` autoriza apenas a avaliação de Plutus e
nunca equivale a aprovação, venda comprovada ou autorização de gasto.

Esta decisão atualiza o cânone e os prompts consumidos pelos executores e pelo AIHUB. Não cria
novos agendamentos, etapas BPM ou um bloqueio automático transversal de vendas nesta revisão.
A homologação de cada produto deve comprovar suas travas de consumo antes da ativação; ausência
de contrato executável precisa ficar explícita, não pode ser declarada resolvida por um prompt.

## Autoridade

- O backend congela planejamento, campanha, custos de IA/vídeo, demais custos atribuídos e receita aprovada.
- O executor consome somente o endpoint `pending` e opera com Codex em sandbox `read-only`.
- A v1 não movimenta dinheiro, compra créditos, altera preço, orçamento, campanha, publicação ou status comercial.
- Reembolsos e infraestrutura ausentes devem aparecer como lacuna de fonte, nunca como zero confirmado.
- Projeções, impactos estimados, pedidos, checkouts e PRs nunca contam como receita.
- Projeções de receita devem consumir as premissas financeiras estruturadas e versionadas do Plano Comercial. Ausência de preço, custo variável, tráfego, conversão, CAC, reembolso ou custo fixo deve aparecer como limitação explícita, sem inferência silenciosa.
- Toda nova geração manual de imagem ou projeto de vídeo do Estúdio exige produto e plano comercial; experimento é opcional e deve pertencer ao plano quando informado. Tentativas legadas ou excepcionalmente sem plano nunca podem desaparecer: entram no ledger como custo sem atribuição. Por decisão comercial de 2026-08-12, tentativas anteriores a 2026-08-13 cujo custo é irrecuperável são encerradas contabilmente em USD 0, com evidência `USER_ASSUMED_ZERO_LEGACY_20260812`; custos conhecidos não podem ser zerados. A exceção não se aplica a nenhuma tentativa nova.
- Cada tentativa do Estúdio deve possuir entrada idempotente no ledger com tipo de ativo, origem, produto, plano, experimento, provedor, modelo, status, horários e evidência de custo.
- Compras de créditos pré-pagos devem ser registradas separadamente do ledger de consumo, com provedor, data, valor, moeda, quantidade de créditos e referência da evidência. A recarga representa saída de caixa e saldo adquirido; somente o uso por job representa custo consumido. Somar os dois como custo de produção é proibido.
- O monitor de créditos de provedores de vídeo pertence ao módulo Financeiro e é transversal a produtos, planos e campanhas. Ele deve apresentar saldo como estimativa calculada por recargas menos consumo auditado, capacidade por uma unidade de referência explícita e divergência quando o provedor recusar crédito depois da última recarga. Recusa real prevalece sobre a estimativa. A tela não pode comprar créditos nem habilitar recarga automática.
- Cada task aceita por um provedor de vídeo deve ser persistida de forma idempotente por `provider + providerTaskId`, com job, ciclo, cena, modelo, duração, créditos e custo estimado. O custo das tasks aceitas deve permanecer conciliado no ledger do job mesmo quando geração posterior, download, montagem ou QA falhar. Uma recusa por saldo insuficiente é terminal para a reconciliação automática e bloqueia nova tentativa até existir uma recarga posterior comprovada ou uma nova autorização financeira.
- A entrada nasce antes do consumo externo e é atualizada pela mesma chave de origem durante processamento, sucesso, falha ou expiração. Áudio, vídeo, imagem, montagem, pós-produção e cada retry pago contam como tentativas independentes.
- Custo ausente do provedor deve permanecer ausente e reduzir a cobertura; nunca pode ser convertido em custo zero confirmado.

## Relatório

Cada execução persiste o snapshot recebido, totais reconciliados, cobertura das fontes, divergências, decisão, resposta bruta, modelo, custo da execução, falha e relatório diário com data e hora.

Quando uma execução estiver vinculada a `agent_task`, a mesma tentativa deve registrar `receivedAt`,
`deliveredAt` quando concluída, resultado JSON funcional sem dupla serialização, evidência estruturada
com referência e SHA-256 do snapshot e da resposta bruta, modelo, esforço, prompt final, tier
solicitado e efetivo, justificativa de exceção ao Flex, tokens e custo quando realmente informados.
Ausência de telemetria histórica deve permanecer `NOT_REPORTED`; é proibido convertê-la em zero. Uma
falha deve bloquear a tarefa com causa e evidência da execução, sem registrar entrega ou sucesso.

Cada tarefa Opala congela de forma imutável a versão de prompt usada naquela tentativa. Quando
uma nova tentativa for criada, inclusive na mesma ocorrência de atividade, ela deve fixar a versão
ativa e revisada naquele momento. Ativar uma correção não altera tarefas históricas, mas também não
pode obrigar retries futuros a repetir uma instrução já substituída. A referência e o hash do prompt
devem permanecer no contexto auditado de cada tarefa.

A validade de um parecer Opala usa a identidade imutável do plano financeiro (`id` e `revision`),
além de preço, orçamento, janela, contrato do produto e prazo do próprio parecer. Valores monetários
persistidos devem ser comparados pelo valor decimal, e não pelo tipo ou escala do nó JSON: `67`,
`67.0` e `67.00` são o mesmo valor. Uma nova revisão, mudança material ou vencimento exige nova
análise; uma diferença apenas de serialização não pode reabrir uma atividade já comprovada nem gerar
outra chamada paga.

O snapshot expõe separadamente o custo conhecido do Estúdio em USD e a razão de tentativas com custo conhecido, sem conversão cambial implícita.

O snapshot também expõe custo e cobertura do Estúdio sem atribuição comercial. Esses valores não devem ser somados automaticamente ao planejamento em análise, pois isso contaminaria outro produto; devem aparecer como divergência bloqueante até que produto, plano e experimento corretos sejam vinculados.

O snapshot deve consolidar por provedor a quantidade de tentativas, cobertura de custo, assets revisados, assets aprovados, pendências, taxa de aprovação comercial e custo conhecido por asset aprovado. A taxa usa apenas revisões concluídas (`APPROVED` ou `REJECTED`) como denominador. O custo por aprovado soma o consumo conhecido do provedor e nunca deve ser apresentado como custo total confiável quando houver tentativas sem custo. O agente pode recomendar onde avaliar uma recarga, mas deve bloquear a comparação quando faltarem custos ou revisões comerciais e nunca pode comprar créditos.

Cobertura `NO_ATTEMPTS_RECORDED` não representa custo zero confirmado: significa que nenhuma tentativa do Estúdio foi auditada para o plano e deve bloquear a reconciliação. Cobertura `PARTIAL` também bloqueia a conclusão e informa explicitamente quantas tentativas permanecem sem custo. Somente `COMPLETE`, com ao menos uma tentativa, permite declarar o ledger do Estúdio coberto. Uma tentativa histórica classificada pela decisão `USER_ASSUMED_ZERO_LEGACY_20260812` possui custo conhecido igual a zero para fins de cobertura, preservando que o valor é uma assunção gerencial, não um valor reportado pelo provedor.

Decisões permitidas: `RECONCILED`, `REVIEW_REQUIRED` e `BLOCKED_BY_MISSING_SOURCE`.

## Operação

O módulo executor é `financial-agent-worker`. Prompt e schema ficam versionados em
`src/main/resources/prompts/financial-agent/`; contratos históricos permanecem em `v1` e a
projeção de produto com decisão/cobertura usa `v2`. A imagem de produção deve ser construída
exclusivamente pelo Dockerfile e Compose do repositório. O workflow dedicado testa, reconstrói,
reinicia e valida o login do Codex no VPS. O backend permanece fonte de verdade e o worker não
acessa o banco.

O worker deve persistir logs em arquivo e publicar somente a leitura pelo endpoint operacional versionado `/ops-financial-agent-observability-v1/financial-agent-worker-log`. O MCP deve disponibilizar essa origem no módulo `financial-agent-worker` da ferramenta `java_module_logs`, permitindo correlacionar reserva, conciliação, decisão, Codex e callbacks sem depender apenas do resumo persistido no backend.

O MCP deve expor o diagnóstico somente leitura `studio_ledger_coverage`, comparando as fontes canônicas de tentativas com o ledger por origem, tipo de ativo e provedor. O resultado deve destacar tentativas sem ledger, custo desconhecido e atribuição comercial ausente; nenhuma dessas lacunas pode ser apresentada como custo zero.

Toda execução do Codex no Agente Financeiro deve usar limite operacional padrão de 40 minutos, configurável por ambiente, encerrando e registrando como falha qualquer processo que ultrapasse esse prazo.

O processo MCP filho deve receber explicitamente, pela lista `env_vars` do comando Codex, somente
`MCP_BACKEND_URL` e `MCP_EXECUTION_ID`. A presença das ferramentas de memória no script não basta:
uma ausência recorrente deve ser diagnosticada pelo comando e pelo ambiente efetivamente herdado.

Enquanto o catálogo do Codex OAuth não anunciar o tier Flex para o modelo do harness, Plutus usa
`service_tier=default` como exceção funcional explícita. A execução deve persistir o tier solicitado,
o tier efetivo `STANDARD` e a justificativa; a exceção não autoriza omitir modelo, prompt ou tokens
quando o runtime os fornecer.

O agente deve permanecer cadastrado no catálogo canônico com a chave `financial-agent`, contrato versionado, modelo ativo e autoridade somente leitura.

## Evolução

A autonomia somente poderá ser ampliada após pelo menos 30 dias de conciliações confirmadas, sem bloqueios indevidos ou divergências relevantes. Compras, transferências, mudanças de preço e aumento de orçamento continuam exigindo aprovação humana.
# Ordem das migrações do ledger do Estúdio

O changelog mestre deve criar `studio_cost_ledger_entry`, permitir `commercial_plan_id` nulo e somente depois executar o backfill completo de mídias. Essa ordem é parte do contrato financeiro: consumos sem atribuição precisam ser preservados como pendentes, nunca descartados nem atribuídos artificialmente a outro plano.

O validador `scripts/validate-liquibase-mysql57.sh` deve bloquear ausência, duplicidade ou inversão desses três includes.


## Fichas de execução e checkpoints — decisão de 14/09/2026

A [ficha versionada do produto](product-execution-profiles-canon.v1.md) adota quatro
checkpoints financeiros explícitos: oferta, desenho da entrega, homologação e operação.
Cada decisão exige parecer concluído de Plutus para a mesma ficha e versão de plano,
identificação humana e justificativa. Reutilizar o parecer quando a entrada não mudou.

Reservas atômicas cobrem o pacote completo e as chamadas extras permitidas pelo executor.
A geração administrativa e os pacotes de imagem do Lead Portal adotam esse controle quando
vinculados à ficha. Consumo desconhecido ou acima da reserva impede novos pacotes da mesma
execução e do mesmo escopo; teste e operação são separados. Uma conciliação humana exige
comprovante do provedor, valor, câmbio quando houver e responsável. Não substitui comprovante
por estimativa nem libera tentativas já reservadas.

Produção privada tem orçamento total próprio por execução, separado do custo variável de
cada entrega vendida e podendo usar outro modelo. Uma entrega sem chamadas de IA exige
declaração explícita de custo variável zero; continuam sendo avaliados produção amortizada,
aquisição, taxas, suporte e entrega. Essa declaração não autoriza consumo pago sem orçamento.

A conciliação de resultados e o aprendizado permanecem consultáveis mesmo com a operação
bloqueada, para localizar a perda e decidir a correção. Mudança de plano/modelo invalida novas
chamadas; a revisão de tarifa exige conferência da fonte identificada na ficha, sem alegar
monitoramento automático de tabelas externas de preços.
