# Matriz de homologação — separação Atena, Têmis e Hermes v2

## Decisão

| Alternativa | Benefício | Risco | Esforço | Aderência a vendas |
| --- | --- | --- | --- | --- |
| Manter sobreposição | Nenhuma migração | Decisões contraditórias e custo duplicado | Baixo | Baixa |
| Unir Atena e Hermes | Contexto concentrado | O autor avalia a própria estratégia e mistura decisão com operação | Alto | Média |
| Especializar com contrato versionado | Autoria única, execução auditável e aprendizado causal | Exige contrato e integração entre tarefas | Médio | Alta |

Escolha: especializar. É a única alternativa que preserva independência, reduz custo duplicado e
permite saber se uma falha veio da estratégia, da comunicação ou da operação.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validação/falha | Evidência esperada |
| --- | --- | --- | --- |
| Estratégia | Atena v2 pesquisa e persiste `MARKET_STRATEGY_V2` | Menos de três alternativas, fontes insuficientes ou fronteira errada são rejeitadas | execução, resposta bruta, fontes, versão e hash |
| Contexto | backend resolve plano e experimento para o mesmo contrato | referência desconhecida fica sem enriquecimento; parecer v1 vira `MISSING` | `processContextJson.marketStrategicContract` |
| Comunicação | Têmis cria três traduções e um contrato fiel | contrato ausente/insuficiente ou alteração estratégica bloqueia antes da execução | referência da estratégia e contrato de comunicação |
| Operação | Hermes cria rota, atribuição, eventos e gates | qualquer campo estratégico, hash ausente ou revisão necessária com sucesso é rejeitado | contrato operacional e auditoria de ferramentas |
| Diagnóstico | Hermes compara eventos com a estratégia imutável | contradição solicita Atena; não gera nova tese | `strategicContractAssessment` |
| Observabilidade | request, response, modelo, tokens/custo, fontes, erro e decisão permanecem correlacionados | falha de parse preserva stack trace e tarefa bloqueada | execução e tarefa persistidas |
| Métricas | vendas, receita, entrega e satisfação continuam fatos externos | recomendação, tarefa, clique ou checkout não contam como venda | funil oficial e critérios continuar/ajustar/parar |
| Segregação | plano e experimento do mesmo portfólio recebem o mesmo hash | outro produto/plano nunca herda o contrato | testes com referências distintas |
| UI/navegadores | o harness expõe as novas fronteiras sem mudar componentes | o manifesto do Rigel deve apontar para a superfície atual | build público, 5 testes de analytics e 12 jornadas em desktop, iPhone e Pixel |

Uma primeira rodada completa sem defeitos encerra a homologação. Se algum defeito for encontrado e
corrigido, a contagem reinicia e exige duas rodadas completas consecutivas sem falha.

## Defeitos descobertos e correções de causa-raiz

| Defeito | Causa-raiz | Correção preventiva |
| --- | --- | --- |
| versões presumidas 2 não correspondiam à produção | agentes já estavam em Atena 3, Hermes 4 e Têmis 2 | changelog incrementa a versão vigente e preserva todas as versões históricas |
| duas URLs da mesma natureza poderiam aparentar corroboração | backend contava fontes, mas não classes independentes | conclusão de Atena exige duas `evidenceClass` distintas |
| hash com 64 caracteres não hexadecimais passava pelo pré-gate | validação verificava comprimento antes do modelo | Hermes e Têmis exigem SHA-256 hexadecimal, versão, status e fronteira antes do modelo |
| bloqueio contínuo de Hermes tinha formato diferente do schema v2 | o atalho de custo zero retornava diagnóstico parcial | gate determinístico agora persiste a mesma estrutura auditável, com três alternativas e critérios |
| manifesto do Rigel apontava para a implementação anterior | remoção de dados empresariais e novos testes alteraram os artefatos | jornada atual foi re-homologada e os hashes foram atualizados somente após build e testes responsivos |

## Rodadas finais consecutivas

| Rodada | Backend | Atena | Hermes | Têmis | PDE backend | Rigel | MySQL 5.7 | Resultado |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- | --- |
| 1 | 1.970 testes | 10 | 30 | 65 | 117 | build + 5 analytics + 12 jornadas em 3 dispositivos | aplicação + reaplicação idempotente | `PASSED` |
| 2 | 1.970 testes | 10 | 30 | 65 | 117 | build + 5 analytics + 12 jornadas em 3 dispositivos | aplicação + reaplicação idempotente | `PASSED` |

As rodadas não executaram modelo externo, publicação, campanha, mensagem, gasto nem venda. Tráfego
`mh_test` permaneceu segregado e o SMTP usado foi exclusivamente descartável.
