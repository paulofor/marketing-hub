# Radar de Neuromarketing e Desejos Digitais — 2026-09-01 01:34

Nesta rodada, três achados se destacam por utilidade prática para o Marketing Hub. O padrão comum é: usuários estão se abrindo profundamente para interfaces de IA, mas isso aumenta — e não reduz — a necessidade de transparência, controle e contexto. Ao mesmo tempo, um trabalho de eye-tracking mostra que atenção publicitária deve ser modelada como uma propriedade do par **criativo + contexto**, e não do criativo isoladamente.

## 1. Brasil: usuários compartilham emoções e dados sensíveis com IA, mesmo preocupados com privacidade

### O que aconteceu
Em 31 de agosto de 2026, o Cetic.br/NIC.br divulgou novos dados da pesquisa **Privacidade e proteção de dados pessoais: perspectivas de indivíduos, empresas e organizações públicas no Brasil**. Pela primeira vez, o levantamento examinou o uso de IA generativa sob a ótica da privacidade.

Entre usuários de IA generativa no Brasil, os tipos de informação compartilhados incluem:

- temas de trabalho ou estudo: 73%;
- preferências e gostos pessoais: 58%;
- sentimentos e emoções: 54%;
- dados de saúde, como sintomas e exames: 52%;
- fotos próprias ou de conhecidos: 50%;
- finanças pessoais: 41%;
- dados pessoais como nome, endereço, data de nascimento ou CPF: 37%.

Ao mesmo tempo, **66% dos usuários de IA generativa dizem estar preocupados ou muito preocupados com o uso de seus dados pessoais pelas empresas responsáveis por essas ferramentas**.

O mesmo levantamento mostra que 32% dos usuários de Internet brasileiros relataram tentativa de golpe com uso de dados pessoais; entre quem vivenciou tentativas, aplicativos de mensagem foram o canal mais citado (84%).

### Desejo/comportamento revelado
Existe um **paradoxo de intimidade digital**: quanto mais útil e conversacional a IA se torna, mais o usuário tende a revelar contexto íntimo — mesmo sem confiar plenamente na infraestrutura que recebe esse contexto.

O usuário não parece querer uma interface fria. Ele quer poder falar livremente. O problema é que espera que essa liberdade não seja convertida silenciosamente em perfil, targeting ou reutilização de dados.

### Por que importa para o Marketing Hub
Agentes em Click-to-WhatsApp e formulários conversacionais podem receber informações muito mais sensíveis do que o designer do funil imaginou inicialmente. Um usuário pode espontaneamente contar problemas financeiros, emocionais, familiares ou de saúde ao explicar o que deseja comprar.

Isso torna inadequado tratar todo texto de conversa como matéria-prima automática para segmentação, personalização ou geração de novos anúncios.

### Aplicação sugerida
Criar uma camada `SensitiveContextBoundary` com classificação automática de informações fornecidas pelo usuário:

- `NORMAL_PREFERENCE`
- `PERSONAL_CONTEXT`
- `SENSITIVE_HEALTH`
- `SENSITIVE_FINANCIAL`
- `SENSITIVE_EMOTIONAL`
- `IDENTIFYING_DATA`

Por padrão, categorias sensíveis poderiam ser usadas apenas para responder à conversa atual, sem entrar automaticamente em perfis comerciais, públicos de campanha ou treinamento interno.

### Experimento/feature concreta
Comparar dois fluxos de agente:

**A — transparência implícita:** apenas a conversa normal.

**B — contexto protegido:** quando o agente percebe que o usuário começa a revelar algo sensível, informa de forma curta: “Posso usar isso para responder aqui, mas não preciso guardar essa informação para personalizar futuras ofertas.”

Medir continuidade da conversa, taxa de resposta, confiança declarada, opt-in de personalização e conversão.

### Impacto potencial
**Muito alto**, especialmente para produtos baseados em agentes, WhatsApp e personalização comportamental no Brasil.

### Fonte
- Cetic.br/NIC.br — 31/08/2026: https://www.cetic.br/pt/noticia/um-em-cada-tres-usuarios-de-internet-no-brasil-relatou-sofrer-tentativa-de-golpe-com-uso-de-seus-dados-pessoais-aponta-cetic-br/

---

## 2. Agentes de IA: empresas acham que estão sendo transparentes, mas os usuários não percebem

### O que aconteceu
Em 31 de agosto de 2026, a Twilio destacou um forte **transparency gap** em agentes conversacionais. Na pesquisa global usada no relatório, foram ouvidos 7.652 consumidores e 660 líderes empresariais em 18 mercados, incluindo o Brasil.

Os números mais relevantes:

- 81% das empresas dizem que seus agentes se identificam imediatamente como IA;
- apenas 22% dos consumidores dizem perceber essa identificação;
- 76% acham que um agente de IA deveria se identificar no início da conversa;
- 49% querem saber exatamente quais dados o agente consegue acessar;
- 54% dizem confiar mais quando existe uma explicação de como a recomendação foi formada;
- 63% querem um caminho rápido para falar com uma pessoa;
- 78% relatam já ter tentado contornar um agente de IA para chegar a um humano.

A pesquisa também encontrou um erro de autopercepção importante: 75% dos consumidores acreditam conseguir identificar um agente de IA textual, mas, quando testados, 90% não conseguiram distingui-lo corretamente.

### Desejo/comportamento revelado
O usuário não quer apenas “saber que existe IA”. Ele quer três respostas logo no começo:

1. **com quem estou falando?**
2. **o que essa IA consegue ver sobre mim?**
3. **como saio daqui e chego a uma pessoa?**

