# Homologação do encadeamento de preparação — 07/11/2026

## Evidência e causa

No backend `35554e596a302efe9ef7e2bee4a1d915dab3ab34`, o ciclo 21 do produto 4,
cadeia 27, experimento 114, versão `musa-pde-entry-v18-video-cinematico`, tem retorno
registrado para o processo 113 (comunicação). A execução 98 do planejamento 126
concluiu as tarefas 899/900/901, com US$ 8,945868 registrados. Seu evento de
continuação abriu a execução 99 da comunicação em 07/11 às 03:04:28 UTC
(00:04:28 de Brasília). Não há execução nem gate do processo 117 nessa referência.
O gate 814 pertence ao experimento 111 e à versão v17; não atende o ciclo 21.
A tela repete uma lacuna genérica de estratégia porque Íris recebe contexto incompleto.

O resolvedor filtrava processos pela posição do retorno, excluindo a construção.
A fila serial também precisa permitir executar o predecessor quando a comunicação
já espera suas entradas, sem ultrapassar tarefas em andamento nem outro contexto.

## Alternativas consideradas

1. Reavaliar todos os processos preparatórios desde o planejamento em qualquer retorno:
   cobre predecessores, mas aumenta consultas e pode reabrir trabalho fora do ajuste desejado.
2. Criar um grafo geral de dependências entre macroprocessos e provas: extensível, porém amplia
   contratos e migração sem necessidade comprovada para esta falha.
3. Exigir construção como predecessor no resolvedor e no motor, reutilizando a verificação
   canônica de Íris: proteção compartilhada com menor mudança e os mesmos gates. Adotada.

Para recuperar o caso, foram comparados retomar a continuação do planejamento concluído,
iniciar o predecessor pelo comando já disponível na tela e criar uma nova recuperação automática
no processo bloqueado. A tela do Processo 3 oferece o caminho mais direto: preserva o Processo 2,
seus resultados e o destino histórico da decisão, sem novo contrato ou inferência duplicada.
A retomada da continuação também é testada, inclusive em concorrência. O motor continua
responsável pela passagem ao 4 após a prova; nenhum agente se autoaprova.

## Matriz local de aceite (definida antes dos testes)

| Caso | Verificação | Aceite |
| --- | --- | --- |
| Caminho 2 → 3 → 4 | Resolvedor e motor, contratos reais com dependências simuladas | 3 antecede 4; os resultados pagos do 2 permanecem |
| Retorno registrado direto ao 4 | Resolvedor, iniciar, retomar e conciliar | 3 pendente é mostrado com causa e link; nenhuma tarefa de Íris |
| Provas incompatíveis | Produto/ciclo/cadeia/versão/fonte, gate revogado e tarefa posterior | Nenhuma autorização por histórico ou prova cruzada |
| Prova válida e reaproveitamento | Verificador existente e origem preservada | Passagem ao 4 somente após homologação atual e independente |
| Idempotência e concorrência | Persistência local e comandos HTTP repetidos/concomitantes | Uma execução por escopo; custos/resultados não duplicados |
| Fila com comunicação anterior | Motor e contexto | Predecessor exato pode executar; trabalho em andamento e outras referências bloqueiam |
| Outro produto | Mesmos contratos com identificadores distintos | Sem exceção por nome, produto ou ID |
| Observabilidade e segregação | Motivo, navegação, eventos, custos e fixtures locais | Pendência funcional auditável; testes não viram dados comerciais |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 em Chromium | Motivo e acesso ao predecessor legíveis; sem decisão de negócio no frontend |
| Limites | Contratos/ledger simulados e leitura antes/depois | Tetos preservados; mídia, cobrança e autoaprovação não habilitadas |

## Melhoria reutilizável do harness

A lacuna observada é a divergência entre o próximo trabalho sugerido, o início direto
pelo motor e a prova exigida por Íris. A correção compartilha a verificação vigente,
expõe a pendência de construção e acrescenta regressões executáveis da passagem.
Hipótese: reduzir bloqueios repetidos e intervenções manuais por pré-requisito omitido.
Métrica técnica: impedir 4 sem prova atual e encaminhar uma única execução de 3.
Impacto sobre conversão e vendas permanece desconhecido; nenhum teste é prova comercial.

## Resultados

Validação local concluída antes de publicar código:

- Rodada completa do backend executada. As seis falhas reveladas eram ligadas à nova injeção
  nas fixtures e à projeção depois de disputa de lock; foram corrigidas localmente.
- Reexecutados os testes afetados e as regressões de motor, fila, custo, origem criativa,
  continuidade, gate real de Íris e arquitetura. Relatórios finais: **5.925 testes em 771 classes,
  zero falhas/erros e 68 condicionais ignorados**. Os novos casos essenciais não são ignorados.
- Persistência H2 e lock real do produto: duas retomadas concorrentes geram uma construção e
  um vínculo de continuação; custo do planejamento preservado. Após prova de 3, o mesmo run de 4
  pode retomar. O comando usado pela UI também é idempotente.
- Bundle frontend compilado; navegador Chromium em desktop, iPhone 15 Pro e Pixel 7 mostrou
  causa, link do predecessor e ausência de retomada/início de 4. Nenhuma mutação por navegação,
  erro de página ou rolagem horizontal. A fixture exporta respostas do serviço Java real.
- Verificador de integridade do bundle: 17 testes aprovados. A versão privada Mira v82 é
  sucessora de compatibilidade de v81, preservando-a imutável; atualiza somente provas de código
  e teste relacionadas. Mantém experiência, fingerprint visual, publicação automática desativada
  e aprovações anteriores, sem renovar homologação comercial ou parecer independente.

Limites: H2 comprova esta regressão de persistência/concorrência, sem alteração de SQL ou schema.
A matriz de navegador emula dispositivos no Chromium, não Safari nativo. Testes não comprovam
venda ou impacto comercial. A publicação e a recuperação produtiva serão conferidas pelo PR,
SHA, workflows, identidade de runtime, tela e dados persistidos antes de encerrar a solicitação.
Os tetos produtivos lidos antes da entrega são US$ 200 acumulados de agentes e US$ 15 de
planejamento audiovisual; mídia do experimento permanece R$ 0. Não foram alterados.
