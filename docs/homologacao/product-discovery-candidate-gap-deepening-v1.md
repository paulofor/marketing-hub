# Matriz de homologação — aprofundamento de lacunas por candidata v1

Data da definição: 2026-09-23

## Escopo

Validar a nova versão do Processo 1 entre a pesquisa factual inicial de Argos e o envio do dossiê a Atena. A versão deve preservar ciclos históricos, exigir comportamento passado consentido e limitar a pesquisa adicional por candidata.

## Decisões comparadas

| Alternativa                                                                             | Benefício                                                                       | Risco                                                                                            | Esforço | Aderência ao objetivo                                                   |
| --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------- | ----------------------------------------------------------------------- |
| Ampliar somente o prompt da tarefa existente                                            | Mudança pequena e rápida                                                        | Mantém pesquisa ampla, entrevistas e aprofundamento numa conclusão única, sem gate persistido    | Baixo   | Baixa: não impede repetição nem comprova a lacuna resolvida             |
| Criar serviço ou subprocesso autônomo de pesquisa com clientes                          | Isolamento completo e evolução independente                                     | Duplica orquestração, contratos e relatório antes de existir volume que justifique outro serviço | Alto    | Média: resolve a separação, mas amplia desnecessariamente a arquitetura |
| Versionar o Processo 1 com uma segunda atividade e contratos leves no domínio existente | Gate explícito, histórico preservado, reuso do worker e auditoria por candidata | Exige migração, tela e compatibilidade entre versões                                             | Médio   | Alta: concentra custo nas lacunas e mantém o backend como coordenador   |

Escolha: Processo 1 v7 com `candidateGapDeepening`. É a menor mudança reutilizável que torna
entrevistas, limites, contrapontos e evidências de resolução parte do aceite antes de Atena.

Para uma lente repetida, também foram comparados: executar mesmo assim, gerando custo sem novidade;
falhar toda a etapa como erro técnico; ou registrar a proposta como rejeitada e encerrar sem coleta.
Foi escolhida a terceira opção, com `REJECTED_REPEATED_RESEARCH_LENS`, porque preserva a auditoria,
não atribui consultas ou custo inexistentes e permite um encerramento funcional honesto.

## Matriz

| Área                  | Cenário                                                                                                                                                   | Evidência de aceite                                                                                                                                 |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Caminho feliz         | Argos entrega duas ou três candidatas, o backend preserva suas identidades, são registradas de cinco a oito entrevistas e a segunda atividade é executada | As duas atividades ficam concluídas; Atena recebe apenas dossiês finais; IDs das candidatas não mudam                                               |
| Gate comportamental   | Há ao menos uma compra, uma desistência e uma situação vinculada a cada candidata                                                                         | O ciclo sai de `AWAITING_CUSTOMER_EVIDENCE` para `READY_FOR_RESEARCH` somente quando todos os critérios forem atendidos                             |
| Validações            | Faltam entrevistas, um dos desfechos ou cobertura de candidata                                                                                            | O aprofundamento não entra na fila e a tela informa a lacuna concreta                                                                               |
| Privacidade           | Código ou relato contém contato, falta consentimento ou a data está no futuro                                                                             | O backend rejeita a gravação sem persistir o relato                                                                                                 |
| Limites               | O plano excede doze consultas por tentativa, duas tentativas, quatro invocações de modelo ou US$ 0,12 estimados de busca                                  | O backend rejeita plano ou conclusão; custo monetário do modelo continua separado, é apurado no ledger após o callback e nunca é convertido em zero |
| Qualidade das fontes  | Plano não informa pergunta, fonte adequada, evidência esperada ou contraponto por candidata                                                               | A etapa falha fechada antes da nova coleta                                                                                                          |
| Reuso                 | Pesquisa inicial, ofertas e evidências válidas já existem                                                                                                 | O worker recebe o corpus anterior e não repete a coleta geral                                                                                       |
| Contradição           | A nova evidência não resolve ou contradiz a hipótese                                                                                                      | A candidata permanece `RESEARCH_MORE` ou é rejeitada; o gate não fabrica aprovação                                                                  |
| Precisão da auditoria | A segunda lente repete parcial ou integralmente o plano e é interrompida antes da coleta                                                                  | Recebe `REJECTED_REPEATED_RESEARCH_LENS`; consultas ficam planejadas/não executadas, sem custo ou atribuição à resolução                            |
| Concorrência          | Dois workers consultam a mesma etapa ou um callback usa lease/rota incorretos                                                                             | Apenas um lease é válido e callbacks cruzados são rejeitados                                                                                        |
| Retomada              | Worker reinicia, lease expira ou callback falha                                                                                                           | Entrevistas e pesquisa inicial permanecem; a etapa pode ser retomada sem duplicar tarefa ou cobrança                                                |
| Observabilidade       | A etapa termina ou falha                                                                                                                                  | Tarefa, tentativas, buscas planejadas/executadas, evidências de resolução, fonte de preço, custos, erros e lacunas ficam auditáveis                 |
| Métricas              | O ciclo conclui                                                                                                                                           | Relatório diferencia entrevista qualitativa, maturidade do dossiê, utilidade posterior, compra, reembolso, custo integral e margem                  |
| Isolamento            | Há ciclos e candidatas diferentes                                                                                                                         | Uma entrevista só pode referenciar candidata do próprio ciclo; ciclos v6 continuam imutáveis                                                        |
| Interface desktop     | Operador consulta o ciclo e registra um relato                                                                                                            | Critérios, limites, fontes, campos obrigatórios e retorno aparecem sem sobreposição                                                                 |
| Interface celular     | Jornada em iPhone 15 Pro e Pixel 7                                                                                                                        | Formulário, consentimentos e botão final permanecem acessíveis por toque e rolagem                                                                  |
| Banco                 | Changelog executa no MySQL 5.7                                                                                                                            | Tabela, chaves e versões 7/19 são criadas de forma idempotente; includes são relativos e campos temporais usam `DATETIME`                           |

A validação física integra a fixture `PdeCommercialPrinciplesMysql57Test`: aplica a cadeia v18,
publica processo v7/cadeia v19, simula ausência de ledger após o DDL e execução DML parcial,
reaplica, reverte os dois changesets e reaplica novamente sem trocar o vínculo histórico do produto.

## Resultado comercial esperado

Esta mudança reduz pesquisa dispersa e entrega a Atena evidências mais próximas de decisões reais. Ela não comprova demanda, utilidade, venda ou lucro: essas confirmações continuam nos processos de estratégia, economia, experiência e validação comercial.
