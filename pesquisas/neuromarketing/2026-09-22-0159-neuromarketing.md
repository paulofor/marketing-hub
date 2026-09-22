# Radar de Neuromarketing e Desejos Digitais — 22/09/2026 01:59

## Resumo executivo

Esta rodada encontrou dois sinais que considero suficientemente úteis para virarem cards do Marketing Hub e um terceiro sinal criativo que vale monitorar sem transformar em regra geral.

O achado mais forte é peer-reviewed e trata de **freemium, trials e recorrência**: quatro estudos de campo mostram que benefícios premium esperados/experimentados e hábitos formados no premium se relacionam com conversão e retenção, mas também revelam efeitos não lineares que tornam inadequada a regra simplista “quanto mais hábito, melhor”.

O segundo sinal vem de uma pesquisa global da Kearney com **21.500 consumidores em 20 países, incluindo o Brasil**. O estudo sugere que escolha, conveniência e personalização acumuladas podem transferir trabalho de comparação e verificação ao consumidor. A aplicação proposta não é retirar autonomia, mas **comprimir a primeira camada da decisão e manter alternativas acessíveis**.

Também apareceu um estudo Omnicom Advertising + Snapchat sobre humor entre usuários de 13–28 anos. Ele reforça humor como linguagem de pertencimento e atenção, mas os resultados comerciais divulgados são autorrelatados, o Brasil não está na amostra e os percentuais destacados vêm da Índia; por isso não criei card.

Foram criados os cards `trial-habito-beneficio-premium` e `compressao-escolha-sem-remover-agencia`. Nenhum POST manual foi feito para a API; os arquivos ficaram na `main` para o fluxo normal de criação de `DRAFT`.

## 1. Trial e recorrência: hábito ajuda quando está ligado ao benefício premium, mas “mais hábito” não é uma regra universal

### Evidência encontrada

Artigo peer-reviewed publicado em 21 de setembro de 2026 no *Journal of the Academy of Marketing Science* reuniu quatro estudos de campo com um jornal regional europeu que usa modelo freemium.

No Estudo 1 (`N=522`), benefícios esperados do premium mediaram a relação entre hábitos na versão gratuita e adesão ao trial gratuito. Uma análise de robustez encontrou ainda uma relação direta em U invertido entre hábito no gratuito e conversão ao trial: níveis moderados se associaram a maior conversão, enquanto níveis muito altos voltaram a cair.

No Estudo 3 (`N=358`), a formação de hábitos específicos no premium e os benefícios efetivamente experimentados durante um trial gratuito se relacionaram com a transição para um trial pago com desconto.

O Estudo 4 é o mais interessante para o Marketing Hub porque usa comportamento real de retenção. Foram `N=2.434` usuários de um trial de seis semanas por €0,99, que passava para €12,99/mês se não fosse cancelado. Após o trial, 46,8% estavam no preço cheio; um mês depois, 36,5% da amostra original permanecia assinante; ao final de 32 semanas, 21,7% permanecia.

O índice de hábito formado durante o trial se associou positivamente à retenção em preço cheio no curto e no longo prazo. Porém, modelos quadrático e spline encontraram uma relação em U invertido: a probabilidade prevista de retenção aumentava do hábito baixo ao moderado e caía nos níveis mais altos.

### Desejo/comportamento revelado

A evidência sugere que **experimentar valor premium repetidamente** importa mais do que simplesmente liberar acesso barato. Também mostra que hábito não deve ser tratado como objetivo isolado: o que importa é que a rotina esteja ligada a benefícios diferenciados e a uma relação sustentável com a oferta.

Não podemos afirmar que uso intenso causa churn. Os autores levantam “trial hopping” como possível explicação para a queda nos níveis mais altos de hábito, mas esse mecanismo não foi testado diretamente.

### Por que importa para o Marketing Hub

Se o Marketing Hub vier a trabalhar com produtos recorrentes, trials, comunidades, ferramentas ou assinaturas, o funil não deveria otimizar apenas `trial_started`. Ele precisa observar:

- quais ações premium o usuário realmente executou;
- se o usuário percebeu o benefício específico daquela ação;
- regularidade de uso durante o trial;
- conversão para preço cheio;
- retenção depois de um ou mais ciclos pagos;
- cancelamento, reembolso e reclamação.

### Aplicação possível

Criar um `PremiumTrialActivationPlan` para produtos recorrentes. Em vez de apenas liberar tudo por X dias, o plano identifica 1–2 ações premium que materializam a diferença da oferta e conduz o usuário a experimentá-las cedo, com transparência sobre preço e renovação.

