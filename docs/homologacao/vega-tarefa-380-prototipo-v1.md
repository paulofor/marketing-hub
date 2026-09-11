# Vega: correção da tarefa #380 e protótipo privado do ciclo 2

Data: 2026-09-11. Homologação local concluída; confirmação operacional registrada abaixo.

## Causa confirmada e alternativas

A tela pública e o MCP (marketinghubdb) confirmaram #377 em falha técnica e #378–#380
bloqueadas por ausência de URL, implementação e aceitação. A #369 produziu especificação,
com geração por IA, execução por worker e eventos persistidos, mas não código executável.
O desenvolvimento deve implementar esse contrato. O contexto de construção do ciclo retorna
URL nula e status PLANNED incondicionalmente; não existe handoff do executável do sucessor.
O harness de Psique aceita somente Mira. As tarefas antigas permanecem imutáveis.
A consulta MCP aos logs de Dédalo funcionou, mas a retenção não contém a #380; sua auditoria
persistida é a evidência histórica. Mira #370 fornece o contraste bem-sucedido, com URL e versão.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Repetir parecer | Pode confirmar disponibilidade transitória | Repete custo sem executável; baixo esforço | Descartada pelo histórico |
| Adaptar v7 comercial | Reaproveita a superfície existente | Mistura ciclos, pagamentos e hipóteses; médio esforço | Descartada |
| Executável privado versionado e handoff por ciclo | Testa a utilidade aprovada e preserva histórico | Implementação e integração completas; maior esforço | Escolhida |

## Matriz definida antes da implementação

| Controle | Aceite |
| --- | --- |
| Contrato comercial | Produto 4, ciclo 2, #92; primeiro ajuste usando itens disponíveis; sem alteração da v7 |
| Contexto BPM | URL, versão e aceitação somente do ciclo explícito; aprovações planejadas e aprendizado preservados |
| Persistência | Sessões, entradas, execuções, eventos, resposta bruta e falhas no backend principal/MySQL 5.7 |
| Integração | UI → backend → pending → worker → modelo simulado → callback → UI, sem banco no worker |
| Valor | Ocasião e combinação produzem cartão com microação, aplicação, ocasião e autoavaliação |
| Retomada | Recarga e nova autenticação restauram cartão e eventos, sem regenerar ou duplicar |
| Falhas | Contexto ausente, credencial inválida, timeout, resposta incompleta e rede indisponível explicados |
| Limites | Pedido de aquisição bloqueado com orientação e reformulação; nenhum pagamento, campanha ou venda |
| Instrumentação | Cinco sinais ligados às ações correspondentes; sem confundir renderização com compreensão humana |
| Segregação | QA/AGENT_VALIDATION separados de HUMAN; duas leituras e versões não compartilham estado |
| Auditoria | Request, response, modelo, tokens, status e custo quando disponível vinculados à execução |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 simulados; foco, recuperação, texto e largura |
| Gate | Dédalo exige executável e aceitação; a homologação técnica exige seus próprios cenários |
| Publicação | Somente após testes, duas rodadas consecutivas sem falhas depois da última correção e revisão do diff |

