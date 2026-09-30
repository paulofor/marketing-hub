# Homologação corretiva do desfecho SAFETY de Alcyone — v4

Data da matriz: 2026-09-30.

## Escopo preservado

Esta correção fecha o bloqueio funcional da tarefa #577 sem validação humana. Ela não recruta,
entrevista ou simula participantes; não publica Alcyone comercialmente, não cobra, não cria
campanha, não autoriza mídia e não transforma sinais de agentes em evidência de mercado. O produto
#11 permanece `PLANNED`, e somente venda reconciliada e contribuição poderão provar demanda.

## Diagnóstico confirmado

O backend já bloqueava o pedido SAFETY antes do provider e persistia a causa em `blocker`. A tela
mostrava essa causa enquanto a sessão estava aberta, porém, depois de `finished=true`, a condição
genérica `Homologação concluída` precedia o estado `BLOCKED`. A captura final e a saída estruturada
do harness perdiam motivo, ausência de resultado e próxima ação segura. Psique bloqueou corretamente
a tarefa #577 por essa lacuna, sem alegar que o mecanismo de proteção havia falhado.

## Alternativas comparadas

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Liberar o gate porque o provider não foi chamado | Nenhuma alteração | Aprova uma experiência que não explica o limite; risco alto | Descartada |
| Acrescentar texto somente ao relatório do agente | Esforço baixo | A interface real continua genérica e a causa pode divergir | Descartada |
| Preservar um desfecho SAFETY no contrato, na tela final e no harness | Causa, efeito e ação ficam auditáveis na mesma fonte | Mudança coordenada de backend, frontend e worker | Adotada |

## Matriz definida antes dos testes

| Área | Caminho esperado | Validação e falha | Evidência exigida |
| --- | --- | --- | --- |
| Bloqueio | pedido corporal ou de compra termina em `BLOCKED` | nenhum pacote ou provider pode ser acionado | `safetyBlockedAt`, zero looks, zero provider |
| Explicação | causa persistida continua visível antes e depois da conclusão | tela genérica não pode substituir SAFETY | título específico, motivo literal e recarga preservada |
| Ausência de resultado | interface e contrato dizem que nada foi gerado | qualquer combinação ou alegação de resultado reprova | `resultGenerated=false` e mensagem visível |
| Próxima ação | orientação permite nova execução dentro do escopo | sessão encerrada não é reaberta nem editada | `safeAction` estruturada e visível |
| Evidência | harness devolve o mesmo desfecho persistido | flag booleana isolada não autoriza aprovação | objeto `safetyOutcome` e gate determinístico |
| Regressões | ADHERENT e RECOVERY mantêm jornada, continuidade e cinco sinais | SAFETY não cria sinal comercial | matriz 3 × 3 e auditoria canônica |
| Dispositivos | desktop, iPhone 15 Pro e Pixel 7 exibem o desfecho | sem overflow, controles sem nome ou perda de contraste | screenshots finais e checks de acessibilidade |
| Efeitos | ambiente continua privado e sintético | pagamento, publicação, campanha, mídia ou chamada paga bloqueiam | flags falsas e custo zero |
| Integração | backend, frontend e Psique usam o mesmo contrato | estrutura ausente ou divergente reprova antes do parecer | testes Java, Playwright, harness e topologia local |

## Resultado local

A topologia equivalente à publicação foi executada com as imagens construídas pelos Dockerfiles do
repositório. O harness independente aprovou **9 de 9** combinações de `ADHERENT`, `RECOVERY` e
`SAFETY` em desktop 1440 px, iPhone 15 Pro e Pixel 7, em 12 segundos. Os **32 gates** ficaram
verdadeiros. O relatório bruto tem SHA-256
`0a2b9f809fd826a48d3422ab53015d3060e4faf846556365dfbae958c3ab23ca`.

Nos três dispositivos, SAFETY preservou o mesmo objeto estruturado: `code=OUT_OF_SCOPE`, motivo
persistido, mensagem de nenhum resultado, ação para iniciar nova execução dentro do escopo,
`resultGenerated=false`, `providerCalled=false` e zero chamadas. As capturas finais mostram o
título específico depois da conclusão; a tela genérica não reaparece. A inspeção visual de desktop
e Pixel 7 confirmou hierarquia, legibilidade, contraste e ausência de overflow.

As validações complementares terminaram assim:

- 204 testes Java do backend PDE aprovados, sem falha;
- 148 testes Java e 22 testes Node de Psique aprovados, sem falha;
- testes unitários novos recusam tanto API sem desfecho quanto harness que tente aprovar somente
  com `safetyBlocked=true`;
- nove testes Playwright, build de produção de Alcyone, TypeScript, Prettier e Spotless aprovados;
- contrato OpenAPI atualizado com `AgentValidationSafetyOutcome` e diff sem inconsistências;
- topologia temporária encerrada com containers, rede e volume removidos.

Não houve chamada a provedor, recrutamento, leitura humana, checkout real, cobrança, publicação
comercial, campanha ou gasto de mídia. A evidência comprova apenas prontidão técnica privada por
agentes e não comprova demanda, disposição a pagar, venda, receita ou margem.
