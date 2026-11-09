# Radar de Design de Experiência — 09/11/2026

**Experience Engine v70 | Marketing Hub**

## Escopo
A listagem Human-Computer Interaction do arXiv de segunda-feira, 09/11/2026 trouxe 28 entradas, em grande parte submetidas em 05–06/11. Foram selecionadas dez pesquisas não cobertas no radar de 08/11, priorizando evidência operacional e consequências práticas. Lista: https://arxiv.org/list/cs.HC/recent .

**Síntese:** um sistema pode melhorar o entendimento de recomendações sem elevar a aceitação, enquanto a personalização sensorial oculta pode influenciar pessoas sem que elas entendam como. Para agentes, fronteiras de autonomia, objetivos e fontes precisam ser explicitamente rastreáveis.

## 1. Organizar explicações pelo perfil interpretativo melhora a compreensão
**Fonte:** Munduku e Negre, preprint de 05/11/2026: https://arxiv.org/abs/2611.08785

- **Descoberta/evidência:** estudo controlado com 122 pessoas; ordenações diferentes das mesmas seis frases de recomendação. Com perfil do próprio usuário, 86,5% de acertos objetivos (211/244), versus 66,0% com outro perfil, 63,9% com perfil populacional e 73,0% com LLM personalizado de comparação. Não houve efeito geral confiável em aceitação de recomendação (p=0,795).
- **Mecanismo proposto:** a ordem das condições pode acompanhar a saliência interpretativa do usuário; não se isolou mecanismo cognitivo e a tarefa avalia também memória imediata.
- **Aplicação:** personalizar apenas a ordem de explicação da oferta no Marketing Hub, preservando integralmente condições de preço, limites e cancelamento.
- **Hipótese/teste:** apresentação padrão versus perfil consentido; medir compreensão objetiva, rejeição e, separadamente, conversão real.
- **Riscos/limites:** amostra de conveniência, inglês/francês, quatro cenários, apenas frases fixas; não representa prova de vendas. **Card neuromarketing criado.**

## 2. Biossinais não informados podem alterar persuasividade de chatbot
**Fonte:** Wei et al., preprint de 06/11/2026: https://arxiv.org/abs/2611.09394

- **Descoberta/evidência:** experimento intraindivíduo com 24 adultos em VR; adicionar características de batimento cardíaco e pupila antes da divulgação elevou persuasividade percebida em 1,42 ponto (escala de 1–7; IC95% 0,66–2,17). Associação agregada com deslocamento de opinião +0,57 ponto na direção do chatbot; odds 3,20 vezes maiores de qualquer deslocamento a favor. Conversas ficaram mais curtas e declarativas.
- **Mecanismo possível:** biossinais associados a instruções de adaptação mudam a forma argumentativa, não necessariamente reconhecem emoções com precisão.
- **Aplicação:** controles transparentes para agentes de consumo: informar uso, garantir modo sem sensores, registrar adaptação e impedir uso oculto para persuasão.
- **Hipótese/teste ético:** avaliar compreensão e autonomia com opt-in, controles e avisos persistentes, sem coleta dissimulada.
- **Riscos/limites:** N=24, contexto VR, temas distintos, idiomas mandarim/inglês, GPT-4 Turbo; efeito da divulgação confundido com ordem. Não há evidência de conversão. **Card de salvaguarda neuromarketing criado.**

## 3. Delegação é preferência contextual, não uma chave ligado/desligado
**Fonte:** Wheeler et al., preprint de 06/11/2026: https://arxiv.org/abs/2611.09730

- **Descoberta/evidência:** survey por cenários com 289 adultos dos EUA familiarizados com IA, 45 casos em nove domínios. Confiança, liderança desejada, experiência anterior e economia esperada associaram-se a maior preferência por automação; perda antecipada de realização pessoal à menor delegação.
- **Mecanismo interpretativo:** pessoas distinguem execução, colaboração, liderança e autoridade final.
- **Aplicação:** configurar no AI Hub autonomia por risco/ação, preservando decisão final humana quando relevante.
- **Hipótese/teste:** comparar agentes com delegação total, assistida e limitada; medir correções, conclusão e controle percebido.
- **Limites:** intenções relatadas, amostra dos EUA e nenhum comportamento longitudinal. **Sem card:** não há coleção para governança de agentes.

## 4. GroundWork: argumentos como estado compartilhado e verificável
**Fonte:** Vink et al., 06/11/2026: https://arxiv.org/abs/2611.09840

- **Descoberta/evidência:** protótipo de grafo tipado de alegações, justificativas e evidências, com edição humana e IA; pesquisa formativa com quatro especialistas, três cenários, apontou inspeção e revisão melhores.
- **Mecanismo:** representar relações de forma persistente pode reduzir reconstrução mental de argumentos escondidos no chat.
- **Aplicação:** vincular conclusões de pesquisas e decisões técnicas a trechos de fonte e validações, inclusive na Harness Library.
- **Hipótese/teste:** comparar revisão de pesquisas por texto e por grafo; medir erros de atribuição e tempo.
- **Limites:** só quatro especialistas, sem comparação robusta de produtividade. **Sem card** por falta de coleção.

## 5. Tracybot: rastrear prompts até linhas de código
**Fonte:** Albu et al., 06/11/2026: https://arxiv.org/abs/2611.09395

- **Descoberta/evidência:** ferramenta VS Code/Git liga alterações de código a unidades de prompt e permite gerar registros de decisão. Focus group N=7 e survey N=24 relataram utilidade percebida para revisão.
- **Mecanismo:** torna visível a intenção que ficaria apenas na conversa.
- **Aplicação:** registrar no AI Hub relação solicitação → execução → commit → trecho de código.
- **Hipótese/teste:** avaliar rapidez e precisão de revisão/manutenção com e sem rastros.
- **Limites:** estudo de percepção, sem redução real de defeitos comprovada. **Sem card** por falta de coleção.

