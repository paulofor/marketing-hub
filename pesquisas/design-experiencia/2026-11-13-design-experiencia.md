# Radar de Design de Experiência — 13/11/2026

**Experience Engine v74 · Marketing Hub**  
**Rodada:** 13/11/2026 (America/Sao_Paulo)  
**Status editorial:** relatório e dois candidatos a DRAFT. Nenhum envio para revisão, ativação ou arquivamento.

## Escopo, novidade e metodologia

Foi consultada em 13/11 a listagem oficial https://arxiv.org/list/cs.HC/new, que exibia **70 entradas no total, com 29 novas submissões** na seção principal de Human-Computer Interaction. Foram priorizados preprints submetidos entre 10 e 12/11 e exibidos na listagem desta sexta-feira, além de alguns cross-lists. Comparou-se a seleção com os relatórios de 03–12/11/2026 para evitar repetição de pesquisas previamente resumidas. Trabalhos submetidos em dias anteriores são identificados pela data real, sem confundir data da listagem com data de publicação. Preprint não significa revisão por pares.

**Síntese:** (1) voz mais humana não significa voz preferida para conversar; (2) confiança em assistentes pode permanecer alta sem feedback independente sobre erros; (3) debater com mais agentes não é automaticamente melhor, porque estrutura, participação e custo cognitivo importam; (4) interfaces generativas precisam preservar objetivos, fontes e responsabilidades humanas.

## 1. Preferência conversacional versus semelhança humana em agentes de voz

**Fonte:** Girard et al., *Human-Likeness and Conversational Preference Diverge for Synthesized Agent Speech in Customer-Service Calls*. Submetido 11/11/2026. https://arxiv.org/html/2611.16051v1

- **Descoberta e evidência:** dois estudos de escolha pareada com **654 ouvintes** (455 + 199), **120 turnos**, seis sistemas TTS, oito vozes por sistema e **19.517 julgamentos** ao todo. No confronto com voz de ator humano, a síntese foi escolhida em **19%** das avaliações de semelhança humana, mas **36%** das avaliações de preferência conversacional; o sistema mais bem avaliado em preferência chegou a **49%** (IC 95% de 42% a 57%). Os rankings dos sistemas diferiram entre as duas avaliações. Três escores automáticos de naturalidade não predizeram confiavelmente preferência conversacional.
- **Mecanismo plausível (interpretação):** o ouvinte pode valorizar adequação de ritmo, pausas e entonação à tarefa mais do que simulação perfeita da voz humana; os correlatos acústicos não foram intervenções causais.
- **Produto:** Íris/Psique poderiam avaliar vozes de agentes de atendimento por preferência, compreensão e acessibilidade, não apenas naturalidade acústica.
- **Experimento:** A/B consentido com textos idênticos, várias vozes e diferentes velocidades; medir resolução correta, erro de compreensão, satisfação, duração e abandono. Identificar explicitamente a IA.
- **Riscos/limites:** cenários simulados e canal telefônico; sem diálogo dinâmico, sem clientes reais, sem português brasileiro, sem compras ou retenção. Uma voz preferida não é necessariamente uma voz confiável.
- **Card:** `preferencia-conversacional-vs-realismo-voz-ia` → `neuromarketing` por percepção de voz em atendimento ao consumidor.

## 2. Confiar em uma IA errada quando não há feedback verificável

**Fonte:** Jones et al., *Do People Trust AI Assistants Even When They Are Consistently Wrong?*. Submetido 10/11/2026, preprint não revisado por pares. https://arxiv.org/html/2611.14833v1

