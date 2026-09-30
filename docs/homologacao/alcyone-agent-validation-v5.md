# Alcyone — reconciliação do retrabalho privado v5

Data: 30/09/2026

## Limite da evolução

Esta evolução promove o protótipo privado de `alcyone-private-v2` para
`alcyone-private-v3` e corrige a entrega do contexto de retrabalho a Dédalo. Ela não autoriza
participante humano, publicação comercial, checkout real, campanha, mídia, alegação de demanda ou
mudança do estado comercial `PLANNED`.

## Evidência que motivou a correção

- Psique bloqueou a tarefa #577 por uma perda funcional do motivo SAFETY na tela final.
- A correção foi publicada no commit `ac1953d4b769abd217cfde0f6c68d3a95cba2d15`, mas o diagnóstico
  continuou declarando `alcyone-private-v2`.
- As tarefas de correção #578 e #579 receberam a aceitação antiga e bloquearam corretamente: o
  contrato exige que `correctedPrototypeVersion` seja diferente de `previousPrototypeVersion`.
- O callback de acesso reconciliava somente a primeira publicação; nenhum contrato reconciliava a
  versão publicada depois de uma rejeição funcional.

## Alternativas comparadas

1. Atualizar apenas o JSON do produto: baixo esforço, mas repete o defeito na próxima correção.
2. Renomear o runtime para v3 sem alterar o backend: identidade correta, porém Dédalo ainda recebe o
   aceite persistido antigo.
3. Versionar o runtime e preparar a tarefa antes do claim: o backend prova diagnóstico e contrato,
   atualiza a fonte canônica e só então permite consumo do executor.

A terceira alternativa foi escolhida porque corrige a causa-raiz, evita nova inferência sobre uma
versão antiga e conserva a aprovação funcional como gate posterior e independente.

## Contrato implementado

- O runtime corrigido declara `alcyone-private-v3`, sucedendo `alcyone-private-v2`.
- Uma preparação extensível é executada pelo backend antes da reserva de tarefas.
- Na atividade `prototypeCorrection`, a mesma versão mantém a tarefa `PENDING`, com motivo e ação
  persistidos, sem claim, prompt, tokens ou custo.
- Uma versão diferente só é aceita após o probe comprovar HTTPS, produto, slug, imagem, commit,
  hash do frontend, contrato funcional e todos os efeitos comerciais desligados.
- A reconciliação grava versão anterior, tarefa de origem, gatilho, diagnóstico e recibo técnico nos
  dois contratos do produto. A reentrega da mesma tarefa é idempotente; outra tentativa exige outra
  versão.
- O contexto entregue aos agentes aceita o recibo histórico v1 e o recibo automático v2 somente
  quando gatilho e tarefa coincidem com a aceitação canônica; uma prova cruzada é descartada.

## Matriz de homologação

| Área | Caminho feliz | Validação/falha | Evidência exigida |
|---|---|---|---|
| Claim de Dédalo | runtime v3 reconciliado e tarefa reservada | v2 ou build mutado sem nova versão permanece pendente | testes de serviço e hook |
| Persistência | definição e experiência apontam para v3 | nenhuma promoção parcial ou flag comercial | testes do reconciliador |
| Runtime | diagnóstico, contrato e fixtures declaram v3 | identidade divergente é recusada | testes PDE e contrato de release |
| Jornada | ADHERENT, RECOVERY e SAFETY em desktop e mobile | seis falhas recuperáveis e SAFETY sem resultado/provider | Playwright e harness 3 × 3 |
| Observabilidade | tarefa, gatilho, commit e hash auditáveis | zero chamada externa, venda, cobrança ou mídia | contratos e relatório do processo |

## Critério de conclusão

A evolução só é concluída quando os testes locais relevantes passam, o runtime v3 e o backend são
publicados pelo mesmo PR, o diagnóstico produtivo comprova a identidade nova, o backend reconcilia
o produto antes do claim e o Processo 3 avança novamente por homologação técnica, Psique e gate
final sem evidência humana fabricada.
