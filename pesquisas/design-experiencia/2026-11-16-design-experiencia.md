# Radar de Design de Experiência — 2026-11-16

**Rodada:** Experience Engine v77 · Marketing Hub  
**Pesquisa realizada em:** segunda-feira, 16/11/2026, manhã (America/Sao_Paulo)  
**Escopo:** UX, design adaptativo, Human–AI Interaction, agentes, atenção, generative UI, multimodalidade, haptics e qualidade audiovisual.  
**Critério:** evitar repetição das rodadas até 15/11. O lote de cs.HC mais recente confirmado durante a consulta é o de sexta-feira, 13/11; trabalhos datados de 28/10, 02/11, 11–13/11 e publicações anteriores foram identificados com suas datas originais. Não tratar uma republicação/listagem como estudo novo.

## Sumário analítico

1. **Permissões de memória de IA:** uma prévia com exemplo permitido e proibido pode deixar várias políticas igualmente possíveis; 10/20 participantes de piloto não souberam prever corretamente pelo menos um uso. Uma interface de preferência precisa testar a transferência para outros contextos.
2. **Realismo de vídeos de IA:** o rastreamento ocular de duas visualizações prediz parte do julgamento humano de realismo (71,3% vs. 61,4% em comparador), mas é evidência de percepção em laboratório, não de engajamento comercial.
3. **Testes de UX de realidade aumentada:** GenAI produziu problemas de usabilidade distintos e bem descritos, porém em média menos graves que os observados por usuários reais. É triagem, não substituição de testes.
4. **Generative UI:** trabalho revisado por pares em ACL 2026 encontrou preferência humana superior para interfaces geradas em tarefas avaliadas, mas não prova melhor conclusão nem conversão em qualquer contexto.
5. **Avaliação heurística supervisionada:** expor a representação da interface e permitir correção humana alinhou resultados dos agentes à opinião de especialistas em quatro cenários Android; evidência exploratória.
6. **Discordância de avaliações humanas:** 4,1% dos itens estudados receberam a mesma classificação de gravidade dos cinco avaliadores. Distribuições de julgamento preservam incerteza melhor que um único rótulo.
7. **Fala contextual para agentes de ação:** um novo benchmark sintético testa se modelos pequenos conseguem extrair intenções e chamadas estruturadas de diálogos ambíguos; falta validação com pessoas em ambientes reais.

## 1. Memória de agentes: prévias simples podem criar falsa segurança

**Fonte e data:** Tonni e Shahid, *What a Memory Preview Cannot Tell You: Ambiguity in AI Memory Permissions*, submetido em **13/11/2026**. https://arxiv.org/abs/2611.17874

**Descoberta e evidência.** Os autores enumeraram políticas diferentes de uso de memória que poderiam produzir exatamente as mesmas respostas nas prévias de teste. Em uma configuração, 36 das 39 combinações de exemplo permitido/bloqueado continham um bloqueio que não distinguia nenhuma regra. Em piloto com 20 pessoas, dez fizeram predição errada ou expressaram dúvida sobre pelo menos um dos dois usos de sua regra.

**Mecanismo comportamental (interpretação).** Confirmar um exemplo específico pode transmitir sensação de controle sem esclarecer o alcance da permissão: lembrar dentro da mesma tarefa não é o mesmo que aproveitar a memória para outro assunto.

**Implicação de produto.** No AI Hub e em agentes que guardam preferências de compradores, exibir o alcance da memória: sessão, tarefa, projeto e usos futuros. Oferecer testes de transferência para outra tarefa e opção de corrigir a regra.

**Experimento.** Comparar prévia simples versus prévia com teste contextual de transferência; medir respostas corretas sobre permissões futuras, correções de política e reutilização indevida de informação.

**Riscos e limites.** Preprint, pequeno piloto, exemplos fictícios e espaço de regras controlado; não foi testado se prévias melhoradas de fato corrigem a compreensão nem se mudam comportamento em produção.

## 2. GazeEval: o olhar ajuda a prever realismo percebido de vídeos gerados por IA

**Fonte e data:** Shi et al., *Evaluating Visual Realism of AI-Generated Videos with Eye Tracking*, **28/10/2026**. https://arxiv.org/abs/2610.38825

**Descoberta e evidência.** Dois experimentos de Visual Turing Test somaram 90 participantes, 438 vídeos de seis geradores e 4.286 observações oculares válidas. Após assistir duas vezes, as pessoas julgavam o realismo. O classificador com informações de olhar atingiu 71,3% de acurácia para a classe de realismo percebido, contra 61,4% para um avaliador automático de características visuais. O desempenho aumenta quando há mais observadores por vídeo, e os dados de saliência visual foram mais informativos que estatísticas simples de movimentos.

**Mecanismo psicológico (hipótese).** A primeira visualização tende a explorar saliências; na segunda o espectador pode buscar detalhes anômalos e conferir sua impressão. O estudo encontrou mudanças de padrões, sem demonstrar um mecanismo neural causal.

