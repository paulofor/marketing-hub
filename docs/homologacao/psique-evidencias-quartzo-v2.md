# Psique — evidências Quartzo v2

Data: 21/09/2026. Escopo: contrato entre captura, upload, contexto e parecer; sem
alterar preço, personalização, entrega contratada, orçamento ou decisão dos gates.

## Caso observado e limites

Capella #7, processo #81 v1, cadeia #17, execução #12, `experiment:88`.
A recuperação da publicação #27 preservou o SHA-256 de origem
`ffa40b6d321da1f1bd757472b99147d2cc8c119190727c4bb9d24ca137356ccb`.
As 14 imagens (página inteira e 13 dobras) permaneceram idênticas após a recuperação.
Nenhuma intervenção manual em container foi necessária; o cache público expirou
pelo seu fluxo normal. Psique #470 então produziu `ADJUST`, custo estimado
US$ 0,677628, com duração registrada de 387 segundos. Não houve aceite comercial
nem venda; custo por homologação comercial concluída ainda não está disponível.

Antes: somente landing; `PageFacts` e identidades eram descartados ao montar a entrada
do modelo, e o backend recusava URL diferente da landing. Depois: landing e checkout
oficial em uma sessão, fatos e hashes no prompt/callback/outbox, preservando limites
entre tela inicial, pagamento, briefing, entrega e uso. Nenhuma etapa paga é repetida
para avaliar a candidata local.

O caso independente de duas páginas falhou no capturador anterior: somente a página 1
foi produzida. A candidata passou nesse caso e nas regressões existentes. O checkout
público de Capella também foi inspecionado sem interação de compra: produto correto,
R$ 67 e opções de pagamento visíveis, com 17 imagens nas duas páginas. Isso confirma
acessibilidade da tela inicial, não liquidação, entrega nem satisfação da compradora.

## Matriz de aceite

| Caso | Evidência exigida |
| --- | --- |
| Landing e checkout válidos | Duas páginas, full-page e todas as dobras, URLs e hashes coerentes |
| Contrato backend | Upload real aceita a segunda página somente para o checkout da revisão Quartzo |
| Outra preferência, página ou processo | Rejeição antes do storage/modelo |
| URL privada ou com credencial | Rejeição antes de abrir navegador |
| Página/dobra ausente ou origem incompatível | Bloqueio técnico anterior ao modelo |
| Reinício/outbox | Mesmos fatos, artefatos e callback sem repetir inferência |
| Regressões | Captura longa, rolagem, pixels, Opala, criativo, callback e falhas preservados |
| Limites comerciais | Sem clique de compra, credenciais, e-mail, pagamento ou aprovação simulada como venda |

Testes usam produtos e referências sintéticos independentes. A integração combina
servidor local, Chromium real, capturador versionado, PNGs, upload, contexto e outbox.
O mesmo conjunto de PNGs também passa pelo serviço de persistência real do backend
com banco/storage substituídos por doubles. A imagem Docker é construída pelo
Dockerfile do repositório e executa os testes reais de captura como usuário de runtime.

Resultados locais: backend com 3.343 testes, 19 condicionais não aplicáveis e nenhuma
falha após corrigir o cadastro do prompt e revalidar os casos afetados; worker com
141 testes, um cenário condicional de Vega não executado e nenhuma falha. A integração
de navegador e upload foi executada nos dois módulos, não ignorada. Foram aprovados
22 testes JS, os sete testes de captura na imagem e nove contratos de empacotamento.
Build, Spotless, sintaxe shell, ShellCheck e revisão do diff passaram. O JAR contém
4.111 classes testadas idênticas e 571 recursos externos íntegros; o catálogo inicializa.

## Reprodução local

1. Instalar as dependências do `customer-agent-worker` com `npm ci` e rodar `npm test`.
2. Rodar a suíte Java do worker com `PSIQUE_BROWSER_INTEGRATION_TEST=true`; Chromium
   e dependências Node precisam estar disponíveis. Sem a variável, apenas a integração
   de navegador é condicional; testes unitários permanecem obrigatórios.
3. Definir `PSIQUE_CAPTURE_EVIDENCE_OUTPUT` para um diretório temporário exclusivo na
   sandbox ao executar `VisualCapturePipelineIntegrationTest`. Esse comando exporta
   somente a fixture sintética e seus PNGs.
4. Usar o mesmo diretório ao executar a suíte backend, incluindo
   `AgentTaskVisualEvidenceServiceTest.acceptsRealWorkerCaptureThroughBackendStorageContract`.
5. Executar build, Spotless, `bash -n` e ShellCheck do contrato Docker, `diff --check`
   e os testes de captura dentro da imagem. Remover containers temporários ao terminar.

O navegador canônico da captura é Chromium em iPhone 15 Pro. Isso não equivale a
homologação de Safari nativo nem da compra real. Prazo de entrega contraditório e
canal/SLA de suporte ausentes continuam exigindo decisão; após isso, a comunicação,
as provas de entrega e os gates pertinentes precisam ser revalidados antes de Psique.

## Fundamentação e rollback

Prompt v2 mantém schema v1 e os dez gates. A candidata acrescenta somente orientação
de leitura dos fatos e seus limites; páginas recebidas são dados, não instruções.
Fontes oficiais consultadas em 21/09/2026: [Prompt engineering](https://developers.openai.com/api/docs/guides/prompt-engineering)
e [Reasoning best practices](https://developers.openai.com/api/docs/guides/reasoning-best-practices).
O prompt v1 permanece versionado. Rollback é feito revertendo o commit pelo fluxo de
PR, mantendo os registros históricos e sem reclassificar pareceres anteriores.
Os testes comprovam transporte de evidências; ganho de acerto, redução de custo e
aumento de vendas exigem medição posterior e não são alegados nesta entrega.
