# IA autônoma — 2026-09-24

## Rodada 18:12 BRT

Há novidades relevantes nesta rodada. O principal trabalho é **Growing Harness**, que transforma feedback de falhas em mudanças persistentes no programa que coordena o agente, mantendo o modelo e as ferramentas fixos. Em BrowseComp-Plus e WebArena-Verified, o método obteve a melhor média em 5 de 6 combinações benchmark/modelo, reduziu chamadas ao LLM em 76,0%–91,8% e custo de inferência em 74,4%–98,6%. Classificação: **(2) mudança persistente de código/harness**. Limitação: resultados de benchmark; o mecanismo externo de otimização continua fixo.

Fonte: https://arxiv.org/abs/2609.26760

**FIRE** mostra que regras de runtime derivadas de falhas podem aumentar a repetibilidade sem mudar pesos. No Terminal-Bench 2.1, repeated success passou de 50,6% para 54,0% no Luna, 55,2% para 60,9% no Terra e 64,4% para 73,6% no Sol. Porém as regras foram elaboradas pelos pesquisadores após análise das falhas. Classificação: **(3) regras persistentes + (5) otimização essencialmente conduzida por humanos**.

Fonte: https://arxiv.org/abs/2609.26048

**RegenHarness** propõe evolução versionada de configurações entre missões de um robô físico, sem atualização online de pesos. Registros de execução alimentam mudanças candidatas em contexto, templates, routing e recovery, submetidas a regressões e autorização de release. Classificação: **(3)**. O paper demonstra o runtime num quadrúpede em armazém, mas ainda não mostra uma série quantitativa longa de versões sucessivamente melhores.

Fonte: https://arxiv.org/abs/2609.27612

**SEMV** é um caso-limite: pesos e estrutura do agente permanecem fixos; o que evolui é a memória externa validada. Atinge 91,88% no COSMOS contra 89,10% do baseline comparável mais forte e reduz negative transfer de 5,7% para 0,2%. Classificação: **(4) memória/contexto**, não evolução real do harness.

Fonte: https://arxiv.org/abs/2609.27175

## Padrão reutilizável

A tendência comum é separar o runtime estável do estado comportamental versionado. Feedback de execução gera uma candidata pequena, ela é comparada com comportamento anterior e só então é promovida, mantendo histórico e rollback. Isso permite que regras, routing, templates e skills evoluam fora da imagem do container; rebuild fica reservado para mudanças realmente estruturais.

Ainda não há demonstração convincente de RSI forte nesta rodada: os mecanismos que geram e aprovam as próximas melhorias continuam fixos ou sob controle humano.
