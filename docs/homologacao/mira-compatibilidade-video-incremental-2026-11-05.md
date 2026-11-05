# Mira — compatibilidade com o registro financeiro incremental

O serviço compartilhado `LearningCycleService` recebeu duas operações de leitura
 e registro de autorização de um único vídeo novo. Nenhum método de construção,
homologação, estratégia, autorização comercial ou transição de Mira mudou.
A v74 fixava o hash anterior desse serviço; o empacotador recusou corretamente a
divergência no PR. O manifesto anterior permanece imutável.

Manter o hash antigo deixa a prova desatualizada; reescrever a v74 apaga a
história. Foi escolhida uma sucessora de compatibilidade, v75, após revalidar os
contratos compartilhados. Ela preserva experiência `mira-private-candidate-v3`,
publicação vinculada e `automaticDeployOnMerge=false`. Não renova parecer de
Psique/Têmis, autorização financeira, publicação, mídia ou conclusão comercial.

Validação local: 47 testes de serviço/controller privados de Mira, registro de
protótipo, continuação e autorização de ciclos passaram. Os 39 testes do
construtor de evidências e seletor de publicação passaram, preservando recusa de
hash divergente, história e isolamento. Esses testes de compatibilidade não
medem vendas nem homologam novamente o produto publicado.

A atestação atualiza somente o hash de `LearningCycleService`, conserva a v74
como referência histórica e registra esta prova de compatibilidade. O seletor
de publicação deve retornar `frontend-version=none` para a inclusão isolada da
v75; a mudança administrativa não publica outro frontend de produto.