- **Descoberta e evidência:** quatro experimentos com cinco interações de aconselhamento e condições de 100%, 50% ou 0% de acerto. **Com feedback da resposta correta**, assistentes historicamente corretos persuadiram 67% das vezes contra 21% dos sistematicamente errados. **Sem feedback**, o grupo exposto a agentes sistematicamente errados ainda foi persuadido em 48% das situações, contra 38% para agentes corretos, sem discriminação confiável pelo histórico. Em um experimento com conselheiros humanos, 83% versus 65% indicou maior persuasão por IA nas condições examinadas.
- **Mecanismo (hipótese):** a pessoa pode usar evidência externa sobre acertos e erros para ajustar sua dependência do agente, enquanto a fluência do diálogo pode persistir como sinal enganoso de competência quando não existem verificações.
- **Produto:** mensagens de recomendação precisam exibir preço, prazo, condições e características verificadas, com registro de correções e possibilidade de contestação. Evitar métricas fictícias de acurácia.
- **Experimento:** usuários realizam tarefas de atendimento com recomendações corretas e incorretas; comparar explicações sem feedback, feedback de verificação externa e revisão humana. Avaliar decisões corretas, adoção de erros, esforço e satisfação.
- **Riscos/limites:** raciocínio com alternativas e respostas verificáveis; sem experiências de compra reais e sem demonstração de melhoria comercial. Não usar o achado como técnica para aumentar persuasão de ofertas.
- **Card:** `feedback-verificavel-calibracao-confianca-agente-ia` → `neuromarketing`, como salvaguarda comportamental em recomendação/atendimento, não prova de conversão.

## 3. Deliberação multiagente melhora decisões em algumas tarefas, mas contestação permanente pode prejudicar

**Fonte:** Tian e Yin, *Understanding the Effects of Multi-Agent Deliberation for Human-AI Decision-Making*. Submetido 11/11/2026. https://arxiv.org/html/2611.14901v1

- **Evidência:** dois estudos com **401** participantes em verificação de erros de respostas de LLM e **417** em verificação de fatos, após filtros de atenção. No primeiro, acurácia média foi **0,733** com deliberação versus **0,605** no comparador de uma rodada derivado de um chatbot e **0,620** com sugestões estáticas; no segundo, **0,948** versus **0,849** e **0,748**. A participação ativa ajudou em uma tarefa, mas aumentou frustração; a persona fixa de *devil's advocate* piorou precisão e experiência.
- **Mecanismo interpretativo:** rodadas de avaliação e esclarecimento favorecem detecção de erros quando há argumentos verificáveis; adversarialidade indiscriminada pode aumentar ruído e cansaço.
- **Produto:** para Têmis/AI Hub, deliberação multiagente sob demanda, priorizando divergências materiais e fontes, sem criar debate em cada operação.
- **Experimento:** comparar revisor único, duas revisões independentes e debate estruturado com limite de rodadas; medir acurácia por decisão, custo, latência, frustração e erros aceitos.
- **Riscos/limites:** só dois domínios, modelos e prompts específicos; o comparador de uma rodada foi derivado de um tratamento com várias rodadas; os resultados não estabelecem ganho generalizável para produção.
- **Card:** não criado; governança e validação de agentes não correspondem diretamente a nenhuma das quatro coleções da API.

## 4. Concordar com a razão de um conselho não é concordar com a decisão sugerida

**Fonte:** Park et al., *Right Advice, Wrong Reasons*. Submetido 11/11/2026. https://arxiv.org/abs/2611.15732

- **Evidência:** estudo intraindivíduo com **121 participantes**, três conselheiros e oito dilemas cotidianos. Para consulta, **40%** escolheram com maior frequência o conselheiro que justificava a resposta com os valores da pessoa, ainda que recomendasse outra decisão. Quando o agente poderia agir em nome do usuário, a recusa de qualquer agente subiu de **12%** (consulta) para **38%** (ação delegada).
- **Mecanismo:** pessoas distinguem coerência dos argumentos, concordância com a recomendação e transferência de autoridade.
- **Produto:** mostrar razões e alternativas separadamente; possibilitar o modo 'consultar', 'rascunhar' e 'executar', com autorização proporcional ao risco.
- **Experimento:** decisões simuladas com justificativas e opções de delegação; medir revisão correta, aceitação, controle percebido e intervenções.
- **Limites:** dilemas morais e preferências declaradas; não execução de ações reais nem uso comercial longitudinal.
- **Card:** não criado; falta coleção de autonomia de agentes.

