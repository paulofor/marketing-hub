# Radar de Design de Experiência — 26 de outubro de 2026

**Experience Engine v56 | Marketing Hub | Segunda-feira, 26/10/2026**

## Recência, escopo e síntese

Consulta à listagem de cs.HC do arXiv em 26/10/2026: a listagem de segunda-feira contém 24 entradas; a maior parte dos trabalhos selecionados foi submetida em 22 ou 23 de outubro. A data de divulgação em uma lista não equivale à data da coleta de dados, nem implica revisão por pares. Estudos de 16 a 25/10 já resumidos não foram reapresentados, exceto pela atualização material sobre confiança factual em IA.

**Conclusão:** sinais visuais de credibilidade não substituem provas; a qualidade de experiências gerativas depende de métricas externas (aprendizagem, conclusão efetiva, usabilidade e adequação contextual), não apenas fluência ou tamanho das respostas. Nesta rodada, nove artigos oferecem nove implicações práticas. Um achado atualiza um card já existente na coleção neuromarketing, sem criar uma chave duplicada.

## 1. Indicadores de confiança em chatbots: percepção inicial não é prova de verdade — CARD ATUALIZADO

**Fonte:** Warren et al., *When Should I Trust This? Designing and Evaluating Factuality Indicators for LLM-based Chatbots*. Submetido em 23/10, listado em 26/10. https://arxiv.org/abs/2610.32883 ; https://arxiv.org/html/2610.32883v1

**Descoberta e evidência.** Pesquisa mista com oito entrevistas, survey de 1.000 usuários e experimento entre participantes com 5.151 pessoas nos EUA. Comparou indicadores de confiança (porcentagens, rótulos e linguagem) e fontes para respostas de jardinagem, matemática e saúde. Indicadores influenciaram pouco os julgamentos iniciais. Após revelar se a resposta estava correta, a diferença de confiança pelo tipo de indicador deixou de ser significativa (F(5,5115)=1,773; p=0,115); o efeito da correção foi grande (eta parcial²=0,485). Confiança alta associada a resposta errada provocou queda de confiança maior que o inverso. São percepções sobre screenshots, não decisões de compra.

**Mecanismo provável.** Pistas de credibilidade e expectativas de autoridade moldam impressões iniciais, mas a compreensão de uma porcentagem de confiança varia entre pessoas; veracidade conhecida tem peso decisivo. A pesquisa não isolou um mecanismo psicológico único.

**Implicação para produto.** Íris/Têmis devem verificar alegações sobre benefícios, preço, garantias e prazos por evidências independentes, sem confiar na assertividade com que um agente redigiu o criativo.

**Hipótese testável.** Auditoria editorial que expõe fonte primária e estado de verificação por alegação identificará mais erros que leitura convencional. Validar primeiro offline com afirmações certas/erradas conhecidas; depois acompanhar desempenho voluntário do funil, sem atribuir vendas ao estudo.

**Riscos e limites.** Os participantes não examinaram anúncios reais nem navegaram em páginas interativas. Citações e porcentagens inventadas aumentam falsa segurança. A natureza causal do efeito observado limita-se à manipulação de indicadores em screenshots.

**Card:** atualização de evidência para a ideia já versionada como **confianca-factual-ia-deferencia-humana**, coleção **neuromarketing**. Não há motivo para criar cardKey novo apenas por mudar a fonte.

## 2. GELLI: situações geradas pelo usuário podem superar vídeos educativos em uma dimensão

**Fonte:** Symonette et al., *GELLI: A Generator for Experiential Language Learning Interactions*. Submetido em 22/10. https://arxiv.org/abs/2610.31987

**Evidência.** Estudo intraindivíduos com 40 aprendizes de japonês: cenários interativos gerados em tempo de uso produziram maior recordação de vocabulário e confiança que vídeos elaborados por especialistas com os mesmos objetivos; não houve diferença estatisticamente significativa no desempenho de fala.

**Mecanismo proposto.** Contextualização pessoal, prática ativa e possibilidade de experimentar num ambiente seguro podem facilitar recuperação de vocabulário, mas o estudo não isolou qual componente causou os resultados.

**Produto e experimento.** Para produtos educativos digitais, testar uma situação prática ramificada contra vídeo passivo, com o mesmo conteúdo. Medir exercícios concluídos, erros em prova posterior sem ajuda e uso voluntário, em vez de somente satisfação.

**Riscos.** Cenários gerados podem conter erros e custar mais; eficácia em japonês não comprova aprendizagem de qualquer assunto, nem compras.

**Sem card:** educação interativa/adaptive UX não se encaixa por si só em video, prazer-audio-visual, neuromarketing ou momentos-de-compra-b2c.

## 3. CRM-Agent-Bench: concluir um atendimento não basta se o agente violou regras

**Fonte:** Banerjee, Lan e Cao, *CRM-Agent-Bench*. Submetido em 23/10. https://arxiv.org/abs/2610.32004

