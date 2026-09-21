# Prompt AIHUB com autonomia de entrega

Data: 21/09/2026. Escopo: texto compartilhado do botão **Prompt para AIHUB**, sua prévia
e cópia manual. Objetivo: orientar a conclusão de todas as atividades do processo corrente,
com PR/merge/deploy pelo modelo, contexto preciso e melhoria fundamentada dos agentes.

## Diagnóstico e decisão

O template `frontend/src/pages/product/prompts/process-aihub-help.v1.md` exigia PR executado
pelo usuário e proibia criação sem novo pedido explícito. O teste de contrato e o cânone
reforçavam a mesma restrição. A frase de compatibilidade de build ainda negava autorização
de deploy. A correção remove essas contradições na fonte compartilhada.

| Alternativa                                     | Benefício                                    | Risco e esforço                                                   | Decisão        |
| ----------------------------------------------- | -------------------------------------------- | ----------------------------------------------------------------- | -------------- |
| Apenas acrescentar autorização                  | Edição pequena                               | Mantém instruções contraditórias; baixo esforço                   | Rejeitada      |
| Revisar template, cânone e contratos existentes | Uma orientação para cópia, prévia e fallback | Precisa proteger contexto e limites; esforço moderado             | Escolhida      |
| Criar gerador e endpoint novos                  | Permite futura personalização                | Duplica fonte e amplia persistência sem necessidade; alto esforço | Fora do escopo |

Os dados já vêm de `BusinessProcessActivityExecutionController`, `ProcessRunController`
e `LearningCycleController`, nos contextos canônicos de execução, automação e ciclo.
`processContextText` preserva nomes/IDs, versões, objetivos, tarefas, processos pais e filhos,
custos e ausências. A alteração não introduz endpoint, mutação de negócio ou inferência de status.
**Copiar contexto do processo** continua entregando apenas os dados oficiais.

O PR histórico #5281 já estava integrado (`0412d002b61841a09ac5b756416b5a950c6336e7`).
O PR aberto #5283 tratava de captura visual de Psique e não desta solicitação. A alteração
dos prompts parte da `main` em branch própria, preservando o vínculo com sua entrega.
O histórico Git local era raso e foi completado antes de comparar commits.

As ocorrências de encerramento relatadas na conversa motivam instruções de idempotência
e preservação da comprovação. Esta mudança não corrige o reconciliador do AI Hub nem
reclassifica as solicitações históricas 3015, 3016 e 3017. Sua causa não foi reinvestigada aqui.

Pesquisa: radar `pesquisas/agentes-inteligentes/2026-09-18-agentes-inteligentes.md`, seções
2–4; fontes RAFT, SkillAA e SoL-Pi conferidas e registradas no cânone. Aplicação: recuperar
experiência pertinente, comparar falhas/sucessos, avaliar candidata pequena, registrar validade
e preservar rollback. Não foram reproduzidos benchmarks nem medido ganho dos agentes.

## Matriz definida antes dos testes

| Critério                | Verificação                                                                                                 |
| ----------------------- | ----------------------------------------------------------------------------------------------------------- |
| Autorização             | PR manual pelo modelo, sem novo pedido; revisão real, HEAD correto, merge e acompanhamento dos deploys      |
| Objetivo do processo    | Atividade/tarefa, entradas, dependências, aceite, subprocesso/retorno e avanço pelo backend                 |
| Falhas de entrega       | Correção local, PR adicional após merge, rerun só com evidência, ausência/pendência não é sucesso           |
| Encerramento            | Histórico raso, 422 sem commits, retomada sem duplicação e resultado comprovado preservado                  |
| Aprendizado             | Histórico pertinente, candidata avaliada, evidências, métricas, validade e rollback                         |
| Cópia e integração      | Build real, contexto uma vez, prévia e fallback idênticos, acentos e quebras de linha                       |
| Identidade e segregação | IDs sintéticos; troca de produto/ciclo não carrega contexto antigo; nenhum segredo ou payload bruto         |
| Falhas de UI            | Carregando, clipboard negado, retomada de cópia, contexto ausente e erro HTTP                               |
| Dispositivos            | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro                                  |
| Observabilidade         | Confirmação de cópia, erro acionável, screenshots/resultados; zero mutações ou métricas comerciais de teste |
| Regressão               | Testes dos componentes/páginas e hooks afetados, TypeScript, build, formatação e diff                       |

Executar uma rodada relevante, corrigindo e repetindo apenas as verificações necessárias.
As validações confirmam o texto entregue e a integração da UI; não demonstram obediência
garantida de um modelo, aprendizagem automática nem mudança de estado no orquestrador.

## Resultado local

- **95 testes aprovados em 7 arquivos**: contrato do prompt, cópia de contexto, painéis de
  execução/automação, página de atividades, histórico da cadeia e hook de consulta.
- **TypeScript e build aprovados**, além de `node --check`, Prettier dos arquivos de
  frontend/roteiro e deste registro, e `git diff --check`.
- **6 configurações de navegador aprovadas**, com 16 cenários cada: desktop, iPhone 15 Pro
  e Pixel 7, em HTTP e contexto seguro. Cópia/colagem real, prévia e fallback íntegros;
  contexto isolado por produto/ciclo, sem erro JavaScript, mutação ou conexão externa.
- A inspeção visual confirmou os controles e o texto completo no card e no celular.
- A consulta pública anterior, somente leitura, respondeu HTTP 200 e confirmou a restrição
  antiga na prévia do prompt. Nenhum processo produtivo foi executado nesta homologação.

Evidências locais em `artifacts/prompt-aihub-autonomia/`: `vitest.log`, `typecheck.log`,
`build.log`, `browser/results.json`, capturas e textos efetivamente colados em `browser/`,
além de `public-before.txt` e `public-before.png`. Build mantém apenas o aviso já existente
de tamanho do bundle. A rodada não revelou defeito funcional; não foi necessário repetir
a matriz nem alterar contratos do backend.

Revisão final: autorização explícita no início; restrições contraditórias removidas;
objetivos e identidade preservados; continuidade, gates, aprendizagem verificável e
encerramento por evidências cobertos. Publicação será comprovada pelo PR, SHA e runs
específicos desta entrega e pela conferência da tela publicada.