## 6. DeepRadio: supervisão antes de ações físicas do agente
**Fonte:** Lei et al., 06/11/2026: https://arxiv.org/abs/2611.09418

- **Descoberta/evidência:** especificação rastreável, verificação entre software e rádio e controle misto. Teste com 20 participantes: mais pessoas concluíram deployment físico verificado que com Codex no GNU Radio Companion; sete tarefas atômicas também foram avaliadas.
- **Mecanismo:** tornar observáveis o estado, as restrições e as evidências pode evitar propagação de decisões não verificadas a hardware.
- **Aplicação:** verificação de invariantes e checkpoints proporcionais ao risco antes de ações irreversíveis do AI Hub.
- **Hipótese/teste:** comparar falhas, rollback e sucesso em simulações controladas de tarefas mutáveis.
- **Limites:** ambiente de engenharia de rádio, sem extrapolação a aplicações genéricas. **Sem card** por falta de coleção.

## 7. Thinking Twice: o julgamento humano inicial pode facilitar revisão crítica
**Fonte:** Keke, Eisenhardt e Meske, depositado em 06/11/2026, apresentado na WI 2025: https://arxiv.org/abs/2611.09269

- **Descoberta/evidência:** experimento com 130 participantes: quando a pessoa decidia inicialmente e só depois consultava a IA, houve maior desempenho e utilização de prompts do que nas condições comparadas, segundo autores.
- **Mecanismo proposto:** conflitos entre a decisão inicial e a resposta da IA desencadeiam reflexão.
- **Aplicação:** modo opcional de revisão para decisões importantes em vez de introduzir perguntas obrigatórias em toda tarefa.
- **Hipótese/teste:** comparar decisões e erros com fluxo sequencial versus simultâneo.
- **Limites:** tarefas experimentais, versão do manuscrito anterior à revisão por pares. **Sem card:** não há coleção de colaboração humano–IA.

## 8. Estereótipos de idade em narrativas geradas
**Fonte:** Sojib et al., 06/11/2026: https://arxiv.org/abs/2611.09043

- **Descoberta/evidência:** análise de 594 histórias geradas por Gemini, GPT e Llama; personagens de meia idade pouco presentes quando idade não especificada, e idosos frequentemente retratados como dependentes ou com realizações tratadas como surpresas.
- **Mecanismo possível:** padrões narrativos de treinamento e enquadramento podem reduzir agência de personagens.
- **Aplicação:** revisar roteiros de criativos e representações de diferentes idades no Marketing Hub.
- **Hipótese/teste:** avaliação cega da diversidade de papéis, agência e compreensão pelo público.
- **Limites:** histórias textuais, sem testar espectadores nem efeito comercial. **Sem card** por falta de aderência a coleção de vídeo sem evidência audiovisual.

## 9. Retrato com replay: geração reprodutível não é filmagem
**Fonte:** Rajpurohit, 06/11/2026: https://arxiv.org/abs/2611.08994

- **Descoberta/evidência:** protótipo oferece sequência de desenho de 44 segundos, replay determinístico sem nova inferência e arquivo vetorial. Em testes sintéticos, autor identificou falhas de preservação tonal no pipeline e fez ajustes técnicos.
- **Mecanismo hipotético:** exibir o processo pode ajudar compreensão e vínculo com o resultado, mas não foi testado com usuários reais.
- **Aplicação:** prévias reproduzíveis de geração criativa com transparência sobre reconstruções.
- **Hipótese/teste:** comparar satisfação, clareza de autoria e percepção do processo com/sem replay.
- **Limites:** nenhum estudo perceptivo com pessoas reais; testes sintéticos podem confundir outras diferenças. **Sem card** por evidência insuficiente.

## 10. Objetivos como camada de representação persistente
**Fonte:** Molinaro et al., 05/11/2026: https://arxiv.org/abs/2611.08757

- **Descoberta/evidência:** proposta conceitual de camada explícita de metas dinâmicas entre pessoas, organizações e agentes; não contém experimento confirmatório.
- **Mecanismo possível:** vincular ações locais a objetivos superiores permite revisar desvios sem depender exclusivamente do chat.
- **Aplicação:** entidades de backend versionadas para metas, restrições, decisões e autorizações no AI Hub.
- **Hipótese/teste:** comparar retrabalho e desvio de intenção em projetos multiagente com e sem estado explícito de metas.
- **Limites:** arquitetura ainda conceitual; inferir metas não equivale a receber autorização. **Sem card** por coleção inexistente.

## Decisão editorial e próximos experimentos
Guia atual conferido na branch main: https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md . Coleções aceitas: video, prazer-audio-visual, neuromarketing, momentos-de-compra-b2c.

Foram selecionados **dois novos cards candidatos a DRAFT**, ambos em neuromarketing: ordenação de explicações por perfil interpretativo e proteção contra persuasão biométrica sem transparência. Fontes revisadas curtas foram geradas antes dos JSONs, com SHA-256 dos bytes correspondentes. Não criamos coleção design-experiencia, nem cards para temas com coleções inadequadas. Não houve operações de revisão, ativação ou arquivamento.

Prioridades: teste de compreensão correta de ofertas com informação integral; controle de adaptação por sensores; rastreabilidade entre objetivos, evidências e execução. Nenhum estudo citado comprova aumento de vendas do Marketing Hub.
