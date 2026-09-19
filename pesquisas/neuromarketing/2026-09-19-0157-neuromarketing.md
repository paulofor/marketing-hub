# Radar de Neuromarketing e Desejos Digitais — 19/09/2026

**Data/hora:** 2026-09-19 01:57 — America/Sao_Paulo

## Resumo da rodada

A rodada encontrou três sinais novos com aplicação potencial no Marketing Hub. O mais acionável é a continuidade de contexto em agentes conversacionais: consumidores relatam forte fricção quando precisam repetir informações para a IA ou novamente após um handoff para atendimento humano. Também surgiu um método peer-reviewed para detectar quais aspectos de um produto se relacionam de forma diferente com satisfação em canais distintos, e um estudo experimental sobre como atenção seletiva participa do aprendizado de sinais associados a recompensa.

Somente o primeiro achado virou card nesta rodada. Os outros dois são úteis para experimentação, mas ainda exigem uma ponte maior até Meta Ads, landing pages e Click-to-WhatsApp.

## Achado 1 — Continuidade de contexto virou requisito de confiança em agentes

### Evidência encontrada

A Twilio publicou pesquisa com **7.652 consumidores e 660 líderes de negócios em 18 mercados, incluindo o Brasil**. Entre os consumidores, **74% disseram ter de repetir informações ao interagir com um assistente de IA**, **76% disseram que o agente humano recebe pouco ou nenhum contexto depois de um handoff** e **71% afirmaram que podem abandonar a conversa cedo se a IA não os reconhecer**. Entre as marcas pesquisadas, 91% relataram queda de satisfação associada a falhas de IA; perda de contexto entre canais ou interações foi citada por 40% como um tipo relevante de falha.

### Desejo/comportamento revelado

O sinal não é apenas “quero personalização”. Ele aponta para algo mais básico: **o usuário espera continuidade e não quer reconstruir a própria história para cada etapa do atendimento**. Repetição transforma automação em trabalho adicional para o consumidor.

### Hipótese interpretativa

Preservar contexto suficiente entre turnos, sessões e handoffs pode reduzir esforço percebido e abandono. A pesquisa é autorrelatada e não demonstra causalidade nem efeito em conversão ou receita.

### Aplicação possível no Marketing Hub

Criar um **ContextContinuityGate** para agentes e Click-to-WhatsApp. O contexto transportado deve ser mínimo e explícito: intenção atual, fatos fornecidos pelo usuário, oferta em discussão, dúvidas já respondidas, decisões já confirmadas e permissões relevantes. Em escalonamento para humano, gerar um resumo curto e transferi-lo junto com a conversa.

A continuidade não deve significar memória ilimitada. Dados irrelevantes, sensíveis ou fora da finalidade esperada não devem ser carregados apenas porque estão disponíveis.

### Experimento sugerido

Comparar dois fluxos equivalentes:

- **A:** o agente pede novamente informações já fornecidas quando muda de etapa ou ocorre handoff;
- **B:** o agente reutiliza apenas o contexto confirmado e o humano recebe um resumo da conversa.

Medir número de perguntas repetidas, abandono antes do CTA, pedido de atendimento humano, tempo até resolução, progressão para CTA/checkout e satisfação. Monitorar também reclamações de privacidade e correções de contexto.

### Impacto potencial

**Alto**, especialmente em Click-to-WhatsApp e agentes de qualificação, porque atua diretamente sobre esforço e continuidade da conversa. O efeito comercial precisa ser medido localmente.

### Limites

Pesquisa de fornecedor e baseada em autorrelato. Os percentuais citados são globais, não isolados para o Brasil. A amostra de líderes representa empresas com 500 ou mais funcionários. O estudo não é um experimento de WhatsApp, Meta Ads ou vendas. LGPD, minimização de dados, transparência e finalidade continuam sendo restrições centrais.

**Fonte:** https://www.twilio.com/en-us/report/navigating-data-deluge-charting-customer-context

## Achado 2 — O mesmo produto pode ter drivers de satisfação diferentes por canal

### Evidência encontrada

Artigo peer-reviewed publicado em **18 de setembro de 2026** na *Scientific Reports* propõe um framework que usa LLMs para extrair e normalizar aspectos mencionados em avaliações e SHAP/XAI para estimar a contribuição de cada aspecto para satisfação em diferentes canais. O trabalho foi validado em **6,8 milhões de reviews** do maior varejista omnichannel de saúde e beleza da Coreia do Sul. O método de normalização melhorou a acurácia em **24,70 pontos percentuais** sobre métodos LLM comparáveis que não usam taxonomias predefinidas nem ferramentas externas.

### Desejo/comportamento revelado

A evidência reforça que consumidores **não necessariamente valorizam ou avaliam os mesmos atributos da mesma maneira em todos os canais**. O canal altera o contexto da decisão e, portanto, pode mudar quais aspectos são mais associados à satisfação.

### Hipótese interpretativa

No Marketing Hub, Meta Ads, landing page, WhatsApp e pós-compra podem ter hierarquias diferentes de objeções, provas e benefícios. Essa é uma extrapolação: o estudo comparou principalmente canais de varejo online/offline, não microetapas de um funil digital.

### Aplicação possível no Marketing Hub

Criar futuramente um **ChannelAspectDivergence**: analisar separadamente comentários de anúncios, respostas de Lead Ads, transcrições do WhatsApp, comportamento da landing e feedback pós-compra. LLMs poderiam agrupar aspectos recorrentes e o sistema estimaria, por canal, quais deles se associam a satisfação, abandono ou progressão no funil.

