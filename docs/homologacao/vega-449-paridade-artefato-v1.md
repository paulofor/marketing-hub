# Vega #449 — paridade entre manifesto, build e pixels

## Diagnóstico confirmado em 18/09/2026

- Produto 4 continua cadastrado no backend como `PDE - Produto Digital Experiencial`, formato
  `Programa guiado de 7 dias`, compra única de R$ 67 e versão
  `musa-pde-entry-v12-primeiro-ajuste-aplicavel`. O fluxo preserva produto, cadeia 14, processo 77,
  ciclo 2 e experimento 92.
- A tarefa #449 terminou `BLOCKED`; não está presa. Ela consumiu USD 0,856368 estimados porque a
  verificação de paridade ocorria somente por textos visíveis e não pela identidade do artefato.
- As tarefas #448 e #449 capturaram os mesmos pixels em `https://v8.clubemusa.com.br`: o full-page
  e todas as cinco dobras possuem os mesmos SHA-256. O diagnóstico público identificou a imagem
  `61144aee0eab34e620fd1a73b6d314ceb07589e2`, publicada em 17/09/2026 às 21:21:42 UTC.
- O código e o manifesto v4 já continham a privacidade recolhida, mas o workflow de `push` construía
  a imagem e mantinha `PDE_DEPLOY_FRONTEND_VERSION=none`. O smoke de `push` ainda era forçado para
  v7. Assim, a alteração chegou ao repositório e à imagem candidata, mas nunca substituiu a v8.
- O `bundleIntegrity=VERIFIED` comprovava somente a coerência interna do pacote entregue a Psique;
  não comprovava que o domínio público servia o mesmo código. Essa distinção não estava executável
  no harness e permitiu uma nova revisão paga sobre pixels antigos.

## Alternativas consideradas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Reexecutar a #449 ou reconhecer a copy visível | Nenhuma mudança estrutural | Repete custo e aceita pixels antigos | Rejeitada |
| Publicar todas as superfícies PDE em todo merge | Reduz esquecimento operacional | Amplia blast radius e pode trocar produtos não homologados | Rejeitada |
| Manifesto declara alvo; workflow publica só o alvo; Psique confere fingerprint público antes do modelo | Fecha transporte e verificação, preserva isolamento e evita cobrança inútil | Exige contrato, fingerprint e testes cruzados | Escolhida |

O desenho aplica o ciclo `execução → trace → causa → candidata → verificação → promoção`, descrito
em `docs/canonical/aihub-aperfeicoamento-agentes-canon.v1.md`. A ideia de transformar uma conclusão
importante em estado verificável foi reforçada por `pesquisas/agentes-inteligentes/2026-09-17-agentes-inteligentes.md`,
seção 1 (ContrAgent); a transferência foi validada localmente e não assume os resultados externos.

## Matriz de homologação definida antes dos testes

| Dimensão | Critério de aceite |
| --- | --- |
| Caso histórico #449 | Copy pode coincidir, mas fingerprint antigo bloqueia antes de anexar pixels ou chamar o modelo |
| Caminho feliz | Versão, experiência e fingerprint públicos iguais ao manifesto permitem captura e parecer |
| Nova execução | Outro identificador de tarefa usa o mesmo contrato sem exceção por produto, ciclo ou tarefa |
| Reinício e retomada | A verificação é refeita; nenhuma resposta histórica é convertida em prova da build atual |
| Callback e idempotência | As regressões existentes de outbox, 5xx, lease e cobrança duplicada continuam aprovadas |
| Publicação | Merge com um manifesto elegível seleciona exatamente uma superfície; ausência seleciona `none`; ambiguidade falha fechada |
| Isolamento | Uma candidata v8 não publica v5, v6, v7, Mira ou Kit WhatsApp |
| Observabilidade | Diagnóstico público expõe versão, experiência, commit e fingerprint do código realmente contido na imagem |
| Privacidade | Detalhes começam recolhidos, abrem, fecham e mantêm o link de direitos visível |
| Integrações | Bundle, imagem Docker, entrypoint, Playwright e worker usam o mesmo contrato versionado |
| Métricas e custo | Testes usam tráfego `INTERNAL_QA`; nenhuma venda, campanha, pagamento ou chamada de IA real |
| Dispositivos | Desktop Chromium, iPhone 15 Pro e Pixel 7 sem overflow e com o mesmo comportamento |
| Regressão | Duas rodadas completas e consecutivas após a última correção, sem novo defeito |

