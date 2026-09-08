# Retomada do deploy da aplicação — v1

## Contrato operacional

O workflow `Build & Deploy containers` publica backend e frontend administrativos a partir
de código integrado em `main`. Deve aceitar `push` e `workflow_dispatch`; a detecção de
módulos só executa para `refs/heads/main`. Todos os jobs de publicação dependem dessa detecção,
inclusive os que usam `always()`. Selecionar outra branch ou uma tag não libera o deploy.

Reativar um workflow não recupera o evento perdido durante a pausa. Após a reativação,
consultar a revisão atual e procurar uma execução correspondente; se ela não existir,
disparar manualmente `main`. O checkout e as imagens continuam vinculados ao SHA congelado
pelo evento, com testes, fila, rollback, retenção e confirmação das revisões publicadas.
A detecção compara o histórico com a revisão efetivamente publicada, recuperando módulos
pendentes mesmo quando o evento manual não contém `github.event.before`.

Os agentes que aguardam a aplicação devem reconhecer tanto `push` quanto `workflow_dispatch`,
sempre na branch `main` e no mesmo SHA. Uma execução de outro commit, branch, tag ou PR não
libera os agentes. A execução correspondente mais recente precisa terminar com `success`;
uma retomada em andamento ou com falha não pode reutilizar o sucesso de uma execução anterior.

Uma nova execução manual não substitui os testes locais nem autoriza código fora do PR.
Não reexecutar um run histórico esperando que ele publique o HEAD atual: o GitHub preserva
o SHA e a referência do evento original.

## Recuperação de 08/09/2026

- PR #5140 mergeado em `3c7e537f04c82733e44690d9071aa83f63ff1385`.
- Workflow `243638963` observado como `disabled_manually` e reativado com autorização do usuário.
- Tentativa real de `workflow_dispatch` em `main` recusada com HTTP 422:
  `Workflow does not have 'workflow_dispatch' trigger`.
- Último run disponível, `34186720906`, pertence a `ad2929c6`, anterior ao merge solicitado.
- O consumidor `wait-for-app-deployment.mjs` também filtrava somente `push`; adicionar apenas
  o gatilho ao publicador manteria a espera dos agentes bloqueada.

Alternativas avaliadas:

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Aguardar outro merge funcional | Usa o gatilho já publicado | Prazo indefinido; não resolve a recuperação manual | Não atende à retomada solicitada |
| Commit vazio para provocar push | Aciona o workflow existente | Altera o histórico só para operar e mantém a lacuna de recuperação | Não adotada |
| Gatilho manual em main e coordenação dos agentes | Recuperação explícita, repetível e auditável | Correção pequena, exige PR antes de publicar | Adotada |

## Matriz local da correção

Definida antes da validação final. As dependências GitHub e VPS dos testes são locais/simuladas;
não há campanhas, vídeos pagos, envio de mensagens ou métricas comerciais de teste em produção.

| Controle | Evidência exigida |
| --- | --- |
| Coordenação dos agentes | Push e retomada manual do mesmo SHA; espera, sucesso, falha e timeout; rejeição de branch/tag/PR/commit diferente |
| Fronteira do workflow | Gatilho manual presente; detecção restrita a main; todos os publicadores dependem dela |
| Recuperação do diff | Backend, frontend e vídeo pendentes identificados pelas respectivas revisões publicadas |
| Segurança operacional | Contratos de deploy transacional, saúde, fila e resiliência do host preservados |
| Sintaxe e integração | Actionlint com suporte a `queue: max`, Node e revisão do diff |

O teste de regressão reproduziu quatro falhas antes da correção: seleção do run manual,
consulta restrita a push, falha manual ignorada e ausência de gatilho/limite a main.
Depois da última correção, executar duas rodadas consecutivas de toda a matriz sem falhas.

Resultado desta recuperação: duas rodadas completas e consecutivas com **8/8 controles**
aprovados em cada uma, incluindo **16 testes Node** por rodada. O teste de integração executa
o bloco Bash real de detecção do workflow contra histórico Git isolado e SSH simulado,
com `EVENT_BEFORE` vazio e um commit posterior somente de documentação. Backend/frontend
pendentes foram reconhecidos; o módulo de vídeo sem alterações não foi selecionado.
As assinaturas SHA-256 dos três arquivos de implementação/teste permaneceram iguais entre
as rodadas. Actionlint validou todos os workflows com a revisão fixada pelo repositório.

Na conferência externa final, o workflow está `active`, mas o SHA `3c7e537f` continua com
zero execuções desse publicador. O frontend reporta `489d57e1` em `/healthz`; a página de Ciclos
não aparece em Chromium desktop/iPhone emulado, e o endpoint
`/api/business-process-chains/learning-cycles/v1/products/3` retorna HTTP 404. A navegação
de diagnóstico bloqueou requisições de escrita. **A correção permanece local, sem PR ou
publicação; esses resultados não são homologação da funcionalidade em produção.**

## Confirmação após o PR

1. Conferir que o workflow está `active`; o merge da correção gera um novo `push`.
2. Acompanhar a execução desse SHA; usar o gatilho manual em `main` somente se for necessário
   recuperar uma execução ausente. Não enfileirar publicação duplicada de um run em andamento.
3. Confirmar sucesso dos jobs aplicáveis, saúde e commit do backend/frontend publicados.
4. Validar **Cadeias de Valor → Ciclos de aprendizado e vendas** pela interface, incluindo
   as atividades de vídeo, em desktop e celular. Não criar artefatos comerciais para um smoke.
5. Conferir os agentes dependentes no SHA correspondente. Runs antigos com timeout preservam
   o histórico; não comprovam o deploy atual.

Referências: [API de workflows do GitHub](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event)
e [semântica de reexecução](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs).