### Experimento concreto

Comparar:

- **A — trial passivo:** acesso liberado, sem sequência de ativação;
- **B — trial orientado a valor:** nas primeiras sessões, o usuário é levado a executar 1–2 ações premium distintivas e recebe uma confirmação curta do benefício que acabou de experimentar.

Manter preço, duração e regras de cancelamento iguais. Medir ativação da ação premium, conversão a preço cheio, retenção em 30/90 dias, cancelamento, reembolso e satisfação.

### Impacto potencial

**Alto para futuros produtos recorrentes; indireto para as ofertas atuais de compra única.** O maior valor do achado é impedir que “engajamento” ou “uso frequente” seja confundido automaticamente com valor e retenção.

### Limites

Os quatro estudos vêm de uma única empresa de notícias europeia. Há componentes autorrelatados nos primeiros estudos; hábitos não foram randomizados. No Estudo 4 existe renovação automática, embora os autores tenham feito análises de robustez e classificado como retidos apenas usuários que ultrapassaram mais de um ciclo de preço cheio. A curvatura em U invertido pode refletir fatores não observados.

### Fonte

- Journal of the Academy of Marketing Science — *Pathways to premium: How habits and benefits shape conversion and retention in freemium subscription models* — publicado em 21/09/2026: https://link.springer.com/article/10.1007/s11747-026-01205-w

## 2. Conveniência pode virar trabalho: comprimir a decisão sem retirar agência

### Evidência encontrada

O Kearney Consumer Institute divulgou em 16 de setembro o *Global Future Consumer Study*, baseado em uma pesquisa conduzida em maio de 2026 com **21.500 consumidores de 20 países, incluindo o Brasil**.

A Kearney descreve um “efeito composto” em que escolha, conveniência e personalização geram cada vez mais opções, inputs, tarefas e interações. O relatório argumenta que parte desse acúmulo transfere ao consumidor trabalho de verificar, coordenar e decidir.

Entre os resultados divulgados, **86% concordaram que se importam mais com o fato de algo funcionar do que com a marca**. A interpretação da Kearney é que confiança tende a depender mais de resultado/desempenho e menos apenas da autoridade da fonte. A recomendação do relatório é devolver tempo ao consumidor com interface e automação sem eliminar descoberta.

### Desejo/comportamento revelado

O sinal não é simplesmente “pessoas querem poucas opções”. O ponto mais útil é: **pessoas podem querer ajuda para reduzir trabalho decisório sem perder a possibilidade de escolher**.

Isso combina com os sinais anteriores deste radar sobre agentes: o consumidor aceita ajuda para pesquisar, comparar e organizar, mas tende a ser mais cauteloso quando o sistema decide de forma irreversível por ele.

### Aplicação possível

Adicionar um modo de `ChoiceCompression` a landing pages e ao Click-to-WhatsApp:

1. mostrar primeiro 2–3 rotas realmente distintas;
2. explicar em uma frase por que cada rota serve a determinado caso;
3. quando houver dados suficientes, recomendar uma delas e dizer por quê;
4. manter “ver outras opções” acessível;
5. nunca esconder uma alternativa economicamente melhor só porque ela rende menos.

### Experimento concreto

Para a mesma oferta com múltiplas variações:

- **A — catálogo completo:** todas as opções visíveis simultaneamente;
- **B — escolha comprimida:** 2–3 caminhos + recomendação explicada + “ver todas”.

Medir tempo até decisão, abandono, repetição de dúvidas, CTA, lead, checkout/pagamento reconciliado, cancelamento, reclamação e arrependimento. Se B aumenta clique mas também aumenta arrependimento ou cancelamento, não é melhoria.

### Impacto potencial

**Alto para UX de agentes, formulários e páginas com múltiplos caminhos; evidência comercial ainda não comprovada.** Pode reduzir carga decisória, mas o ganho precisa aparecer no funil completo e na satisfação posterior.

### Limites

O estudo é survey, não experimento causal. A amostra global foi deliberadamente mais jovem por causa do caráter prospectivo do relatório. Os resultados divulgados são agregados e não há corte do Brasil para os percentuais usados aqui. As projeções para 2036 são cenários da consultoria, não resultados observados e não são usadas como prova de impacto comercial.

### Fonte

- Kearney Consumer Institute — *The tyranny of too much / Global Future Consumer Study* — divulgação em 16/09/2026: https://www.prnewswire.com/news-releases/landmark-kearney-report-spotlights-the-future-consumer-21-500-weigh-in-on-20t-in-spending-up-for-grabs-and-more-302880652.html

