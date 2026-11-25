# Radar de Design de Experiência — 25/11/2026

## Síntese
Triagem: https://arxiv.org/list/cs.HC/new (25/11, 23 entradas, 12 novas submissões). Seis trabalhos de 23–24/11 e dois trabalhos complementares sobre consumo. Preprints não são confirmação por revisão por pares. Busca por identificadores no repositório não encontrou COGENT, estudo de pré-teste prospectivo ou o artigo de compras por voz. O avanço central é separar simulação do usuário de execução eficiente, e tornar o harness uma interface controlável. Nenhum resultado comercial foi observado no Marketing Hub.

## 1. COGENT: arquitetura cognitiva para simular usuários
Chi et al., Stanford/Microsoft, preprint 23/11. https://arxiv.org/html/2611.31559v1
- Evidência: AgentNetBench, previsão da ação exata 59,8% vs 52,2% (agente comum) e 53,7% (persona em prompt). Tipo de ação 84,1% vs 79,0% e 80,9%, diferenças significativas. Em 43 tarefas A11y-CUA, distância de sequência menor frente aos dois comparadores, mas não frente ao comparador de orçamento computacional semelhante. Divergência agente-humano ainda maior que entre humanos. SENSE-42: ordenação das 21 relações entre atributos rho=0,860; magnitudes mal calibradas e associações atributo-comportamento testadas não significativas no simulador.
- Mecanismo: módulos de percepção, memória, interpretação, intenção e ação permitem ligar estados a decisões em vez de apenas declarar uma persona. Inspirado em ACT-R e ciclo de ação de Norman.
- Produto: Psique pode ter módulos explícitos de conhecimento, atenção, esforço e frustração, com rastreabilidade de como influenciam escolhas. Não trata estado simulado como emoção humana medida.
- Experimento: comparar persona por prompt e arquitetura modular com orçamento equivalente em percursos de usuários reais inéditos; medir erros, abandono, ações e calibração.
- Limites: benchmarks desktop, uma execução parcial na análise de atributos; sem validação de compras ou microemoções. Sem card: aderência comercial indireta e sobreposição parcial com pesquisas de 20 e 24/11.

## 2. Simulação prospectiva de respostas a questionários
Bafna et al., preprint 23/11. https://arxiv.org/abs/2611.31532
- Evidência: ondas de pesquisa local 2025 (N=1271) e 2026 (N=1161); GPT-4.1 previu 22 perguntas novas antes da coleta de respostas humanas. Correlação de postos para médias 0,71; para distinção cidade/condado 0,88, outros contrastes variaram. Pré-registro reduz risco de memorizar as respostas-alvo.
- Mecanismo: histórico de respostas condiciona previsão de padrões agregados, não replica necessariamente indivíduos.
- Produto: selecionar perguntas de pesquisa com clientes antes de investir em campo. Registrar previsões com data e testar em dados novos.
- Experimento: comparar priorização por especialistas, LLM e seleção aleatória; medir utilidade real das perguntas, erros de descarte e custo.
- Limites: pesquisa local não comercial, 22 perguntas; resumo oficial consultado. Sem card: método geral, sem medida B2C direta; novidade metodológica frente ao estudo retrospectivo de 20/11.

## 3. Onde está a interface do harness?
Johal e Schombs, preprint 24/11. https://arxiv.org/abs/2611.31739
- Descoberta: artigo de posição distingue lacuna de especificação (intenção e contingências não explicitadas) e de supervisão (ações difíceis de observar e controlar).
- Mecanismo: delegação assíncrona rompe o ciclo habitual de feedback humano.
- Produto: editor de permissões e requisitos, limites por ferramenta, exceções, revisão de evidências, trilha de execução e parada.
- Experimento: comparar painel editável de harness com chat/log tradicional em tarefas de risco controlado; medir ações fora de escopo, detecção e tempo de intervenção.
- Limites: proposta conceitual, sem ensaio de eficácia. Sem coleção legítima.

## 4. Calm Agent: projetar para atenção intermitente
Jin et al., 24/11. https://arxiv.org/html/2611.32009v1
- Evidência: protótipo Orvlet e três modos de interação sob demanda (delegar, ajustar, verificar) sobre representação persistente de tarefa; artigo de posição.
- Mecanismo: recuperar estado por interface compacta pode reduzir troca de contexto e esforço de reconstrução.
- Produto: cartões de tarefas no AI Hub com estado, última evidência, bloqueio, controles e próxima decisão; alertar por necessidade real.
- Experimento: medir tempo de retomada, interrupções, falhas de supervisão e produtividade primária versus chat.
- Limites: não há ensaio controlado de redução de carga; atenção reduzida não deve esconder risco. Sem coleção legítima.

