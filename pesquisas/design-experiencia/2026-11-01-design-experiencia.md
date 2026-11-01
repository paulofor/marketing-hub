# Radar de Design de Experiência — 01/11/2026
**Experience Engine v62 | Marketing Hub**

**Recorte:** domingo, 1º de novembro de 2026 (America/Sao_Paulo). Sem novo lote regular de cs.HC no fim de semana; esta rodada examina artigos submetidos entre 27 e 29/10 e divulgados na listagem de 30/10/2026, priorizando títulos ainda não tratados em 30 e 31/10. Índice: https://arxane.org/cs.HC . A data da submissão do preprint não substitui a data original de publicação do trabalho quando houver artigo anterior.

## Síntese

A questão central da rodada é a distância entre **gerar algo** e **assegurar que a experiência humana funciona de ponta a ponta**. Em vídeo, audiodescrição pode ser semanticamente correta e ainda falhar no encaixe temporal e no ritmo da narrativa. Em agentes, o software pode executar uma ação correta sobre uma informação que mudou, ou satisfazer a forma de uma tarefa sem preservar sua intenção. Em interfaces, representações persistentes e assistência contextual podem ajudar, mas o custo de validação também importa.

## 1. MTG-AD — audiodescrição inline depende de ritmo, silêncio e conteúdo

**Fonte:** Venugopalan et al., *Mind The Gap (MTG-AD): Dataset and Fine-grained Evaluation for End-to-End Inline Audio Descriptions*, submetido em 29/10/2026, NeurIPS 2026. https://arxiv.org/html/2610.39955v1

**Descoberta e evidência:** MTG-AD contém 433 vídeos gerados por usuários, 35,9 horas e 8.126 segmentos de audiodescrição produzidos com colaboração de profissionais videntes e cegos. A pesquisa avaliou nove modelos multimodais e métricas de conteúdo e sincronização aplicadas ao áudio sintetizado. Modelos descreveram eventos visuais e reduziram sobreposição com falas, mas ainda apresentaram problemas de ritmo e continuidade. Na avaliação com sete pessoas cegas ou com baixa visão, as versões profissionais foram ligeiramente preferidas, embora as automáticas fossem consideradas úteis à compreensão.

**Mecanismo interpretativo:** narrar informações visuais essenciais que não estão disponíveis no áudio, dentro de pausas naturais, pode reduzir lacunas de compreensão sem colidir com diálogos. O estudo não isola causalmente cada componente.

**Produto/experimento:** em Apolo, oferecer faixa opcional de audiodescrição em vídeos demonstrativos: selecionar informações essenciais, gerar voz, verificar temporização e rever com participantes cegos ou com baixa visão. Comparar compreensão, tempo com sobreposição de fala, preferência e interrupções. Usar pagamentos e CTA somente como métricas adicionais se houver experimento comercial próprio.

**Riscos/limites:** vídeos em inglês e filtrados para permitir pausas; n=7 na avaliação focal de usuários cegos/baixa visão; modelos podem omitir ou inventar informação e prejudicar o áudio. Não há evidência de melhora de conversões. **Card criado na coleção video** como hipótese de acessibilidade e qualidade audiovisual, não como prova comercial.

## 2. EnvOS — um agente pode estar certo sobre um estado que já mudou

**Fonte:** Kadu et al., *EnvOS: Auditing Computer-Use Agents When the World Moves On*, submetido em 29/10/2026. https://arxiv.org/abs/2610.40246

**Descoberta e evidência:** benchmark certificado com mudanças de ambiente durante tarefas. Em agendamento de clínicas, Claude Opus 5 resolveu 0/16 casos sem informação adicional, 2/16 com lembrete para rever e 15/16 quando o estado atualizado foi mostrado. Na avaliação do próprio verificador, o pipeline detectou 25/39 episódios adulterados de um conjunto externo, contra quatro por checagem simples de logs.

**Mecanismo:** raciocínio correto sobre observação antiga continua produzindo ação incorreta. A interface e a atualização do estado importam tanto quanto o modelo.

**Produto/experimento:** no AI Hub, utilizar leitura just-in-time de estado crítico, validação de versão/etag e checagem de invariantes antes de salvar. Simular alteração de requisitos, preços ou status durante a execução e medir ações sobre dados obsoletos, retrabalho e reversões.

**Riscos/limites:** cenários de benchmark, amostras pequenas e resultados variáveis por modelo; confirmação visual de estado não substitui controle transacional de backend. **Sem card: governança de agentes não é coleção válida.**

## 3. Comparação humana de textos: sobreposição reduz memória de curto prazo

**Fonte:** Blanchard et al., *Usable String Matching by Humans through Overlaid Presentation*, submetido em 29/10/2026. https://arxiv.org/abs/2610.39084

**Evidência:** dois experimentos online com 178 participantes compararam caracteres e trechos em apresentações sobrepostas ou lado a lado. A sobreposição permitiu comparação mais rápida e melhor precisão em strings longas; mesmo assim, as taxas médias de erro ficaram em pelo menos 4%.

