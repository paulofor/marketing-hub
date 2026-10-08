# Conclusão do Processo 3 de Capella — 08/10/2026

## Contexto confirmado e causas

Produto 7, Capella / Agenda Cheia Nail Design, Quartzo; cadeia 26, ciclo 5,
experimento 98, Processo 117 v11, execução 47. A prova técnica 648 foi registrada
como aprovada, mas o processo estava pausado antes de `psiqueAdherent`.

Os contratos aprovados de Dédalo 597 e 599 exigem **36 arquivos funcionais**:
20 PNG, dez legendas individuais, cinco mensagens individuais e calendário de
sete dias, nas pastas declaradas. O compositor privado e seu validador aceitavam
24 arquivos, com textos agregados. O teste comercial legado comprova que essa
forma existe historicamente; ela não atende ao novo contrato privado.

O harness de kits também omitia `screenshotEvidenceKeys` em cada cenário. Os
harnesses de Mira e Vega já oferecem esse vínculo. A passagem independente de
Psique exige a chave para associar o cenário ao snapshot persistido **antes**
de chamar o modelo. A matriz técnica anterior não testava essa passagem.

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Reduzir o contrato para 24 arquivos | Pouca alteração | Muda a entrega aprovada para acomodar o defeito | Rejeitada |
| Corrigir o compositor existente e o aceite | Cumpre a entrega e reutiliza o motor | Exige preservar pacotes históricos e conferir a revisão independente | Adotada |
| Criar outro serviço e reconstruir a cadeia | Pode atender novos formatos | Duplica capacidade e amplia o trabalho sem necessidade | Rejeitada |

Nenhuma aprovação histórica será reescrita. Pacotes, custos, janela de 08–16/10 e
mídia R$ 0 permanecem preservados. Compatibilidade do pacote precisa ser comprovada
antes de qualquer parecer pago. IA exige limite próprio de Capella; os limites de
outros produtos não se transferem. Prontidão técnica não comprova vendas ou lucro.

## Matriz de aceite definida antes dos ajustes

| Dimensão | Verificação |
| --- | --- |
| Entrega real | Compositor → ZIP com os 36 arquivos exatos → aceite backend → primeira aplicação e download |
| Validação | PNG nativo, dez legendas e cinco mensagens UTF-8; referências do calendário resolvem; pacote incompleto/extra/traversal recusado |
| História | ZIP legado permanece legível, sem virar prova suficiente do contrato novo; artefatos e pareceres antigos preservados |
| Agentes | Harness técnico → cenário novo → upload → vínculo dos snapshots → entrada do parecer independente → callback → gate |
| Cenários | ADHERENT, RECOVERY e SAFETY, desktop/iPhone/Pixel; recuperação reutiliza resultado e SAFETY não compõe |
| Isolamento | Perfil nails-v1 e barber-v1, produtos/ciclos/experimentos distintos; identidade cruzada recusada |
| Falhas | Callback preservado e replay; ausência de captura bloqueia antes do modelo; sem cobrança ou composição duplicada |
| Observabilidade | Arquivos/hashes, versão do contrato, entradas/saídas, horários, snapshots e causa persistidos |
| Economia | Zero chamadas a provedores de imagens, SMTP real, mídia e compras nos testes; custo desconhecido não é zero |
| Processo | Aprovações independentes exigidas e gate do backend; retorno ao processo/ciclo somente com objetivos aceitos |

Na primeira tentativa local, MySQL 5.7 falhou com erro 28, `No space left on device`
na engine efêmera. O runner passou a utilizar `tmpfs` limitado para o banco descartável,
sem remover imagens, volumes ou recursos de outras execuções.

## Resultados

- Matriz HTTPS real com MySQL 5.7, backend e compositor: nove percursos por perfil,
  `nails-v1` e `barber-v1`, 18 no total. Três cenários, desktop/iPhone/Pixel,
  arquivos baixados com hash correspondente e pacote atual de 36 arquivos.