**Evidência.** Benchmark de 555 tarefas stateful e multirrodada em nove domínios empresariais. Entre 16 sistemas, o sucesso estrito ficou de 64,32% a 83,60%. O contrato verifica ações, argumentos, ordem, resposta, estado final e estado protegido; sistemas parecidos em acerto diferiram em custo e confiabilidade por domínio.

**Mecanismo operacional.** A percepção de confiança depende de o processo realmente preservar registros, autorizações e efeitos externos, não só da resposta final plausível.

**Produto e experimento.** No AI Hub, definir invariantes antes de chamadas a ferramentas: mudanças desejadas, estados que devem ser preservados e critérios observáveis de sucesso. Executar regressões determinísticas e comparar custo por tarefa estritamente correta.

**Riscos.** Benchmark não equivale a qualidade de atendimento real; os testes e regras podem não cobrir contingências de produção.

**Sem card:** governança de agentes não tem coleção válida.

## 4. Cores que parecem diferentes para humanos não seguem necessariamente a quantização RGB

**Fonte:** Shyndali et al., *From Perception to Pixels*. Submetido em 23/10. https://arxiv.org/abs/2610.32473

**Evidência.** Com 110 participantes observando 120 cores, os autores construíram um dicionário de 64 cores perceptivamente distinguíveis e o avaliaram em 4.562 imagens com paletas de especialistas. A extração de cores ficou mais próxima dessas referências pelas métricas utilizadas. Cores muito saturadas foram mais distinguíveis; tons profundos/escuros tenderam a se agrupar perceptualmente.

**Mecanismo provável.** Distâncias numéricas em espaços de cor não correspondem perfeitamente à separação visual percebida.

**Produto e experimento.** Para editor de criativos, comparar extração de paletas RGB padrão e paletas guiadas por distinção perceptiva, verificando legibilidade, contraste e consistência de marca por regras determinísticas.

**Riscos.** Distinção de cores não demonstra atenção, emoção nem aumento de CTA. Acessibilidade e daltonismo requerem testes próprios.

**Sem card:** extração algorítmica de paletas não foi estudada em persuasão ou consumidores.

## 5. Supervisão humana detecta inadequação mais frequentemente que erros evidentes

**Fonte:** Shakiba e Chaffey, *Beyond Correctness*. Submetido em 23/10. https://arxiv.org/abs/2610.32118

**Evidência.** Análise de 962 turnos documentados por 145 alunos iniciantes, incluindo 172 intervenções relatadas. Em 74,4% das intervenções apareceram problemas de adequação ao trabalho, contra 16,9% envolvendo correção; entre intervenções classificáveis, 51% alteraram saídas e 28,2% refinaram prompts.

**Mecanismo interpretativo.** Uma solução pode executar sem falha mas não atender ao objetivo, estilo ou contexto; supervisão é mais ampla que busca de bugs.

**Produto e experimento.** Têmis deve separar avaliação factual, adequação ao público e cumprimento do briefing. Testar essa rubrica sobre entregas anteriores e medir reprovações corretas e falso aceite.

**Riscos.** Diários são autorrelatos de estudantes; proporções não medem taxa global de erros de agentes profissionais.

**Sem card:** avaliação de agentes sem coleção aderente.

## 6. Animações declarativas menores não necessariamente custam menos

**Fonte:** Huang, *Shorter Is Not Cheaper*. Submetido em 23/10. https://arxiv.org/abs/2610.32129

**Evidência.** Comparação controlada de animações explicativas em código HTML/JavaScript livre ou especificação declarativa: 20 pedidos de cinco domínios, 360 execuções Claude e 240 Qwen3-14B. Embora as especificações economizassem 25–48% dos tokens visíveis de resposta, não houve redução significativa do tempo de geração; no Qwen com raciocínio, apenas 17% das especificações passaram nos testes semânticos completos, contra 62% das páginas HTML/JS.

**Mecanismo técnico.** Representação menor pode aumentar esforço de raciocínio e dificuldade de aprender formatos novos; tokens de saída não capturam custo total.

**Produto e experimento.** Para Apolo, medir tempo e custo de geração *até uma explicação animada válida*, incluindo raciocínio, reparos, testes e aprovação; não escolher formato somente pelo tamanho do JSON.

**Riscos.** Benchmarks são de animações explicativas geradas por LLM, não vídeo publicitário ou preferência dos espectadores.

**Sem card:** comparação de formatos de código para animação, não evidência audiovisual de experiência do consumidor.

## 7. Enjoo em VR não pode ser inferido por uma única métrica ocular

**Fonte:** Demirseren et al., *Gaze Under Strain*. Submetido em 23/10, aceito ACM VRST 2026. https://arxiv.org/abs/2610.32019

**Evidência.** Em 19 participantes durante exposição virtual de 20 minutos, estabilidade da fixação deteriorou-se com maior desconforto, com interação com o tempo; exploração ocular teve padrões dependentes do tempo; alocação do olhar não acompanhou diretamente o desconforto.

**Mecanismo.** Demandas vestibulares e visuais podem modificar estabilização ocular, mas comportamentos distintos reagem diferentemente ao desconforto.

**Produto e experimento.** Em experiências imersivas futuras, usar medidas repetidas de desconforto, não apenas tempo de visualização ou fixações. A comparação deve priorizar interrupção voluntária, acessibilidade e conforto.