A transparência que existe somente na configuração técnica não vale se não for percebida na experiência.

### Por que importa para o Marketing Hub
Isso se aplica diretamente a Click-to-WhatsApp. Uma mensagem com avatar de robô ou nome como “assistente virtual” pode parecer suficiente para quem criou o agente, mas ainda assim não ser interpretada claramente como uma interação com IA.

O ponto crucial é medir **transparência percebida**, não apenas `disclosure_configured = true`.

### Aplicação sugerida
Criar `AgentTrustContract`, exibido no primeiro turno da conversa, com quatro elementos mínimos:

- `identity`: “Sou um agente de IA da marca X”;
- `purpose`: “Posso explicar, comparar e preparar opções”;
- `data_scope`: “Uso apenas as informações desta conversa e os dados autorizados”;
- `human_exit`: “Se quiser, posso chamar uma pessoa a qualquer momento”.

Também criar métricas como:

- `DisclosureRendered`
- `DisclosurePerceived`
- `HumanExitVisible`
- `HumanEscalationRequested`
- `RepeatedQuestionCount`

### Experimento/feature concreta
A/B/C em Click-to-WhatsApp:

- **A:** agente sem disclosure explícito;
- **B:** “Sou o assistente virtual da marca”;
- **C:** “Sou um agente de IA da marca. Posso pesquisar e comparar opções; você decide e pode falar com uma pessoa quando quiser.”

Medir primeira resposta, continuidade após 3 e 5 mensagens, abandono, pedido de humano e conversão.

### Impacto potencial
**Muito alto e de baixo custo de implementação.** Pode aumentar confiança sem reduzir a eficiência da automação.

### Fontes
- Twilio — 31/08/2026: https://www.twilio.com/en-us/blog/insights/ai-disclosure-transparency-gap
- Relatório e metodologia: https://www.twilio.com/en-us/report/scale-automation-with-losing-customer-confidence

---

## 3. Eye-tracking + IA: atenção depende fortemente da combinação entre anúncio e contexto

### O que aconteceu
Um estudo do **Journal of Marketing**, publicado na edição de julho de 2026 e ainda não registrado nas rodadas anteriores deste radar, apresenta o **AdGazer**, um modelo que combina teoria de atenção, machine learning, um LLM multimodal e uma base de eye-tracking em larga escala.

A base contém **3.531 anúncios display e seus respectivos contextos**, com tempos agregados de olhar para o anúncio e para a marca.

Nos testes holdout, o modelo obteve correlações de aproximadamente:

- **0,83** para tempo de olhar no anúncio;
- **0,80** para tempo de olhar na marca.

O ponto mais importante para o Marketing Hub: atributos do **contexto onde o anúncio aparece** contribuíram conjuntamente com pelo menos **33% da previsão de atenção ao anúncio** e cerca de **20% da previsão de atenção à marca**.

### Desejo/comportamento revelado
A atenção do consumidor não é propriedade apenas do criativo. Ela depende de **compatibilidade semântica, visual e perceptiva com o ambiente em que aquele criativo aparece**.

Isso questiona uma prática comum: avaliar uma imagem ou vídeo isoladamente e dar a ele um “attention score” universal.

### Por que importa para o Marketing Hub
O `CreativePreScreenAgent` pode ser melhor se não perguntar apenas:

> “este criativo chama atenção?”

mas:

> “este criativo chama atenção **neste contexto, placement e momento**?”

Para Meta Ads, o contexto pode ser aproximado por placement, formato, tipo de feed, objetivo da campanha e sinais do conteúdo adjacente quando disponíveis.

### Aplicação sugerida
Criar um `CreativeContextFitModel` com features separadas:

- `CreativeVisualFeatures`
- `CreativeSemanticFeatures`
- `PlacementFeatures`
- `AudienceState`
- `ContextTopic`
- `BrandVisibilityScore`
- `PredictedAdAttention`
- `PredictedBrandAttention`

A unidade básica de análise deixa de ser `creative_id` e passa a ser algo próximo de:

`creative_id + placement/context + audience_state`.

### Experimento/feature concreta
Pegar 6 criativos e distribuí-los por placements/contextos diferentes. Antes da mídia, gerar um score de compatibilidade criativo-contexto. Depois comparar com dados reais de thumb-stop, CTR, vídeo assistido, CPL e reconhecimento de marca quando houver pesquisa de lift.

Com histórico suficiente, medir se `CreativeContextFit` explica performance melhor do que um score visual do criativo isolado.

### Impacto potencial
**Muito alto para a camada de pré-seleção de criativos.** Pode impedir que o Hub descarte uma peça boa apenas porque ela foi avaliada fora do contexto em que realmente funciona.

### Fonte
- Ye, Wedel e Pieters — *Journal of Marketing*, Vol. 90, Issue 4, julho de 2026: https://journals.sagepub.com/doi/10.1177/00222429251396114

---

## Síntese para o Marketing Hub

A rodada sugere uma evolução importante do modelo conceitual:

**contexto do usuário → confiança para revelar informação → limites de uso dos dados → transparência do agente → contexto de mídia → atenção → ação.**

Prioridades sugeridas para backlog:

1. `AgentTrustContract`
2. `SensitiveContextBoundary`
3. `CreativeContextFitModel`

A principal ideia transversal é: **quanto mais íntima e inteligente a interface se torna, mais explícitos precisam ser seus limites; e quanto mais sofisticada a previsão de atenção se torna, menos faz sentido avaliar o criativo fora do contexto em que ele será visto.**
