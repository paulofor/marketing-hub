# Radar de Design de Experiência — 2026-09-11

## Resumo executivo

A rodada de 11 de setembro de 2026 reforça uma mudança importante no design de produtos com IA: **a interface precisa ajudar o sistema a localizar o tipo de desalinhamento, não apenas registrar que algo deu errado**. Três frentes se destacam:

1. feedback pode ser mais útil quando distingue **objetivo errado** de **método errado**;
2. explicações funcionam melhor quando são **específicas, coerentes e verificáveis**, não simplesmente longas;
3. autonomia, skills e múltiplos agentes precisam de **exposição e organização adaptativas**, porque mais contexto, mais automação ou mais agentes não garantem melhor experiência.

Também apareceram evidências úteis para avaliação escalável de explicações e para conteúdo gerado por IA em comunicação organizacional.

---

## 1. Feedback de IA pode separar “você entendeu meu objetivo errado” de “você executou do jeito errado”

**Descoberta.** O artigo peer-reviewed *Neural Value Alignment: Human-AI Collaboration Under Goal-Action Ambiguity*, publicado online em 24 de agosto no *IEEE Transactions on Cybernetics* e divulgado institucionalmente pela KAIST em 9–10 de setembro, distingue dois sinais cognitivos: reward prediction error (RPE), ligado a discrepância no resultado/objetivo, e state prediction error (SPE), ligado à transição/forma de agir.

**Evidência.** Em uma tarefa experimental com EEG, RPE, SPE e sua coocorrência foram decodificáveis corticalmente em diferentes contextos. Em simulações, a combinação RPE+SPE acelerou o alinhamento entre humano e IA mesmo quando a decodificação era imperfeita.

**Mecanismo psicológico/comportamental.** Um usuário pode estar insatisfeito porque o sistema perseguiu o objetivo errado ou porque perseguiu o objetivo certo por um caminho inadequado. Misturar esses dois erros em um único feedback (“não gostei”) perde informação causal importante.

**Implicação para produto.** Não é necessário usar EEG para aproveitar o princípio. Em um agente digital, uma rejeição pode abrir um feedback de baixa fricção:

- “O objetivo que entendi estava errado.”
- “O objetivo estava certo, mas a forma de executar estava errada.”

O primeiro caso revisa intenção e critérios; o segundo preserva intenção e revisa plano, ferramenta, sequência ou apresentação.

**Hipótese/experimento.** Comparar feedback genérico com feedback objetivo-vs-método e medir re-prompts, tempo de recuperação, conclusão da tarefa e abandono.

**Riscos/limites.** O estudo não testou uma interface comercial; a aplicação textual é uma hipótese derivada. Não usar o achado como justificativa para coleta biométrica sem necessidade e consentimento.

Fontes:
- https://pubmed.ncbi.nlm.nih.gov/42636135/
- https://doi.org/10.1109/TCYB.2026.3722605
- https://www.eurekalert.org/news-releases/1143282

---

## 2. Explicação útil parece depender mais de especificidade e coerência do que de quantidade de texto

**Descoberta.** *Decoding user preferences: A study of explanations for algorithmic decision support in procurement* comparou cinco condições de explicação — No Explanation, What, Why, How e Objective — em um sistema de decisão assistida.

**Evidência.** Experimento within-participant com 40 pessoas. As explicações, em geral, elevaram aceitação e confiança em relação à condição sem explicação. Os participantes preferiram justificativas informativas, tecnicamente específicas, logicamente coerentes e suficientemente concisas.

**Mecanismo psicológico/comportamental.** Uma explicação útil reduz incerteza interpretativa porque permite ao usuário construir um modelo mental verificável da recomendação. “Mais rationale” não é o mesmo que “melhor rationale”.

**Implicação para produto.** Para recomendações de agentes, testar um formato compacto:

`recomendação → critério principal → evidência específica → limite/incerteza`

Isso é melhor candidato que parágrafos genéricos de justificativa.

**Hipótese/experimento.** Comparar sem explicação, explicação genérica e explicação específica/coerente, medindo compreensão objetiva, aceitação apropriada, correções e carga cognitiva.

**Riscos/limites.** N=40 e cenário simulado. Explicações convincentes também podem aumentar confiança em recomendações erradas, portanto a métrica não deve ser “confiança máxima”.

Fonte:
- https://doi.org/10.1016/j.ergon.2026.104029

---

## 3. Evidência nova reforça “bounded agency”: confiança foi maior em níveis intermediários de autonomia, não no máximo

**Descoberta.** Um artigo publicado em 10 de setembro em *Human-Intelligent Systems Integration* combinou revisão/meta-análise de 68 estudos, survey de 200 profissionais e 30 entrevistas em áreas como clima, saúde, finanças e administração pública.

**Evidência.** Os autores observaram associação não linear entre autonomia da IA e confiança: nos dados analisados, confiança foi maior em regiões intermediárias de delegação e caiu quando a autonomia se aproximou do máximo. O próprio artigo alerta explicitamente que os pontos de inflexão observados não são percentuais universais.

