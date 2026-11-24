# Radar de Design de Experiência — 24/11/2026

## Síntese

A listagem cs.HC do arXiv de 24/11 apresenta 61 entradas (incluindo trabalhos originalmente submetidos em outras datas). Seleção de seis pesquisas, com três trabalhos de 21–23/11 e três anteriores recém-listados. A principal oportunidade é medir a experiência além da exposição inicial e distinguir consenso aparente de evidência verificável. Não há demonstração de ganho comercial no Marketing Hub.

Fonte da triagem: https://arxiv.org/list/cs.HC/recent

## 1. Vídeo imersivo pode deixar efeitos persistentes
Toida et al., preprint 22/11/2026. https://arxiv.org/abs/2611.29647 ; https://arxiv.org/pdf/2611.29647
- Descoberta/evidência: 84 participantes comparados em vídeo estereoscópico 180° e 2D de um grupo musical japonês. Proximidade psicológica ajustada superior até aproximadamente 7 dias (p=0,002 no dia 7); diferença entre grupos não significativa no dia 14 (p=0,076), embora o grupo imersivo ainda estivesse acima de sua linha de base. Impressão subjetiva superior até 14 dias. Entre os 75 que completaram os dois seguimentos, engajamento voluntário precoce autorrelatado em 17/37 (45,9%) vs. 5/38 (13,2%); RR 3,49, IC95% 1,44–8,49. Nenhum participante alcançou a categoria transacional A4.
- Mecanismo possível: presença social e interesse situacional podem prolongar a conexão com o conteúdo; presença não foi medida diretamente.
- Produto: experiências de demonstração imersiva opcionais, não substituição obrigatória de vídeo comum. Avaliar retorno depois da exposição.
- Experimento: comparar conteúdo equivalente, controlar ambiente e dispositivo quando viável, acompanhar retorno voluntário, lembrança objetiva, conforto e custo em 3, 7 e 14 dias; mensurar vendas separadamente.
- Limites: headset em laboratório vs. 2D online em dispositivos próprios, conhecimento prévio desigual, amostra pequena, estímulo único e comportamento autorrelatado. Não prova efeito isolado de formato, prazer, mecanismo neural ou vendas.
- Card: video.

## 2. Mostrar concordância multimodal pode dificultar correção de erros
Mishra et al., preprint originalmente submetido em 15/09/2026, listado em 24/11. https://arxiv.org/abs/2611.28017 ; https://arxiv.org/html/2611.28017v1
- Descoberta/evidência: experimento pré-registrado N=240 sobre preços de carros usados, com acurácia da IA de 75% nos estratos de concordância e divergência. Na concordância, revelar saídas por modalidade aumentou adesão correta de 70,8% a 80%, mas reduziu correções de erros de 37,5% a 25%. Quando havia divergência, as taxas ficaram quase inalteradas. Explicações elevaram acurácia sem demonstrar melhor detecção de erros.
- Mecanismo possível: consenso aparente entre imagem e atributos pode parecer confirmação independente, mesmo quando ambos provêm do mesmo modelo.
- Produto: recomendações acompanhadas de atributos verificáveis, preço total e origem, não selos derivados do consenso interno.
- Experimento: comparar consenso exibido, evidência verificável e interface básica, com casos corretos/incorretos; medir correção, compreensão, abandono e pagamentos reais separadamente.
- Limites: decisão experimental sobre carros usados, acurácia manipulada, não transações. Sem prova comercial.
- Card: momentos-de-compra-b2c.

## 3. SkillLens: depurar skills por versões e evidências
Yan et al., 21/11/2026. https://arxiv.org/html/2611.28706v1
- Evidência: comparação randomizada com 20 participantes, 10 por condição, usando 100 casos FRAMES e uma família de skill de busca. 9/10 concluíram em ambas; medianas entre concluintes de 26 vs. 32 minutos (descritivas). SUS 70 vs. 47,25. Menor demanda mental e temporal autorrelatada após correção de Holm. Participantes inspecionaram regressões além do caso motivador; não se comprovou superioridade das versões finais.
- Mecanismo: preservar ligações entre versões, resultados, casos, traces e diffs reduz reconstrução mental do contexto.
- Produto: painel de manutenção de skills com comparações por versão, hipóteses, testes contrafactuais e rollback.
- Experimento: comparar chat/logs planos com painel em habilidades reais, usando conjunto de teste reservado e medindo regressões, custo e tempo.
- Limites: amostra pequena, tarefa única, comparação de workflows completos; tempo não inferencial. Sem coleção válida para manutenção de skills.

