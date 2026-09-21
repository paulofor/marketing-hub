# Matriz de homologação — plano financeiro v1

Definida antes dos testes em 15/09/2026. Somente sandbox; sem chamada paga, publicação ou
alteração de Vega/cadeia 14/ciclo 2/experimento 92. Dados com identificadores sintéticos e
escopo TEST. O novo cadastro produtivo aguarda a disponibilização da tela pelo fluxo autorizado.

As operações que exercitam a fila usam também escopo `LIVE` **somente dentro do banco isolado**,
com produtos fictícios 95101–95120 e provedor simulado. O backend recusa a fila paga para `TEST`.
Nenhuma dessas identidades existe como dado criado pelo teste no sistema produtivo.

## Evidências de diagnóstico

- Tela pública `/products/4/financial`: apresenta realizado agregado por nicho e câmbio fixo;
  não possui plano de custos de entrega e cenários por versão.
- MCP `db_health`: banco `marketinghubdb` respondeu. Produto 4 continua Vega, tipo ID 1.
- MCP: `product_execution_profile_v1` sem registros na consulta; não presumir ficha aprovada.
- MCP / `information_schema.COLUMNS`: os IDs de `product`, `product_type_definition`,
  `commercial_plan` e `financial_agent_execution` são `BIGINT` assinados e não nulos,
  compatíveis com as referências da nova migração.
- Há pareceres financeiros históricos em `financial_agent_execution`; a conciliação existente
  não equivale a plano de viabilidade. Preservar os contratos e a auditoria dessa fila.

## Critérios de homologação

| Área | Casos e comprovação |
| --- | --- |
| Caminho feliz | Criar modelo por tipo, adotar revisão em produto, salvar e recarregar plano; projetar cenários e solicitar Plutus simulado |
| Cálculos | BRL/USD com fonte; receita líquida; deduções únicas; IA por resultado útil; contribuição; margem; CAC máximo; fixos/investimento; equilíbrio |
| Falhas | Campos ausentes não viram zero; percentuais inválidos; contribuição negativa; receita nula; uso intenso; limite/tentativas incompatíveis |
| Integração | Controller/service/repository reais; MySQL 5.7; fila Plutus simulada; falha técnica e retomada sem novo gasto |
| Histórico | Revisões imutáveis; modelo copiado não altera versão anterior; mudança de plano comercial/vencimento invalida revisão |
| Segregação | Dois produtos, tipos diferentes, outros IDs/entradas, referências indevidas, TEST/LIVE separados |
| Concorrência | Duas solicitações da mesma análise convergem para uma execução; gravação otimista evita sobrescrita |
| Observabilidade | Status e causas do backend; fontes, responsável, revisão, parecer, custo ausente explícito; nenhuma projeção exibida como venda |
| Tela | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; links, edição, recarga, validação, erro de API e estado de carregamento |
| Persistência | Migração, reaplicação/idempotência, rollback e recriação do schema; inicialização JPA; histórico após reinício |
| Regressão | Testes unitários dos módulos Java alterados, testes relevantes do frontend, typecheck, build e revisão do diff |

Uma rodada completa sem defeitos encerra a homologação. Havendo defeito corrigido, concluir
duas rodadas completas consecutivas após a última correção. Casos sintéticos exercitam
generalização; não constituem held-out independente nem comprovação de lucro real.

## Extensão da matriz — preflight econômico Quartzo em 21/09/2026

Definida antes da matriz completa da correção de Capella. O caso original usa preço de R$ 67,
envelope variável de R$ 13,50, CAC máximo de R$ 25, envelope fixo de R$ 73,20, meta explícita de
cinco vendas e R$ 143,15 de custos históricos encerrados. Nenhuma chamada real a Plutus, campanha,
compra ou dado produtivo é criada pela homologação.

