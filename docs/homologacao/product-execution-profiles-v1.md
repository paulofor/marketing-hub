# Matriz local — cadeia comum e fichas de execução v1

Definida antes dos testes em 14/09/2026. Escopo: backend, UI administrativa, contexto BPM e
controle de consumo de imagens. Produtos reais não são modificados nem geram chamadas pagas.

## Evidência de partida

- MCP `db_query`: Capella #7, pacote personalizado de ativos visuais, entrega personalizada e
  assistida; Vega #4, programa guiado; Mira #10, aplicação privada de leitura.
- `Product` já distingue tipo, formato, entrega e receita, mas não possui ficha de execução
  versionada. `ProcessRun` preserva cadeia/processo/ciclo/referência.
- `LOOP-DESTINO-CONDICIONAL-LANDING-OBRIGATORIA`: não transformar chamada de subprocesso em
  obrigatoriedade universal; não dispensar gates nem trocar contexto durante o avanço.

## Matriz de aceitação

| Área | Casos obrigatórios |
| --- | --- |
| Caminho feliz | Criar ficha de imagens, revisar economia, vincular contexto, consultar atividades e contexto do agente, consumir dentro do limite |
| Reúso | Imagens em outro tipo; jornada guiada; ferramenta IA; pacote digital; tipo sem capacidade explícita; entrega sem custo variável de IA e produção privada com outro modelo |
| Validações | Campos ausentes, cenários inválidos, moeda/custo incompleto, margem insuficiente, parecer de outro produto/versão |
| Versões | Nova revisão preserva vínculo, cadeia/processos fixos, isolamento entre produtos, ciclos e referências |
| Gates | Quatro checkpoints financeiros; nenhum gate humano dispensado; desconhecido bloqueia |
| Integrações | Endpoints reais do backend, persistência, contexto BPM, geração com provedor local substituto |
| Falhas | Concorrência no orçamento, repetição idempotente, falha cobrada, custo desconhecido, custo acima da reserva, resposta tardia, limite privado global e orçamento zero explícito |
| Observabilidade | Ficha, parecer, decisão, bloqueio e consumo persistidos; sem segredo ou dado de outro cliente |
| Métricas | Estimativa distinta de resultado; testes fora das métricas comerciais; nenhum ganho de vendas alegado |
| UI | Desktop Chromium, iPhone e Pixel emulados; formulários, erro, loading, links de contexto e histórico; gerador administrativo em origem HTTP não confiável |
| Banco | MySQL 5.7: aplicação, reaplicação, constraints e rollback em topologia descartável |

Uma rodada integral sem defeitos encerra a matriz. Após correção de defeito, exigir duas
rodadas integrais consecutivas sem falhas; reiniciar a contagem se surgir outro defeito.

## Resultados

Homologação concluída em 14/09/2026: duas rodadas locais completas e consecutivas aprovadas
após a última correção. Resultados anteriores e diagnósticos não entram nesta contagem.

| Rodada | Resultado | Testes executados | Duração total | Banco, reinício e navegação |
| --- | --- | --- | --- | --- |
| 1 | PASS | 3.989 | 694,7 segundos | PASS |
| 2 | PASS | 3.989 | 700,8 segundos | PASS |

| Suíte | Executados por rodada | Opcionais ignorados por rodada |
| --- | --- | --- |
| Backend | 2.974 | 8 |
| Plutus / financial-agent-worker | 38 | 0 |
| Dédalo / landing-generator-agent-worker | 62 | 0 |
| AI Worker | 240 | 2 |
| Frontend | 675 | 0 |
| Total | 3.989 | 10 |

Foram **7.978 testes executados sem falhas**, além dos casos da matriz integrada. Os dez
ignorados pertencem a suítes opcionais preexistentes: replays e integrações condicionadas
por configuração/artefatos no backend, duas condições de vídeo e dois casos legados do
planejamento de landing no AI Worker. Nenhum caso obrigatório desta matriz foi ignorado.

As duas rodadas também comprovaram:

- TypeScript e build da interface; desktop, iPhone 15 Pro e Pixel 7 emulados, mais o gerador
  em origem HTTP não confiável. Formulários, decisões, revisões, links e erros passaram.
- Oito reservas simultâneas: quatro aceitas e quatro bloqueadas pelo teto em cada rodada.
  O gerador HTTP produziu um lote sintético e bloqueou o seguinte por custo desconhecido.
- Histórico idêntico antes e depois do reinício do backend, isolamento de referências e
  persistência real no MySQL 5.7, com aplicação, rollback e reaplicação dos quatro changesets.
- Nenhuma chamada paga, nenhum dado produtivo alterado e limpeza dos containers, rede e
  volumes temporários do projeto Compose exclusivo após cada rodada.

Impressão SHA-256 do código/configuração/testes nas duas rodadas:
`b304123758857e8d51bef44e8de076361df4afc7614788f709088906d5e383a5`.
O runner conferiu que esses arquivos não mudaram durante cada rodada. A consolidação
posterior alterou somente este relatório. O diff foi revisado, os includes relativos do
Liquibase conferidos e as classes Java alteradas possuem comentário de responsabilidade.
As alterações permanecem locais, sem commit, PR ou publicação.

## Escopo concretizado

- Tela administrativa de fichas por produto, com quatro capacidades, custo completo, histórico,
  vínculo explícito, decisão financeira e conciliação de custo com comprovante identificado.
- Backend escolhe os trabalhos especializados e conserva os IDs/versionamento da cadeia e dos
  subprocessos. O contexto dos agentes contém a ficha; a fila e os comandos revalidam os gates.