**Mecanismo psicológico/comportamental.** Aumentar autonomia pode inicialmente reduzir esforço e aumentar utilidade; depois de certo ponto, perda de controle, accountability pouco clara e dificuldade de intervenção podem reduzir confiança calibrada.

**Implicação para produto.** Tratar autonomia como variável de design, não como chave on/off. O Experience Engine deveria definir por tarefa: `sugerir`, `preparar`, `executar com confirmação`, `executar dentro de limites` ou `executar autonomamente`.

**Hipótese/experimento.** Para uma mesma tarefa, comparar suggestion-only, bounded execution e high-autonomy, medindo tempo, correções, sensação de controle, confiança calibrada e qualidade final.

**Riscos/limites.** Há alta heterogeneidade entre estudos e parte dos dados é autorrelatada. Não existe base para adotar “60%” ou “65%” como regra de produto.

Fonte:
- https://link.springer.com/article/10.1007/s42454-026-00111-4

**Observação editorial.** Esse achado reforça materialmente o card já existente `delegacao-agente-com-controle-humano`; não foi criado um novo card separado para evitar duplicação de ideia nesta rodada.

---

## 4. LLMs já conseguem pré-avaliar qualidade de explicações com correlação útil com humanos

**Descoberta.** O preprint *XAI-Arena*, submetido em 8 e atualizado em 10 de setembro, propõe usar LLM-as-a-judge para avaliar explicações de IA em várias dimensões e para diferentes stakeholders.

**Evidência.** O framework avalia simplicidade, clareza, adequação à tarefa, calibração de confiança, actionability, transparência, faithfulness e interpretabilidade. Na validação humana reportada, as avaliações do LLM tiveram correlação de Spearman `ρ = 0,693` com avaliações humanas (`p < 0,001`).

**Mecanismo.** O valor não está em substituir usuários reais, mas em permitir triagem rápida e multidimensional de muitas explicações antes de gastar tempo com avaliação humana.

**Implicação para produto.** Um pipeline de UX pode usar:

`100 explicações → juiz sintético multidimensional → 10 candidatas → usuários reais → experimento`

**Hipótese/experimento.** Criar um rubric de explicação para customer-agent e verificar se scores sintéticos predizem compreensão e preferência humana local.

**Riscos/limites.** Correlação não equivale a substituição; LLM judges podem compartilhar vieses com os sistemas avaliados. Ainda é preprint.

Fonte:
- https://arxiv.org/abs/2609.09428

**Card.** Não virou card porque o achado é predominantemente uma prática de avaliação/harness e nenhuma das quatro coleções atuais representa isso sem distorção.

---

## 5. Em agentes com skills, importa não apenas qual skill selecionar, mas como ela é exposta ao modelo

**Descoberta.** *SkillAlign*, submetido em 7 e atualizado em 9 de setembro, testa interfaces diferentes para a mesma skill: instruções completas, hints, resumos comprimidos, workflows ou nenhuma exposição.

**Evidência.** Em ALFWorld e SkillsBench, a forma de exposição alterou substancialmente sucesso da tarefa e custo de contexto. Exposição compacta top-k pôde superar a injeção da biblioteca inteira; políticas adaptativas mostraram sinal aprendível, embora ainda longe do oracle.

**Mecanismo.** Contexto extra pode competir por atenção do modelo, distrair ou ativar procedimentos inadequados. “Mais memória/mais instrução” não é monotonicamente melhor.

**Implicação para produto.** Para agentes, criar um **Context/Skill Presentation Policy** que decida não só *o que* recuperar, mas *em qual forma* apresentar: full, hint, summary, workflow ou nada.

**Hipótese/experimento.** Fixar modelo, tarefa e skill library e variar apenas a forma de exposição, medindo sucesso, tokens, latência e erros de ferramenta.

**Riscos/limites.** Benchmarks de agentes, não UX humana; ainda é preprint/EMNLP 2026.

Fonte:
- https://arxiv.org/abs/2609.07255

**Card.** Não virou card por falta de coleção válida: é um achado de arquitetura de harness/agentes.

---

## 6. Organização do time de agentes pode importar tanto quanto o modelo individual

**Descoberta.** *ORCH: Organizational Principles Enable Collective Intelligence in Embodied AI*, submetido em 10 de setembro, aplica princípios de teoria organizacional para construir hierarquias específicas por tarefa em equipes de agentes.

**Evidência.** O trabalho avaliou 25 missões de resposta a incêndios, equipes de até 50 agentes heterogêneos e oito LLMs. Organizações humanas desenhadas com ORCH melhoraram, em média, score final em 63,97% e eficiência de execução em 74,29% frente a quatro frameworks comparadores; organizações geradas automaticamente por LLMs também obtiveram ganhos substanciais.

**Mecanismo.** Trabalho paralelo e trabalho dependente de pré-requisitos exigem estruturas diferentes. O sistema combina interdependência pooled para atividades concorrentes e sequential para etapas ordenadas.