| Área | Critério adicional de aceite |
| --- | --- |
| Caminho feliz | Duas escolhas geram revisão pronta; preflight estrutura contrato, meta, envelopes e três sensibilidades; Plutus simulado recebe uma única execução |
| Validações | Meta incompatível com preço, cenário-base sem lucro, pacote aberto e envelope adulterado bloqueiam antes da fila paga |
| Tempo e custos | Custos históricos encerrados aparecem na recuperação sem dupla dedução; período sobreposto exige classificação e não é presumido |
| Compatibilidade | Planos detalhados preservam seus cenários e investimento declarado; revisão agregada não exige decomposição artificial |
| Retomada | Repetição da mesma revisão converge para a execução já criada; falha técnica continua auditável sem cobrança duplicada |
| Processo Quartzo | Parecer `APPROVE` com cobertura completa conclui `economics`, mantém vendas como não comprovadas e libera somente a revisão humana de Psique |
| Leitura do processo | Fontes e snapshot Quartzo são resolvidos uma vez por referência e transação, sem compartilhar o `ObjectNode` mutável entre gates |
| Interface | Resumo mostra envelopes variável e fixo; edição avançada só os substitui por decisão explícita; formulário simples roda em desktop, iPhone e Pixel |
| Observabilidade | Request, versão do prompt/schema, base da projeção, decisão, custo do parecer e impedimentos de preflight permanecem vinculados à revisão |
| Isolamento | Produtos 95101–95120, MySQL e provedor simulado; TEST/LIVE locais separados e nenhuma identidade de Capella escrita em produção |

A preservação dos dados é verificada na reaplicação e no reinício da aplicação. O rollback
da migração remove a tabela nova e é testado somente no banco descartável; a recriação do
schema não recupera seus registros. Um rollback apenas do código pode manter a tabela aditiva.

## Extensão da matriz — identidade visual Quartzo em 21/09/2026

Definida antes dos testes integrados dos módulos de publicação e Psique. As páginas, tarefas e
revisões são sintéticas; o navegador usa `mh_test=1`. Nenhuma chamada ao modelo, publicação
produtiva, campanha ou gasto é autorizado por esta matriz.

| Área | Critério adicional de aceite |
| --- | --- |
| Construção | GeraSalesPage preserva o snapshot sem marcador e publica o SHA-256 exato desse snapshot no documento enviado |
| Transformação | Pixel e otimização de imagens podem mudar os bytes sem perder a identidade da origem auditada |
| Entrega | Lead Portal expõe SHA-256 do HTML persistido antes de analytics e Clarity dinâmicos, igual nas rotas JSON e standalone |
| Captura | Chromium mobile lê origem auditada e HTML servido, mesmo quando `/version-diagnostics.json` devolve HTML |
| Gate feliz | Origem esperada e observada coincidem, runtime tem hash válido, CTA está na primeira dobra e os PNGs são persistíveis |
| Falhas | Origem divergente, hash servido ausente/inválido ou CTA ausente bloqueiam antes do modelo com responsável e ação |
| Idempotência | Repetir a consulta não cria tarefa, publicação ou chamada paga; a página histórica só muda pelo fluxo autorizado |
| Observabilidade | Tarefa conserva publicação, hashes, URL final, dispositivo, dobras e SHA-256 de cada captura |
| Segregação | Fixtures usam slugs e tarefas locais; `mh_test=1` impede analytics comercial e nenhum dado produtivo é gravado |

O caso original de Capella usa apenas os hashes produtivos em consulta de diagnóstico; a
homologação de escrita usa dados locais. Após a publicação da correção, a página #27 precisa ser
republicada sem nova geração para receber o marcador de origem antes da primeira revisão de Psique.

## Resultados

**Homologação concluída em 15/09/2026:** duas rodadas locais completas, consecutivas e sem
falhas após a última correção. Ambas usaram as mesmas fontes de implementação, incluindo
a apresentação completa das recomendações de Plutus. O runner encerrou com código zero.

| Verificação | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend | 3.096 aprovados; 9 ignorados preexistentes | 3.096 aprovados; 9 ignorados preexistentes |
| Worker Plutus | 40 aprovados | 40 aprovados |
| Frontend | 33 aprovados, typecheck e build | 33 aprovados, typecheck e build |
| API, segregação e concorrência | 51 verificações aprovadas | 51 verificações aprovadas |
| Navegação desktop/iPhone/Pixel | 68 verificações aprovadas | 68 verificações aprovadas |
| MySQL 5.7, reinício, reaplicação e rollback | Aprovados | Aprovados |

- Fontes de implementação alteradas: fingerprint SHA-256
  `c1d4c7e84e1271c3cc3d17219f7e1a516f02db481a18dad641e93acd9233269f`.
- [Resumo da primeira rodada](../../artifacts/product-financial-plan/round-1/summary.json) e
  [resumo da segunda rodada](../../artifacts/product-financial-plan/round-2/summary.json).
