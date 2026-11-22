# Orçamento de contexto de Dédalo — 22/11/2026

## Caso e limites

Base preservada: main bc0d309b6c0e7c91cbed8240a64185815216b6da, PRs 5865/5866.
Consulta somente leitura via MCP confirmou tarefa 922 BLOCKED, sem resultado e sem custo
informado. Não interpretar ausência como zero. Nenhuma retomada, sucessora ou aprovação
foi executada nesta investigação. Teto informado US$200, conhecidos estimados US$113,33,
reserva zero, saldo estimado US$86,67 e dispensa 140 não são alterados pela correção.

## Medição na fonte persistida

Consultas SQL de CHAR_LENGTH/OCTET_LENGTH/JSON_QUOTE no prompt preservado, sem exportar
conteúdo privado para Git:

| Seção | Caracteres |
|---|---:|
| Prompt integral | 1.115.310 |
| Prompt escapado com JSON_QUOTE | 1.177.163 |
| Núcleo do agente | 8.898 |
| Atividade com contexto | 1.106.410 |
| processContextJson serializado | 916.954 |
| taskTarget (JSON_EXTRACT) | 139.184 |
| operatorGuidance | 40.845 |
| researchIntelligence | 7.963 |
| historicalContinuityArtifacts interno | 510.062 |
| versionContinuity interno | 316.255 |
| productBigIdea interno | 14.764 |
| learningSalesCycle interno | 63.943 |

Prompt UTF-8: 1.146.196 bytes. Limite reportado: 1.048.576 caracteres.
Tamanhos internos são projeções JSON MySQL; não somar níveis nem confundi-los com o wire
exato do Codex. Schema e envelope final do transporte antigo não estavam persistidos como
uma única requisição; não atribuir tamanho exato sem captura. O preflight novo mede envelope
JSON escapado com schema, em bytes UTF-8, com orçamento conservador de 900.000 e reserva
para transporte/ferramentas. Não é um medidor de tokens ou prova de custo.

## Alternativas

1. Só deduplicar o prompt: simples, mas insuficiente quando o conteúdo único excede o limite.
2. Projetar no backend com novas rotas: fonte central, porém exige novo contrato de autorização
   e recuperação de snapshots para todos os consumidores.
3. Projetar no executor e disponibilizar snapshot imutável via MCP somente leitura: preserva
   a entrada canônica e a lease/outbox; atende o executor sem habilitar shell. Escolhida.

A projeção mantém estrutura histórica, critérios, identidade, aceites e pendências. Folhas
textuais extensas do histórico/continuidade/correções tornam-se referências por SHA-256,
deduplicadas sem resumo nem truncamento. O snapshot tem nome por tarefa e hash da entrada,
não sobrescreve o de outra execução. MCP só conhece esse snapshot, valida hash e pagina o
texto JSON integral; registra as leituras. Não há acesso arbitrário a caminhos ou rede.
Big Idea e contrato corrente ficam inline. Mínimo irredutível bloqueia antes do processo.
A recuperação de callback continua usando a outbox existente, sem nova inferência.

## Matriz de homologação e aceite

- Volume superior ao limite e mínimo irredutível: recusar antes de iniciar processo.
- Limite serializado exato e primeiro byte excedente; Unicode, aspas e barras.
- Duplicatas, contexto aninhado, histórico separado de aceites e pendências.
- Recuperação MCP integral em páginas, SHA, referência desconhecida e identidade sintética.
- Caminhos comuns, prototypeCorrection, callback/outbox e idempotência existentes.
- Mensagem de negócio legada e nova sem classe/JSON; diagnóstico fechado.
- Desktop e mobile: conferir legibilidade e abertura do diagnóstico sem disparar execução.
- Nenhuma métrica sintética conta como venda, aceite do Vega ou consumo real de 922.

Validação local: 120 testes do worker passaram; após ampliar a projeção de payloads,
reexecutadas as regressões de orçamento e consumidor. Teste MCP real passou em leitura
paginada/Unicode/hash/isolamento. Backend: síntese humana e 21 testes de controles operacionais
passaram. Frontend: nove testes, typecheck e navegação do cartão em Chromium desktop,
iPhone 15 Pro e Pixel 7 passaram. Navegação sintética isolada, sem chamadas ao backend real.
Pacote de compatibilidade comercial gerado: 857 arquivos e 224 manifestos.

Não houve execução paga para provar o comportamento do modelo ao decidir quais referências
ler: comprovadas a disponibilidade da ferramenta e a recuperação determinística integral.
A correção não aprova critérios e não atesta a experiência funcional do produto Vega.
Publicação e revisão devem ser conferidas no PR desta entrega; estes testes são evidência local.