**Riscos.** Amostra pequena; dados de olhar não permitem detectar emoções individuais com confiança.

**Sem card:** conforto em VR ainda não se traduz em mecanismo comercial validado de prazer audiovisual.

## 8. Eye tracking pode parecer válido enquanto registra dados inválidos

**Fonte:** Chernysh et al., *What Mean Error Misses*. Submetido em 23/10 para ETRA 2027. https://arxiv.org/abs/2610.32915

**Evidência.** 30 pessoas testaram três headsets. Mediana dos erros médios individuais: Quest Pro 1,04°, Vive Pro Eye 1,50°, Vive Focus Vision 2,41°. Foram observadas pausas de até 6,04 s no clock do Quest Pro; durante fechamento de olhos instruído, o Quest Pro assinalou amostras como válidas, com deslocamentos registrados.

**Mecanismo técnico.** A média do erro ignora lacunas temporais e rótulos de validade incorretos; inferências comportamentais podem se apoiar em dados mal caracterizados.

**Produto e experimento.** Qualquer pipeline de atenção ocular deve auditar timestamps, perdas, piscadas e validade antes de inferir atenção do criativo.

**Riscos.** Resultados específicos de dispositivos VR; não extrapolar para webcam, celulares ou campanhas.

**Sem card:** confiabilidade instrumental, não comportamento de compra.

## 9. Testar layout RTL em execução revela defeitos que o linter não vê

**Fonte:** Yadav, *Runtime BiDi Structural Verification*. Submetido em 23/10. https://arxiv.org/abs/2610.32934

**Evidência.** Verificação de invariantes de layout detectou 13 de 14 bugs reais reconstruídos em bibliotecas de interface e não alertou nas versões corrigidas desses 14 casos. Testes adicionais incluíram componentes reais e casos negativos; o trabalho ainda não inclui reparo automático.

**Mecanismo.** Uma interface internacionalizada pode satisfazer regras estáticas e continuar invertendo margens, alinhamento e direcionalidade incorretamente na renderização.

**Produto e experimento.** Caso o Marketing Hub amplie idiomas, automatizar teste de interface em runtime para direções LTR e RTL, com comparação de geometria e contratos de leitura.

**Riscos.** Não dispensa avaliação de acessibilidade, nem se pode generalizar a taxa de detecção além dos casos avaliados.

**Sem card:** engenharia de acessibilidade, não conteúdo das coleções.

## Experience Engine v56 — implicações priorizadas

**Intenção e evidências → autorização e contexto → execução testável → percepção com limitações explícitas → avaliação de resultado real.**

**Prioridade A (alto impacto imediato):** revisão de afirmações e fontes de IA antes de publicar ofertas. A atualização de Warren et al. reforça o card de 24/10: confiança percebida e correção factual são variáveis distintas.

**Prioridade B (alto impacto operacional):** contratos executáveis de invariantes no AI Hub e métricas de custo por tarefa realmente correta, inspirado no CRM-Agent-Bench.

**Prioridade C (produto futuro):** interfaces de prática contextualizadas (GELLI), mas jamais confundir ganho em vocabulário com venda.

**Critério de validação do Marketing Hub:** antes do mercado, somente fontes públicas, análises por agentes e testes determinísticos; depois, comportamento voluntário observado, custo reconciliado e vendas reais. Nenhum estudo acima comprova conversões no Marketing Hub.

## Card e governança

Guia atual consultado: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md . Coleções aceitas: video, prazer-audio-visual, neuromarketing e momentos-de-compra-b2c.

**Card preparado:** atualização da ideia existente **confianca-factual-ia-deferencia-humana**, coleção **neuromarketing**, para incorporar evidência nova de 5.151 participantes; a fonte revisada contém apenas evidência de Warren et al. e separa resultados observados, interpretação e hipótese comercial. *publishedOn*: 2026-10-26; *validUntil*: 2026-12-10. É candidato a DRAFT, nunca IN_REVIEW, ACTIVE ou ARCHIVED por esta execução.

**Achados fortes não transformados em card:** GELLI (aprendizagem interativa), CRM-Agent-Bench (agentes e contratos), Beyond Correctness (supervisão humano–IA) e Shorter Is Not Cheaper (engenharia generativa); nenhum desses resultados justifica forçar uma coleção existente.

## Fontes verificadas

- Listagem diária arXiv cs.HC (26/10/2026): https://arxiv.org/list/cs.HC/recent
- Warren et al.: https://arxiv.org/abs/2610.32883
- Symonette et al.: https://arxiv.org/abs/2610.31987
- Banerjee et al.: https://arxiv.org/abs/2610.32004
- Shyndali et al.: https://arxiv.org/abs/2610.32473
- Shakiba & Chaffey: https://arxiv.org/abs/2610.32118
- Huang: https://arxiv.org/abs/2610.32129
- Demirseren et al.: https://arxiv.org/abs/2610.32019
- Chernysh et al.: https://arxiv.org/abs/2610.32915
- Yadav: https://arxiv.org/abs/2610.32934
