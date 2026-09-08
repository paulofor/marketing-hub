# Radar de Design de Experiência — 2026-09-08

## Síntese executiva

A rodada de 8 de setembro reforça uma mudança importante no design de produtos com IA: **confiança, adaptação e “humanidade” não deveriam ser declaradas ou maximizadas; precisam ser conquistadas pelo comportamento observável do sistema e calibradas à tarefa**.

O achado mais novo veio de um experimento publicado hoje na *Scientific Reports*: mudar o enquadramento de uma política de privacidade para enfatizar competência, benevolência ou integridade não aumentou confiança, disposição para seguir recomendações nem disposição para compartilhar dados. Em paralelo, um novo estudo de recuperação de falhas em sistemas multiagente mostra uma arquitetura muito promissora para UX: explicação mínima em operação normal e diagnóstico profundo apenas quando a gravidade justifica, evitando “explanation fatigue”.

Outros trabalhos desta rodada indicam que: criatividade, prazer e alfabetização em IA têm relação importante com adoção de ferramentas co-criativas; agentes podem alcançar valor instrumental comparável ao humano sem receber o mesmo valor moral/social; perfis personalizados podem se beneficiar de **decadência de preferências**, em vez de memória permanente; e sinais sociais que amplificam emoção em humanos não produzem automaticamente o mesmo efeito quando o emissor é artificial.

O padrão de produto é: **menos promessas abstratas; mais evidências, controles reais, estado observável, adaptação com validade temporal e escalada proporcional ao risco.**

## 1. Copy de privacidade, sozinha, não cria confiança

**Descoberta.** O artigo peer-reviewed *Privacy policy framing and trust in recommender systems: an experimental study*, publicado em **8 de setembro de 2026** na *Scientific Reports*, testou políticas de privacidade formuladas para enfatizar três componentes clássicos de confiança: competência, benevolência e integridade.

**Evidência.** No experimento online, nem a presença da declaração de privacidade nem o tipo de enquadramento aumentaram significativamente confiança no recomendador, disposição para seguir suas recomendações ou disposição para compartilhar informações. Um estudo anterior dos mesmos autores, publicado em 26 de junho, encontrou ainda que uma política longa reduziu confiança em relação a uma política curta ou ausente; pedir mais dados também reduziu a disposição de compartilhá-los.

**Mecanismo.** A evidência sustenta principalmente um resultado negativo: **sinalização declarativa de confiabilidade tem efeito limitado quando usada isoladamente**. A hipótese de design é que o usuário responda mais a propriedades observáveis da experiência — quantidade de dados pedida, clareza contextual, possibilidade de recusar, editar ou apagar e coerência entre promessa e comportamento — do que à frase “seus dados estão seguros”.

**Implicação para produto.** Tratar privacidade como parte da interação, não como um bloco jurídico adicionado ao formulário. Um fluxo poderia explicar “por que preciso deste dado” exatamente no ponto de coleta, solicitar o mínimo necessário e oferecer controles reais.

**Experimento possível.** Comparar: A) copy de privacidade genérica; B) copy curta + coleta mínima; C) coleta mínima + explicação contextual + controles de recusa/edição. Medir conclusão, abandono por campo, compartilhamento voluntário, qualidade do lead e confiança percebida separadamente.

**Riscos/limites.** Os estudos são de recomendadores/e-commerce e não medem vendas no Marketing Hub. Os dois trabalhos são dos mesmos autores, portanto não são uma replicação independente. Não se deve esconder informação legal ou simplificar consentimento de maneira manipulativa.

Fontes:
- https://www.nature.com/articles/s41598-026-66241-6
- https://www.tandfonline.com/doi/full/10.1080/0144929X.2026.2686167

## 2. Explicação de agentes pode ser proporcional à gravidade da falha

**Descoberta.** *From Explainability to Actionability: a Tiered Adaptable Multi-Agent Framework with Agent Reasoning Tools for Collaborative Failure Recovery*, publicado em **5 de setembro de 2026** em *Information Systems Frontiers*, propõe separar recuperação de falhas em níveis. O Tier 1 usa um loop executor–critic para corrigir problemas rotineiros; somente falhas realmente sistêmicas são escaladas ao Tier 2, que produz diagnóstico baseado em evidências, plano de recuperação e material para validação humana.