## Resultado da candidata

- O manifesto v5 preserva produto 4, cadeia 14, processo 77 v1, ciclo 2, experimento 92, preço,
  checkout, público, promessa e formato guiado de sete dias. O cadastro continua sendo do tipo
  `PDE - Produto Digital Experiencial`; nenhuma reclassificação ou mudança de oferta foi feita.
- O fingerprint local aprovado é
  `09e282435e2496f1a4fc96c1fbd90dbb8c368a3b87114a9b3af8899c3f10ef42`. A imagem Docker v8
  construída localmente expôs exatamente esse valor, a versão v8 e a experiência v12 no diagnóstico.
- O resolvedor selecionou somente `v8` para o manifesto v5 e recusou ausência de vínculo, hash
  divergente, fonte diferente e mais de uma publicação automática no mesmo merge.
- Psique aprovou o caminho com identidade coincidente e recusou a reprodução da #449 mesmo com CTA
  e copy coincidentes, antes de upload, prompt ou chamada ao modelo.
- `mvn verify` de Psique: 125 testes, zero falha/erro e uma integração externa dispensada; inclui
  outbox, callback 5xx, reinício, lease interrompida, prevenção de nova inferência e prompt abaixo do
  teto. Build do frontend, pacote comercial com 154 arquivos e imagem Docker v8 foram aprovados.
- Testes de fingerprint/resolvedor/pacote: 22 aprovados. Contrato HTTP/diagnóstico: 18 aprovados.
  `bash -n`, ShellCheck, Spotless, `git diff --check` e sintaxe do workflow foram aprovados para os
  arquivos alterados.
- Depois da última correção, duas rodadas consecutivas aprovaram 36 jornadas cada em Desktop
  Chromium, iPhone 15 Pro e Pixel 7. Foram cobertos v12 gratuito, CTA/condições antes do vídeo,
  privacidade recolhida e reversível, compra Pepper simulada, acesso por 90 dias, entrega, retomada,
  reembolso idempotente e segregação de métricas `INTERNAL_QA`.
- Plutus não foi chamado novamente: preço, envelope e premissas econômicas desta versão não mudaram,
  e a atividade econômica já estava concluída. Os testes não comprovam venda, receita ou lucro; a
  cobertura de custo da execução produtiva continua parcial.

## Comparação e situação real

| Critério | Baseline v4 | Candidata v5 |
| --- | --- | --- |
| Seleção de deploy no merge | `none` | somente `v8` declarada |
| Prova entre pacote e domínio | copy/CTA | versão + experiência + SHA-256 da fonte |
| Build público antigo | percebido após revisão paga | bloqueado antes do modelo |
| Evolução do manifesto | teste preso ao nome da revisão | revisão vigente e predecessor lidos dinamicamente |
| Teste comercial | pixels antigos chegaram a Psique | duas matrizes locais completas sem IA paga |

A candidata foi aceita localmente e a v4 permanece disponível para rollback. Produção continua com
a tarefa #449 `BLOCKED` e o domínio v8 antigo até PR e deploy oficiais. Nenhuma nova tarefa foi aberta,
nenhuma venda/campanha foi acionada e nenhuma inferência paga foi repetida. A retomada produtiva deve
ocorrer uma única vez somente depois de o diagnóstico público comprovar o fingerprint v5.