**Implicação para o Marketing Hub.** Avaliar realismo e anomalias em amostras de vídeo IA com inspeção humana explícita. Eye tracking é opção de pesquisa consentida, não requisito para operacionalizar um pipeline de produção. Separar realismo de fidelidade do produto e de utilidade do roteiro.

**Experimento.** Revisão convencional de uma passagem versus protocolo de duas passagens, com auditoria cega de defeitos e medição de custo por vídeo aprovado. Se futuramente houver pesquisa de olhar, testar sinais agregados sob consentimento e comparar com notas humanas.

**Riscos e limites.** Os estímulos eram vídeos de cerca de cinco segundos, sem pessoas nem narrativa, e o público buscava diferenciar o real do artificial. Nenhum dado de CTA, retenção de anúncios ou vendas; privacidade sensível de biometria ocular; modelo tem erros. Não inferir emoções, persuasão ou intenção de compra a partir do olhar.

**Decisão editorial:** card candidato a DRAFT na coleção `video`, com fonte revista separada.

## 3. GenAI encontra questões de usabilidade em AR, mas tende a priorizar problemas menos graves

**Fonte e data:** Kaschub et al., *GenAI for Usability Testing of AR Apps: An Exploratory User Study*, IEEE Transactions on Visualization and Computer Graphics, publicado online em **02/11/2026**. https://doi.org/10.1109/TVCG.2026.3747494 ; resumo https://pubmed.ncbi.nlm.nih.gov/42970658/

**Descoberta e evidência.** O estudo avaliou um aplicativo e uma tarefa de realidade aumentada com 48 avaliações: 24 usuários humanos e 24 execuções independentes de GenAI. Três especialistas avaliaram a gravidade dos problemas identificados. A IA produziu mais problemas distintos e formulados rapidamente, mas com gravidade média significativamente menor do que os relatados por pessoas.

**Mecanismo técnico/comportamental (interpretação).** A inspeção de quadros estáticos pode identificar inconsistências descritíveis, mas não reproduz plenamente desconforto, expectativas ou dificuldades vivenciadas em interação real.

**Aplicação.** Usar avaliadores sintéticos para uma primeira triagem de telas no Marketing Hub; escalar problemas críticos para testes humanos com tarefas e objetivos definidos.

**Experimento.** Comparar apenas testes humanos versus triagem de IA + validação humana: taxa de problemas críticos detectados, falsos positivos, tempo e custo.

**Limites.** Apenas uma tarefa e aplicativo AR; GPT-5.1 em protocolos experimentais; não há demonstração de equivalência com usuários reais nem prova de melhoria comercial.

## 4. Generative UI: preferências por interfaces não equivalem a sucesso universal

**Fonte e data:** Chen et al., *Generative Interfaces for Language Models*, Findings of ACL **julho de 2026** (evidência complementar revisada por pares). https://aclanthology.org/2026.findings-acl.74/

**Evidência.** O artigo compara interfaces geradas dinamicamente com respostas conversacionais em diversos tipos de tarefa. Os autores reportam vantagem de preferência humana, de até 72% segundo a medida descrita no resumo, variando conforme a condição avaliada. O resultado não deve ser lido como +72% em produtividade ou vendas.

**Mecanismo interpretativo.** Controles visuais e estruturas específicas da tarefa podem reduzir esforço para localizar, comparar e corrigir informação que ficaria dispersa numa conversa textual.

**Aplicação.** No AI Hub, experimentar tabelas, controles editáveis e representações de progresso geradas a partir da intenção, com estado persistente e confirmação antes de ações irreversíveis.

**Experimento.** Comparar chat versus interface contextual em tarefas de configuração e verificação, medindo conclusão correta, erros, número de interações, duração, satisfação e acessibilidade.

**Riscos e limites.** A preferência é dependente de tarefas e protótipos; interfaces geradas dinamicamente também podem ser inacessíveis, inconsistentes, inseguras ou difíceis de auditar.

## 5. Avaliação heurística de UX: inspeção supervisionada em vez de agentes sem revisão

**Fonte e data:** Lee, Baek e Park, *Structured Human-AI Teaming for UX Heuristic Evaluation with Human-in-the-Loop Supervision*, publicado em **21/08/2026** (evidência complementar revisada por pares). https://doi.org/10.1177/10711813261475226

**Evidência.** Quatro cenários de aplicativos Android compararam pipeline de dois agentes automatizados com versão na qual um supervisor corrigia a representação extraída das telas e revisava problemas heurísticos. A versão supervisionada teve resultados mais próximos do julgamento dos especialistas humanos.

**Mecanismo proposto.** Corrigir representações intermediárias evita que interpretações erradas da tela contaminem a avaliação. O benefício não exige gerar mais alertas, e sim alertas endossáveis.

**Aplicação.** Registrar componentes de UI, suposições do agente, evidência visual e divergências corrigidas; revisão humana de problemas de maior gravidade.

**Experimento.** Teste cruzado em dez ou mais fluxos reais, com vários revisores, tempo de inspeção, taxa de problemas confirmados e carga de trabalho.

