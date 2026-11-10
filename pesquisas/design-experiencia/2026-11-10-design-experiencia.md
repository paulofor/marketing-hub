# Radar de Design de Experiência — 10/11/2026

**Experience Engine v71 | Marketing Hub**

## Escopo, novidade e síntese
Pesquisa realizada em 10/11/2026 na listagem de Human-Computer Interaction do arXiv: https://arxiv.org/list/cs.HC/new . A lista exibia 81 entradas, das quais 50 novas submissões. Seleção crítica de 11 pesquisas não incluídas nos relatórios de 31/10–09/11. Atenção às datas: a inclusão na listagem de 10/11 **não significa que cada estudo tenha sido originalmente submetido nesse dia**; o arXiv registra, por exemplo, 22/08 para o estudo RADIA e 02/09 para VIVID. A pesquisa EgoVeil aparece em registros de novembro.

**Síntese:** vínculos emocionais percebidos em agentes não garantem mais autorrevelação ou interação; privacidade de vídeo pode exigir controles personalizados; avaliações de IA precisam separar confiança, precisão, autonomia e resultados efetivos. Para o AI Hub, evidência recente reforça controles técnicos e avaliação end-to-end, não só desempenho isolado de modelo.

## 1. RADIA — autorrevelação de robôs melhora rapport percebido, não a autorrevelação do usuário
**Fonte:** Anupindi et al., artigo aceito no RO-MAN 2026; versão arXiv indica envio original em 22/08/2026. https://arxiv.org/abs/2611.09905 | https://arxiv.org/html/2611.09905v1

- **Descoberta/evidência:** experimento presencial intraindivíduo (N=44) com ordem contrabalanceada. Robô que referia memórias autobiográficas **simuladas** teve rapport percebido de 4,13/5, versus 3,47/5 no controle (d=0,79; p<0,0001). Qualidade de conversa também melhorou (d=0,77). A frequência de autorrevelação pelos usuários não mudou significativamente (p=0,512), e os turnos médios caíram de 18,73 para 15,46 (p=0,00204).
- **Mecanismo interpretativo:** reciprocidade e aparente relevância pessoal podem elevar a sensação de vínculo; o estudo não isolou mecanismos e não demonstra confiança factual.
- **Aplicação:** agentes de atendimento podem testar contextualização transparente e reciprocidade sem alegar que a IA viveu experiências humanas.
- **Hipótese/teste:** atendimento objetivo versus diálogo contextual transparente. Medir resolução, acertos de compreensão, satisfação e dados solicitados; medir CTA, checkout, pagamentos e retenção apenas se observados.
- **Riscos/limites:** autobiografia fictícia pode enganar. Amostra pequena de conveniência, robô físico, sessão única, sem resultado comercial. **Card neuromarketing criado** para estudo e salvaguardas, não para autorizar simulação de vivências.

## 2. EgoVeil — a privacidade visual é contextual e individual
**Fonte:** Wang e Zhang, preprint divulgado em novembro de 2026. https://arxiv.org/abs/2611.10578

- **Descoberta/evidência:** sistema para vídeo em primeira pessoa que transforma regiões em observações semânticas, aplica memória de preferências explícitas e decide que partes mascarar. Em dados de 10 usuários de óculos inteligentes, atingiu 91,93% de balanced accuracy e 84,42% de Mask recall, nos protocolos dos autores.
- **Mecanismo interpretativo:** a mesma cena pode ser sensível para uma pessoa e não para outra; preferências e contexto mudam a decisão de ocultação. Métricas de máscara não garantem anonimização.
- **Aplicação:** na produção de UGC e vídeos em primeira pessoa, orientar revisões de rostos, telas, documentos, locais e terceiros por autorizações e preferências explícitas.
- **Hipótese/teste:** regras genéricas versus checklist contextual com inspeção humana; medir exposição de informações, falsos negativos, custo e integridade do vídeo.
- **Riscos/limites:** somente dez usuários, óculos inteligentes, risco de inferências após desfoque, ausência de teste de segurança de publicação ou comercial. **Card video criado** como orientação de validação.

## 3. Human–AI Collaboration at Scale — fricção não é sempre falha
**Fonte:** Shao et al., arXiv com envio original em 17/08/2026. https://arxiv.org/abs/2611.09902

- **Descoberta/evidência:** análise de 249.834 conversas reais com IA; tarefas de alto risco produziram conversas quase duas vezes mais longas que tarefas efêmeras. O estudo classificou formas de colaboração, aprendizagem e fricção; orientação ativa ocorreu em cerca de 67% dos diálogos examinados; fricções apareceram em cerca de metade.
- **Mecanismo:** revisar, discordar e pedir esclarecimento pode ser comportamento produtivo, não necessariamente falha de UX.
- **Aplicação/experimento:** no AI Hub, separar fricção que corrige riscos de fricção que apenas atrasa. Registrar mudanças úteis versus repetição sem progresso e tempo até resolução.
- **Riscos/limites:** categorização observacional mediada por modelo; não prova que aumentar fricção cause melhores resultados. **Sem card:** colaboração e avaliação de agentes não têm coleção válida.