- Duração das matrizes: 598,13 e 614,62 segundos; não representam latência do produto em operação.
- Capturas locais: [desktop](../../artifacts/product-financial-plan/round-1/browser/desktop-plan.png),
  [iPhone](../../artifacts/product-financial-plan/round-1/browser/iphone-plan.png) e
  [Pixel](../../artifacts/product-financial-plan/round-1/browser/pixel-plan.png).
- Parecer completo inspecionado visualmente em [desktop](../../artifacts/product-financial-plan/review-preview/desktop-plutus.png)
  e [iPhone](../../artifacts/product-financial-plan/review-preview/iphone-plutus.png), por consulta
  somente leitura à fixture. Avisos de contas Meta na casca da tela são dados simulados de outros módulos.
- Referências internas dos documentos OpenAPI verificadas com SnakeYAML, sem referência ausente.
- Diff revisado: nenhuma migração histórica alterada; include relativo declarado; comentários
  de responsabilidade nas classes/métodos Java; nenhuma alteração de produção, commit ou PR.
- Containers, rede e volumes de teste removidos ao final; consulta Compose confirmou ausência de containers do projeto.

Reprodução: `python3 infra/testing/product-financial-plan/run-local.py --rounds 2`.
`--integration-diagnostic` e `--persistence-only` são diagnósticos parciais e não contam como
rodada completa. O runner remove containers, rede e volumes temporários ao encerrar cada rodada.

## Diagnósticos locais durante a implementação

### Correção do preflight Quartzo em 21/09/2026

- Suítes completas: backend com 3.324 testes aprovados e 19 ignorados condicionais; worker
  Plutus com 51 aprovados; frontend com 718 aprovados em 170 arquivos. Typecheck, builds,
  Spotless, Prettier, compilação Python e `git diff --check` aprovados.
- Integração descartável MySQL 5.7: 51 verificações de API, oito solicitações concorrentes,
  duas execuções simuladas de fila e zero chamadas reais ao modelo. Migração, reaplicação,
  reinício, persistência e limpeza da topologia foram aprovados.
- Interface: 68 verificações da tela avançada e 87 da preparação simplificada em desktop,
  iPhone 15 Pro e Pixel 7. Os dados permaneceram no ambiente `TEST` local.
- A primeira execução completa revelou duas expectativas antigas que ainda aceitavam plano
  agregado sem envelope fixo. As fixtures foram corrigidas para o contrato vigente; a proteção
  não foi removida. As suítes e a integração relevantes passaram após o ajuste.
- Diagnóstico integrado final aprovado em 185,19 segundos, fingerprint das fontes
  `2b2204b8d5a721ee701e290e2224ea52ce3d0531f4fd833d282577e9d944024d`. A rodada inclui a
  evidência condicional à presença do envelope fixo e o equilíbrio operacional calculado sem
  investimento inicial.
- A homologação comprova o contrato e o próximo passo do processo com Plutus simulado. Não
  comprova venda, demanda, margem realizada nem melhora de resposta do modelo real.
- A regressão de contexto comprova que a tela não recompõe as mesmas fontes para cada atividade na
  mesma transação. A produção vigente permaneceu entre 20,49 s e 31,41 s nas três medições; a
  redução de latência precisa ser aferida somente após a publicação autorizada desta correção.

### Identidade visual Quartzo em 21/09/2026

- Suítes completas: backend com 3.324 testes aprovados e 19 ignorados condicionais; Psique com
  131 testes Java aprovados, um condicional ignorado e 18 testes de navegador aprovados; Lead
  Portal com 60 testes aprovados. Packages, Spotless, sintaxe Node e `git diff --check` aprovados.
- Fluxo integrado real local: Lead Portal em H2 recebeu uma página segregada, entregou origem
  `1b5234d766c8e498a10726a41e984ff4baf0ff314c1d69d8c97996fc306d6c9b` e HTML servido
  `0dbb7461336b8c5f374fd037dc8e65d427841fdc22eeeaf17f31b59baf2e5390`. Psique capturou
  iPhone 15 Pro, CTA na primeira dobra, full-page e três dobras com SHA-256 próprios. Foram feitas
  zero chamadas pagas; a fixture foi apagada ao final.
- A primeira inicialização manual do aplicativo não carregou o datasource H2 de teste e tentou a
  configuração MySQL padrão, falhando antes de abrir conexão. A homologação foi reiniciada com URL,
  driver, usuário e schema H2 explícitos e então passou. Nenhuma escrita produtiva ocorreu.