Integração OpenAI segue [Flex](https://developers.openai.com/api/docs/guides/flex-processing)
e [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).
As rodadas locais usam provedor simulado; testes publicados devem ser segregados e não provam
validação humana nem aumento de receita.

## Resultado local

Depois de corrigir os defeitos encontrados, `publication1` e `publication2` passaram
integralmente e consecutivamente, com os 15 grupos de verificação de
`infra/testing/vega-private-prototype/run-round.sh`. Cada rodada contém:

- 262 testes de backend, 62 de Dédalo e 94 de Psique;
- 20 testes do formulário administrativo, cinco do executor e dois do contrato de publicação;
- 27 controles de integração, incluindo concorrência, idempotência, falhas, retomada e segregação;
- cinco cenários no navegador local, os mesmos cinco dentro da imagem de Psique e três percursos
  do formulário administrativo em desktop, iPhone e Pixel simulados;
- compilação e tipos dos dois frontends, MySQL 5.7 com a migração real, TLS/proxy, imagens e diff.

Entre as correções verificadas estão a leitura do token pelo configtree canônico do backend,
a transmissão sem buffering dos relatórios pelo proxy e a classificação das capturas como
`FULL_PAGE`, aceita pelo contrato de upload de evidências. A simulação do provedor é explícita.
Os arquivos completos estão em `artifacts/vega380/publication1` e `publication2` na sandbox.

## Publicação autorizada

As cinco imagens foram construídas pelos Dockerfiles deste repositório e receberam a tag
`vega380-v9`. As camadas, configuração de execução e plataforma foram comparadas após a
transferência. Docker clássico e containerd apresentam identificadores distintos para o mesmo
conteúdo importado; por isso a conferência inclui os hashes de todas as camadas e da configuração,
sem tratar a diferença do identificador do armazenamento como mudança do código.

O protótipo permanece privado, sem pagamento ou campanha. A v7 comercial e as tarefas históricas
permanecem como referências independentes. A execução de Dédalo e a homologação posterior são
passos distintos; publicação da imagem não equivale à aprovação do agente.

## Confirmação operacional

As imagens foram ativadas nos hosts autorizados: backend e administrativo em `191.252.181.168`,
frontend privado e executor de ajustes em `163.245.200.7`, Psique em `163.245.202.80`.
As credenciais e sessões existentes foram preservadas em memória durante a execução do Compose.
O proxy passou em `nginx -t`; o diagnóstico público identifica a v9. O MCP confirmou a migração
`2026-09-11-vega-private-prototype-v1` como `EXECUTED` no banco real.

Pela tela administrativa, foi registrado um evento `REWORK` no ciclo 2, revisão 3, com a URL,
imagem, testes e retorno à atividade `prototypeCorrection`. O aprendizado do #91 permaneceu
preservado. O registro não encerrou o ciclo nem ativou o experimento #92.

| Execução criada pela tela | Resultado persistido | Evidência |
| --- | --- | --- |
| #382 — Dédalo, atividade 3.6 | `COMPLETED`, decisão `READY` | Correção da v8 para v9; retorno obrigatório a `technicalHomologation` |
| #383 — Psique, atividade 3.5 | `COMPLETED`, decisão `APPROVED` | Cinco cenários `PASS`, 13 verificações verdadeiras e capturas #103–#107 |

A #383 executou desktop, iPhone, Pixel, recuperação no iPhone e segurança no Pixel. As quatro
gerações reais com `gpt-5-mini` terminaram com sucesso, request/response e tokens persistidos;
o caso fora de escopo foi bloqueado pela regra de segurança, sem chamada ao modelo. As cinco
sessões pertencem a `AGENT_VALIDATION`. Não houve sessão humana, cobrança, campanha ou gasto
de mídia. A auditoria não inventa custo monetário quando o provedor fornece apenas tokens.

O navegador conferiu novamente a tela publicada em desktop, iPhone e Pixel: #382 e #383
concluídas, ciclo 2/experimento #92/versão v9 visíveis, aprendizado anterior presente e botão
**Executar atividade** habilitado na **3.7 — Psique · cenário aderente**. Não houve erro de
JavaScript nem transbordamento horizontal. A captura real do protótipo em iPhone também foi
inspecionada visualmente.

O MCP confirmou #92 em `PLANNED`. A comparação dos campos comerciais do produto antes/depois
confirmou preservação da URL comercial v7, estado comercial, contrato e experiência anteriores.
A tarefa #380 permaneceu `BLOCKED` no histórico; não foi alterada artificialmente.

Relatórios JSON e imagens operacionais: `artifacts/vega380/production/` na sandbox. As tarefas
e capturas também permanecem auditáveis no Marketing Hub. A topologia local, o registry
temporário, os volumes e os processos de testes foram encerrados. Nenhum commit ou PR foi criado.

Limites: esta entrega conclui a correção 3.6 e sua homologação técnica. As avaliações funcionais
3.7–3.9, a integridade e os gates comerciais posteriores continuam sendo atividades próprias.
Os testes automatizados não substituem observação humana nem demonstram aumento de vendas.
