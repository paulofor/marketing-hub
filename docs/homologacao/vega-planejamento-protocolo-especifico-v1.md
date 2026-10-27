# Vega — protocolo de homologação específico — 27/10/2026

## Evidência e decisão

Vega, ciclo 16, experimento 109, cadeia 27: Atena #824 e Plutus #825 aprovaram
uma comparação REFERENCE/REDUCED. Dédalo #826 retornou ADJUST ao inspecionar a
v15: ocasião, peças e preferência já antecedem o resultado, sem cadastro.
O contexto do backend entregava a todos os produtos o perfil de comparação
documental de Mira. A candidata v16 ainda não consta do catálogo de implementações;
por isso Atena também não recebeu a descrição estruturada da base disponível v15.
O caso anterior #702 confirma a recorrência de planejamento sem entrada real.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Repetir Dédalo com o mesmo contrato | Sem alteração de software | Repete custo e contradição | Rejeitada |
| Dificultar a referência ou dispensar a revisão | Faz a comparação passar | Fabrica vantagem e enfraquece o aceite | Rejeitada |
| Delimitar o protocolo e devolver a contradição à estratégia | Corrige a origem e reutiliza as provas | Validação de contexto e regressões entre módulos | Adotada |

O catálogo continua descrevendo implementação, não aprovação. Versões disponíveis
do mesmo produto são referências explícitas; nunca se apresentam como a candidata
nova já construída. Só a versão exata com capacidade declarada recebe o perfil de
comparação documental. Os demais fluxos preservam os três cenários e dispositivos,
sem uma exigência implícita de reduzir entrada. Atena permanece autora da estratégia;
não se altera seu parecer histórico nem se presume uma nova vantagem comercial.

## Matriz local definida antes dos testes

| Área | Aceite |
| --- | --- |
| Original | Candidata nova de Vega não recebe REFERENCE/REDUCED; recebe a v15 como referência de implementação, sem herdar aceite |
| Outro produto | Identificadores e versões sintéticos não herdam comparação ou catálogo de outro produto |
| Caminho válido | Mira com capacidade documentada conserva 18 combinações; gates reais permanecem exigidos |
| Produtor/consumidor | Contexto real do backend é aceito por Atena; resposta que impõe comparação fora do escopo é recusada antes da passagem |
| Recuperação | Serviço real da tela reabre a origem contraditória antes de Plutus/Dédalo, preservando tentativas; tarefa ativa não duplica |
| História | Ciclos encerrados, fases posteriores e entradas legadas compatíveis permanecem intactos |
| Auditoria | IDs, fontes, resposta e custos preservados; ausência de custo não vira zero |
| Segurança comercial | Nenhuma inferência, campanha, pagamento ou mensagem externa nos testes; dados sintéticos segregados |
| Interface | Conferência do acompanhamento publicado em desktop e celulares; nenhuma alteração da experiência comercial neste ajuste |

## Limite financeiro observado

Ledger MCP dos experimentos 100, 103, 106, 109 e pesquisa 73: US$ 18,3364016
conhecidos estimados. Reserva histórica #709 de US$ 3 e provisão #821 de US$ 0,50
permanecem, total conservador US$ 21,8364016, superior ao teto cumulativo US$ 20.
Estimativa Codex não é fatura. A correção local é independente dessa decisão;
novas tarefas dos agentes aguardam cobertura autorizada. A mídia do ciclo 16
continua em R$ 0, sem transferência automática do predecessor.

## Aprendizado reutilizável

Hipótese: protocolo específico e base explícita reduzem retrabalho de arquitetura
e custo até aceite. Medir retornos por comparação indevida, tempo parado e custo
por passagem. Testes comprovam o contrato, não conversão, demanda ou lucro.
Reverter se uma comparação implementada perder seus controles ou se uma versão
histórica for reidentificada como nova. Preservar revisões independentes.

## Validação local e ambiente

- Regressões direcionadas do backend: 35 casos, incluindo contexto da construção
  de Mira, retomada pelo serviço real e identidades diferentes.
- Atena: 80 testes; Dédalo: 104 testes; empacotamento das evidências: 16 testes.
- O processo da suíte completa encontrou o limite efetivo da sandbox (cgroup de
  3 GiB). Uma JVM com heap de 3 GiB foi encerrada com código 137; com heap de
  1.400 MiB, a retenção entre centenas de classes esgotou a memória. A validação
  foi dividida em lotes locais com JVM nova, preservando os casos já aprovados.
  Nenhuma configuração produtiva ou global de Java/Node foi alterada.
- Compatibilidade de Mira registrada em novo manifesto v51, preservando v50,
  hashes históricos e `automaticDeployOnMerge: false`. Não renova pareceres.

Resultado da rodada completa: 5.415 casos catalogados no backend, com 5.358
executados aprovados, zero falhas/erros e 57 testes condicionais não habilitados
localmente (incluindo integrações MySQL). As 742 classes com testes geraram
relatórios; classes auxiliares sem métodos de teste não geram relatório próprio.
Os 38 lotes restantes terminaram com sucesso, preservando os casos aprovados
antes da divisão. ArchUnit: 92 casos aprovados. Atena e Dédalo: 184 aprovados.
O pacote de 674 arquivos/162 manifestos passou na conferência de hashes.
Nenhuma alteração de schema, regra comercial, produto público ou mídia.