- Reprodução da parte navegador/entrega, com Lead Portal local já saudável:
  `node infra/testing/quartzo-page-identity/run-local.mjs http://127.0.0.1:18081`.

- Cálculo: dez casos unitários aprovados; um cenário médio rentável com consumo máximo
  deficitário é reprovado pelo teste de uso intenso.
- MySQL real detectou `ENUM` implícito do Hibernate sobre `VARCHAR` do changelog. Mapeamento
  explícito JDBC corrigido; bootstrap com `validate` passa a proteger a compatibilidade.
- O harness de teste exigiu tipagem explícita do argumento JDBC/Mockito e limpeza dos renders
  entre testes. Foram corrigidos no próprio ambiente local.
- A navegação local do bundle usa `VITE_API_URL` apontando para o proxy isolado: o padrão de
  produção usa a porta 80. A primeira tentativa pelo navegador expôs essa diferença de ambiente.
- O contrato antigo de contexto de Plutus aceitava 4.000 caracteres, insuficientes para fontes
  e cenários válidos completos. Um teste reproduziu a rejeição; o limite passou a 64.000 com
  validação explícita antes da fila. A integração com o service real verifica preservação do
  contexto na tarefa e no request persistido, sem truncamento. Entradas acima do limite são rejeitadas.
- A conferência de navegação horizontal em celular detectou títulos sem semântica de cabeçalho
  de coluna. A reprodução isolada encontrava zero `columnheader`; `scope="col"` tornou o
  cabeçalho reconhecível. A matriz preserva essa verificação e o acesso à última coluna.
- `FinancialCodexRunner` preenche `dailyReport` com `executiveSummary` na projeção. A tela
  precisava ler também o resultado estruturado para apresentar investimentos/tetos sugeridos,
  critérios de continuar/ajustar/parar e as premissas por cenário, incluindo uso intenso.
  O componente passou a apresentar esses campos; testes unitários e de navegador conferem
  o parecer completo e preservam valores ausentes como desconhecidos.
- Matriz preliminar de API: 49 verificações, oito chamadas concorrentes para a mesma revisão,
  apenas uma execução por revisão; migração, rollback e restauração aprovados. A rodada não
  conta como homologação completa: era diagnóstico de persistência e houve edição de arquivos
  de integração durante sua execução. O runner detectou corretamente a mudança de fingerprint.

## Plutus: comparação da candidata

O prompt anterior recebia contexto livre e não distinguia o novo plano financeiro do histórico
do plano comercial. A candidata declara revisão/produto, usa os cálculos persistidos como base,
preserva três cenários no schema e exige avaliar uso intenso nas premissas e limitações.
Testes conferem composição, identidade, campos nulos, fontes e compatibilidade da conciliação
legada. Não houve chamada paga: melhora de resposta de um LLM real não foi medida nesta sandbox.

Foi consultado OpenAI Docs, [Prompt engineering](https://developers.openai.com/api/docs/guides/prompt-engineering),
para separar instruções estáveis e contexto, mantendo contratos verificáveis. Não houve troca
de modelo, ampliação de autoridade ou alteração de pesos.

## Limites e entrega operacional

- A API/JPA e a migração do novo plano usam MySQL 5.7 real na topologia descartável. Os cadastros
  de referência são sintéticos e a resposta de Plutus é simulada. O contrato com o service
  existente é exercitado também em teste unitário; nenhum modelo pago foi solicitado.
- Os testes móveis usam Chromium com emulação de iPhone 15 Pro e Pixel 7. Não representam
  homologação em Safari nativo ou aparelhos físicos.
- A suíte geral do backend mantém nove testes preexistentes ignorados, relativos a fixtures
  opcionais de outros fluxos e a uma comparação de HTML já desabilitada no repositório. Os
  casos específicos do plano financeiro e a matriz MySQL/navegador são executados integralmente.
- Valores de receita, tarifas e custo de parecer presentes nas fixtures são sintéticos. O
  resultado comprova cálculo e fluxo; não comprova lucro, melhoria de conversão ou economia real.
- A tela e os contratos estão preparados no repositório. O cadastro operacional pelo frontend
  depende da publicação pelo fluxo de PR/deploy do usuário; nenhum plano produtivo foi criado.
- Os checkpoints existentes de oferta, desenho, homologação e operação continuam exigindo
  suas decisões. O plano não aplica quotas a executores nem encerra processos automaticamente.
