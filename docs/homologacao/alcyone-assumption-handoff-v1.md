# Alcyone — passagem durável de Atena para Plutus

## Evidência e causa — 05/10/2026

Atena #19, plano #34 v4, concluiu a proposta econômica sem produzir uma execução de Plutus.
O histórico confirma a mesma falha em Atena #14 de outro plano: duas propostas concluídas,
nenhuma validação financeira persistida. A regressão JPA real reproduziu fila vazia depois
do commit. `AFTER_COMMIT` reutilizava recursos da transação encerrada; as gravações posteriores
não recebiam commit. Não é ausência de autorização nem rejeição econômica.

Fonte primária: https://docs.spring.io/spring-framework/docs/6.1.6/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html,
consultada em 05/10/2026. A resposta paga de Atena permanece concluída, com custo e auditoria.

## Alternativas

- Repetir Atena: duplica consumo e mantém a passagem defeituosa.
- Enfileirar antes do commit: seria atômico, mas uma falha posterior poderia reverter a resposta
  já paga e induzir nova inferência.
- Persistir a passagem em transação própria após o commit, com retomada idempotente pela tela:
  escolhida; preserva a resposta e recupera somente a dependência não concluída.

## Matriz definida antes da correção

| Caso | Aceite |
| --- | --- |
| Passagem real | Conclusão de Atena → evento → nova transação → fila persistida e consultável |
| Outra identidade | Mesma passagem em outro plano/proposta, sem exceções por produto ou ID |
| Rollback inicial | Nenhuma validação financeira liberada antes da conclusão da proposta |
| Falha posterior | Proposta e custo preservados; retomada da mesma resposta sem Atena adicional |
| Concorrência | Uma validação e uma tarefa por proposta; lock do plano e índice único |
| Versão alterada | Não aplicar proposta antiga ao contexto novo nem apagar sua história |
| Entrada inválida | Recusar outro plano, proposta incompleta, texto substituído ou execução não concluída |
| MySQL 5.7 | Migração incremental, NULLs históricos, vínculo único e rollback sem perda de auditoria |
| Tela | Comando existente reaproveita proposta vigente; desktop e celulares mantêm contexto |
| Isolamento | Fontes e provedores simulados; nenhum dado QA conta como venda ou consumo real |
| Publicação | PR/revisão/merge/checks/deploy verificados antes de retomar pela tela |

O contexto de Atena passa a incluir a oferta, a próxima ação e a versão já persistidas.
Isso corrige a mesma omissão que Plutus apresentou; texto não cria orçamento nem autorização.
A passagem associa explicitamente a proposta à execução financeira e não exige igualdade de
commits entre componentes. Proposta da mesma versão é reutilizada; falha exige diagnóstico,
e uma mudança real de contexto permanece versionada. A retomada não recria a inferência.

## Limites

A correção garante persistência e recuperação do trabalho, não aprovação financeira.
Custos integrais ausentes continuam ausentes. Homologação privada com fixtures não vira entrega
personalizada comercial. Teto total de USD 10, mídia zero, preço hipotético e experimento #97
permanecem preservados. O resultado funcional e a publicação serão registrados após validação.

## Resultado local

- A regressão original falhou com fila vazia. A correção passou em seis cenários JPA/HTTP,
  incluindo outro plano, rollback, falha posterior, oito retomadas concorrentes e entrada inválida.
- Compatibilidade prioriza a versão explícita do snapshot; a alternativa temporal serve apenas
  para histórico sem esse campo. Cobre perda de fração de segundo, versão alterada e JSON inválido.
- MySQL 5.7.44 aprovou aplicação/reaplicação, nulos históricos, custo preservado, unicidade e
  rollback recusado para proteger auditoria. A precisão de custos anterior também permaneceu válida.
- Suíte completa do backend: 3.995 casos, zero falhas/erros e 27 dispensas explícitas. Os dez
  contratos afetados foram reexecutados depois do ajuste de exportação UTF-8, sem repetir a suíte.
- Frontend: 869 testes aprovados, TypeScript e build. O novo harness
  `infra/testing/atena-plan-question/assumption-replay.cjs` usa respostas HTTP exportadas do teste
  real de controller/JPA, com datas e UTF-8 equivalentes à produção. Desktop, iPhone e Pixel
  enviaram duas retomadas e exibiram a mesma proposta e uma única validação, sem HTTP externo.
- Matriz financeira de persistência: API/JPA/MySQL reais e Plutus simulado, concorrência, reinício,
  migração e rollback, PASS em 35,70 segundos. Não é prova comercial nem matriz completa de produto.
- Pacote de revisão comercial: 317 arquivos/61 manifestos, 15 testes do construtor aprovados.
  Spotless, Prettier, análise estática de Liquibase, `bash -n`, ShellCheck e diff conferidos.
- Containers e volumes temporários do projeto exclusivo foram removidos. Nenhum modelo foi
  chamado nas validações locais. A proposta #19 permanece concluída com custo USD 0,16287560;
  a retomada produtiva só ocorre após PR, publicação e versão comprovados.