## 4. VIVID — resultado operacional difere da acurácia isolada do agente
**Fonte:** Fang et al., arXiv com envio original em 02/09/2026. https://arxiv.org/abs/2611.09921

- **Descoberta/evidência:** agente corporativo de gestão turística com Qwen3-8B; em 283 perguntas auditadas, sucesso estrito end-to-end de 59,06%. Em implantação de três meses para mais de 1.500 funcionários, tempo médio por pedido passou de aproximadamente 10 minutos a menos de 2, com erro final abaixo de 1% após controles humanos. Otimização de cache reduziu latência média em 34,4%.
- **Mecanismo técnico:** validação humana, ferramentas confiáveis, otimização de latência e regras de processo podem ser mais relevantes para a operação que uma única pontuação de LLM.
- **Aplicação/experimento:** medir no AI Hub taxa de jobs finalizados com qualidade, tempo com e sem revisão e custo por tarefa correta.
- **Riscos/limites:** estudo de caso sem comparação randomizada; números dependem do negócio, ferramentas e revisão humana. **Sem card:** não há coleção para agentes corporativos.

## 5. Criatividade assistida por IA pode reduzir sentimento de autoria
**Fonte:** Carson, Hanel e Van Tilburg, três experimentos (N total=651), envio original em 01/09/2026. https://arxiv.org/abs/2611.09918

- **Descoberta/evidência:** condições com IA apresentaram menor autenticidade percebida da própria criação, conexão identitária, afeto positivo e motivação intrínseca; maior desconforto em determinadas medidas. Modelos de mediação associaram o efeito à percepção de ter colocado menos cuidado pessoal na obra.
- **Mecanismo proposto:** autoria como expressão de identidade; a mediação estatística não comprova o caminho causal em contextos comerciais.
- **Aplicação/experimento:** testar em editores de criativos a possibilidade de autoria visível, edição manual e explicitação do controle. Medir orgulho, propriedade percebida, retenção e qualidade de artefato.
- **Riscos/limites:** tarefas criativas e amostras específicas, sem avaliação de campanhas ou compras. **Sem card** por não corresponder diretamente às quatro coleções de percepção de consumidor e produção audiovisual.

## 6. Vídeos curtos e desgaste acadêmico: cuidado com causalidade e questionários
**Fonte:** Hossain, envio original em 12/08/2026. https://arxiv.org/abs/2611.09899

- **Descoberta/evidência:** survey com 863 universitários e 67 itens. Exposição a vídeos curtos teve associações graduais com fadiga, fragmentação de atenção e outros indicadores; dimensão de uso compulsivo teve eta²=0,117 e rho=0,273. Porém viés de concordância explicou 59–89% da variância em diferentes construtos e o ajuste inverteu uma correlação de +0,55 para -0,49.
- **Mecanismo hipotético:** alternância frequente de estímulos pode sobrecarregar atenção; o levantamento não estabelece essa causalidade.
- **Aplicação/experimento:** em vídeo educacional, testar interrupções e ritmo, usando tarefas de atenção observáveis e instrumentação balanceada, não apenas satisfação.
- **Riscos/limites:** autorrelato transversal, confusão e problemas de validade dos construtos; sem alegar que vídeos curtos causam burnout. **Sem card:** relação indireta com criativos do Marketing Hub e base metodológica ainda limitada.

## 7. Voz sintética adaptativa precisa de contexto real
**Fonte:** Tuttösi et al., artigo RO-MAN 2026, envio original 26/08/2026. https://arxiv.org/abs/2611.09914

- **Descoberta/evidência:** estudos industriais qualitativos sobre ajustes de pitch, duração e intensidade de TTS em cuidados a idosos e aeroporto. Em Lyon: 263 interações observadas, feedback de 138 pessoas; 65% avaliaram negativamente respostas a perguntas sobre voos em um recorte do estudo.
- **Mecanismo:** ruído, necessidades auditivas e adequação da informação interferem na compreensão; simpatia do robô não substitui clareza e precisão.
- **Aplicação/experimento:** comparar TTS padrão e ritmos/volumes adaptados para voz em diferentes condições de ruído e compreensão.
- **Limites:** sem demonstração causal ou efeito específico em português brasileiro. **Sem card:** voz utilitária sem coleção de UX multimodal, não confundir com prazer audiovisual.

