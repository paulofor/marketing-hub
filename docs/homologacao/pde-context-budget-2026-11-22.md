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

Revisão adicional antes da integração: a projeção mantém o catálogo Opala até sua validação e
preserva os tipos originais fora do contexto, usando precisão decimal no contexto JSON. Regressão
específica protege o caminho antes válido. O próprio consumidor executa uma verificação Node sem
modelo para carregar a ferramenta e conferir todos os hashes antes de registrar início de inferência;
snapshot corrompido é recusado. Oito testes específicos do orçamento/leitor passaram, além das
regressões do consumidor e catálogo. O CI instala as dependências fixadas do MCP antes desses testes.

O CI completo detectou uma omissão no cadastro do novo prompt: 6.088 testes, uma falha em
`AgentHarnessCatalogTest`. A origem foi corrigida no manifesto v3 do harness, sem alterar o
teste nem flexibilizar cobertura. A suíte do catálogo passou localmente após registrar o artefato;
a versão corrigida deve passar novamente pelo CI completo antes do merge.

## Complemento identificado na conferência publicada

O PR #5868 foi integrado em `acd17621920304a9f5a8639dd4938860ea57b6ce`, com CI,
publicação da aplicação e do executor concluídos. A revisão somente leitura da página real
identificou uma lacuna na cobertura visual: o cartão escondia o erro, mas as projeções do
cabeçalho e da situação da atividade ainda reproduziam o erro persistido. A consulta leve de
síntese também omitia estado/erro, retornando resumo nulo para uma interrupção sem resultado.

Alternativas: filtrar só no frontend deixa outros consumidores expostos; regravar motivos
históricos modifica auditoria desnecessariamente; projetar linguagem de negócio no contrato de
leitura preserva os registros e cobre os consumidores. Adotada a terceira opção, com diagnóstico
integral mantido em `executionError` e sem alterar estados, gates, contadores ou autorização.
A consulta exata da síntese inclui estado/erro sem carregar prompts/provas. O frontend mantém a
síntese já recebida enquanto a leitura complementar ainda não trouxer outra.

Prevenção: regressão das projeções completas (cabeçalho, fila, atividade e tarefa), dois IDs
sintéticos distintos, custo desconhecido, zero aceites/11 pendências e erro bruto preservado na
auditoria; consulta da síntese segregada por produto/processo/referência; teste da página inteira
com diagnóstico fechado e ausência de POST. Esta ampliação corrige a lacuna do harness visual,
sem alteração de testes de arquitetura. Nenhuma retomada de Vega faz parte do complemento.

Validação local do complemento: 168 testes backend passaram (incluindo 92 ArchUnit sem
modificação, 39 do serviço, 13 da consulta JPA e 21 do catálogo operacional), 22 testes da
página e typecheck passaram. A primeira rodada conjunta terminou em código 137; as verificações
restantes passaram em execução sequencial, sem flexibilizar testes. Chromium desktop,
iPhone 15 Pro e Pixel 7 confirmaram página sem erro bruto visível e diagnóstico recuperável
por abertura explícita, com dados sintéticos e sem acesso ao backend real. Não houve inferência.
