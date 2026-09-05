# Matriz de homologação — limpeza de imagens Docker temporárias v1

## Gargalo e decisão

A engine isolada acumulou 216 imagens e 33,8 GB durante homologações extensas. A maioria das imagens
locais antigas não possuía marcador de ciclo de vida, impedindo distinguir descarte seguro de cache ou
artefato ainda necessário.

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Executar `docker image prune -af` após cada rodada | Recuperação ampla e imediata | Corrida com outros builds e perda de bases/caches | Rejeitada |
| Agendar workflow em runner efêmero do GitHub | Agendamento simples | Não alcança a engine da sandbox onde ocorre o acúmulo | Rejeitada |
| Marcar propriedade e sessão, limpar periodicamente com locks | Remove apenas artefatos descartáveis e protege concorrência | Exige usar helper e wrapper nos novos builds | Escolhida |

## Casos obrigatórios

| Dimensão | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Sessão encerrada com imagem rotulada e sem container | Remove somente as tags temporárias da sessão |
| Validação | Sessão, nome, tag, idade ou modo inválido | Falha antes de chamar Docker |
| Concorrência | Duas coletas ou sessão ainda ativa | Segunda coleta sai sem mutação; sessão ativa é preservada |
| Falha | Daemon indisponível ou Docker recusa remoção | Falha observável ou preservação segura, nunca remoção forçada |
| Integração | Wrapper inicia watcher, executa build e encerra | Lock ativo durante o comando e coleta final após liberação |
| Segurança | Imagem possui tag temporária e tag externa | Remove apenas a tag do namespace temporário |
| Imagem em uso | Container parado ou ativo referencia a imagem | Preserva a imagem até o container ser removido |
| Observabilidade | Toda passagem | Publica contadores de decisão sem expor dados sensíveis |
| Segregação | Fixture `scratch` e sessão exclusiva | Nenhum container, imagem produtiva, venda, campanha ou dado real é alterado |
| Navegadores/dispositivos | Utilitário sem interface | Não se aplica; nenhuma superfície web é modificada |

Uma rodada local completa sem defeito conclui esta homologação. Se um defeito for encontrado e corrigido,
a contagem reinicia e são exigidas duas rodadas completas e consecutivas sem falha após a última correção.

## Resultado local — 2026-09-05

Após ampliar a cobertura para daemon indisponível e encerramento com falha, duas rodadas completas e
consecutivas passaram. Cada rodada executou sintaxe Bash, ShellCheck, Actionlint, todos os contratos
operacionais do workflow e o teste na engine real. Foram comprovados encerramento normal, coleta
periódica, preservação do status de falha, sessão ativa, container existente, dry-run, tag externa e
lock concorrente. Ao final, não restou imagem com o rótulo temporário nem container criado pelos testes.

## Recorrência encontrada na leitura de Mira — 2026-09-05

O caminho sem `AIHUB_HOMOLOGATION_SESSION` explícita gerava data com `T` e `Z` maiúsculos. A mesma
sessão compunha o nome do repositório Docker, que recusava o build com `repository name must be
lowercase`. Os testes anteriores usavam sessões explícitas e não cobriam esse caminho padrão.

O wrapper agora gera identificadores em minúsculas. Wrapper e helper recusam nomes incompatíveis
antes do build. O contrato cobre a sessão automática e entradas inválidas, incluindo maiúsculas e
separadores repetidos. A homologação de Mira exercita o wrapper sem sessão explícita, constrói as
duas imagens e verifica sua limpeza ao encerrar, sem remover bases ou imagens alheias.