**Evidência.** No benchmark principal, o Tier 1 atingiu mediana F1 de **0,93**, contra **0,81** de um fluxo ART de passagem única, ganho relativo de aproximadamente 15% (`p < 0,001`). Os casos escalados tinham desempenho significativamente pior que os não escalados, indicando que o mecanismo de escalada estava selecionando casos realmente difíceis. Na avaliação humana final, após exclusões previstas na análise, restaram oito especialistas: compreensão média 4,36/5, coerência do plano 4,27/5 e 5 de 8 planos (62,5%) receberam aprovação para implantação.

**Mecanismo.** A ideia central é reduzir carga cognitiva: o usuário não recebe uma narrativa profunda a cada pequena correção. Um monitor leve acompanha métricas e só ativa explicação detalhada quando há evidência de falha importante. O próprio artigo descreve isso como forma de evitar **explanation fatigue**.

**Implicação para produto.** Um agente não deveria ter um único “modo explicável”. A experiência pode usar um **Failure Severity Router**: operação normal → feedback mínimo; erro recuperável → correção + resumo curto; falha sistêmica ou decisão crítica → evidências + diagnóstico + plano + checkpoint humano.

**Experimento possível.** Comparar explicação sempre detalhada, explicação mínima e explicação adaptativa por severidade. Medir tempo para recuperação, taxa de reversão correta, confiança calibrada, sensação de controle e carga cognitiva.

**Riscos/limites.** O benchmark é específico e parte da validação humana tem N pequeno. A arquitetura é promissora, mas não prova benefício em todo tipo de agente ou interface.

Fonte: https://link.springer.com/article/10.1007/s10796-026-10815-2

## 3. Em IA co-criativa, prazer, criatividade e alfabetização em IA entram no próprio modelo de adoção

**Descoberta.** Um artigo publicado em **7 de setembro** em *Humanities and Social Sciences Communications* estudou adoção de ferramentas de pintura com IA entre profissionais de arte usando uma extensão do Technology Acceptance Model.

**Evidência.** O estudo analisou dados de **465 profissionais de arte** por PLS-SEM. Criatividade percebida, motivação hedônica e alfabetização em IA tiveram relações significativas tanto com utilidade percebida quanto com facilidade de uso percebida. Influência social apareceu principalmente ligada à utilidade; autoeficácia, principalmente à facilidade de uso. Confiança percebida moderou a relação entre intenção comportamental e uso.

**Mecanismo.** Em produtos co-criativos, “funciona?” parece insuficiente como pergunta de UX. A experiência também precisa preservar a sensação de autoria, descoberta, prazer e competência do usuário. A alfabetização em IA pode reduzir o custo mental para explorar o espaço de possibilidades e entender como conduzir o sistema.

**Implicação para produto.** Onboarding de IA generativa deveria talvez ensinar uma ação criativa imediatamente, em vez de apresentar um catálogo de features. Um primeiro sucesso que mostre “eu consigo dirigir essa IA” pode ser mais valioso do que uma introdução puramente funcional.

**Experimento possível.** Comparar onboarding tradicional por recursos com onboarding por “primeiro resultado co-criado”: usuário escolhe intenção, manipula uma dimensão e vê uma transformação imediata. Medir tempo até primeiro resultado útil, número de refinamentos, conclusão da criação e retorno em sete dias.

**Riscos/limites.** É um estudo de survey/modelagem estrutural, concentrado em profissionais de arte de uma região; associações não provam causalidade nem generalizam automaticamente para outros produtos com IA.

Fonte: https://www.nature.com/articles/s41599-026-08868-4

## 4. Agentes podem ter valor instrumental comparável ao humano sem equivalência social

**Descoberta.** Incluímos nesta rodada uma meta-análise de maio porque ainda não havia aparecido no radar e sua força de evidência é muito superior à maioria dos estudos individuais. *A systematic review and meta-analysis of psychological and behavioural responses in human-agent vs. human-human interactions* reuniu **162 estudos elegíveis**, dos quais 146 entraram na meta-análise, totalizando **468 tamanhos de efeito**.

