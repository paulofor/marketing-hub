# OPRM — instruções locais

## Registro obrigatório

- Todo trabalho feito neste projeto deve ser registrado em `/docs/registros/oprm1.md`.

## Endpoints

- Se precisar criar endpoints no backend para fluxos do OPRM, crie no coletor do OPRM quando o contrato pertencer ao executor/coletor.

## Investigação obrigatória antes de corrigir sintomas

- Para qualquer problema em pipeline, status, score, gate, materialização, contaminação de conteúdo, divergência de tela ou comportamento inesperado, não altere diretamente a etapa que apenas exibiu ou bloqueou o problema sem antes provar a causa-raiz.
- Antes de propor ou implementar correção, rastreie o fluxo completo do dado afetado:
  1. Identifique o registro/ciclo/job afetado e o status ou campo que denunciou o problema.
  2. Consulte a fonte persistida via backend/MCP quando houver caso concreto, incluindo tabelas de entrada, etapas intermediárias e saída final.
  3. Identifique em qual etapa o dado foi criado, transformado, classificado, propagado ou bloqueado pela primeira vez.
  4. Diferencie dado vindo da fonte externa, dado gerado por IA, dado criado por regra determinística e dado apenas exibido pela UI.
  5. Compare a regra aplicada com o documento canônico ou plano operacional correspondente antes de mudar código.
  6. Corrija a primeira etapa que introduz, classifica ou persiste incorretamente o dado; não corrija apenas o gate, a tela, o mapper final ou o sintoma downstream.
  7. Preserve rastreabilidade: quando um conteúdo problemático precisar continuar auditável, registre-o como risco/alerta/diagnóstico em vez de apagá-lo silenciosamente ou transformá-lo em sinal positivo.
  8. Adicione teste de regressão cobrindo a causa-raiz e, quando aplicável, teste também o falso positivo que poderia mascarar a correção.
  9. Registre no PR e em `/docs/registros/oprm1.md` a origem comprovada do problema e o motivo da correção escolhida.

## Contaminação por linguagem de solução ou metadado técnico

- Em etapas de pesquisa inicial, coleta, extração, síntese, gate ou materialização, trate termos de solução, oferta, produto, mecanismo, automação, IA, software, ferramenta, curso, campanha ou landing page como possíveis sinais de contaminação até provar que fazem parte literal e necessária da fonte/cânone.
- Conteúdo contaminante vindo de fonte externa deve ser preservado como evidência auditável ou risco quando necessário, mas não pode virar sinal positivo, oportunidade, hipótese, oferta ou mecanismo antes da etapa canônica apropriada.
- Se o problema aparecer em uma etapa final, investigue obrigatoriamente as etapas anteriores para descobrir onde a contaminação entrou ou foi promovida de risco para valor funcional.