**Mecanismo interpretativo:** converter comparação de memória em percepção de diferenças visuais pode reduzir carga cognitiva.

**Produto/experimento:** para Têmis e o editor do Marketing Hub, oferecer comparação visual de versões de textos legais, preços e condições, sem dispensar diff determinístico. Medir tempo de revisão e erros humanos remanescentes.

**Limites:** tarefas de comparação textual, não experiência de compra; erros residuais inviabilizam uso como única barreira de segurança. **Sem card: UX de verificação não é coleção disponível.**

## 4. LogicTree — objetos persistentes podem preservar a estrutura de uma criação

**Fonte:** Zhu et al., *LogicTree: Object-Oriented Writing for Structured Human-AI Academic Authoring*, 28/10/2026. https://arxiv.org/abs/2610.38136

**Evidência:** protótipo Overleaf com objetos de escrita hierárquicos, auxílio para geração e revisão localizado por problema. Estudo intraindivíduo com 12 participantes indicou menor carga de trabalho e maior qualidade lógica medida que o baseline conversacional.

**Mecanismo:** representar argumentos e componentes como objetos manipuláveis reduz a necessidade de reconstruir a estrutura por prompts sucessivos.

**Produto/experimento:** organizar briefings, hipóteses, capítulos, criativos e critérios como entidades versionadas em workspace do AI Hub; comparar com chat puro em tarefas equivalentes.

**Limites:** amostra pequena e escrita acadêmica. **Sem card: generative UI/colaboração não possui coleção própria.**

## 5. Assistência proativa: decidir quando NÃO interromper faz parte da UX

**Fonte:** Talukder e Haque, *When to Assist and What to Suggest*, submetido em 29/10/2026. https://arxiv.org/abs/2610.40611

**Evidência:** avaliação de 28.058 pontos de decisão para assistência proativa e 1.144 avaliações humanas em 32 diálogos. Um decodificador 1B integrou decisão de ajudar e geração de sugestão; apresentou 1,31–1,76× menor tempo do que pipeline 1B comparável em métricas automáticas, mas avaliações humanas ficaram abaixo da configuração maior de 8B.

**Mecanismo proposto:** intervenções excessivas criam custo de atenção; o histórico relevante pode tornar sugestões mais adequadas, mas também exige cuidado com privacidade e conflitos com a intenção presente.

**Produto/experimento:** criar política de intervenção explícita para agentes, incluindo pausa, relevância e reversibilidade; medir interrupções injustificadas, uso real das sugestões e resultado da tarefa.

**Limites:** benchmark específico e métricas automáticas não equivalem a satisfação generalizada. **Sem card: Adaptive UX não é coleção válida.**

## 6. Decompose, Don't Expand — deslocar tarefas determinísticas para código

**Fonte:** Gowda, *Decompose, Don't Expand: Capability-Independent AI Systems by Construction*, submetido em 28/10/2026. https://arxiv.org/abs/2610.38144

**Evidência:** experimento em um domínio com nove modelos, incluindo um checkpoint local de 494M parâmetros. Ao resolver contexto e seleção de operações em código determinístico, oito ou nove modelos passaram a ter resultados iguais nas etapas extraídas; a rotulação sem decomposição continuou dependendo da capacidade do modelo.

**Mecanismo técnico:** quando uma subtarefa já tem definição determinística, delegá-la ao LLM aumenta variabilidade sem necessariamente acrescentar valor.

**Produto/experimento:** no AI Hub, deslocar validação de esquema, manipulação de arquivos, checagem de diffs e políticas de autorização para backend executável, mantendo LLM para interpretação aberta. Comparar custo por execução correta e falhas entre modelos menores e maiores.

**Limites:** demonstração empírica central em um domínio e exemplos ilustrativos de outros; não demonstra que qualquer pipeline pode ser decomposto. **Sem card: engenharia de agentes fora das coleções.**

## 7. IA como mediadora entre criadores

**Fonte:** Kim, Kim e Choi, *Generative AI as a Mediator in Creator Collaboration*, submetido em 28/10/2026, publicado em PACMHCI/CSCW 2026. https://arxiv.org/abs/2610.38488

**Evidência:** estudo multimétodo com dez roteiristas e dez designers visuais em desenvolvimento de jogos. Os participantes relataram que a IA ajudou a alinhar intenções entre especialidades, mas também poderia enfraquecer comunicação direta e confiança entre colaboradores.

**Mecanismo:** tradução de intenções entre formas de representação pode criar entendimento compartilhado; uma mediação excessiva pode ocultar discordâncias importantes.

**Produto/experimento:** testar mediação estruturada entre agentes de roteiro, imagem e aprovação de criativos, registrando os conflitos reais e a decisão final do operador. Medir retrabalho e qualidade de handoff.

**Limites:** pesquisa qualitativa num contexto específico; não prova ganhos de produtividade comercial. **Sem card: colaboração entre agentes não é coleção aceita.**

## 8. Lookover — muito engajamento visível pode esconder pouco diálogo substantivo

