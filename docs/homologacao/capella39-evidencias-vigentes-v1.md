# Homologação da leitura de evidências — execução #39

Data: 04/10/2026. Escopo: Capella #7, Quartzo v1, cadeia #26, processo técnico
#122/v7, execução #39, referência `experiment:88`. Pai declarado #118/v12,
atividade `preflight`; não há ciclo nem tarefa de agente nesta execução.

## Diagnóstico confirmado antes da implementação

- A tela e o MCP mostram três instâncias concluídas (#477–#479) e o run técnico #16.
- O relatório apresenta plano #35, mas APIs e banco vinculam esse plano exclusivamente
  ao experimento #96. O experimento #88 pertence ao plano #2.
- A revisão financeira #9/revisão 5, plano #2/v4, usa contrato v1 e parecer #62.
  Sua validade terminou em 02/10/2026. A revisão #10/barber-v1 não pertence ao #88.
- A publicação #32 conserva o hash de HTML da homologação histórica, mas o validador
  oficial recusa o fingerprint comercial atual. A conclusão BPM histórica continua
  aparecendo como objetivo vigente; não é prova da validade atual.
- O #88 está `INVALIDATED`, com janela encerrada em 29/09, teto de R$ 125 e parada
  em cinco compras. O parecer #62 é limitado a R$ 100 e duas compras. Não alterar
  esses contratos, estender validade ou iniciar análise paga para obter conclusão.

## Matriz definida antes dos testes

| Área | Critério de aceite | Validação local |
| --- | --- | --- |
| Identidade | Referência de experimento seleciona somente seu plano direto/portfólio; ausência não adota outro plano do produto | Caso original e identificadores independentes; planos sem vínculo e referências comerciais |
| Evidência vigente | Conclusão técnica obsoleta deixa de comprovar o objetivo; instância e custo anteriores permanecem intactos | Executor real e motor BPM com fontes locais; fingerprint alterado, ausente, inválido e vigente |
| Recuperação | Nova prova válida permite nova ocorrência pelo comando canônico; repetição idêntica não duplica | Revalidação, idempotência e erro de leitura, sem chamadas pagas |
| Integração | Relatório e controle automático consomem a mesma verdade; bloqueio financeiro impede conclusão e retorno ao pai | Fluxo local com repositories simulados e transações locais; progresso, comando e espera persistida |
| Economia | Parecer vencido identifica revisão e plano corretos; teto acima de Plutus permanece bloqueado | Regressões financeiras e preservação dos resultados válidos de compra/medição |
| Observabilidade | Erros conservam contexto e stack trace; consultas não sobrescrevem histórico | Logs, respostas estruturadas e verificação de ausência de escrita durante leitura |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 apresentam plano, progresso e pendência enviados pelo backend | Frontend local com respostas da validação local; nenhuma API produtiva de escrita |
| Empacotamento | Classes e contratos corrigidos pertencem ao JAR validado | Testes unitários/integração, arquitetura, formatação e build local |
| Isolamento | QA, fontes e identificadores sintéticos não entram em campanhas, compras, receitas ou agentes pagos | Dependências simuladas, nenhuma autorização de mídia e custo comercial desconhecido preservado |

## Alternativas e escolha

| Alternativa segura | Benefício | Risco | Esforço | Aderência e escolha |
| --- | --- | --- | --- | --- |
| Renovar toda a homologação por testes determinísticos | Prova técnica completa | Repete verificações ainda válidas e não resolve a validade econômica | Alto | Viável, reservada para fontes técnicas integralmente alteradas |
| Separar o fingerprint comercial em contratos menores | Invalidação mais específica | Exige outro contrato e pode omitir dependências ainda não demonstradas | Alto | Viável como evolução separada, sem evidência suficiente neste escopo |
| Revalidar fontes canônicas no executor e resolver o plano pela referência | Preserva tentativas, gates e isolamento entre candidatas | Mantém bloqueios legítimos até existir prova atual | Baixo | Escolhida; corrige as duas causas observadas sem revisão paga |

Esta homologação comprova contratos operacionais. Não comprova venda, receita ou margem.
O vencimento financeiro é uma dependência legítima; a entrega de código não renova o parecer.

## Resultados locais e evidências

- Código anterior: as sete novas regressões falharam. Correção: 73 testes direcionados
  aprovados; suíte completa com 3.838 aprovados e 24 skips por condições documentadas.
  Inclui arquitetura, persistência H2, entrega simulada, idempotência e recuperação.
- A fixture do serviço BPM real alimenta o frontend local: seis cenários de
  desktop/iPhone 15 Pro/Pixel 7 aprovados, plano correto, conclusão atual coerente,
  pendências visíveis, sem overflow, erro JavaScript ou chamada de escrita produtiva.
- JAR aprovado: 4.206 classes idênticas às testadas, 744 recursos externos íntegros
  e nove testes do verificador de empacotamento aprovados. Spotless e build passaram.
- Publicação #32: SHA de origem conferido no MySQL e HTML servido; três dispositivos
  carregaram os oito elementos de imagem e os seis CTAs para o mesmo checkout.
  Capturas e bytes servidos estão vinculados por SHA-256. O teste passou a aguardar
  cada imagem lazy no viewport; a passagem rápida inicial deixava decodificação pendente.
- Meta: ambas as campanhas continuam `PAUSED`/`PAUSED`. QA conservou 711 eventos,
  máximo ID 5299. Nenhuma cobrança, chamada de modelo, mídia ou compra real.

Registro: [evidências estruturadas](capella39-evidencias-v1.json). A confirmação
posterior à publicação deve ser vinculada ao PR e ao SHA integrado; estes resultados
locais não declaram antecipadamente que a correção já está publicada.

Reprodução visual, com o frontend local na porta 4173:

```bash
cd backend/ads-service
mvn -Dtest=TechnicalPreflightCurrentEvidenceFlowTest \
  -Dpreflight.output=/tmp/preflight-current-evidence test
cd ../..
PREFLIGHT_PROCESS_EVIDENCE_DIR=/tmp/preflight-current-evidence \
  node backend/ads-service/src/test/resources/preflight-current-evidence-ui.cjs
```

Para consultar a pendência exata, use o plano financeiro do produto #7 com
`revisionId=9`, na edição avançada. A revisão mais recente, #10/barber-v1/plano #35,
não deve substituir a revisão da referência `experiment:88`.

## Complemento: contrato transacional da consulta

O PR #5487 foi integrado em `4ab65f09d972dd573ed3ebed22ed9e2267bcf09e`, com
workflows e deploy aprovados. A validação funcional posterior encontrou HTTP 500
no relatório: MySQL 1792, `SELECT ... FOR UPDATE` dentro de transação somente leitura,
em `ExperimentTechnicalPreflightActivityExecutor.requiresFreshExecution`. Os logs
preservam os request IDs `0cc2c9d9-b1ea-46a9-a301-9f44a1650200` e
`4b724b93-5016-4f81-8d7d-4b55ebfd8607`. Os mocks anteriores não verificavam SQL.
O histórico já documentava a mesma causa em Opala/Quartzo; o repositório já dispõe
de uma consulta sem lock, que deve ser reutilizada. A reserva continua nos comandos.

Antes da validação complementar, a matriz incorpora JPA real, transações Spring e
MySQL 5.7 isolado: ausência de ocorrência; quatro provas vigentes; alteração de
fingerprint/run; pendência funcional; preservação do histórico/custo; renovação e
idempotência entre transações. As consultas não podem emitir lock ou escrita;
os comandos devem conservar a reserva. O mesmo teste também inspeciona SQL em H2.

Alternativas: reutilizar a consulta existente sem lock corrige o contrato com baixo
esforço e preserva a segurança dos comandos; projetar a última ocorrência a partir
do histórico já carregado evita outra consulta, mas exige mudar o contrato genérico;
criar uma projeção consultiva própria é possível, com custo de manutenção maior.
Escolhida a primeira, sustentada pelas correções anteriores e pelo SQL observado.

O teste anterior falhou nas cinco consultas: H2 apontou `FOR UPDATE` pela inspeção
do SQL; MySQL 5.7 reproduziu erro 1792. Com a correção, 37 testes direcionados e
104 testes do runner conjunto Opala/Quartzo/preflight passaram. `bash -n`,
ShellCheck e Spotless passaram; a topologia temporária foi removida pelo runner.

As [evidências complementares](capella39-evidencias-complementares-v1.json)
preservam 13 testes de compra/entrega com integrações simuladas, falha e recuperação;
37 testes do executor de comunicação (35 aprovados, dois skips condicionais); bytes
de HTML/JS/privacidade pós-compra idênticos aos arquivos versionados; homologação
visual de sucesso/falha em três dispositivos e capturas imutáveis no repositório.
Texto, imagens/alt, links e estilos da publicação servida correspondem à fonte #32.
As diferenças de HTML bruto são inserções do publicador (telemetria e imagens
otimizadas); não constituem alteração da oferta. Nenhum pagamento, IA ou mídia foi
acionado. Capturas e testes segregados comprovam comportamento técnico, nunca venda.

A suíte completa complementar passou com 3.843 aprovações e 24 skips condicionais;
build e verificação do JAR também passaram. A rodada visual local repetiu os seis
cenários pertinentes sem escrita produtiva. Antes da retomada, o MCP confirma #39
em `WAITING_INPUT`, atividade `surfaces`, duas provas vigentes e o histórico
#477–#479 preservado. A liberação técnica e a dependência financeira serão
reconferidas pela tela após a publicação desta correção complementar.

## Complemento: workspace durante a renovação pendente

O PR #5488 foi integrado em `d65abcd6946285428cdbbc9fbe25983728710dd5`;
o relatório respondeu e foi validado em três dispositivos. O deploy
`37182157691` sofreu HTTP 500 do GitHub Packages na publicação do SDK, com
o mesmo POM e workflow anteriormente aprovados. Sua segunda tentativa terminou
com sucesso; o runtime confirmou o SHA e os 13 workflows aplicáveis terminaram.
A auditoria agendada de branches de pesquisas tem pendências próprias e não
pertence aos checks ou ao deploy deste escopo.

A renovação pela atividade `transaction` criou o run #17 e preservou o #16.
O backend passou a exigir as quatro provas atuais e manteve #477–#479 intactas.
Ao recarregar a tela, porém, nenhum workspace permaneceu: `readiness` obtinha a
identidade somente após aprovar a evidência e retornava identidade nula para
predecessora pendente ou falha funcional. O formulário existia no frontend;
o contrato do relatório impedia sua montagem. Nenhum gate foi registrado nem
nova tentativa criada para contornar a falha.

| Alternativa | Benefício | Risco | Esforço e aderência |
| --- | --- | --- | --- |
| Expor navegação ao painel oficial do experimento no bloqueio | Reutiliza outra tela pronta | Exige sair e retornar ao subprocesso para sua prova | Baixo; alternativa viável |
| Criar um workspace específico de recuperação | Distingue espera e execução | Duplica o painel e os contratos existentes | Médio; desnecessário neste caso |
| Resolver identidade validada antes dos gates e conservar o workspace | Recupera o próprio fluxo sem aprovar pendências | Exige recusar referência inválida ou de outro produto | Baixo; escolhida |

A matriz complementar, definida antes dos testes, cobre: run pendente com
predecessoras incompletas; fonte inválida e produto divergente; gates vigentes;
histórico sem escrita; registro dos quatro resultados pela tela e retorno ao
bloqueio financeiro; leitura transacional sem lock; desktop/iPhone/Pixel, sem
overflow, erros JavaScript ou writes produtivos. As dependências de IA, pagamentos
e mídia continuam simuladas. Seis regressões reproduziram a falta de workspace
no código anterior. Após a correção, os 33 testes direcionados, 110 testes no
runner MySQL 5.7 e nove cenários visuais passaram. A suíte completa passou com
3.850 aprovações e 24 skips condicionais (3.874 casos), seguida de build aprovado.
O harness reutilizado simula somente o callback oficial; comprova uma submissão
segregada por dispositivo e conserva a pendência financeira após o registro.
`bash -n`, ShellCheck, parser YAML e Prettier passaram. Spotless seleciona os seis
arquivos Java por expressão regular e registra explicitamente os arquivos conferidos.
Após a formatação, 38 casos direcionados passaram novamente com MySQL 5.7,
incluindo a presença do workspace durante a leitura física. O pacote final conserva
4.206 classes idênticas às testadas, 744 recursos íntegros e 510 cartões carregados.
A topologia temporária foi removida com seus volumes.

Registro: [evidências da renovação pendente](capella39-renovacao-pendente-v1.json).
O filtro `PREFLIGHT_PROCESS_SCENARIOS=pending` permite repetir só a recuperação
quando outra alteração não afetar os estados `current` e `stale`; filtro inválido
falha. Nenhuma nova infraestrutura ou chamada paga foi adicionada.

## Complemento: entrada extensa na homologação

O PR #5489 foi integrado em `1bf506185199d41dd084674e3cb10ebcc9fde994`,
publicado pelo run `37186558107` e confirmado pelo runtime e pela tela nos três
perfis. Os dez workflows encadeados terminaram; os oito resolvers de workers
confirmaram esse SHA sem exigir publicação de módulos não alterados. O head
padrão dos workflows avançou apenas por um Markdown de pesquisa independente.

O registro das quatro provas pela tela retornou HTTP 500 no request
`4befe003-c0a8-4d54-a591-2e7fb26144d1`. As referências montadas pelo operador
ultrapassavam 512 caracteres. O schema MySQL e a validação existente usam esse
limite tanto para referência quanto para resumo. O log confirma que a validação
barrou o dado antes da persistência, por `IllegalArgumentException`; o handler
converteu a recusa funcional em erro interno. O teste antigo de `NOT_APPLICABLE`
indevido esperava HTTP 5xx, comprovando a mesma falha de classificação histórica.
A hipótese inicial de falha de gravação foi descartada. A tentativa #17 permanece
pendente e as provas anteriores não serão reescritas.

| Alternativa | Benefício | Risco | Esforço e aderência |
| --- | --- | --- | --- |
| Corrigir somente a referência enviada | Desbloqueia esta submissão | Outros erros de entrada continuam como HTTP 500 | Baixo; incompleta para recorrência |
| Ampliar as colunas para transportar o manifesto inteiro | Permite texto extenso | Altera schema e incentiva duplicação da evidência | Médio; desnecessária |
| Reutilizar a recusa HTTP 400 existente e orientar referências curtas na tela | Preserva limites, texto e evidência completa por link | Exige regressões de fronteira e recuperação | Baixo; escolhida |

Matriz definida antes dos testes: referência e resumo com 512/513 caracteres;
erro no primeiro e último gate sem mutação parcial; gates incompletos,
duplicados e estado inválido; correção no mesmo run sem nova tentativa, custo ou
chamada paga; preenchimento de identidade; desktop, iPhone 15 Pro e Pixel 7 com
bloqueio do envio inválido e recuperação após corrigir o texto. O frontend deve
preservar a entrada extensa e mostrar orientação, sem truncamento silencioso.
O backend continua dono dos gates e da disponibilidade do processo.

As onze regressões HTTP e duas regressões de formulário falharam antes do ajuste.
A primeira rodada corrigida passou com 36 testes backend e 11 frontend; o fluxo
visual pendente passou nos três perfis, incluindo bloqueio 513 e recuperação 512.
A suíte global frontend em worker único encontrou nove arquivos com falhas
(cinco testes e quatro suítes sem coleta): 828 testes passaram. O checkout limpo
da main `47bfbbbd4f3f4fe8692582765bdb78ea511ed6e8` reproduziu os mesmos nove
arquivos e cinco testes (826 aprovações, sem os dois novos casos). Essa rodada compartilhava
worker e acumulava mocks globais/DOM entre arquivos; não confirmou defeito
nas telas. A execução com isolamento em forks passou integralmente: 845 casos
em 184 arquivos. O typecheck da entrega passou.
A primeira suíte Java completa foi interrompida com exit 137 após 2.129 casos,
sem falha funcional, enquanto a comparação frontend também usava memória.
As validações foram serializadas e o servidor de preview foi encerrado durante a
suíte, sem alteração de infraestrutura ou publicação para testar.

Dois casos adicionais de código de gate ausente/nulo reproduziram HTTP 500 por
`Set.of(...).contains(null)`; o log local confirmou `NullPointerException`. A
verificação explícita de nulo mantém a mesma recusa funcional, sem alterar o catálogo
aceito. A regressão também cobre a lista de gates nula. A suíte Java final incluiu
esses casos e passou isoladamente: 3.887 casos, 3.863 aprovações, 24 skips
condicionais, nenhuma falha/erro e build aprovado. O JAR conservou 4.206 classes
idênticas às testadas, 745 recursos íntegros e 511 cartões carregados.

Registro: [evidências da entrada de homologação](capella39-entrada-homologacao-v1.json).
A suíte global frontend deve usar isolamento; nesta sandbox foi executada com
`npm test -- --run --pool=forks --maxWorkers=1 --minWorkers=1`. O modo
`singleThread` foi adequado somente ao teste individual, não à suíte completa.

O build frontend, Spotless final, repacotamento com recursos atualizados, parser
Swagger, Prettier e revisão do diff passaram antes do commit. Não há alteração de
changelog ou de limite persistido; a confirmação publicada será vinculada ao PR
e à retomada pela tela. As referências propostas foram conferidas antes do envio:
327, 165, 304 e 236 caracteres, com resumos entre 316 e 358 caracteres.