## 8. Autonomia conversacional pode estar ausente, não sendo progressivamente perdida
**Fonte:** Chen, Barberá e Wei, envio original 06/09/2026. https://arxiv.org/abs/2611.09923

- **Descoberta/evidência:** 2.877 diálogos (46.952 turnos), validação suplementar em 500. Em 80,8% observou-se autonomia operacional zero ou baixa ao longo do diálogo; somente 4,7% apresentaram queda significativa. As pontuações são uma operacionalização textual, não medida integral da autonomia humana.
- **Mecanismo:** a pessoa pode jamais exercer contestação ou planejamento explícito na conversa, em vez de perder esses comportamentos com o tempo.
- **Aplicação/experimento:** pontos de decisão opcionais e alternativas testáveis em operações importantes; medir se o operador identifica erros e retém controle.
- **Limites:** observacional, autonomia inferida de texto e possível confusão de domínio. **Sem card:** governança de agência.

## 9. Personalização por valores contextualmente relevantes
**Fonte:** Poole e Jahanbakhsh, EMNLP 2026, envio original 01/09/2026. https://arxiv.org/abs/2611.09919

- **Descoberta/evidência:** 1.501 cenários de tensão entre valores e 152 perfis derivados de survey. Abordagens que escolhem valores relevantes ao pedido superaram ausência de personalização, perfis demográficos e aplicação integral do perfil nas métricas do benchmark.
- **Mecanismo hipotético:** ativar somente valores pertinentes pode reduzir interferências irrelevantes.
- **Aplicação/experimento:** em experiências opcionais, comparar personalização contextual explícita e perfis amplos; medir aderência a valores declarados e respeito à instrução atual.
- **Limites:** perfis e cenários benchmark, não consumidores reais; não inferir valores sensíveis. **Sem card:** arquitetura de alinhamento, coleção ausente.

## 10. ExCritique — não avaliar explicações de IA por uma nota única
**Fonte:** Hossain, Mudireddy e Hoque, preprint de 2026. https://arxiv.org/abs/2611.11932

- **Descoberta/evidência:** entrevistas formativas com nove pesquisadores e experimento controlado com 16 profissionais; painel interativo ajudou a localizar problemas de explicações automáticas e a avaliar métricas de qualidade.
- **Mecanismo:** evidência por item, distribuição de erros e comparação de critérios tornam falhas mais inspecionáveis.
- **Aplicação/experimento:** no AI Hub, painel de falhas de agentes com filtros, trechos de evidência e divergência entre avaliadores; medir defeitos detectados por minuto.
- **Limites:** estudo pequeno de VQA, sem resultado comercial. **Sem card:** explicabilidade e observabilidade de agentes não têm coleção.

## 11. Mentoria no onboarding — utilidade depende da dúvida específica
**Fonte:** Zhou, Chen e Budak, ICWSM 2027 aceito, dados de 2021–2026. https://arxiv.org/abs/2611.11083

- **Descoberta/evidência:** comparação observacional com pareamento em 34.073 iniciantes da Wikipédia. Uma resposta de mentor se associou a +1,8 ponto percentual em participação de edição no curto prazo (22% versus cerca de 20% estimados) e maior efeito para perguntas de regras, procedimentos e dúvidas concretas; sem efeito detectável para não saber o que fazer.
- **Mecanismo interpretativo:** uma resposta é mais acionável quando reduz uma incerteza bem delimitada.
- **Aplicação/experimento:** atendimento de onboarding que diferencia dúvidas operacionais de falta de objetivo; medir ativação e primeiro resultado do usuário, não cliques no chat.
- **Limites:** pareamento observacional não elimina confusão residual, onboarding de voluntários não equivale a compra. **Sem card:** não é momento de compra B2C.

## Síntese prática e governança editorial
**Para Marketing Hub:** separar rapport, atenção, compreensão e confiança percebida dos resultados observados de compra; preservar privacidade verificável em vídeos UGC.
**Para AI Hub:** avaliar agentes por tarefa final, evidência auditável, capacidade de intervenção útil e custo, não pelo número de turnos ou pela aparente autonomia.

O guia atualizado em main `harness-library-api/docs/guia-uso-api-cards.md` foi consultado nesta rodada. As coleções autorizadas continuam `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`. Não foi criada coleção `design-experiencia`.

**Cards candidatos a DRAFT produzidos:** `autorrevelacao-agente-rapport-sem-autodivulgacao-usuario` (`neuromarketing`) e `privacidade-contextual-video-egocentrico-ugc` (`video`). Fontes independentes, hashes SHA-256 exatos e JSONs de 16 campos. A integração na main não equivale a revisão nem ativação; eventual workflow de cadastro deve ser verificado separadamente.