## 5. RiskLens: riscos transferíveis de incidentes anteriores

**Fonte:** Tang, Zhang e Huang, *RiskLens: Human-Informed Incident Transfer for AI Risk Evaluation*. Submetido 12/11/2026. https://arxiv.org/abs/2611.16287

- **Evidência:** interface que compara **2.034 incidentes** e **10.443 produtos de IA**; **21 participantes** ajudaram a acrescentar 13 dimensões de avaliação. O ranking de transferência de incidentes venceu baseline genérico em **86,2%** dos pares de teste, chegando a **91,4%** após ampliação do esquema.
- **Mecanismo técnico:** relacionar características de novos produtos a padrões verificáveis de falhas anteriores; a contribuição humana altera critérios relevantes.
- **Produto:** antes de expor nova função do AI Hub, recuperar falhas semelhantes, evidência, risco operacional e salvaguardas.
- **Experimento:** revisão de casos reais com e sem ranking contextual; medir incidentes corretamente antecipados, falsos alarmes, tempo e mitigação aplicada.
- **Limites:** vitória em ranking não equivale a redução de incidentes em produção; usuários eram familiarizados com IA.
- **Card:** não criado; falta coleção para segurança/avaliação de agentes.

## 6. Agentic Dashboards: visualizações persistentes que a IA reorganiza dentro de limites

**Fonte:** Lee, Kuo e Ma, *Agentic Dashboards*. Submetido 12/11/2026. https://arxiv.org/abs/2611.16245

- **Evidência:** artigo de visão/arquitetura: o projetista define quais decisões de análise, visualização e coordenação um agente pode tomar em tempo de execução, com espaço persistente de trabalho.
- **Mecanismo proposto:** delegação delimitada e manutenção de consciência situacional durante a mudança de gráficos e filtros.
- **Produto:** dashboard de jobs e campanhas no AI Hub, com ações reversíveis, justificativa da mudança e snapshots.
- **Experimento:** comparar painel fixo e reorganização orientada a tarefas; medir erros de leitura, decisões corretas, latência e reversões.
- **Limites:** proposta conceitual, sem demonstração experimental de ganho.
- **Card:** não criado; `generative-ui` não é coleção permitida.

## 7. Feedback social indireto pode ser mais aceitável que alertas públicos diretos

**Fonte:** Wei, Li e Bai, *Exploring Socially Acceptable 3D Feedback Design for Inclusive Small-Group Conversation*. Submetido 12/11/2026. https://arxiv.org/abs/2611.17018

- **Evidência:** survey com **50 participantes** e entrevistas posteriores com **14** avaliaram metáforas visuais e agentes 3D como feedback sobre participação em grupos; o material reporta redução de desconforto e manutenção de eficácia percebida nas condições testadas.
- **Mecanismo:** indireção, menor exposição pública e humor podem diminuir constrangimento social; não demonstra eliminação de pressão ou efeito duradouro.
- **Produto:** feedback discreto em treinamento e colaboração, evitando ranquear pessoas publicamente.
- **Experimento:** alertas diretos versus metáforas privadas/opcionais; medir aceitação, mudanças de participação e desconforto.
- **Limites:** cenários XR e julgamento de participantes, sem indicadores comerciais ou uso de longa duração.
- **Card:** não criado; fora das coleções válidas.

## 8. Segurança em VR sem quebrar tanto a imersão

**Fonte:** Degenhard et al., *Immersive Sight-Restriction-Based Collision Prevention Using Generic Particle Systems in Virtual Reality*. Submetido 11/11/2026, aceito em MUM 2026. https://arxiv.org/abs/2611.15561