- Seis cenários independentes atravessaram o consumidor real de Psique, upload
  simulado com SHA dos pixels, sanitização, prompt, modelo simulado e callback.
  Dois fluxos atravessaram o consumidor de Têmis e o gate real do backend, que
  apontou `pde-communication-sales-journey` sem conceder mídia ou publicação.
- Oito grupos de controles reais de API passaram: identidade, autenticação,
  consentimento, contatos fictícios, concorrência, replay, limite e revogação.
- A matriz anterior aceitava saída sem quantidade declarada e sem vínculo de
  captura por cenário. A matriz corrigida exige formato, 36 arquivos (zero em
  SAFETY), hash baixado e vínculo antes da interpretação. A prova anterior 648
  e os seus artefatos não foram apagados nem convertidos em aprovação nova.
- Backend: rodada completa limpa e isolada, 4.367 testes, zero falhas/erros;
  37 casos condicionais não se aplicavam sem seus runners. Regressões finais
  dos contratos alterados foram conferidas separadamente, além da matriz acima.
- Pagamentos: 63 testes aprovados, incluindo preservação dos 24 arquivos comerciais
  legados e 36 arquivos privados nos dois perfis. Psique: suíte de 168 testes,
  sem falhas/erros, além dos seis casos reais parametrizados da matriz.
- Scripts: `bash -n`, ShellCheck, wrapper Chromium com pin do certificado local,
  sintaxe JavaScript, testes de pós-compra e empacotamento canônico de evidências.
  Java 21, Node/npm/npx da imagem persistente; executáveis globais preservados.
- Spotless passou em todas as classes alteradas do backend e na suíte de Psique.
  A consulta global do backend encontrou dez arquivos preexistentes fora deste diff;
  eles não foram alterados para ampliar o escopo. O empacotamento Java dos três módulos passou.
- O modelo simulado de Psique confere campos obrigatórios do schema entregue ao CLI
  e devolve o identificador da captura recebida, além dos IDs de evidências. Não
  depende de respostas parciais que o schema real recusaria.

Os modelos, storage de capturas e persistência BPM do gate foram doubles locais;
as respostas simuladas nunca foram enviadas a produção. Isso comprova os contratos
executáveis e o caminho de continuação, não o parecer independente real nem vendas.
O banco MySQL, compositor, ZIP, navegador, consumidores e verificadores utilizados
nessa passagem são os implementados no repositório.

## Limitações e recuperação do ambiente local

O filesystem da sandbox apresentou zero espaço disponível para usuários comuns.
O teste de configuração de pagamentos foi repetido apontando sua sonda de espaço
para `/dev/shm`, que tinha 64 MB livres; a sonda não foi desativada e nenhuma regra
de produção foi alterada. O MySQL descartável usou `tmpfs` limitado a 512 MB. A
engine foi acessada pelo hostname interno informado em `DOCKER_HOST`.

Uma rodada inicial concorria com outra compilação no mesmo `target` do backend e
teve classes indisponíveis durante a leitura. A rodada limpa e isolada acima passou.
O runner limita os processadores reconhecidos pela JVM para não exceder a quantidade
de threads da sandbox. A conferência do gate recusou corretamente o HTTP da fixture;
foi reutilizado o proxy TLS local já existente, com CA de teste e pin do certificado
no Chromium. Não foi relaxado HTTPS no contrato real.

A topologia do projeto Compose exclusivo foi removida com volumes e órfãos ao final.
Não foram removidos recursos de outras execuções nem publicados containers por SSH.

## Situação antes da publicação

Execução 47 pausada, com os resultados históricos preservados. As três revisões de
Psique e uma de Têmis precisam de limite de IA próprio de Capella; o gasto histórico
conhecido era USD 0,9603416, com cobertura parcial. Nenhuma nova chamada paga foi
realizada nesta homologação local. Publicação, SHA, runs e estado final do processo
serão vinculados no PR da solicitação após conferência em produção.
