# Matriz de homologação — arquivos de comportamento no harness

## Escopo e critério de aceite

Validar a leitura somente leitura dos arquivos que definem ou restringem o comportamento de cada
agente em `GET /api/agents/{id}/details` e `/agents/:id/details`. A entrega é aceita quando os oito
agentes catalogados exibem exatamente os arquivos Markdown e JSON de seus diretórios
comportamentais, com conteúdo, versão, origem e SHA-256, sem mistura entre agentes ou exposição de
segredos.

## Cenários

| Área                   | Cenário                                               | Resultado esperado                                                                                 |
| ---------------------- | ----------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Caminho feliz          | Abrir o detalhe de um agente com harness completo     | A seção informa a contagem e lista todos os arquivos recolhidos por padrão.                        |
| Conteúdo Markdown      | Abrir prompt, núcleo ou biblioteca                    | Conteúdo integral legível, com quebra de linha e sem execução de HTML.                             |
| Conteúdo JSON          | Abrir schema de saída                                 | JSON válido aparece em árvore expansível.                                                          |
| Integridade            | Conferir caminho, conteúdo e hash                     | Conteúdo empacotado é idêntico ao arquivo de origem e o SHA-256 possui 64 caracteres hexadecimais. |
| Cobertura              | Comparar manifesto com os diretórios dos oito agentes | Nenhum `.md` ou `.json` elegível fica ausente ou aparece no agente errado.                         |
| Histórico              | Abrir arquivo legado da Psique                        | O arquivo continua visível e sua descrição informa que é histórico.                                |
| Falha de catálogo      | Solicitar agente sem manifesto                        | A tela informa ausência sem inferir arquivos.                                                      |
| Falha de empacotamento | Remover uma fonte declarada em cenário de teste       | O backend falha fechado ao carregar o catálogo.                                                    |
| Segurança              | Inspecionar conteúdo e renderização                   | Nenhum valor de secret ou raciocínio privado é servido; texto é escapado pelo React.               |
| Segregação             | Comparar dois agentes                                 | Cada resposta contém somente os arquivos vinculados à `agentKey` solicitada.                       |
| Observabilidade        | Inspecionar resposta administrativa                   | Contrato `agent-harness-v2`, tipo, origem e hash permitem rastrear a fonte exata.                  |
| Métrica                | Contar fontes versionadas                             | Cobertura esperada: 115 de 115 arquivos em oito agentes.                                           |
| Desktop                | Chromium 1366 × 900                                   | Leitura e expansão sem overflow horizontal.                                                        |
| Mobile                 | iPhone 15 Pro e Pixel 7 emulados                      | Resumo, badges, caminhos, hash, Markdown e árvore JSON permanecem utilizáveis sem overflow.        |

## Regra de rodadas

Como ajustes foram necessários durante a primeira execução dos testes de interface, executar duas
rodadas locais completas e consecutivas sem falhas após a última correção. Qualquer nova falha
reinicia a contagem.