## 5. MyMuse: memória que revela relações entre interesses
Bae et al., 24/11. https://arxiv.org/html/2611.32169v1
- Evidência: estudo formativo N=10 e dois estudos intrassujeitos com 16 participantes. Relatório narrativo de registros de obras recebeu avaliações superiores em consciência de interesses recorrentes, conexões entre obras e descobertas inesperadas frente a apresentação por obra.
- Mecanismo possível: comparar registros pessoais de diferentes momentos facilita reconhecer padrões que cada interação isolada não mostra.
- Produto: histórico de exploração consentido que distingue registros do usuário de inferências da IA, com possibilidade de corrigir conexões.
- Experimento: comparar histórico cronológico e síntese por temas; medir descoberta verificável, correção de inferências e retorno voluntário.
- Limites: amostra pequena, museus, autorrelatos; sem prova de prazer, compras ou mecanismo neural. Sem coleção adequada.

## 6. GenUI educacional: ganhos de usabilidade não garantem emoção ou eficiência
Li et al., 24/11. https://arxiv.org/abs/2611.31899
- Evidência: estudo randomizado, 282 questionários pós-tarefa, VisualCodeChat vs chatbot textual; usabilidade e eficácia percebida superiores nas comparações não ajustadas, sem diferenças significativas de eficiência, satisfação ou emoção posterior.
- Mecanismo possível: etapas e visualizações reduzem exigência de estruturar perguntas e acompanhar estado.
- Produto: oferecer visualização contextual e alternativa textual; testar por experiência prévia e dispositivo.
- Experimento: tarefas objetivas de aprendizado e transferência, carga subjetiva, erros e acessibilidade.
- Limites: domínio de algoritmos de grafos, questionários e análises exploratórias. Sem coleção legítima.

## 7. Descrições auditivas concretas em compras por voz
Sollfrank, Feldner e Kim. Publicado online 14/12/2025, volume 61(4) de novembro de 2026. https://doi.org/10.1177/10949968251410949 ; https://journals.sagepub.com/doi/full/10.1177/10949968251410949 ; https://researchbox.org/4792
- Evidência: dois experimentos (391 online e 121 laboratório). Maior vividez favoreceu lembrança; no laboratório, nas condições com ambas as descrições, compra da opção vívida 59%, neutra 23%, nenhuma 18%. Compra de EUR 1,50 com remuneração de EUR 5. Comparação entre opções p<0,001. Número de atributos não moderou significativamente estimulação da marca no estudo 1.
- Mecanismo: imagem mental e estimulação percebida da marca; mediação não isola mecanismo causal.
- Produto: descrever situações concretas de uso verificável em recomendações faladas, mantendo especificações acessíveis.
- Experimento: comparar linguagem concreta e neutra, fatos, duração, voz e preço iguais; medir compreensão, lembrança, compra, devolução e satisfação.
- Limites: bens cotidianos, laboratório, fala pré-gravada, mensagens de 30 segundos; sem teste de produto digital. Card momentos-de-compra-b2c, distinção do estudo de modalidade texto/voz de 22/11.

## 8. Haptics + IA: mais estímulos podem prejudicar decisão
From techno-sensory challenges to human-centered experiences: The paradox of Haptic–AI integration in luxury retail. Journal of Retailing and Consumer Services, volume 93 (2026). https://doi.org/10.1016/j.jretconser.2026.104958 ; https://econpapers.repec.org/article/eeejoreco/v_3a93_3ay_3a2026_3ai_3ac_3as0969698926002390.htm
- Evidência: resumo indexado relata maior sobrecarga e adiamento de compra quando pistas cognitivas de IA se somam à experiência háptica, atenuados por clareza informacional ou imagem mental.
- Mecanismo proposto: competição por recursos cognitivos entre processamento sensorial e deliberativo.
- Produto: permitir desligar estímulos e apresentar informação em etapas.
- Experimento: testar haptics isolados versus combinação com IA, com igual informação e consentimento; medir compreensão, conforto e decisão.
- Limites: texto integral inacessível nesta rodada, data da primeira publicação não confirmada, contexto de luxo; sem extrapolar para conversão. Sem card até revisão integral.

## Cards
Guia consultado na main (blob 2d38df09cdb01ad604c3edc88a57a22011c092d1). Um candidato, coleção momentos-de-compra-b2c: imagem-mental-descricao-auditiva-compra-voz. Fonte revisada: cards/fontes/2026-11-25-imagem-mental-descricoes-voz.md. SHA-256 58914a038b5a72abc205a86e890954338cc486e6ca358e7b572376b08e8999dc. Contrato: cards/2026-11-25-imagem-mental-descricoes-voz.json. Validade 09/01/2027. Nenhuma revisão editorial, ativação ou arquivamento solicitado. 