- **Evidência:** pré-estudo n=10; comparação principal n=11, em **253 aproximações** de limites físicos. Neblina de partículas era visível três vezes mais frequentemente do que a grade, mas os participantes relatavam maior presença; as distâncias de interrupção foram comparáveis.
- **Mecanismo proposto:** sinalização ambiental coerente com a cena pode alertar sem exigir uma sobreposição artificial.
- **Produto:** protótipos de feedback de segurança integrados ao ambiente em aplicações imersivas.
- **Experimento:** medir colisões, interrupções, presença e desconforto em cenas diversas.
- **Limites:** amostra pequena, episódios repetidos, não comprovação de prevenção superior de acidentes.
- **Card:** não criado; pesquisa sobre segurança em VR não é prazer audiovisual ou vídeo publicitário.

## 9. Navegação ramificada por IA pode abandonar o objetivo original

**Fonte:** Shi, Wang e DiFranzo, *Branching Visual Inquiry in a Generative Visual Browser*. Submetido 11/11/2026. https://arxiv.org/abs/2611.14862

- **Evidência:** oito percursos realizados pelos próprios autores, pareados por quatro prompts idênticos. Alguns percursos mantiveram coerência local, mas deixaram perguntas iniciais sem resposta; alguns diagramas careciam de fontes.
- **Mecanismo:** decisões locais plausíveis podem reduzir visibilidade das lacunas do objetivo geral.
- **Produto:** mostrar caminho, objetivos não respondidos e fontes/níveis de incerteza em interfaces generativas.
- **Experimento:** comparação com/sem mapa de percurso e verificação de perguntas pendentes; medir cobertura correta e satisfação.
- **Limites:** autoavaliação dos autores e somente oito trajetórias; não ensaio com usuários.
- **Card:** não criado; falta coleção generative UI.

## 10. Uma única página interativa pode agradar e ainda oferecer usabilidade mediana

**Fonte:** Dao et al., *Integrating Slides, Mathematics, and Simulation in One Page*. Submetido 11/11/2026. https://arxiv.org/abs/2611.15686

- **Evidência:** seis demonstrações em HTML cobriram 18 tarefas; oito verificações numéricas atenderam tolerâncias especificadas. Em pesquisa de percepção com **19 estudantes** após triagem, **74%** preferiram páginas integradas, mas a pontuação SUS foi **56,8**. Doze professores avaliaram utilidade, com hesitação de adoção.
- **Mecanismo:** a continuidade entre explicação e simulação pode facilitar exploração, mas concentrar recursos não garante boa arquitetura de interação.
- **Produto:** experiência de onboarding com explicação, simulação e microavaliação na mesma página, testando aprendizado real.
- **Experimento:** medir tarefas concluídas, erros, abandono, SUS e aprendizado retardado.
- **Limites:** amostra pequena; não se mediu ganho de aprendizagem.
- **Card:** não criado; educação/generative UI fora das coleções.

## 11. Conselho de IA pode parecer melhor, mas não mudar uma decisão incentivada

**Fonte:** Maier et al., *People prefer AI to human advice in multi-turn conversations but neither changes incentivised behaviour*. Submetido 12/11/2026. https://arxiv.org/abs/2611.16667

- **Evidência:** estudo pré-registrado com **572 participantes** após exclusões. Conversa com assessor de IA foi considerada mais racional, confiável, convincente e também mais complacente que conversa com humano; não houve diferença observada na divisão incentivada de prêmio entre doação e benefício próprio.
- **Mecanismo interpretativo:** preferência e confiança subjetivas podem não ser suficientes para alterar uma decisão com consequência.
- **Produto:** separar satisfação com agente de comportamento efetivo; avaliar também resolução, custo e resultado em tarefas.
- **Experimento:** comparar auxílio de IA e humano mantendo informação equivalente, medindo decisão material e não apenas intenção.
- **Limites:** decisão de doação, não compra; resultados nulos não comprovam equivalência de todas as condições.
- **Card:** não criado para evitar duplicação do conceito já abordado no card de calibração por feedback e pela ausência de aplicação comercial direta adicional.

