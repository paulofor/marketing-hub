# Mira — retirar dependência obsoleta do deploy — 07/10/2026

O run 37654762386 reconheceu e repetiu corretamente o HTTP 500, mas GitHub Packages
recusou as três publicações (16:51:32, 16:53:34 e 16:55:34 UTC). Não há correção local
para a resposta do servidor. A investigação do contrato que tornava essa chamada
obrigatória confirmou que o deploy do APP não possui consumidor dessa biblioteca:
os oito agentes atuais, vídeo e process worker não dependem de ads-service via Maven.

A busca dos POMs versionados encontrou um único consumidor, ai-worker. Seu workflow
ai-worker.yml já executa `mvn install` no backend antes de testar e empacotar o worker;
o Dockerfile recebe somente o JAR aprovado. O publish remoto do APP é um acoplamento
obsoleto, não uma condição de capacidade desses executores. Artefatos e versões
históricas do registry são preservados.

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Repetir publicação ou aguardar o registry | Pode recuperar disponibilidade externa | As três tentativas falharam; mantém uma dependência sem consumidor | Não resolve o acoplamento |
| Distribuir JAR por mecanismo novo aos oito agentes | Evitaria o registry | Nenhum deles usa essa dependência; infraestrutura desnecessária | Descartada |
| Retirar o publish do APP e proteger o contrato local já existente | Permite deploy sem essa chamada externa; mantém revisão e provas reais | Ajuste pequeno, condicionado à verificação dos consumidores | Adotada |

## Matriz local definida antes da alteração

- Caso original: GitHub Packages indisponível não deve ser chamado pelo backend-image;
  conferência do JAR, classes testadas, duas imagens e saúde permanecem obrigatórias.
- Consumidor real: instalar o backend e compilar/testar AI Worker offline, com cache
  isolado sem a biblioteca previamente instalada, sem acesso a registry/IA externos.
- Outro consumidor/identidade: fixture com dependência e instalação local válida passa;
  novo consumidor dependente do registry sem esse caminho deve falhar o contrato.
- Caminho anterior válido: workers sem dependência permanecem independentes; Dockerfile
  do consumidor recebe o JAR testado, sem recompilar ou resolver snapshot em produção.
- Falhas: publicação remota reintroduzida no APP, instalação depois dos testes ou omitida
  no consumidor, jar ausente/divergente e pacote parcial continuam sendo recusados.
- Integração: contratos de deploy/rsync/saúde, transporte, CI e evidências; bash -n e
  ShellCheck. Sem alteração Java, Dockerfile, orçamento ou artefato de Mira.

Mira permanece pausada na execução 54/ciclo 9/experimento 102, com mensagem 634
preservada e custo estimado US$8,5320024 sob teto TOTAL US$10. Não há mídia ou vídeo pago.

## Resultado local e prevenção reutilizável

- Antes da alteração, o novo teste falhou exatamente pela presença de `maven-deploy-plugin`
  no job do APP. Depois, cinco testes passaram: caso original, consumidor real, outra
  identidade válida, instalação ausente/tardia recusada e executor sem a dependência.
- Cache Maven isolado no workspace, excluindo previamente `com/marketinghub/ads-service`:
  backend instalado offline; dependências públicas ausentes obtidas somente por mirror
  Maven Central; em seguida AI Worker compilado/empacotado offline e 41 testes de
  arquitetura passaram, sem falhas ou erros. Nenhum acesso ao GitHub Packages ou IA.
- A imagem do consumidor recebe `target/app.jar`, de 297.096.157 bytes, SHA-256
  `0d570fc57a6bcdf7b70d84ad38b7529a53ad4b23144de9584cb50fbb148f6bf9`.
  Isso comprova o consumidor local; não publica uma imagem de produção manual.
- Contratos relacionados: 14 testes de download, 13 de CI, 16 do pacote de evidências
  e nove de recursos empacotados passaram. Pacote: 414 arquivos/77 manifestos. JAR
  backend: 4.257 classes iguais às testadas, 761 recursos íntegros e catálogo de 523
  cartões inicializado. Sintaxe YAML, `bash -n` e ShellCheck passaram nos scripts
  relevantes e quatro blocos reais do publicador.

A alteração remove somente a publicação Maven remota do APP. Mantém testes completos
ou pacote aprovado do mesmo conteúdo, validação Liquibase, conferência do JAR, imagens
versionadas, transporte íntegro, deploy transacional, saúde e revisão pelo PR. Não usa
`continue-on-error` nem reduz checks obrigatórios. O contrato genérico verifica todos os
consumidores identificados e permite outro consumidor com instalação local anterior
à sua validação. O teste substitui a regressão do retry da operação agora retirada;
a ocorrência e a correção anteriores do HTTP 500 permanecem no histórico.

Limites: não prova disponibilidade do GitHub Packages, venda, contribuição, publicação
comercial ou vídeo produzido. Uma nova dependência remota legítima requer fluxo próprio
no consumidor e evidência funcional, em vez de restaurar uma obrigação global no APP.
A publicação e a retomada de Mira ainda dependem da conferência do novo SHA e dos jobs
aplicáveis, sem repetir a mensagem 634 nem conceder novo teto de IA.