**Implicação para produto.** Em um AI Hub, não montar multi-agent apenas como “vários agentes conversando”. A topologia deve refletir dependências reais: especialistas paralelos onde possível, coordenação/handoff onde necessário.

**Hipótese/experimento.** Para um workflow fixo, comparar flat swarm, supervisor único e organização derivada das dependências do processo.

**Riscos/limites.** Cenário simulado/embodied e preprint. Ganhos não devem ser projetados diretamente para agentes de marketing.

Fonte:
- https://arxiv.org/abs/2609.11737

**Card.** Não virou card por falta de coleção válida; é arquitetura de agentes, não neuromarketing, vídeo, prazer audiovisual ou momento de compra.

---

## 7. Conteúdo gerado por IA pode competir com conteúdo humano quando a origem está oculta — qualidade observável dominou a escolha

**Descoberta.** Um estudo exploratório de eye tracking publicado em 9 de setembro apresentou seis pares de materiais de comunicação organizacional, sempre com uma alternativa humana e outra gerada por IA, sem revelar a origem na primeira etapa.

**Evidência.** Com 20 participantes e 120 decisões participante-tarefa, alternativas geradas por IA foram selecionadas em 66,7% das decisões desse conjunto específico de estímulos. A identificação posterior da origem da IA foi correta em 57,5% dos casos. Atenção visual foi amplamente semelhante entre origens; entrevistas apontaram clareza, estrutura, profissionalismo, autenticidade e adequação comunicacional como critérios de decisão.

**Mecanismo psicológico/comportamental.** Sem um label de origem saliente, o usuário parece avaliar principalmente cues observáveis de qualidade e adequação. Origem percebida não determinou automaticamente rejeição.

**Implicação para produto.** Avaliar criativos e mensagens por qualidade observável, não por suposição de que “human-made” ou “AI-made” vencerá. Manter revisão humana e regras distintas por modalidade.

**Hipótese/experimento.** Em conteúdos do mesmo briefing, comparar opções humanas e AI-assisted às cegas antes de revelar autoria, depois medir escolha, compreensão, confiança e comportamento real.

**Riscos/limites.** Piloto N=20, apenas seis pares fixos, sem inferência populacional. Não demonstra superioridade geral de conteúdo gerado por IA.

Fonte:
- https://www.mdpi.com/2076-3387/16/9/436

**Card.** Não criado nesta rodada: a evidência é exploratória demais para virar orientação persistente no catálogo.

---

## Síntese arquitetural — Experience Engine v11

```text
usuário
   ↓
intenção + contexto + histórico
   ↓
MISALIGNMENT ROUTER
   ├── objetivo errado
   ├── método errado
   └── ambos/incerto
   ↓
AUTHORITY POLICY
   ├── sugerir
   ├── preparar
   ├── executar com confirmação
   └── executar dentro de limites
   ↓
CONTEXT / SKILL PRESENTATION POLICY
   ├── full
   ├── workflow
   ├── summary
   ├── hint
   └── none
   ↓
AGENT ORGANIZATION
   ├── paralelo quando independente
   └── sequencial quando há pré-requisito
   ↓
EXECUÇÃO
   ↓
EXPLANATION POLICY
   ├── critério
   ├── evidência específica
   ├── limite/incerteza
   └── profundidade proporcional ao risco
   ↓
SYNTHETIC PRE-EVALUATION
   ↓
validação humana + comportamento real
```

O princípio mais importante da rodada é: **o sistema precisa localizar onde está o erro antes de adaptar a experiência**. Se o objetivo está errado, mais eficiência só acelera na direção errada; se o objetivo está certo e o método está errado, refazer toda a intenção desperdiça contexto. O mesmo vale para explicações, autonomia, skills e equipes de agentes: a adaptação útil precisa atuar na camada correta.

## Cards criados

### `feedback-ia-objetivo-vs-metodo`

- Coleção: `neuromarketing`
- Por que importa: transforma um resultado neurocomputacional em uma hipótese simples de UX para recuperação de falhas em agentes.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-11-feedback-ia-objetivo-vs-metodo.md`
- SHA-256: `bfd59e5ccd156c1bb380426af9d94e06aa85e0babfe58d02e3862d5c1458d563`
- JSON: `pesquisas/design-experiencia/cards/2026-09-11-feedback-ia-objetivo-vs-metodo.json`

### `explicacao-ia-especifica-coerente`

- Coleção: `neuromarketing`
- Por que importa: fornece uma regra testável para como agentes devem justificar recomendações sem cair em rationale genérico.
- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-09-11-explicacao-ia-especifica-coerente.md`
- SHA-256: `d4f73b26c9efd132e2e60a3824baa584965c0c1be787fc161607b6e8c56e63f0`
- JSON: `pesquisas/design-experiencia/cards/2026-09-11-explicacao-ia-especifica-coerente.json`

Os dois arquivos são candidatos a `DRAFT`. Nenhuma revisão, ativação ou arquivamento foi executado automaticamente.
