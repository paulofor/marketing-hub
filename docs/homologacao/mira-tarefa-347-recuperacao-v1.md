# Recuperação da tarefa 347 de Mira

Data: 2026-09-07

## Objetivo e critério operacional

O gargalo era a homologação técnica do produto Mira, vinculada à atividade
`technicalHomologation`, instância BPM `207` e referência
`product:10@agent-validation-v1`. A correção deveria terminar com a atividade em
`COMPLETED`, evidência direta nos três perfis de dispositivo, recuperação e limite seguro
aprovados e nenhum efeito comercial externo.

## Causa-raiz confirmada pelo histórico

1. A tarefa `347` falhou porque o PDE produtivo ainda executava o commit `ee4a443a`, anterior ao
   endpoint `POST /api/pde/mira/private/v1/internal/agent-validations/sessions`.
2. O workflow de `main` havia construído e enviado as imagens, mas considerou o job de deploy
   `skipped`; portanto, o verde do build não comprovava a versão ativa.
3. Depois da atualização isolada do backend, a tarefa `348` chegou à rota nova, mas encontrou o
   frontend v7 antigo, sem o marcador `agent-validation-mode`.
4. A primeira tentativa do deploy oficial também revelou que o segredo dedicado de SSH não
   correspondia mais a uma chave autorizada no host. A credencial foi rotacionada sem remover as
   chaves existentes e sem expor material secreto.

O histórico confirmou `LOOP-DEPLOY-STALE-IMAGE`: a falha era divergência entre artefato construído
e artefato realmente publicado, não defeito do contrato funcional do Mira.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Reiniciar a tarefa sem atualizar o PDE | Menor esforço imediato | Repetiria o `404` | Rejeitada |
| Atualizar automaticamente todos os frontends em cada merge | Elimina divergência de commit | Pode trocar uma versão comercial em uso sem selecionar o slot | Rejeitada |
| Automatizar backend/workers e validar a v7 ativa; publicar frontend apenas pelo slot explícito | Fecha a ausência de deploy e preserva isolamento comercial | Exige smoke real de compatibilidade em todo merge | Escolhida |

## Correção e execução produtiva

- O run `34083598821` publicou backend e workers do commit
  `20c1037ce8e98160a4527d13b311ce8d60a1b37d` pelo pipeline oficial.
- O run `34084300412` publicou explicitamente somente o frontend v7 do mesmo commit e concluiu os
  testes de produção, inclusive o smoke Playwright de Mira.
- A rota interna passou a responder `403` sem credencial, comprovando ao mesmo tempo existência e
  proteção.
- A tela administrativa criou as tentativas `348` e `349`. A tentativa `349` terminou
  `COMPLETED`, com decisão `APPROVED`, e liberou o avanço do processo para
  `psiqueAdherent`.
- O banco confirmou a instância de atividade `207` em `COMPLETED`,
  `objective_achieved = true`, `evidence_quality = DIRECT` e sem motivo de bloqueio.

## Evidências funcionais finais

- Cinco cenários passaram: aderente em desktop 1440, iPhone 15 Pro e Pixel 7; recuperação no
  iPhone; e limite de segurança no Pixel.
- As evidências visuais `81` a `85` foram persistidas e seus hashes SHA-256 conferiram com os
  arquivos servidos pelo backend.
- O frontend administrativo mostrou `5 de 10 atividades concluídas` e
  `Homologar tecnicamente a versão real` com objetivo atingido, sem erros de console em desktop ou
  iPhone.
- Os logs do PDE registraram os payloads brutos segregados para `ADHERENT`, `RECOVERY` e `SAFETY`.
- Todos os checks de privacidade, acessibilidade básica, responsividade, recuperação, bloqueio
  seguro e segregação de tráfego ficaram verdadeiros.
- Efeitos externos permaneceram nulos: nenhuma compra, publicação, campanha ou gasto de mídia.

## Prevenção no repositório

O workflow local passa a publicar backend/workers em todo `push` elegível de `main`, sem trocar
frontends por efeito colateral. O pós-deploy valida a rota interna protegida, a saúde da v7 e o
smoke segregado de Mira contra o frontend realmente ativo. O teste de contrato bloqueia a remoção
dessas garantias, e a recorrência foi registrada em `docs/registros/loops.md`.

## Homologação local final

Depois do último ajuste foram executadas duas rodadas completas e consecutivas, sem falhas. Cada
rodada cobriu:

- 19 testes Java do backend PDE;
- 29 testes Java do Customer Agent Worker;
- 2 contratos Node do observador de navegador;
- build TypeScript/Vite do frontend;
- 3 perfis Playwright em série na sandbox;
- rota local retornando `403` sem token;
- harness real local com decisão `APPROVED`, 5 cenários, 3 dispositivos e 5 artefatos;
- contrato de isolamento, deploy, proxy e smoke direcionado do GitHub Action.

Nenhum commit, push ou Pull Request foi criado nesta recuperação.