**Evidência.** Em interações com papéis e desempenho comparáveis, confiança comportamental, alinhamento social, senso de agência pessoal, desempenho objetivo e experiência de interação foram em geral comparáveis entre agentes e humanos. Ao mesmo tempo, participantes mostraram menos comportamento pró-social e engajamento moral com agentes, atribuíram-lhes menos agência e responsabilidade e os perceberam, em média, como menos competentes, agradáveis e socialmente presentes.

**Mecanismo.** O padrão agregado sugere duas camadas diferentes: **valor instrumental** pode se aproximar do humano; **valor intrínseco/social** não necessariamente. Isso ajuda a explicar por que um agente pode ser um bom colaborador operacional sem ser percebido como parceiro moralmente equivalente.

**Implicação para produto.** Não é necessário maximizar humanização para obter colaboração funcional. Para agentes do Marketing Hub, pode ser mais importante mostrar competência observável, evidências, estado, reversibilidade e responsabilidade do que tentar criar uma persona excessivamente humana.

**Experimento possível.** Mesma capacidade do agente em duas apresentações: “especialista funcional” versus persona fortemente humanizada. Medir conclusão da tarefa, confiança calibrada, clareza sobre quem é responsável pela decisão, handoff e satisfação.

**Riscos/limites.** A revisão agrega estudos desde 2000 e múltiplas gerações de agentes, com heterogeneidade alta em várias medidas subjetivas. O resultado não significa que antropomorfismo seja sempre ruim nem prova impacto comercial.

Fonte: https://www.nature.com/articles/s44271-026-00466-z

## 5. Personalização deveria esquecer: preferências podem ter meia-vida

**Descoberta.** Um sistema de humano digital multimodal publicado em **7 de setembro** combina LLaMA + CLIP por cross-attention e inclui um componente de personalização que atualiza o estado do usuário após cada interação usando um mecanismo explícito de **interest decay**.

**Evidência.** O trabalho usa MSVD, MSR-VTT e 1.000 amostras de interação anotadas por um painel de 60 participantes. O sistema reporta 90,3% de matching imagem-texto, 87,6% de reconhecimento de intenção e 82,5% de recomendação personalizada. Na avaliação subjetiva, satisfação chegou a 4,6/5 e naturalidade a 4,5/5; remover o módulo de personalização reduziu a satisfação em **0,7 ponto**, e substituir o LSTM com decay por um LSTM convencional reduziu a acurácia de recomendação personalizada em **4,2 pontos percentuais**. O custo foi maior latência end-to-end: 2,4 s versus 1,8 s na comparação reportada.

**Mecanismo.** Em vez de tratar toda preferência histórica como permanente, o perfil atribui menos peso a interesses antigos quando não são reforçados. Isso resolve um problema clássico de memória: o sistema pode “lembrar corretamente” algo que já deixou de ser relevante.

**Implicação para produto.** Evoluir o Preference Ledger para incluir `confidence + recency + reinforcement + decay`. Preferências explícitas estáveis podem decair lentamente; sinais inferidos de comportamento deveriam perder peso mais rapidamente.

**Experimento possível.** Comparar memória sem expiração, janela fixa e preferência com decay. Medir correções do usuário (“não quero mais isso”), relevância percebida, rejeição de recomendações e número de vezes que histórico antigo interfere na tarefa atual.

**Riscos/limites.** O artigo avalia um sistema específico e usa amostras anotadas, não um estudo longitudinal naturalista com milhares de usuários. A queda de satisfação na ablação não prova que o mecanismo de decay, isoladamente, causará melhor UX em outro produto. Há ainda trade-off de latência.

Fonte: https://link.springer.com/article/10.1007/s44163-026-02107-0

## 6. Sinais sociais “humanos” não transferem automaticamente seu impacto emocional para agentes artificiais

**Descoberta.** Um artigo de **4 de setembro** no *International Journal of Social Robotics* comparou a mesma fala emocional emitida por humanos, androids e robôs humanoides. O sinal de intenção comunicativa era simples: olhar diretamente para o participante e mover a boca enquanto falava.

**Evidência.** Foram dois estudos comportamentais (`N1 = 36`, `N2 = 72`; o segundo pré-registrado). Para speakers humanos, a condição comunicativa aumentou o arousal associado a palavras emocionais; para androids e robôs, os efeitos equivalentes foram menores e estatisticamente não significativos. A valência — o significado positivo/negativo da palavra — foi transmitida por todos. Os efeitos humanos foram pequenos, e a análise apontou animacidade percebida como variável relevante para o padrão.