**Fonte:** Zhou et al., *Lookover: Scaffolding Student Participation in Social Annotation*, 29/10/2026, poster UIST Adjunct 2026. https://arxiv.org/abs/2610.39363

**Evidência:** entre 2.873 comentários de uma disciplina, 53,5% não receberam resposta e 38,3% surgiram nas últimas 12 horas antes do prazo. Os autores propõem uma interface para encontrar discussões relevantes e oferecer perguntas que aprofundem a participação; o estudo inicial com o protótipo envolveu apenas cinco pessoas.

**Mecanismo:** quantidade de comentários não mede construção de conhecimento; localizar uma discussão e contribuir substantivamente são esforços distintos.

**Produto/experimento:** em produtos com comunidades ou experiências educacionais, comparar métricas de postagens com respostas substantivas, resolução de dúvidas e retorno voluntário.

**Limites:** uma turma específica, sem evidência de conversão nem comprovação de efeito causal do protótipo. **Sem card: comunidade/educação não são coleções válidas.**

## 9. ADRDS — narrativa com ramificação dirigida pela audiência

**Fonte:** Lo Duca, *Towards Audience-Driven Real-Time Data Storytelling*, submetido em 29/10/2026 (CHIRA 2026). https://arxiv.org/abs/2610.39658

**Proposta:** modelo de narrativa com decisões e ramificações definidas previamente, nas quais respostas de audiência alteram a sequência apresentada. O trabalho descreve arquitetura, regras de cobertura, consenso e incerteza; não demonstra resultados de compras ou testes controlados de UX.

**Mecanismo hipotético:** participação pode tornar a informação mais relevante ao contexto da audiência, desde que resultados inconclusivos e desacordos sejam tratados explicitamente.

**Produto/experimento:** testar uma apresentação interativa de produto com escolha voluntária de perguntas e ramificações contra vídeo linear, medindo compreensão e satisfação sem inferir intenção de compra de dados insuficientes.

**Limites:** proposta conceitual, sem demonstração de eficácia; cautela com privacidade e interpretação abusiva de sinais. **Sem card: não existe evidência suficiente para orientar video ou neuromarketing como achado empírico.**

## 10. Intento e software: a pergunta certa antes de gerar pode valer mais que correções posteriores

**Fonte:** Goel et al., *Measuring Where Intent Gets Lost in Agentic Smart Contract Synthesis*, submetido em 27/10/2026. https://arxiv.org/abs/2610.37830

**Evidência:** autores disponibilizam 600 tarefas e comparam três modelos em síntese de smart contracts. Verificações apenas sobre funções compartilhadas ocultavam diferenças relevantes entre soluções; perguntas específicas antes da geração recuperavam requisitos omitidos e corrigir depois nem sempre preservava comportamento solicitado.

**Mecanismo:** gerar a partir de uma especificação incompleta cristaliza ambiguidades que verificadores superficiais deixam passar.

**Produto/experimento:** para geração de código do AI Hub, testar entrevistas curtas sobre invariantes, resultados esperados e condições de parada, com execução comparativa de testes independentes.

**Limites:** domínio de contratos inteligentes; resultados não podem ser generalizados a qualquer software sem ensaio próprio. **Sem card: desenvolvimento de agentes está fora das coleções.**

## Experience Engine v62

**Objetivo da pessoa → contexto e estado atuais → escolha de modalidade → geração controlável → verificação semântica e temporal → avaliação humana acessível → decisão autorizada → resultado real.**

**Prioridade para o Marketing Hub:** um experimento de audiodescrição inline em vídeos, com revisão humana do público-alvo. **Prioridade para o AI Hub:** testes de estado obsoleto (EnvOS), especificação mínima antes de executar (smart contracts) e extração de tarefas determinísticas do LLM.

## Card e contrato

Antes de criar o card, foi consultada a versão de `harness-library-api/docs/guia-uso-api-cards.md` na `main` e o OpenAPI em `docs/swagger/harness-library-api-swagger.yaml`. Coleções válidas: `video`, `prazer-audio-visual`, `neuromarketing`, `momentos-de-compra-b2c`.

**Um candidato a DRAFT:** `audiodescricao-inline-silencios-compreensao-video`, coleção `video`, `publishedOn=2026-11-01`, `validUntil=2026-12-16`, `sourceKind=MARKDOWN`.

Fonte revisada: `pesquisas/design-experiencia/cards/fontes/2026-11-01-audiodescricao-inline-silencios-video.md`.
JSON: `pesquisas/design-experiencia/cards/2026-11-01-audiodescricao-inline-silencios-video.json`.
SHA-256 dos bytes da fonte: `157dc24d0339c8998f30edf86e11c8c6ea057cfc8fb07e84983f1beb209665fe`.

O card descreve a evidência de acessibilidade e sua extrapolação experimental, não comercializa resultados inexistentes. Não houve seleção artificial de outros cards. Achados fortes, especialmente EnvOS, LogicTree, Decompose Don't Expand, mediação de criadores e testes de requisitos, ficaram no relatório por falta de coleção legitimamente aderente. Não enviar para revisão, não ativar, não arquivar; eventual workflow cadastra somente DRAFT.