## 3. Humor como pertencimento: forte sinal criativo, mas ainda estreito para virar card global

### Evidência encontrada

Omnicom Advertising e Snapchat estudaram 6.028 usuários diários de plataformas sociais entre 13 e 28 anos nos EUA, Reino Unido, Canadá, França, Alemanha e Índia.

Nos resultados divulgados para a Índia, 87% disseram que humor é uma forma de sua geração falar sobre assuntos sérios e 90% disseram que ajuda a expressar coisas que não diriam diretamente. 91% disseram prestar mais atenção a marcas engraçadas, 88% relataram maior consideração de compra e 93% disseram contar a outras pessoas. Ao mesmo tempo, quatro em cinco disseram que marcas online frequentemente parecem “pessoas mais velhas tentando fazer piada”, indicando rejeição a humor forçado ou culturalmente deslocado.

### Hipótese interpretativa

Para jovens, humor pode funcionar menos como simples estímulo de dopamina e mais como **código social de pertencimento, intimidade e reconhecimento cultural**. Isso é relevante para a nossa discussão anterior sobre criativos emocionais: emoção não precisa ser dramaticidade; pode ser o sentimento de “essa marca entende como a gente fala”.

### Aplicação possível

Para públicos jovens, testar humor que nasce de uma situação real do nicho e que a audiência reconheça, em vez de inserir uma piada genérica numa copy de venda. O criativo deve continuar mostrando oferta e benefício reais.

### Experimento concreto

Comparar uma peça informativa direta com uma peça que mantém a mesma informação/prova, mas começa com uma observação cultural ou situação engraçada típica do público. Medir retenção, compartilhamento, comentários qualitativos, CTA, lead e pagamento; observar também sinais de cringe/rejeição.

### Impacto potencial

**Moderado e muito dependente do público.** Pode melhorar atenção e identificação, mas não deve ser generalizado para públicos mais velhos ou para qualquer nicho.

### Limites

Os percentuais comerciais são autorrelatados, não compras observadas. O Brasil não participou da amostra, e os números destacados são do recorte indiano. A amostra cobre apenas 13–28 anos. Por isso este achado não virou card nesta rodada.

### Fontes

- Omnicom — estudo Snapchat + Omnicom: https://www.omc.com/newsroom/snapchat-and-omnicom-advertising-study-reveals-91-of-indias-next-gen-pays-closer-attention-to-funny-brands/
- LBB — metodologia e resultados detalhados: https://lbbonline.com/news/91-of-india-s-next-gen-pays-closer-attention-to-funny-brands-snapchat-and-omnicom-advertising-study

## Cards criados nesta rodada

### `trial-habito-beneficio-premium`

Criado porque há evidência peer-reviewed nova, com quatro estudos de campo e retenção comportamental real, para uma regra reutilizável de produto: **trial deve permitir experimentar e repetir benefícios premium distintivos; hábito isolado não é um objetivo comercial suficiente**.

Fonte revisada:
`repo:pesquisas/neuromarketing/cards/fontes/2026-09-22-trial-habito-beneficio-premium.md`

SHA-256 dos bytes UTF-8 finais:
`35d095fa64f3d03e899501523c269d8c43f2ec53b120ab08269f90ef2c6f3ee4`

Validade escolhida: 21/09/2028, por ser evidência peer-reviewed relativamente robusta, embora a generalização de domínio exija teste.

### `compressao-escolha-sem-remover-agencia`

Criado porque transforma um sinal amplo de carga decisória em uma regra de UX testável e reutilizável: **reduzir a primeira camada de escolha sem esconder alternativas nem retirar controle**.

Fonte revisada:
`repo:pesquisas/neuromarketing/cards/fontes/2026-09-22-compressao-escolha-sem-remover-agencia.md`

SHA-256 dos bytes UTF-8 finais:
`de1cd2bdb3721eac73c84411df4090ab8fdf0cf422631534eff71dec735bac8d`

Validade escolhida: 16/09/2027, porque é uma pesquisa atitudinal/prospectiva e expectativas sobre IA, personalização e interfaces podem mudar rapidamente.

## Governança

A coleção `neuromarketing` continua aceita pelo guia atual da Biblioteca do Harness. As fontes revisadas foram criadas antes dos JSONs e os cards usam somente os campos previstos pelo contrato documentado. Nenhum POST manual foi feito para `https://mkthub.api.br/v1/cards`; os arquivos foram versionados em `main` para o fluxo automático de `DRAFT`.