## 4. Intenções precisam ser objetos explícitos e editáveis
Ram et al., 23/11/2026. https://arxiv.org/abs/2611.30793
- Evidência: taxonomia de seis facetas aplicada a 198 artigos de 11 venues SIGCHI. Identifica lacunas na representação de intenções, proveniência autoral e distinção entre preferências provisórias e compromissos.
- Mecanismo: usuário pode ter intenção incompleta ou em evolução; congelá-la como requisito definitivo pode gerar retrabalho.
- Produto: registrar requisito, preferência provisória, decisão delegada e hipótese da IA separadamente; permitir editar e rastrear origem.
- Experimento: testar briefing com estado de intenção versus prompt único, medir retrabalho e erros por pressupostos.
- Limites: síntese taxonômica, não ensaio causal de uma interface. Sem coleção válida.

## 5. Usuários simulados nem sempre contestam como humanos
Huang, 04/10/2026, listado em 24/11. https://arxiv.org/abs/2611.28024
- Evidência: conversas WildChat em que humanos que responderam contestaram mais após desafio do assistente (20,5%) do que endosso (8,2%); um simulador GPT teve 27,1% em ambos. Outros simuladores exibiram efeitos mais próximos de humanos.
- Mecanismo possível: contestação humana pode refletir posição e preferências, enquanto certos simuladores respondem mais à correção percebida.
- Produto: calibrar cada simulador contra dados consentidos de usuários reais; não aceitar concordância sintética como satisfação.
- Experimento: testar reações a correções, recusa e recomendações, medir diferenças por situação e modelo.
- Limites: amostra de conversas selecionadas, análise dos que responderam, sem comprovação em compra. Sem coleção válida; não atualizar card de A/B retrospectivo com desfecho distinto.

## 6. Humanos e LLMs descrevem layouts de maneiras diferentes
Virdi e Pu, 05/10/2026, listado em 24/11. https://arxiv.org/abs/2611.28025
- Evidência: piloto N=6 e dados de preferência de 19 pessoas para pontuar layouts; LLMs tendiam a codificar o layout na primeira mensagem e humanos iteravam com mensagens menores. Diferença de pontuação concentrada em precisão de tamanho e posição.
- Mecanismo: expressar coordenadas por linguagem natural pode exigir esforço adicional.
- Produto: permitir seleção, arraste, redimensionamento e bloqueio de regiões, combinados com instruções.
- Experimento: comparar edição textual e direta, medir correções, regressões e esforço.
- Limites: piloto mínimo, wireframes específicos; sem efeito comercial comprovado. Sem coleção válida para layouts genéricos.

## Cards e decisão editorial
Guia consultado na main: harness-library-api/docs/guia-uso-api-cards.md (blob 2d38df09cdb01ad604c3edc88a57a22011c092d1). Duas fontes criadas primeiro e hashes calculados dos bytes recuperados em base64. Datas de edição: 24/11/2026; validade até 08/01/2027.
- video: video-imersivo-proximidade-engajamento-persistente — cards/2026-11-24-video-imersivo-efeito-persistente.json; SHA-256 88de0df7ba680a3bc027db1bfb5080985aefb32ea9f584de6db40b32887c0b81.
- momentos-de-compra-b2c: consenso-modalidades-nao-equivalente-evidencia-compra — cards/2026-11-24-consenso-modalidades-confianca-compra.json; SHA-256 5c3bd5d4caff27571b989a4783d614b10a2ff9ad29e251dad92415ac2170eec2.

Os demais achados ficam no relatório por ausência de coleção legítima. Candidatos a DRAFT, sem submissão à revisão editorial, ativação ou arquivamento. A publicação automática, se executada pelo workflow do repositório, deve criar apenas DRAFT. Pagamentos reconciliados e testes humanos prospectivos são necessários para demonstrar eficácia comercial.