**Limites.** Pesquisa exploratória com quatro cenários, um supervisor e sem medição de esforço operacional. Não demonstra redução de defeitos ou vantagem comercial em produção.

## 6. Quando os avaliadores discordam, apagar a discordância gera falsa certeza

**Fonte e data:** Bisharat et al., *Calibrate, Don't Retrain: Decomposing the Value of Annotator Disagreement in Cyberbullying Severity*, submetido em **11/11/2026**. https://arxiv.org/abs/2611.15010

**Evidência.** No conjunto de classificação de severidade estudado, os cinco avaliadores concordaram integralmente em apenas 4,1% dos comentários. O artigo relata que modelar uma distribuição de avaliações e calibrar probabilidades preservou melhor a pluralidade de julgamentos em três datasets (gravidade, abuso e ironia), sem precisar refazer todo o treino.

**Mecanismo interpretativo.** Problemas subjetivos não possuem necessariamente um único valor verdadeiro; a maioria pode esconder minorias e os diferentes graus de incerteza.

**Aplicação.** Em avaliações de criativos ou moderação de conteúdo, armazenar distribuição e justificativas de juízos humanos, não apenas uma média. Diferenciar discordância genuína de erro técnico verificável.

**Experimento.** Pedir avaliação independente de criativos e comparar decisões baseadas em nota agregada versus distribuição e variabilidade, acompanhando consistência e mudanças após revisão.

**Limites.** O domínio estudado é cyberbullying, não publicidade. Distribuição de opiniões não comprova eficácia emocional ou comercial, nem substitui regras de segurança.

## 7. Talk to Act: interpretar intenção conversacional como chamadas estruturadas

**Fonte e data:** Krishnareddy et al., *Talk to Act: A Synthetic Multi-Turn Dialogue Benchmark for Human Intent Extraction and Action Planning in Robotics*, submetido em **12/11/2026**. https://arxiv.org/abs/2611.17549

**Evidência.** O benchmark reúne diálogos sintéticos anotados com ações, referências a objetos e planos. Os autores avaliaram modelos abertos entre 270 milhões e 8 bilhões de parâmetros sem ajuste específico e encontraram desempenho promissor em algumas tarefas de extração de intenção.

**Mecanismo técnico.** Manter referências entre turnos e produzir argumentos estruturados permite separar o que a pessoa quer da ação concreta que será proposta ao executor.

**Aplicação.** No AI Hub, validar chamadas a ferramentas derivadas de conversas longas, incluindo referências ambíguas, estado da tarefa, autorização e confirmação para ações não reversíveis.

**Experimento.** Comparar entendimento por modelo pequeno e por modelo maior em diálogos reais consentidos; medir chamadas corretas, ambiguidades não identificadas e segurança das ações.

**Limites.** Dados sintéticos e tarefas de robótica; resultados não provam confiabilidade em operação de computador nem capacidade de executar ações com segurança.

---

## Decisão editorial, coleções e integração

A versão atual de `harness-library-api/docs/guia-uso-api-cards.md` foi consultada diretamente na `main` em 16/11/2026. Somente as coleções `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c` são aceitas. Não foi inventada `design-experiencia`.

**Um card candidato a DRAFT:** `olhar-realismo-percebido-videos-ia` em `video` (avaliação perceptual de material audiovisual produzido por IA; não é card de neuromarketing nem de prazer). O documento-fonte foi criado antes do JSON; o SHA-256 de seus bytes é `3e5a862e83cb42ce4f1895a7ee947010646576a0c60cf6773ee2f825bc148c25`.

- Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-11-16-realismo-video-rastreamento-ocular.md`
- JSON: `pesquisas/design-experiencia/cards/2026-11-16-realismo-video-rastreamento-ocular.json`
- `publishedOn=2026-11-16`, `validUntil=2026-12-31`, `sourceKind=MARKDOWN`, sem campos extras e sem ativação/revisão/arquivamento automático.

**Sem card por falta de coleção realmente aderente:** prévias de memória de IA, UX sintético de AR, generative UI, inspeção heurística supervisionada, discordância de avaliadores e interpretação de intenção de agentes. Seus mecanismos técnicos não devem ser artificialmente convertidos em um card de neuromarketing ou de momentos-de-compra.

**Observação:** publicar arquivos no GitHub não é confirmação independente do cadastro na API. A automação de ingestão dos cards, caso executada, deve criar apenas DRAFT.

## Fontes principais

- [Listagem recente de cs.HC](https://arxiv.org/list/cs.HC/recent)
- [Permissões de memória](https://arxiv.org/abs/2611.17874)
- [GazeEval — realismo audiovisual](https://arxiv.org/abs/2610.38825)
- [Teste de UX AR com GenAI — IEEE](https://doi.org/10.1109/TVCG.2026.3747494)
- [Generative Interfaces — ACL](https://aclanthology.org/2026.findings-acl.74/)
- [Supervisão humana de heurísticas UX](https://doi.org/10.1177/10711813261475226)
- [Discordância e calibração](https://arxiv.org/abs/2611.15010)
- [Talk to Act](https://arxiv.org/abs/2611.17549)
