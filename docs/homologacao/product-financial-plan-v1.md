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

A preservação dos dados é verificada na reaplicação e no reinício da aplicação. O rollback
da migração remove a tabela nova e é testado somente no banco descartável; a recriação do
schema não recupera seus registros. Um rollback apenas do código pode manter a tabela aditiva.

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