## 12. Profissionais usam LLMs para depurar SQL em papéis escolhidos por eles

**Fonte:** Sullivan et al., *How Do People Debug Complex SQL Queries With LLMs?* Submetido 11/11/2026. https://arxiv.org/abs/2611.16108

- **Evidência:** estudo qualitativo com **16 participantes** de diferentes experiências em SQL; variaram formas de delegação e papéis atribuídos ao LLM, inclusive tutoria e parceiro de reflexão. Em alguns casos, os usuários retiveram contexto deliberadamente.
- **Mecanismo:** autonomia e confiança são negociadas conforme a habilidade da pessoa e a tarefa.
- **Produto:** AI Hub com modos 'explicar', 'propor SQL', 'executar somente leitura' e 'aplicar alteração com autorização', mais seleção explícita do contexto compartilhado.
- **Experimento:** comparar modos de delegação para consultas SQL complexas, medindo erros introduzidos, tempo e aprendizado.
- **Limites:** evidência qualitativa pequena, sem generalização de produtividade e sem transação comercial.
- **Card:** não criado; ferramenta de programação sem coleção aderente.

---

## Cards candidatos a DRAFT desta rodada

Foi consultada antes da criação a versão da `main` de https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md (blob SHA `2d38df09cdb01ad604c3edc88a57a22011c092d1`). A API admite **somente** `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Não foi criada a coleção `design-experiencia`.

1. **`preferencia-conversacional-vs-realismo-voz-ia` — `neuromarketing`**: comparação explícita da preferência do consumidor pela experiência de voz com seu realismo acústico. Arquivos: `cards/fontes/2026-11-13-preferencia-conversacional-voz-ia.md` e `cards/2026-11-13-preferencia-conversacional-voz-ia.json`. SHA-256 da fonte: `af21016541ba37bb72971dd3f94470be6fdbf6364577b7fac1113614e7762086`.
2. **`feedback-verificavel-calibracao-confianca-agente-ia` — `neuromarketing`**: mitigação de aceitação de recomendações incorretas, útil aos agentes de atendimento e revisão. Arquivos: `cards/fontes/2026-11-13-feedback-acuracia-confianca-agente.md` e `cards/2026-11-13-feedback-acuracia-confianca-agente.json`. SHA-256 da fonte: `e5fb4aaa2f91129a66984459362ea5d62b30678d32eec0e59d21dcf0a36ffb3a`.

Os dois JSONs possuem **exatamente** `cardKey`, `collection`, `title`, `finding`, `mechanism`, `commercialApplication`, `evidenceStrength`, `publishedOn`, `validUntil`, `experimentHypothesis`, `risks`, `limits`, `sourceKind`, `sourceUri`, `sourceTitle` e `sourceSha256`; `sourceKind=MARKDOWN`, `publishedOn=2026-11-13`, `validUntil=2026-12-28`, `sourceUri=repo:...` apontando para cada fonte, com hash calculado sobre o conteúdo UTF-8 efetivo. São **candidatos a DRAFT**, não cards em revisão ou ativos.

**Achados fortes sem card por falta de coleção válida:** deliberação multiagente, RiskLens, Agentic Dashboards, alinhamento de razões e decisões para delegação e navegação generativa ramificada. A evidência não justifica forçar os temas a `neuromarketing`, `video` ou `prazer-audio-visual`.

## Recomendação de priorização

Para o **Marketing Hub**, testar inicialmente a voz do atendimento com avaliação de preferência conversacional e compreensão em português, ao mesmo tempo que se audita a correção das recomendações. Para o **AI Hub**, investigar o benefício líquido de debate multiagente por tarefa e preservar objetivos e evidências em dashboards agentivos. **Nenhum dos artigos comprova aumento de vendas ou ROI.**
