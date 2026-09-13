# Aquisição por problema compartilhado — homologação local v1

Data: 2026-09-13. Escopo: orientação do usuário aos agentes de estratégia e campanhas.
Entrega: cânone e prompts operacionais de Atena, Íris e Hermes. Não cria produto, campanha,
experimento, autorização de gasto ou artefato comercial em produção.

## Decisão e evidências

Comparação comercial e justificativa registradas no
[cânone de Atena](../canonical/agente-estrategista-experimentos-v1.md#produto-específico-e-aquisição-por-problema-compartilhado--2026-09-13).
Produto vertical, aquisição ampla por dor e grupo de verticais próximas são alternativas, não
receitas universais. O exemplo de negócios com agenda é hipótese de planejamento, sem resultado
medido. Preservam-se os testes e contratos aprovados de Vega, Mira e dos demais produtos.

Foram comparados três modos de incorporar a orientação: apenas documentação (baixo esforço,
sem garantia de chegar ao executor), ampliar schemas e persistência (campos dedicados, mas maior
custo e migração sem necessidade funcional) e usar campos existentes com prompts e cânone
alinhados (orientação chega aos executores e preserva contratos). Adotado o terceiro. Trata-se de
instrução ao modelo, não de validação semântica determinística nem comprovação de resultado comercial.

Fontes locais consultadas:

- [Matriz de responsabilidades](../canonical/matriz-responsabilidades-agentes-canon.v1.md): Atena
  decide estratégia, Íris comunica, Plutus valida economia e Hermes opera/mede.
- [Histórico da separação Atena/Hermes](separacao-atena-hermes-v2.md) e
  [LOOP-AGENTES-ESTRATEGIA-OPERACAO-SOBREPOSTAS](../registros/loops.md): impedir que testar verticais
  reintroduza autoria estratégica em Hermes.
- Recursos e carregadores de `experiment-strategist-worker`, `growth-operator-worker` e
  `communication-agent-worker`: localizar prompts realmente consumidos em pesquisa, parecer de
  oportunidade, BPM, decisão do ciclo e comunicação. Recursos históricos permanecem preservados.

## Matriz definida antes da execução

| Critério | Validação local | Evidência esperada |
| --- | --- | --- |
| Caminho feliz de Atena | Fila HTTP local → prompt real → modelo simulado → callback | Contexto do sucessor, instrução integral e auditoria preservados |
| Estratégia de oportunidade e pesquisa | Contratos existentes e leitura dos recursos ativos | Dossiê mantém proveniência; esquema não ganha campos incompatíveis |
| Execução de Hermes | Processos simulados nos escopos de experimento e plano | Constituição integral recebida, estratégia por hash preservada, tokens simulados auditados |
| Comunicação de Íris | Montagem real do prompt e validação do pacote | Mesmo contexto/versão, prova e limites preservados; mensagem dentro da elegibilidade |
| Falhas e integração | Suítes dos três workers | Bloqueio de contrato ausente, hash divergente, contexto cruzado e resposta inválida; reenvio sem nova inferência |
| Métricas e segregação | Revisão de instruções e testes de auditoria/escopo existentes | CAC sem compras não vira zero; gasto sem atribuição fica agregado; QA não vira venda |
| Produto e causalidade | Revisão comparada com cânone e prompt de sucessor | Expansão não modifica teste aprovado; exploração não declara causa nem vencedor |
| Compatibilidade | Revisão do diff e recursos usados pelos loaders | Nenhum schema, endpoint, persistência ou versão produtiva alterados |
| Navegadores/dispositivos | Não aplicável nesta entrega | Nenhuma tela, mídia ou fluxo de interação alterado |

As simulações usam HTTP de loopback, arquivos temporários e respostas fixas, sem credenciais reais,
envio de mensagens, IA paga ou escrita em produção. IDs de fixture permanecem apenas na sandbox.
Uma rodada completa sem defeitos encerra a homologação; defeito encontrado em rodada exige
correção e duas rodadas completas consecutivas após a última correção.

## Resultado

Uma rodada local completa dos critérios aplicáveis, concluída em 2026-09-13 às 04:07 UTC:

| Módulo | Aprovados | Falhas/erros | Ignorados |
| --- | ---: | ---: | ---: |
| Atena | 33 | 0 | 0 |
| Hermes | 34 | 0 | 0 |
| Íris | 26 | 0 | 1 |
| Total | 93 | 0 | 1 |

Executado `mvn -B -ntp spotless:apply test spotless:check` em cada módulo, com `spotlessFiles`
restrito à classe de teste alterada. Os três builds concluíram com sucesso. A preparação inicial
usou um glob no filtro do Spotless, que exige expressão regular; o comando foi corrigido antes
de qualquer teste iniciar. Não houve defeito revelado pela rodada nem repetição para cumprir
quantidade mínima.

O único teste ignorado é `IrisCycleInputIntegrationTest`, condicionado a `VEGA_IRIS_INPUT_FILE`,
que recebe uma entrada produzida pela homologação completa do backend de Vega. Essa entrada não
foi produzida nesta tarefa de orientação. O backend, seu schema e seus gates não foram alterados;
os testes ativos de Íris verificaram composição, contexto privado, prova, hash e bloqueio. Não se
alega ter refeito a homologação do backend, recuperado Vega/Mira ou validado uma campanha.

Os testes ampliados capturam o prompt efetivamente recebido pelos processos simulados de Atena e
Hermes e o comparam com a auditoria e a constituição versionada integral. A montagem de Íris
preserva constituição e contexto. Os testes existentes de rejeição, segregação e reenvio passaram.
A revisão dos recursos confirmou elegibilidade, atribuição, hipóteses e limites econômicos nos
campos existentes. O diff passou em `git diff --check`, sem alteração de schema, endpoint ou banco.

Evidências locais desta sessão:

- [Resumo extraído do Surefire](../../artifacts/acquisition-shared-problem/round1/summary.json).
- [Atena](../../artifacts/acquisition-shared-problem/round1/atena.log),
  [Hermes](../../artifacts/acquisition-shared-problem/round1/hermes.log) e
  [Íris](../../artifacts/acquisition-shared-problem/round1/iris.log).

Homologação técnica do escopo concluída. Testes de contrato verificam carregamento, transporte,
fronteiras e auditoria; não provam que um modelo seguirá toda orientação nem que haverá aumento
de vendas. A eficácia comercial dependerá de experimento futuro autorizado, com amostra,
atribuição e margem. Alterações somente na sandbox, sem commit, PR, deploy ou publicação.
