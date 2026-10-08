# Vega — coordenação da publicação privada do ciclo 10

Solicitação #3275, 08/10/2026. Preserva ciclo 10/experimento 103, variante privada
`musa-pde-entry-v13-primeiro-ajuste-aplicavel`, raiz pública v12 e a mesma fonte
`d557a6097b433976b993b63af251364b60a0ef70ff2e762af28e4920f811d373`.
Não muda condições testadas, autorização financeira, oferta, mídia ou cobrança.

## Causa comprovada e histórico

O [PR #5542](https://github.com/paulofor/marketing-hub/pull/5542) foi validado na
sandbox e integrado como `5422e3f3acc7d49f177ee4046fbc1de78d68cf13`. No
[run PDE 37739520406](https://github.com/paulofor/marketing-hub/actions/runs/37739520406),
a imagem privada foi promovida antes de o backend independente oferecer v13.
A checagem pública iniciou às 06:56:01 UTC e duas esperas de 300 segundos
expiraram; a terceira passou após a capacidade aparecer. O
[deploy do APP 37739520429](https://github.com/paulofor/marketing-hub/actions/runs/37739520429)
começou às 06:59:25 e terminou com sucesso às 07:04:57 UTC. A leitura posterior
da API confirmou o catálogo v12/v13. Não se atribui o erro ao produto ou à IA.

O [watchdog 37741268420](https://github.com/paulofor/marketing-hub/actions/runs/37741268420)
também falhou: comparou a variante privada v13 do manifesto com a identidade
pública v12 do inventário. Ambas são deliberadamente distintas. O diagnóstico
não implica que a raiz pública deva mudar para a experiência privada.

## Correção compartilhada e escolha

Retentar após o backend ficar pronto resolveria somente essa ocorrência.
Alongar a espera dos navegadores manteria a promoção antecipada. A correção
escolhida aguarda a capacidade antes dos comandos remotos e da promoção, sem
acoplar os publicadores, cancelar suas filas ou criar outro coordenador.

O publicador existente valida manifesto, fonte, modo privado, identidade e
limites, depois consulta somente o contrato público por GET. Aguarda até 30
minutos pela capacidade declarada, com timeout HTTP; falha antes da promoção
quando ela não chega. Transporte, 502 e 429 admitem espera limitada. Recusa
403 e resposta insegura. Contratos comerciais preservam seu caminho sem GET
privado. Não cria sessão, inferência, pedido ou tarefa.

O watchdog reutiliza o mesmo validador estrito do publicador antes de resolver
a identidade pública do manifesto privado. Continua conferindo versão,
produto, origem, commit e todas as superfícies suportadas. Identidade comercial
errada e fronteira privada inválida continuam sendo falhas.

## Validação local e prevenção

24 testes do contrato de publicação e 35 do monitor passaram. A regressão
cobre atraso original, outro ciclo/experimento/variante, predecessor válido,
Mira comercial, prazo, transporte, falha permanente, cotas e fonte. O comando
real atravessa um servidor HTTP local e faz somente GET sem credencial. O
workflow executa essa regressão e coloca a espera antes dos comandos remotos.
Actionlint versionado valida YAML e shell com ShellCheck.

A matriz da experiência já aprovada no PR #5542 permanece válida: código,
fonte, mensagens, limites e versão privada não mudaram. Não se repete a matriz
sem mudança funcional. [Evidência local](evidencias/vega-ciclo10/local-release-readiness.json).
Os testes são internos; não constituem parecer de Psique, venda ou lucro.

## História e limites

Manifesto privado v5 substitui v4 somente para coordenação e evidências, com
arquivos anteriores preservados. Mira recebe atestação v23 somente porque seu
hash vinculava o workflow compartilhado alterado; oferta, fonte, vídeos, imagem
e pareceres anteriores não mudam. Seu deploy automático continua desabilitado.

O sucessor 10/103 foi preparado pela UI sem novas chamadas de IA. O teto
original de US$ 10 e custo estimado conhecido de US$ 2,3667816 permanecem
históricos; o saldo não é novo orçamento nem cobrança conciliada. A política
vigente exige decisão explícita para seu uso no sucessor. Sem essa resposta,
não se inicia IA nova, mídia, pagamento ou vídeo pago. A v13 ainda precisa das
revisões próprias no fluxo, sem herdar aprovação do predecessor.
