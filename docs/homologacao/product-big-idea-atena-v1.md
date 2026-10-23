# Big Ideas por Atena — homologação

Solicitação de 23/10/2026: criar as Big Ideas dos quatro produtos em PLAY com pesquisa
de demanda. Lacuna observada: nenhum dos quatro produtos tinha ideia registrada e a
geração de Atena exigia repetir o planejamento completo. Pesquisa genérica também
se torna a estratégia vigente do plano; não é um substituto seguro para esta tarefa.

Alternativas: novo agente (duplica responsabilidade e repasses); repetir o Processo 2
(risco para contextos já aceitos); pedido específico a Atena na fila existente
(adotado, menor escopo e identidade explícita do produto).

## Matriz definida antes da execução

| Critério | Validação local |
| --- | --- |
| Caminho completo | Pedido pela UI → fila de Atena → resposta simulada → adoção → revisão persistida |
| Identidade | Produto original, outro ID e tentativa cruzada; resposta não pode trocar produto |
| Concorrência | Duplo clique/reenvio reutiliza pesquisa; edição humana concorrente impede sobrescrita |
| Evidências | Três alternativas, duas classes de fontes, desejo/mecanismo/prova/limites completos |
| Falhas | Conteúdo inválido, callback perdido, reinício com recibo; nenhuma nova inferência automática |
| História | Pesquisa de Big Idea não substitui estratégia vigente, experimento, campanha ou aprovação |
| Custos | Tokens e estimativa preservados; ausência continua desconhecida, sem extrato obrigatório |
| Interface | Desktop, iPhone e Pixel: espera, erro, pesquisa concluída e adoção acessíveis |
| Segregação | IDs sintéticos e modelo simulado; zero chamadas pagas ou eventos comerciais locais |

Hipótese de melhoria: reduzir trabalho manual e divergência entre desejo, entrega e
comunicação. Medir ideias completas, referências verificáveis e retrabalho por incoerência.
Esses resultados não medem conversão. Alta demanda exige mercado acessível, desejo
recorrente/urgente, alternativa atual com esforço e disposição de pagar; fontes públicas
são sinais. Compras conciliadas e contribuição continuam como provas comerciais.

Fontes consultadas em 23/10/2026:
- [Ipsos: Big Idea](https://www.ipsos.com/en-us/uncover-your-brands-big-idea): relevância,
  benefício distintivo e ligação crível com a entrega.
- [Christensen Institute: Jobs to Be Done](https://www.christenseninstitute.org/theory/jobs-to-be-done/):
  progresso procurado em uma circunstância específica, incluindo dimensões funcionais e emocionais.
- [Google Trends: limites dos dados](https://support.google.com/trends/answer/4365533?hl=pt-BR):
  interesse relativo não equivale a volume absoluto ou compra.

Aplicação restrita a pesquisa pública, agentes e testes determinísticos; não recrutar
participantes ou solicitar opiniões. O cânone mantém a evolução manual explícita e a
fixação da Big Idea em novos contextos, sem migração de experimentos em andamento.

## Resultado local — 23/10/2026

- Backend: 146 testes pertinentes e de arquitetura aprovados; bateria completa posterior com
  5.210 testes, zero falhas/erros e 57 integrações opcionais não executadas. A persistência
  desta entrega foi validada separadamente no MySQL real. Casos de identidade cruzada,
  campos ausentes, demanda apresentada como comprovada, edição concorrente, reenvio do aceite
  e preservação da referência do experimento.
- Atena: 68 testes aprovados, incluindo processo Codex simulado com os argumentos reais,
  duas identidades, perda de callback, reconstrução após reinício e interrupção sem resposta.
  O recibo preserva prompt, saída, fontes e tokens; nenhuma dessas recuperações repete o modelo.
- Interface: cinco testes de componente, TypeScript e build aprovados. Chromium desktop,
  iPhone 15 Pro e Pixel 7 emulados executaram pedido → espera → proposta → adoção → histórico,
  com a API real e persistência MySQL 5.7. Duplo pedido reutilizou a pesquisa; resposta de outro
  produto foi recusada; nenhuma revisão duplicada ou overflow horizontal.
- Consultas nativas reais no MySQL comprovaram isolamento entre produtos e preservação do
  parecer estratégico do plano, mesmo após uma pesquisa de Big Idea mais recente. Reinício e
  reaplicação dos changelogs existentes preservaram os registros. Topologia temporária removida.
- Prompts e schema incluídos no catálogo de Atena. A validação premium do harness passou.
  Testes da interface incorporados ao workflow existente, sem nova infraestrutura paralela.
  Spotless, Prettier, actionlint e contratos do workflow frontend aprovados.

Reprodução: runner existente `infra/testing/product-big-idea/run-local.py`, com projeto Compose
exclusivo da sessão e `BIG_IDEA_BROWSER=1` quando houver preview local na porta 15179.
Modelo, fontes e identidades dos testes são sintéticos; não constituem evidência de mercado.

## Compatibilidade das provas compartilhadas

O primeiro CI encontrou o catálogo anterior na atestação Mira v41. O teste de Têmis
reproduziu localmente a mesma divergência; o histórico de
`LOOP-PDE-REVISAO-MANIFESTO-GLOBAL-MUTAVEL` confirma essa dependência. A pesquisa nova
acrescenta dois artefatos ao catálogo, sem alterar o produto homologado. A v42 preserva
a v41 por hash, registra esse delta e mantém as demais provas e limites. Não renova
pareceres comerciais nem autoriza publicação ou consumo de modelos.

Validação de compatibilidade: catálogo do backend, contexto versionado e serviço privado
de Mira (30 testes); carregadores de provas de Psique e Têmis; contrato e montagem do
pacote imutável. Esses comandos complementam a matriz acima antes de atualizar o PR,
evitando depender somente dos testes do módulo diretamente alterado.

Adoção da capacidade: usar a pesquisa específica nos produtos sem Big Idea, conferir as fontes
e aplicar a proposta na própria gestão. Se surgir inconsistência de identidade ou evidência,
preservar o recibo, corrigir a causa e recuperar o resultado disponível. Evolução comercial usa
nova revisão com motivo; não alterar silenciosamente experimentos já fixados.
