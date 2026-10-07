# Mira — recuperação da publicação Maven — 07/10/2026

O backend do run 37648024303 passou pela conferência do JAR e do catálogo. A publicação
Maven falhou às 16:00:56 UTC com `HTTP Status: 500`. O loop de recuperação existente
reconhecia somente `status code: 5xx` e classificou o erro do servidor como permanente.
O run anterior 37637394844 publicou a mesma biblioteca com sucesso às 14:36:17 UTC;
não há evidência de credencial ausente, compilação ou pacote inválido neste caso.

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Reexecutar o workflow sem ajuste | Pode recuperar HTTP 500 pontual | Preserva a mesma classificação incorreta para próximas falhas | Não resolve a recorrência |
| Corrigir o reconhecimento no loop existente | Recupera os formatos Maven atual e anterior, com o mesmo limite | Ajuste pequeno e teste do shell operacional | Adotada |
| Substituir registry e distribuição aos workers | Remove esta dependência | Migração ampla sem causa comprovada no contrato da biblioteca | Fora de escopo |

## Matriz local definida antes da implementação

Executar o bloco real do workflow com Maven e espera locais, sem publicar um pacote:
caso original com ANSI e HTTP Status: 500 seguido de sucesso; outro artefato/URL com
502/504; formato anterior 503; persistência limitada a três tentativas e esperas de
20/40 segundos; 401/403 e erros funcionais sem retry; falhas de transporte já válidas;
sucesso direto com uma publicação; mesmas coordenadas/argumentos em cada tentativa;
remoção dos logs temporários em todas as saídas. Validar também transporte dos artefatos,
rsync transacional, contratos de CI, pacote de evidências e bash -n/ShellCheck.

A mudança preserva o loop existente e não repete inferência, altera experiência, cria
vídeo, ativa mídia ou concede orçamento. Mira continua pausada na execução 54;
mensagem 634 preservada e custo estimado cumulativo de US$ 8,5320024 sob teto US$ 10.

## Resultados locais

O contrato falhou em quatro verificações antes da correção, incluindo a mensagem ANSI
original e falha persistente. Após o ajuste, passaram cinco testes com 14 cenários de
publicação. Os contratos de transporte (14 testes) e de CI (13 testes), rsync local
real, saúde/revisão do frontend e pacote de evidências (414 arquivos/77 manifestos)
também passaram. O shell versionado e o bloco de publicação passaram bash -n e
ShellCheck. Não foi necessário criar outro helper de publicação ou mudar atestações.
