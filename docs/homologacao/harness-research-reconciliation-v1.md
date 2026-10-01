# Reconciliação das pesquisas e dos cards do harness — 01/10/2026

## Escopo e evidência anterior

Base auditada: `2093f02ccca564a0d5a992e0c563838ead9fe93b`.
Dez branches de pesquisa existiam no GitHub, sem PR aberto. Duas já eram ancestrais da main;
oito tinham commits não integrados. Alguns produtores gravavam partes diretamente na main e
deixavam a conclusão na branch. O guia da Biblioteca instruía gravação direta na main e não
exigia PR, validação do conjunto ou acompanhamento da integração.

A publicação `36539225237`, de 29/09, falhou por fonte repo não versionada. O reparo posterior
`a26291f4b` recuperou essa fonte; a reconciliação `36697619620` sincronizou 183 JSONs com sucesso.
Esse sucesso cobria apenas arquivos da main, sem descobrir trabalho nas branches.

## Destino de cada branch

| Branch | SHA | Decisão |
| --- | --- | --- |
| automation/gartner-2026-09-28-1045 | a99a5e75d4457987f98294fbf5c5d810892d17c5 | Recuperar o relatório ausente. |
| automation/gartner-2026-09-29-1037 | acb5d4bb9b8f557e30b8cf8c6f1ac6cfd441eaa3 | Já integrada; preservar histórico. |
| automation/gartner-2026-09-30-1123 | 5f295fde4f658256b76f1327e0b980ce0fa5fdda | Já integrada; preservar histórico. |
| automation/ia-aplicada-2026-09-29 | f7132191a32c3d7dd28d31c5ac19cec5d7ed7185 | Recuperar relatório; manter fonte e hash corrigidos da main. |
| automation/ia-aplicada-2026-09-30 | 3c4f60440ce84d778db79ee4f1e9ffef1cf9a0aa | Recuperar relatório, fonte e card. |
| automation/neuromarketing-2026-09-30-0137 | 6a0a49a8e5dd66378f3d05837e52e97cbfcf01fc | Consolidar conclusão do relatório com a versão da main. |
| radar-design-experiencia-2026-09-27 | 9203b31c41ba0972f1542cce718b5022799f6b74 | Conteúdo já presente no relatório radar da main; evitar artigo duplicado e preservar commits. |
| radar-prazer-audiovisual-2026-09-27 | 3b7f2d17db66cd24050995be8ce10da9f7226adb | Recuperar três cards; fontes já presentes. |
| radar-prazer-audiovisual-2026-09-29-complete | a4cfcd34ae1a6c533e4a7743cdcbb5c0b2029212 | Recuperar conclusão do relatório. |
| radar-prazer-audiovisual-2026-09-30 | a1defe923a3bc4133bc2a042768da6c71a5d07ed | Recuperar relatório, duas fontes e dois cards; manter design da main, posterior à branch. |

As oito pontas pendentes serão preservadas como pais do commit de consolidação, depois da
homologação local. O PR usa merge normal para conservar essa ancestralidade. Não apagar branches
nem reintroduzir versões antigas de fontes corrigidas. Repetições de cardKey com fontes diferentes
representam versões editoriais; não confundir com duplicação de um mesmo payload.

## Matriz local de aceite (definida antes dos testes)

| Área | Cenário | Critério |
| --- | --- | --- |
| Caminho feliz | Recuperação e inventário Git | Todas as dez pontas integradas ou mapeadas; nenhuma pesquisa exclusiva perdida. |
| Contrato | Validação offline de todos os cards | JSON, datas, limites, fontes repo e SHA-256 válidos; nenhuma chamada externa. |
| Falha e atomicidade | Card inválido depois de card válido | Nenhuma publicação antes de validar todo o lote. |
| Integridade | Fonte alterada sem atualizar hash | Falha antes da publicação; PR também bloqueado. |
| Git | Branch integrada, pendente, conflitante ou com código/temporários | Inventário determinístico com SHA e motivo; nada é mesclado automaticamente. |
| Recuperação | Produtor grava branch e não abre PR | Auditoria periódica acusa pendência e preserva relatório como artifact. |
| Integrações | Publicador contra API simulada | Idempotência, resposta inválida, erro HTTP e transporte exercitados sem credencial produtiva. |
| Consumidor | Argos e backend | Catálogo regenerado, testes dos consumidores e recursos empacotados válidos. |
| Observabilidade | Auditoria e publicação | Resumo distingue branch pendente, integrada, bloqueada e quantidade sincronizada. |
| Governança | Aplicações propostas nas pesquisas recuperadas | Homologação por agentes/determinismo; mercado voluntário e pagamentos reconciliados para evidência comercial. |
| Métricas e segregação | Rodada local | Fixtures isoladas, custo externo zero; não criar venda, cliente ou evidência de mercado. |
| Navegadores/dispositivos | Entrega sem alteração de UI | Não aplicável à automação Git/publicador; consulta visual da Biblioteca apenas como confirmação operacional. |

## Resultado

Homologação local concluída antes do commit:

- `bash -n`, ShellCheck, Actionlint e `git diff --check` aprovados.
- Publicador homologado com API simulada e validação offline dos 189 JSONs, sem chave produtiva.
- Nove cenários de auditoria Git aprovados; 162 testes do Argos aprovados após instalar as
  dependências locais declaradas no lockfile.
- Backend completo: 3.755 testes, zero falhas/erros, 23 cenários condicionais já ignorados pelo
  repositório; nenhuma alteração Java, SQL ou Liquibase. API da Biblioteca: nove testes aprovados.
- Pacote backend: 4.204 classes testadas idênticas às empacotadas, 716 recursos externos íntegros,
  catálogo de comportamento inicializado e catálogo de pesquisas com 485 cartões no JAR.
- Biblioteca pública conferida com Chromium: HTTP 200, catálogo anterior com 478 fontes.
- Dois marcadores `.sha256` das novas fontes foram incluídos no próprio PR, evitando commit
  posterior do gerador de hashes. O marcador antigo de 11/09 sem Markdown correspondente foi
  preservado como histórico; não é fonte consumível nem divergência de um card.

O inventário anterior confirmou oito pontas pendentes; o commit de consolidação e o merge
preservam essas pontas como ancestrais. PR e publicação serão acompanhados pelo SHA integrado.
Os cards recuperados permanecem candidatos DRAFT; integração Git não substitui revisão e ativação
editorial. Fontes Markdown empacotadas continuam disponíveis pela seleção canônica existente.
