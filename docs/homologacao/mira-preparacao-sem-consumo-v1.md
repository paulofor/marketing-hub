# Preparação administrativa sem consumo — 06/10/2026

## Evidência e causa

Mira #10, cadeia #26, ciclo #3 ADJUSTED, experimento #93, proposta #3 aprovada no evento #26,
sem sucessor. UI e MCP confirmam duas sessões, nenhuma venda e contribuição de R$ -27,19.
A tela oferece um cadastro manual de ciclo antes da preparação que reutiliza o parecer.
O botão “Preparar continuidade sem gasto” chamava o mesmo caminho usado pela aprovação:
quando havia recibo humano, iniciava também o processo e sua fila de tarefas pagas.
Aprovação da hipótese histórica não comprova autorização de novo consumo de IA.
O histórico de Capella sem aprovação humana não iniciava esse processo; o teste anterior
cobria essa condição, mas não a mesma promessa da UI aplicada a uma decisão já aprovada.

## Alternativas e escolha

1. Apenas trocar o rótulo: esclarece o efeito, mas continua impedindo cadastro sem consumo.
2. Criar outro wizard ou motor: separa ações, mas duplica preparação, locks e idempotência.
3. Usar a preparação existente e separar seu início: menor mudança, permite registrar o
   sucessor sem consumo e mantém o comando de execução e a passagem automática existentes.

Aplicada a terceira opção. A tela prioriza o parecer, explica que o formulário não é necessário
nessa recuperação e conserva o cadastro manual para os outros casos. Nenhum prompt de agente
precisa ser alterado: a lacuna observada está no contrato do comando e em sua cobertura de teste.

## Matriz de homologação definida antes dos testes

- Caso Mira: decisão ADJUST aprovada → novo experimento PLANNED e ciclo com aprendizado;
  mídia zero, janela nula, nenhum processo/tarefa iniciado e predecessor imutável.
- Outro produto e IDs, decisão INCONCLUSIVE aprovada: mesma proteção e nenhuma contaminação.
- Replay/concorrência: um único sucessor e nenhuma execução adicional.
- STOP, parecer obsoleto, foco diferente, recibo inválido e persistência rejeitada: bloqueio
  ou rollback, preservando história, custos e auditoria.
- Caminho antes válido: nova aprovação automática continua planejamento → construção →
  comunicação; a recuperação administrativa não cancela uma execução existente.
- Integração HTTP real e MySQL 5.7 local, com callbacks/modelo simulados e dados segregados;
  conferir quantidade de processos e tarefas antes/depois e após polling.
- Tela local em Chromium desktop, iPhone 15 Pro e Pixel 7: preparar sem transcrição, explicar
  ausência de consumo, resposta correta, replay por recarga, sem erros JS/overflow.
- Unitários Java, testes frontend afetados, typecheck/build, formatação e revisão do diff.
- Produção: somente após PR/merge/deploy verificados, criar o sucessor pela tela e confirmar
  no MCP a linhagem e a ausência de novas tarefas; não liberar IA/mídia/vídeos sem limite próprio.

## Direção comercial a transportar

A decisão já aprovada mantém público e preço de R$ 49 pelas duas organizações. A variável
principal é a informação exigida antes do primeiro resultado útil. Comunicação fiel deve
explicitar desejo reconhecido, primeira ação fácil, demonstração antes do compromisso,
diferença paga e repetição condicionada à contribuição conciliada. Vídeos devem mostrar
interface e resultado reais; uma peça de anúncio e outra de demonstração, opcionais para
seguir à CTA, com legendas/fallback e revalidação da versão. Não usar depoimento sintético.
A comparação de produto não poderá atribuir efeitos isolados a várias mudanças simultâneas;
variação de narrativa ou formato será teste separado. O briefing será materializado pela
UI/fluxo oficial. Este registro não é aprovação em nome de Atena, Plutus, Íris ou Apolo.

## Ajuste observado na inspeção visual

Ao abrir o sucessor, a tela confundia a referência herdada à proposta anterior com uma
nova decisão em execução e mostrava Atena aguardando trabalho automático. O painel de
decisão agora respeita a etapa atual; a memória permanece no histórico. Regressão de
componente e navegador impede repetir essa orientação falsa no planejamento.

## Resultados

- Backend: 4.148 casos contabilizados; 4.121 executados com sucesso e 27 dispensados pela
  configuração existente. Zero falhas/erros. A primeira execução foi encerrada por OOM
  (cgroup de 8 GiB, exit 137) durante build do frontend/serviço local simultâneos. Após
  encerrar os serviços já validados, somente as classes restantes foram executadas em
  quatro lotes, com cache de contextos limitado. Nenhum teste aprovado foi removido.
- Frontend: 47 testes afetados aprovados, typecheck e build concluídos. A inspeção visual
  encontrou o aviso indevido do parecer herdado; após corrigir, repetidos somente testes
  de tela, typecheck/build e navegador relacionados.
- MySQL 5.7 local: 14 verificações da decisão/preparação, incluindo ausência de novos
  processos/tarefas após polling, concorrência e rollback físico. Passagem automática
  planejamento → construção → comunicação aprovada para dois produtos, preservando pausa.
- Chromium desktop, iPhone 15 Pro e Pixel 7: preparação pela tela aprovada, nenhum processo
  iniciado, contexto herdado preservado, sem erro JS, overflow ou chamada externa.
- Spotless, Prettier, sintaxe Python, bash -n e ShellCheck do runner consultado passaram.
  Diff revisado; sem changelog ou prompt alterado. Topologia temporária removida com volumes.
- Publicação e criação produtiva serão comprovadas no PR vinculado a esta entrega.

Nenhum resultado sintético comprova venda ou margem.