**Mecanismo.** A hipótese dos autores é que olhar, fala direta e movimento facial ganham força quando o receptor atribui ao emissor uma mente/estado interno real e percebe a mensagem como mais autorreferente. Simular os sinais superficiais não garante a mesma interpretação psicológica.

**Implicação para produto.** Lip-sync, contato visual, avatar humano e voz emocional não deveriam ser tratados como “amplificadores de emoção” universais. Para agentes digitais, o efeito precisa ser medido. Em alguns casos, clareza e competência podem importar mais que realismo social.

**Experimento possível.** Em um mesmo roteiro, testar avatar neutro, avatar com contato visual/expressividade e apresentador humano, medindo atenção, arousal autorrelatado, compreensão e CTA. Isso deve ser tratado como teste, não como previsão de que o humano necessariamente vencerá.

**Riscos/limites.** O estudo é com robôs em vídeo, palavras isoladas, participantes alemãs em faixa etária restrita e efeitos humanos pequenos. A extrapolação para avatares de publicidade ou agentes conversacionais digitais é incerta.

Fonte: https://link.springer.com/article/10.1007/s12369-026-01438-3

## Experience Engine v8

```text
Usuário
   ↓
intenção + contexto + histórico
   ↓
PREFERENCE STATE
   ├── origem
   ├── confiança
   ├── recência
   ├── reforço
   └── DECAY
   ↓
EXPERIENCE / MODALITY ROUTER
   ↓
BEHAVIORAL TRUST LAYER
   ├── pedir o mínimo de dados
   ├── explicar a necessidade no contexto
   ├── oferecer controles reais
   └── mostrar evidências/proveniência
   ↓
TASK EXECUTION
   ↓
FAILURE SEVERITY ROUTER
   ├── normal → feedback mínimo
   ├── recuperável → correção + resumo
   └── sistêmica/alto risco → diagnóstico + evidência + plano + validação humana
   ↓
RELATIONAL CALIBRATION
   ├── competência instrumental
   └── sinais sociais/emocionais apenas quando úteis e validados
   ↓
resultado
   ↓
confiança calibrada + carga cognitiva + autonomia + comportamento real
```

## Insight principal da rodada

A ideia mais forte de hoje é: **confiança não deve ser “desenhada” principalmente como uma mensagem; deve emergir da experiência operacional**.

Isso vale para privacidade, explicabilidade e antropomorfismo. Dizer “somos seguros” não criou confiança no novo experimento. Explicar tudo o tempo todo pode produzir fadiga. Parecer humano não garante a resposta emocional associada a humanos. A direção mais promissora é construir sistemas que **pedem menos, mostram mais evidência, dão controle causal real, esquecem preferências que envelheceram e aprofundam explicações somente quando há razão para isso**.

## Cards gerados nesta rodada

Foram criados **dois cards**, ambos na coleção válida `neuromarketing`:

1. `privacidade-copy-nao-cria-confianca` — importante para formulários, agentes e personalização porque transforma um achado experimental sobre confiança e compartilhamento de dados em uma hipótese testável de UX: comparar copy isolada com minimização de dados + explicação contextual + controles reais.
2. `agentes-valor-instrumental-sem-equivalencia-social` — importante para orientar o design de agentes: a meta-análise sugere que colaboração funcional pode ocorrer sem equivalência social humana, o que justifica testar competência observável e accountability contra humanização excessiva.

Fontes revisadas e hashes foram salvos em `pesquisas/design-experiencia/cards/fontes/`; os JSONs correspondentes foram salvos em `pesquisas/design-experiencia/cards/` como candidatos a DRAFT.

O estudo de **recuperação de falhas por severidade** é um achado forte, mas **não virou card** nesta rodada porque seu valor principal é arquitetural para harness/agentes e não há hoje uma coleção aceita pela API que represente esse tema sem forçar enquadramento. O estudo de **preference decay** também ficou apenas no relatório por enquanto: é promissor, porém a evidência está muito ligada a um sistema multimodal específico para justificar um card comportamental geral.
