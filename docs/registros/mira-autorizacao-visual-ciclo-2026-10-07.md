# Mira: autorização visual congelada do ciclo privado

## Estado, histórico e causa comprovada

Consulta MCP de 07/10/2026, 23:06 UTC: Mira 10, cadeia 26, ciclo 9,
experimento 102; nenhuma tarefa paga ativa. Estimativa acumulada conhecida:
US$9,0629936 de US$10 autorizados para preparação/revisão. Mídia e geração paga
de vídeo permanecem desautorizadas. Isso não é custo comercial conciliado.

A publicação do PR 5536 concluiu o contrato entre especificação e renderização.
O replay existente da tarefa 636 reutilizou a saída paga, sem inferência, mas
o upload do PNG recebeu HTTP 409: a autorização visual congelada não correspondia
à prova aprovada. O erro foi persistido com `READY_FOR_RENDER` e o mesmo custo
estimado US$0,5309912; a recuperação delegada e a resposta original foram preservadas.

Por que aconteceu: `AgentTaskVisualEvidenceService` reconhecia somente o formato
de transferência produto → experimento (`visualProofAuthorization` e
`approvedVisualArtifacts`). O contexto de ciclo privado validado pelos providers
e pelo revisor já entregava `validationGate` e `approvedUpstreamArtifacts`.
O contrato arquivado da tarefa 636 contém gate 545, produto/versão/URL corretos,
homologação 627 aprovada e o hash integral do resultado citado no gate; não contém
o formato legado. O teste legado de upload continua válido. O histórico contradiz
ausência de prova ou falha do renderer: a recusa acontecia antes do storage.

Lacuna concreta do harness: o teste de materialização anterior substituía o upload
por uma resposta HTTP simulada, sem exercitar o validador real de autoridade visual.
Agora a prova local passa pelo serviço real de gravação e pelo serviço real de
derivação, com apenas S3 e repositórios como doubles; não usa endpoint produtivo
para descobrir o próximo erro nem gera imagem de produção manualmente.

## Alternativas comparadas

| Alternativa | Benefício | Risco e esforço | Aderência |
|---|---|---|---|
| Injetar o formato legado em todo contexto de ciclo | Reutiliza o leitor anterior | Duplica autoridade e não corrige snapshots existentes; esforço moderado | Parcial |
| Usar a URL atual do cadastro ou dispensar a autorização | Desbloqueio curto | Pode aceitar outra versão/produto ou prova não aprovada | Baixa |
| Reconhecer o gate canônico congelado e reaplicar a saída existente | Corrige a fonte compartilhada e preserva contrato/custo | Exige hash, identidade e replay limitado; esforço proporcional | Escolhida |

## Correção e limites

`FrozenCreativeVisualAuthorization` resolve exclusivamente a autoridade congelada
de produto ou de ciclo. O ciclo exige prontidão, identidades, destino aprovado,
declaração de preparação interna, flags de cobrança/publicação/mídia negativas,
homologação técnica aprovada e SHA256 exato de seu resultado registrado no gate.
O upload reutiliza o destino aprovado sem consultar a URL comercial mutável.

A recuperação usa a fila e o mecanismo existentes, sem processo paralelo.
O filtro SQL inclui apenas o erro específico de autoridade de uma saída pronta.
O service ainda exige a recuperação delegada anterior com zero inferência/custo,
mesmo gate/versão/URL e estratégia atual, e registra
`IRIS_FROZEN_CYCLE_VISUAL_AUTHORITY_RECOVERY_V1` antes do upload. É uma tentativa
por tarefa; repetição, troca ou revogação do gate e rejeições funcionais ficam fora.
Comparação de identidade numérica não depende de Jackson representar o ID como
`Integer` ou `Long`.

Nenhuma exceção é condicionada ao nome de Mira ou ao ID 636. Autorização de IA
não aprova uso da peça, publicação comercial, pagamento, mídia ou produção de vídeo.
Revisões independentes posteriores e decisão humana permanecem obrigatórias.
Experiência privada v3 e histórico dos ciclos permanecem preservados.

## Matriz de homologação definida antes dos testes

| Critério | Prova local |
|---|---|
| Caminho feliz ponta a ponta | Especificação arquivada → PNG do renderer versionado → upload real → metadados → derivação aprovada |
| Identidades diferentes | Outro produto/experimento/ciclo com prova e gate consistentes |
| Caminho antes válido | Autorização de transferência de produto e callback anterior preservados |
| Validações e falhas | Produto, referência, versão, URL, gate, hash, decisão técnica, flags comerciais e preparação incompatíveis recusados |
| Integrações | Serviço real de storage com S3/repositórios doubles; seleção JPA em MySQL 5.7 da sandbox |
| Recuperação | Handshake obrigatório, gate atual conferido, tentativa única, resultado/custo/raw audit preservados |
| Observabilidade e segregação | Auditoria da causa/tentativa/zero inferência; `AGENT_VALIDATION` e `PRIVATE_PREPARATION`, sem venda simulada em dados comerciais |
| Navegadores e dispositivos | Frontend sem alteração; prova visual e navegação previamente homologadas em Chromium desktop, iPhone 15 Pro e Pixel 7, a reconferir após persistência produtiva |

O arquivo `infra/testing/mira-creative-recovery/frozen-cycle-visual-input.json`
preserva o resultado técnico integral para conferir o hash aprovado. Não possui
credenciais ou evidência humana. A revisão de pixels de produção só pode começar
após publicação verificada e gravação pelo worker versionado.

## Validação

Primeira rodada afetada: 159 testes, zero falhas ou erros, um cenário opcional
dependente de captura externa não aplicável. O teste de gravação usou o PNG
1080 × 1350 produzido localmente pelo renderer versionado a partir da prova 523,
SHA256 de origem `45ec50db5899063f59b2d788ee24d9f7f326e18bb3c9194dd32fe63184132fa7`.
Não houve chamada de modelo ou escrita produtiva nesses testes.

Rodada final após os ajustes: **299 testes, zero falhas/erros**; inclui 92 condições
de arquitetura, controller, provider atual, jornada privada e persistência.
Dois cenários opcionais exigem, respectivamente, captura externa e exportação de
uma jornada encerrada; não se aplicam à peça ainda em preparação e não substituem
os testes determinísticos executados. A primeira preparação do teste físico
usou o driver H2 e o endereço localhost por engano; corrigidos para o driver MySQL
e o hostname da engine isolada, sem alterar código produtivo.

O repositório real passou no **MySQL 5.7.44**: um teste físico, sem falha/erro,
incluindo namespace, callback anterior e recusa de autoridade, e excluindo rejeição
funcional e outro agente. Topologia temporária removida com volumes e órfãos.
Spotless, comentários Java e diff passaram. JAR e classes testadas coincidem:
4.259 classes, 761 recursos íntegros e catálogo inicializado com 523 cartões.

PR, SHA e publicação só constituem entrega após evidência correspondente. Os
resultados do artefato e pareceres do produto são distintos da publicação de código.
