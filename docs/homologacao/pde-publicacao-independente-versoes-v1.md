# Homologação — publicação independente das versões PDE v1

Matriz definida antes dos testes em 18/09/2026. O escopo é o mecanismo compartilhado de publicação
da PDE Platform; não altera oferta, preço, público, checkout, experimento ou conteúdo comercial.

## Evidência de partida

- O backend confirmou Vega #4 como `PDE - Produto Digital Experiencial`.
- A inspeção somente leitura do host oficial encontrou v5, v6, v7 e v8 em containers e imagens
  próprios, mas o workflow removia e recriava backend e workers em toda publicação de frontend.
- O mesmo workflow removia a versão selecionada antes de comprovar a imagem candidata.
- O Watchdog selecionava apenas a candidata MUSA mais recente, sem comprovar as versões anteriores
  ainda suportadas.
- A recorrência já aparece em `LOOP-PSIQUE-MANIFESTO-NOVO-COM-PIXELS-ANTIGOS`.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Recriar apenas o serviço com `--no-deps` | Mudança pequena e isolamento das demais versões | A versão alvo sai antes da candidata ser validada; rollback depende de intervenção | Rejeitada |
| Roteador blue-green permanente por versão | Troca praticamente sem interrupção | Introduz nova camada crítica, migração do proxy e estado operacional adicional | Evolução futura |
| Candidata efêmera, promoção transacional e rollback pela imagem anterior | Pré-valida o artefato, preserva a versão anterior até a troca e cabe na topologia atual | Pequena janela de troca apenas na versão alvo | Adotada |

## Matriz de aceitação

| Área | Casos obrigatórios |
| --- | --- |
| Caminho feliz | Subir candidata da versão alvo, validar health/diagnóstico/API, promover a mesma imagem e emitir recibo |
| Isolamento | Atualizar v8 sem recriar v5, v6, v7, backend ou workers; atualizar backend sem recriar frontends |
| Falhas | Candidata inválida não toca a versão ativa; falha após a troca restaura a imagem anterior; target desconhecido bloqueia |
| Rollback | Preservar identidade, imagem e metadados da versão anterior; validar a restauração antes de encerrar com erro |
| Backend | Publicação separada; compatibilidade local com todas as versões MUSA suportadas antes da troca |
| Artefato | Commit, imagem imutável, fingerprint, contrato e SHA-256 do contrato coincidem no candidato e no recibo |
| Watchdog | Verificar v5, v6, v7 e v8 individualmente; uma falha identifica a superfície afetada |
| Observabilidade | Recibo por componente com imagem anterior/nova, IDs, contrato, horários, status e rollback |
| Regressão | Scripts com `bash -n` e ShellCheck; workflows com Actionlint; testes unitários dos módulos ajustados |
| Dispositivos | Jornadas integradas existentes em Chromium desktop, iPhone 15 Pro e Pixel 7 |
| Segregação | Dados locais/simulados; nenhum tráfego, venda, cobrança ou chamada paga de IA |

Depois da última correção encontrada durante a implementação, serão exigidas duas rodadas locais
completas e consecutivas sem falhas. Um novo defeito reinicia a contagem.

## Resultados

Duas rodadas completas e consecutivas foram aprovadas após a última correção, em 18/09/2026,
sem alteração de fonte entre elas.

| Verificação | Rodada 1 | Rodada 2 |
| --- | ---: | ---: |
| Backend PDE | 182 testes aprovados | 182 testes aprovados |
| Fingerprint frontend | 3 testes aprovados | 3 testes aprovados |
| Worker de IA / retenção | 10 / 4 testes aprovados | 10 / 4 testes aprovados |
| Watchdog de produção | 27 testes aprovados | 27 testes aprovados |
| Contrato de release / resolvedor | 7 / 14 testes aprovados | 7 / 14 testes aprovados |
| Inventário de runtimes / pacote comercial | 5 / 13 testes aprovados | 5 / 13 testes aprovados |
| Consistência pública / contrato integrado | 18 / 3 testes aprovados | 18 / 3 testes aprovados |
| Recuperação real de proxy backend | 11 testes e topologia aprovados | 11 testes e topologia aprovados |
| Jornadas completas de navegador | 39 aprovadas | 39 aprovadas |
| Build frontend Vega / Mira | aprovado | aprovado |
| Bash, ShellCheck, Actionlint, Python, JSON e diff | aprovados | aprovados |

As jornadas foram executadas em Chromium desktop, iPhone 15 Pro e Pixel 7. Elas cobriram Vega v5,
v6, v7 e v8, Mira, primeiro ajuste gratuito, checkout e acesso simulados, retomada, conclusão,
reembolso idempotente e segregação de métricas de teste.

A regressão transacional em Docker comprovou:

- candidata com fingerprint divergente recusada antes da troca;
- promoção exclusiva da v8 sem recriar v5, v6, v7, backend ou workers;
- identidade do novo artefato observada também pelo proxy público simulado;
- falha injetada após o cutover restaurando imagem, diagnóstico e rota pública anteriores;
- troca e rollback do backend sem recriar frontends, com recuperação de DNS nos proxies;
- atualização isolada do worker de IA;
- continuidade das demais versões durante a atualização de outra superfície.

Durante a implementação, antes das duas rodadas finais, foram corrigidos os seguintes defeitos de
causa-raiz encontrados pela própria matriz: janela de cache DNS do proxy, ausência de fingerprint na
imagem de Mira, atribuição incorreta da revisão do backend a imagens não publicadas, manifesto sem
vínculo semântico ao produto/target, ordenação lexicográfica após a revisão v9, falta de comprovação
da URL pública dentro da transação e ausência de uma suíte própria do worker de IA no workflow.

O pacote comercial vigente passou a ser
`pde-platform/contracts/musa-v12-commercial-homologation-v6.json`. Ele preserva produto 4,
experimento 92, experiência `musa-pde-entry-v12-primeiro-ajuste-aplicavel`, preço de R$ 67, checkout
e condições comerciais; sua publicação seleciona somente a superfície v8.

## Limites e estado operacional

- As dependências externas, pagamento, e-mail e tráfego foram simulados localmente; nenhum dado de
  teste foi contabilizado como venda, receita ou satisfação humana.
- Nenhuma chamada paga de IA, campanha, cobrança, PR ou publicação foi realizada.
- A execução produtiva #8 e a tarefa #449 permanecem no estado já publicado até a integração e o
  deploy oficiais. Esta homologação comprova a correção local e a prevenção de recorrência, não o
  estado futuro de produção.
- Todas as topologias temporárias usaram o projeto Compose exclusivo
  `aihub-190ea341-7c8a-4809-8c31-0b0e57914752-cc0ee64122` e foram removidas com volumes e órfãos.