### Experimento sugerido

Em vez de usar a mesma ordem de argumentos em todos os canais, identificar o principal aspecto observado em cada etapa e testar uma mensagem adaptada. Exemplo: Meta Ads enfatiza identificação/problema; landing enfatiza prova e clareza; WhatsApp enfatiza objeção específica e esforço para começar. Comparar com uma hierarquia única de benefícios para todos os canais.

### Impacto potencial

**Médio a alto** como ferramenta de aprendizado do funil. Antes de automatizar decisões, é necessário comprovar que a divergência aparece nos próprios dados do Marketing Hub.

### Limites

Um único varejista sul-coreano de saúde e beleza; reviews online/offline não equivalem a Meta/landing/WhatsApp; SHAP descreve contribuição no modelo e não causalidade; a melhora de 24,70 pontos percentuais refere-se à normalização de aspectos, não a vendas.

**Fonte:** https://www.nature.com/articles/s41598-026-71125-w

## Achado 3 — Recompensa não cria saliência automaticamente; atenção seletiva parece participar do aprendizado

### Evidência encontrada

Estudo preregistrado publicado em **18 de setembro de 2026** no *Psychonomic Bulletin & Review* investigou o chamado *value-modulated attentional capture* (VMAC). Participantes aprenderam associações entre uma característica visual e recompensa. O efeito típico — maior captura de atenção pelo estímulo associado a recompensa alta — apareceu apenas quando os participantes eram levados a prestar atenção seletivamente à característica que predizia a recompensa. Uma meta-análise complementar encontrou efeito significativo quando a característica relevante era priorizada (**SMD 0,36; IC95% 0,25–0,47**) e efeito pequeno/não significativo quando não era priorizada (**SMD 0,08; IC95% -0,05–0,21**).

### Desejo/comportamento revelado

O estudo não mede desejo de compra, mas oferece um mecanismo útil: **um sinal de valor pode ganhar prioridade atencional por aprendizagem, porém a pessoa precisa de alguma forma processar a característica que realmente prediz o resultado**. Apenas decorar a interface com elementos de “recompensa” não garante atenção aprendida.

### Hipótese interpretativa

Um sinal consistente que realmente prediz um benefício percebido — por exemplo, economia confirmada, disponibilidade real ou progresso — pode se tornar mais fácil de reconhecer ao longo de interações repetidas. Isso é uma hipótese de aplicação; o estudo foi uma tarefa de busca visual em laboratório.

### Aplicação possível no Marketing Hub

Evitar tratar cor de CTA, badge de desconto ou animação como gatilho universal. Quando houver benefício real recorrente, manter um sinal visual/semântico consistente ligado ao benefício e garantir que o usuário consiga perceber claramente a relação entre sinal e resultado.

### Experimento sugerido

Comparar um badge meramente decorativo de recompensa com um sinal consistente ligado a um resultado verificável, por exemplo “economia confirmada de R$ X” ou “vaga disponível agora”. Medir compreensão do benefício, atenção quando disponível, CTA e comportamento posterior. Nunca fabricar escassez, economia ou recompensa.

### Impacto potencial

**Exploratório**. Interessante para desenho de sinais recorrentes e aprendizado de interface, mas ainda distante de uma regra comercial validada.

### Limites

Tarefa laboratorial de atenção visual; não mede anúncios, compras, CTR ou conversão; a meta-análise complementar não foi preregistrada; não autoriza inferir que uma cor ou recompensa específica aumentará vendas.

**Fonte:** https://link.springer.com/article/10.3758/s13423-026-02964-x

## Cards desta rodada

Foi criado **1 card**:

### `continuidade-contextual-agente`

Merece virar card porque captura uma regra operacional distinta das já existentes sobre personalização e delegação: **não obrigar o usuário a reconstruir o contexto quando a conversa continua ou muda de agente**. É diretamente testável em Click-to-WhatsApp e agentes do Marketing Hub e possui evidência recente em 18 mercados, incluindo o Brasil.

Fonte revisada:
`pesquisas/neuromarketing/cards/fontes/2026-09-19-continuidade-contextual-agente.md`

Card:
`pesquisas/neuromarketing/cards/2026-09-19-continuidade-contextual-agente.json`

SHA-256 da fonte revisada:
`7abce04891e65e2fcc0da787997a261728e19718d5c1b01c528122e9e2b80efa`

Os achados de divergência entre canais e VMAC **não viraram cards** nesta rodada: o primeiro ainda exige validação nos canais digitais específicos do Marketing Hub e o segundo é um mecanismo laboratorial cuja aplicação comercial seria prematura.

## Limites gerais da rodada

Nenhum dos achados comprova aumento de vendas no Marketing Hub. Surveys descrevem percepção e comportamento declarado; modelos XAI descrevem associações nos dados; estudos de atenção em laboratório não equivalem a resposta a anúncios. Aplicações devem entrar como hipóteses testáveis e ser avaliadas com eventos reais do funil, sem transformar correlação em causalidade.

## Fontes

- Twilio — *Navigating the Data Deluge: A Compass for Customer Context*: https://www.twilio.com/en-us/report/navigating-data-deluge-charting-customer-context
- Choi, Son & Choi — *An omnichannel strategy development framework leveraging customer opinion divergence using large language models and explainable AI*, Scientific Reports, 18/09/2026: https://www.nature.com/articles/s41598-026-71125-w
- Garre-Frutos et al. — *The role of selective attention in value-modulated attentional capture*, Psychonomic Bulletin & Review, 18/09/2026: https://link.springer.com/article/10.3758/s13423-026-02964-x
