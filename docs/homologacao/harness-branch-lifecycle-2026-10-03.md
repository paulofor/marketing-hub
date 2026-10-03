# Integração e limpeza das branches de pesquisa — 03/10/2026

## Evidência e causa confirmadas

Base: `b8efd3b067442dea8af6dc5bb8090d2807345f50`.
O GitHub tinha 14 branches: main, uma branch compartilhada e 12 de pesquisa, sem PR aberto.
A exclusão após merge estava habilitada. Dois heads de Gartner já eram ancestrais da main;
dez branches tinham complementos ainda não incorporados. O workflow `37073005462`
detectou nove pendências em 11 branches, mas apenas reportava a falha; `radar/` não entrava
no filtro. Não se tratava de perda da configuração de exclusão após merge: as branches
de origem não chegavam a um PR e a consolidação anterior preservava referências.

A rodada local também encontrou um card de vídeo inserido diretamente na main pelo commit
`f4b38f02ec1f9bee564e48149ed11bdca8f51073`: ele apontava para Markdown nunca versionado e
recomendava entrevistas de descoberta. Isso bloqueava o catálogo completo, mesmo quando
os novos cards estavam corretos. Fonte recuperada por nova revisão das páginas oficiais
de Tavus/Synthesia, com hash novo e aplicação alinhada a agentes e mercado voluntário;
o original continua no histórico. Não se fabricou conteúdo para satisfazer o hash antigo.

Alternativas: apagar todas perderia complementos; limpar manualmente repetiria a falha;
consolidar o conteúdo revisto e tornar a limpeza restrita executável preserva o investimento.
Escolhida a consolidação com histórico conservado, seguida de limpeza condicionada ao SHA.

## Matriz de aceite definida antes dos testes

| Área | Cenário | Aceite |
| --- | --- | --- |
| Inventário | Três prefixos, histórico completo, igualdade e divergência | Todas as pesquisas reconhecidas; histórico raso bloqueado; diferença não vira integração. |
| Caminho feliz | Branch ancestral da main, sem proteção, PR ou run em curso | Referência removida; commit continua alcançável na main. |
| Validações | Branch pendente, compartilhada, protegida, PR aberto, run em qualquer página | Nenhuma remoção; motivo persistido. |
| Concorrência | Push depois da auditoria ou entre consulta e exclusão; main alterada | Lease ou conferência preserva a nova ponta; sem forçar a remoção. |
| Falhas e retomada | API indisponível ou branch já removida | Falha acionável sem exclusão; nova execução idempotente. |
| Integração local | Git bare real com APIs simuladas | Exercitar remoção real e lease sem usar GitHub produtivo como teste. |
| Publicação | Catálogo completo, fonte/hash, template vazio e duplicação | Cards válidos antes da primeira chamada; template fora do catálogo; fonte e versão canônicas preservadas. |
| Segurança e observabilidade | Job de manutenção, permissões e artifact | Executar apenas código da main, nunca PR; serializar manutenção; relatório com SHA, decisão e motivo. |
| Consumidores | Catálogo Markdown de Argos/backend e API de cards | Recursos e catálogo válidos; cards novos permanecem DRAFT. |
| Métricas e segregação | Homologação | Fixtures locais; nenhuma venda, campanha ou chamada paga de IA. |
| Navegadores/dispositivos | Biblioteca administrativa | Consulta desktop/mobile da publicação; automação Git não altera UI. |

## Decisão por branch

| Branch | SHA auditado | Decisão |
| --- | --- | --- |
| automation/design-experiencia-2026-10-01-report | 9c8241a06f5464576466c786cb073079943a081a | Recuperar relatório. |
| automation/design-experiencia-2026-10-02 | 84ea231d2c0b9d4286116ef2fb0adc283d904cf7 | Fonte de personalização recuperada; JSON `{}` fica no histórico; feedback duplicado mantém card/fonte canônicos integrados no #5474. |
| automation/design-experiencia-2026-10-02-final | f9038193c0bbbd105015e7dca871139a978fc20b | Recuperar relatório; card/fonte de fricção já idênticos na main. |
| automation/gartner-2026-10-01-1106 | 38e6d4657b37ae9005d52cddbfd408d1ec7198c5 | Já integrada. |
| automation/gartner-2026-10-02-1041 | 8be65e336deee885c871f8f2991942ada208724e | Já integrada. |
| automation/ia-aplicada-2026-10-01-0441 | 35b35613f7a67609511d64ace323e119260a907c | Recuperar dois cards e suas fontes. |
| automation/ia-aplicada-2026-10-02-0432 | 39478bafde12d6e70042d2c119472966302f48d1 | Recuperar relatório, card e fonte. |
| automation/neuromarketing-2026-10-01-0126 | d7b21b98b5d7dc65cc94d470e1191d744bc22a36 | Recuperar relatório, dois cards e fontes. |
| automation/neuromarketing-2026-10-02-0139 | ea6e235aca8bd1d1a3397b91cddbdfae815d529e | Recuperar relatório, card e fonte. |
| radar/agentes-inteligentes-2026-10-01 | 0e56bef9dd8271362a7f2608f90118fc2ab90b4c | Recuperar relatório; passa a ser reconhecida pelo auditor. |
| radar-prazer-audiovisual-2026-10-01 | 368a76bfd00fafc37dfa7feab2d7eb65f66b0856 | Recuperar card e atualizar apenas seu estado documental no relatório; fonte canônica preservada. |
| radar-prazer-audiovisual-2026-10-02 | e5dcf867b2047aeb76259ae0826c90e9beb77f34 | Recuperar card e fonte. |

Seis relatórios, oito cards válidos e oito fontes ausentes recuperados; um relatório existente
conciliado. O template vazio não contém conhecimento a fabricar; a fonte correspondente foi
preservada e o relatório original já declara que não foi criado card para essa evidência.
Duplicações não criam outra versão editorial. As pontas originais serão pais da consolidação
após homologação local, e o PR usará merge normal. A branch compartilhada não é alvo da limpeza.

Os achados externos foram recuperados como pesquisa, sem nova comprovação científica nem
aprovação editorial. Quando aplicados a agentes, a fonte primária precisa ser conferida;
integração Git e JSON válido não provam causalidade, qualidade de produto ou vendas.

## Validação local concluída

- Auditor: 23 testes aprovados, com Git bare real e APIs simuladas; nenhum remoto produtivo
  foi utilizado como mecanismo de teste. Lease, paginação, falhas e retomada comprovados.
- Publicador: fixtures aprovadas; catálogo completo com 200 cards válidos. `bash -n` e
  `shellcheck` aprovados nos scripts de publicação; Actionlint aprovado no workflow alterado.
- Argos: 162 testes aprovados; catálogo reconstruído com as fontes finais e regressão
  específica da biblioteca aprovada. API da Biblioteca: nove testes aprovados.
- Backend: 3.822 testes, sem falhas ou erros, 24 previamente ignorados; 21 testes dos
  consumidores da biblioteca repetidos após restaurar a fonte ausente e pacote aprovado.
- Pacote: nove testes do verificador aprovados; 4.204 classes idênticas às testadas,
  738 recursos externos íntegros e catálogo inicializado no JAR com 506 referências.
- Diff revisto sem arquivos temporários. As fontes recuperadas conservam o conteúdo das
  pontas auditadas; apenas espaços finais do relatório de agentes foram normalizados.

A consulta produtiva anterior respondeu com 491 referências. A identidade da main, o PR,
os workflows, a remoção efetiva das referências e a biblioteca publicada serão conferidos
depois do merge; essas evidências de entrega pertencem ao registro do PR, sem antecipar sucesso.
