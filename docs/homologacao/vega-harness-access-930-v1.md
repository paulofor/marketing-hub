# Recuperação da homologação Vega — 24/11/2026

## Diagnóstico observado

#5876/main e9795e3d preservada; 925 concluída uma vez, hashes e consumo conferidos.
103/processo127v12 mantém 4/11; 930 EXECUTOR_FAILURE, sem resultado. O ciclo22 está
OPEN/ADJUSTMENT, versão correta; existem exatamente18 acessos AGENT_VALIDATION
válidos, sem homologationClosed. Todos anteriores à 930. O harness faz emissão
nova para cada cenário/dispositivo e mais uma identidade no SAFETY, sem recuperar
credenciais. Logs antigos já não estão na janela MCP; o erro persistido e os dados
foram conferidos. A reprodução local HTTP/MySQL deve confirmar o motivo do409.

## Alternativas de recuperação

| Caminho | Benefício | Risco/custo | Escolha |
| --- | --- | --- | --- |
| Tabela própria de tentativas ligada às18 identidades | Modelo relacional explícito, concorrência por tentativa | Migração e rollout maiores | Viável, desnecessário nesta entrega |
| Cofre de credenciais recuperáveis com vinculação auditada | Recuperação central para vários executores | Nova chave de criptografia, rotação e armazenamento sensível | Viável, custo operacional maior |
| Contrato interno idempotente com compartimentos no agregado existente | Recuperação sem nova sessão, preserva histórico e locks existentes | Exige isolamento e checkpoint privado | Adotado |

Aumentar teto, apagar sessões ou usar dados legados como prova de uma nova tentativa foram excluídos pelas restrições do pedido.

A reutilização governa18 identidades físicas existentes, nunca renova prazo nem
remove fechamento/revogação. Cenário/dispositivo/papel reservam compartimento;
tentativa tem workspace próprio. Uma tentativa ativa não pode ser substituída;
conclusão explícita permite outra tentativa, mantendo a anterior inacessível e
auditada. Credencial de recuperação vem do harness, só seu hash é armazenado;
checkpoint privado mantém a chave fora de logs/artefatos. Limite cumulativo de24
operações por identidade permanece e inclui histórico e compartimentos.

## Matriz definida antes dos testes

- HTTP/controller/transações/JPA/MySQL5.7 reais: reproduzir limite18 e recuperar sem19ª
  sessão, repetir e concorrer sem duplicar, checkpoint/reinício e recusa de conflito.
- Duas identidades de produto/ciclo/experimento diferentes; versão/fonte erradas,
  credencial inválida, expiração, revogação, ciclo fechado e homologação encerrada.
- Cenário/dispositivo/papel/tentativa isolados, histórico preservado e término idempotente;
  limite24 cumulativo, operações/sinais sintéticos excluídos de qualquer prova comercial.
- Worker real e navegador Chromium desktop/iPhone15Pro/Pixel7: nove combinações,
  interrupção/reinício no meio de cenário, retomada sem duplicar operação/prova.
- Encaminhamento: EXECUTOR_FAILURE não seleciona correção funcional inerte;
  reprovação funcional continua selecionando Dédalo, Psique/Têmis mantêm autoridade.
- Mensagens públicas sem stack trace; diagnóstico sanitizado separado e correlação.
- Candidata: comparar código/prova com histórias da924 e BigIdea v3 antes de revisão;
  especificação da925 não vira implementação por mudança de status.
- Entrega: testes locais, diff, PR/revisão/checks/main/deploy/saúde antes da retomada103.
- Operação: tetoUS$200, custos desconhecidos e recibos129/140/141 preservados;
  nenhuma nova inferência925, dispensa, audiovisual, mídia ou cobrança.

## Aprendizado reutilizável

Antes: cada reexecução consumia acessos cumulativos e o diagnóstico do processo
priorizava atividade condicional sem gatilho. Depois pretendido: recuperação
idempotente e seleção da pendência aplicável. Métrica técnica: novas sessões e
operações duplicadas em retry devem ser zero; recorrência e tempo parado serão
medidos após adoção. Sem alegação de aumento de vendas.

## Provas observadas na sandbox

- Reprodução HTTP/409 no limite18; recuperação idempotente mantém18 identidades.
  Duas identidades de produto/ciclo/experimento, quatro consumidores concorrentes,
  chave conflitante, outra fonte/versão, validade e fechamento passaram.
- A matriz real percorreu nove combinações desktop/iPhone15Pro/Pixel7, com backend,
  controller, gateway e MySQL5.7 locais. A revisão de captura não mostrou transbordamento.
- O teste de reinício mata Node/navegador após persistir a primeira geração em cada
  cenário e inicia outro runner com o mesmo checkpoint. Comparação usa operationId,
  distinguindo replay intencional de operação duplicada; resultado e capturas são
  reutilizados na repetição final. Nenhuma inferência de provedor.
- A primeira configuração de proxy local tentou upgradeHTTP2 na fronteira Vite;
  o proxy diagnóstico passou aHTTP1.1. A fixture do navegador foi segregada em96022
  para não disputar91022 com a suíte histórica. Nenhuma mudança produtiva nesses casos.
- Controle financeiro público reconsultado:11 custos não reportados, teto e saldo
  iguais aos valores anteriores. Contagem anterior12 não foi repetida como atual.
- Código da candidata continua sendo regras contextuais delimitadas. A924 aceita
  PROTOTYPE_SLICE e adia visual/PWA/comercial; o gate exige PRODUCT para comunicação.
  Este trabalho não reescreve essa especificação nem fabrica cobertura ou parecer.

A integração final incluiu a allowlist real do gateway: o teste detectou a rota
nova recusada antes do backend. Controller e transporte foram alinhados, com
regressão de autorização. Na lista compacta do histórico, a mensagem é projetada
pelo backend; o erro original continua somente na auditoria. Entre reescrever a
falha, filtrar no frontend e corrigir a projeção, adotou-se a projeção para
preservar a fonte e a autoridade do backend. A retomada também usa o identificador
da última operação confirmada; novo rascunho não mostra cartão antigo.

Os tempos do harness usam os horários persistidos das operações e checkpoint de
início gravado antes da captura, evitando reiniciar a medição em uma recuperação.
A suíte JS genérica foi interrompida por custo/tempo de capturas não relacionadas;
a validação essencial do script alterado é a matriz real e o runner com queda,
HTTP, controller e persistência. Não é declarada aprovação dessa suíte genérica.

## Ajuste de publicação encontrado pelo CI antes do merge

O precheck real recusou a nova atestação automática: a preparação v15, somente de
compatibilidade, guardava dois campos de diagnóstico do harness dentro de
`privateExperience`. A versão automática v13 já publicada tinha somente os sete
campos funcionais esperados. Copiar a v15 para o novo manifesto automático herdou
metadados incompatíveis com esse contrato estrito. A correção manteve a whitelist
funcional e moveu os dois campos para `sharedHarnessCompatibility`, sem alterar o
validador nem testes de arquitetura. O CLI `validate-release` foi reproduzido
localmente com o manifesto real e passou após a correção, assim como30 testes de
release e os contratos existentes de seleção/empacotamento. A integração anterior
#5876 permanece preservada; nenhum deploy foi usado como teste.