- Dispensa de audiovisual é persistida e reconhecida pelo consumidor da fila; nenhum objetivo
  ou aprovação é inventado. Cadeia/ciclo/experimento são preservados na ida e no retorno.
- Plutus recebe as premissas completas; Dédalo recebe instruções de imagens, entrega, acesso e
  limites sem obrigação de webapp. A análise é reutilizada nos quatro checkpoints.
- Reservas cobrem a geração administrativa e os pacotes do Lead Portal, incluindo o pior caso
  de retries do AI Worker. Falha, custo desconhecido, sobrecusto e concorrência foram incluídos
  na matriz. Bloqueio financeiro do backend aparece corretamente no log do worker.
- Nenhuma chamada paga, produto real, campanha, venda ou execução publicada foi criada.

## Aprendizados durante a implementação

1. A dispensa apenas visual deixaria o consumidor BPM esperando uma predecessora. A prova
   passou a ser gravada como instância canônica e validada pelo mesmo contrato da fila.
2. Trocar uma revisão não pode mudar o destino dos links nem a versão da execução. As fichas
   e os links usam o vínculo exato, incluindo subprocessos publicados que depois sejam retirados.
3. Reservar apenas as imagens finais subestimaria as três tentativas possíveis da imagem-base.
   O contrato de teste compara a reserva do backend com o limite versionado do executor.
4. Uma referência de produto/plano incompatível não pode consumir ou atribuir custo ao contexto
   de outro produto; a geração administrativa confere os vínculos antes do provedor.
5. Produção inicial e entrega por venda podem ter custos e modelos diferentes. A ficha separa
   os orçamentos e permite entrega sem consumo variável de IA de forma explícita, preservando
   os demais custos e bloqueando chamadas pagas sem orçamento.
6. O tipo primitivo da quantidade privada confundia campo omitido com zero. A entrada passou
   a exigir a quantidade explicitamente; o contrato HTTP testa ausência mesmo quando os custos
   são zero. As rodadas completas anteriores à correção não contam para a homologação final.
7. A página de atividades não consumia `sourceReference` do link. Consulta e comando passam
   a aceitar a referência validada, e a retomada automática transporta a identidade congelada.
   Testes HTTP, de serviço, de hooks e de navegação verificam contextos históricos e sem ciclo.
   Uma ficha ainda sem vínculo não oferece links que possam escolher outra execução.
8. O administrador público usa HTTP: `crypto.randomUUID()` não existe nesse contexto.
   A correlação da geração passou a usar `getRandomValues`, disponível também em HTTP.
   O browser abre a aplicação em uma origem HTTP local não confiável, gera um lote sintético
   com reserva real no banco e verifica o bloqueio do próximo lote por custo pendente.
   Referências: [randomUUID](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID)
   e [getRandomValues](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues).

A navegação no browser confere os parâmetros enviados pela página de destino também diante
de falha de consulta simulada. O contrato HTTP real, o serviço com referências históricas e
a execução automática com persistência são verificados nas suítes Java; os hooks e a página
conferem que o ciclo mais recente não substitui uma referência explícita sem ciclo.

A montagem dos testes também exigiu corrigir um test double JDBC, um seletor de alertas do
browser e o cache do utilitário Liquibase após rollback. Uma tentativa de suíte com compilação
concorrente no mesmo `target` foi descartada. O runner definitivo executa as operações Maven
sequencialmente e invalida a rodada se houver mudança de código durante a execução.

## Reprodução e evidências

A tela nova fica em `/products/{productId}/execution-profiles`, acessível pela cadeia e pelas
atividades do produto. O cadastro informa capacidade e resultado, custos e orçamentos; o
vínculo congela uma revisão antes do início da execução. A análise de Plutus e as quatro
decisões ficam na mesma tela, com acesso ao parecer financeiro e ao histórico dos processos.

Execute `python3 infra/testing/product-execution-profiles/run-local.py --rounds 2` na sandbox.
O runner usa exclusivamente o projeto Compose autorizado, IDs sintéticos 94001–94080,
MySQL 5.7, Plutus/provedor substitutos e Chromium local. Impede tráfego do browser para
serviços externos e remove containers/volumes ao encerrar, inclusive em falha.

`artifacts/product-execution-profiles/results.json` e `round-N/result.json` preservam os
resultados finais e a impressão do código. `round-N` também contém logs por módulo,
`api-results.json`, `browser-results.json`, concorrência, screenshots dos três dispositivos
e do gerador HTTP, reinício, apply/reapply/rollback e comprovação da limpeza.
`--skip-unit` é apenas diagnóstico e nunca produz resultado de matriz completa.

O changelog mestre apenas inclui o arquivo incremental novo com
`relativeToChangelogFile: true`. Nenhum changeset antigo foi alterado. A validação integral
do mestre no runner Liquibase MySQL 5.7 também será executada no eventual PR solicitado
pelo usuário, conforme `docs/database/liquibase-mysql57.md`.

## Limites da conclusão

Os testes provam contratos, integração local e bloqueios; não provam rentabilidade comercial
real nem qualidade perceptual de uma imagem paga nova. Nenhum produto em curso foi migrado.
A adoção exige a funcionalidade publicada pelo fluxo de revisão do usuário e uma ficha
criada/vinculada pela tela antes de iniciar a nova execução. Outros executores de consumo
precisam integrar a reserva quando ganharem implementações próprias; não são declarados
protegidos por esse conector de imagens. As tarifas informadas exigem conferência da fonte
na revisão financeira; não há monitoramento automático de preços externos nesta entrega.
