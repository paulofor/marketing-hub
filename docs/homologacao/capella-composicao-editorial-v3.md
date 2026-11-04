# Capella — composição editorial v3

## Evidência e escolha

Em 04/11/2026, a homologação técnica 896 aprovou os nove cenários da candidata
`capella-beleza-v3`, ciclo 19, experimento 112. A revisão independente 897 rejeitou
o acabamento: post e story receberam 3/5 em profissionalismo e valor percebido;
o story também recebeu 3/5 em legibilidade. As capturas 1475 e 1476 confirmam
negrito em todos os níveis e informações secundárias comprimidas no rodapé.
O compositor `ReferenceKitArtwork.fit` aplicava `Font.BOLD` a todo texto, enquanto
o story ampliava quase somente a fotografia. A biblioteca pertinente não é a causa.

Foram comparadas três alternativas: trocar as fotos (não corrige hierarquia),
alterar silenciosamente o compositor v2 (viola a identidade histórica) e criar
uma composição editorial versionada com o mesmo acervo (adotada). A família
`beauty-v3` separa os níveis tipográficos e reserva espaço próprio ao contato e
à atribuição. Mantém mensagem, convite, fotos e licença; não cria oferta nova.

## Matriz de aceite definida antes da execução

| Área | Aceite |
| --- | --- |
| Composição | Seis especialidades; post 1080 × 1080 e story 1080 × 1920; nome, serviço, mensagem, contato e origem legíveis e sem sobreposição |
| Limites | Textos longos tratados sem cortar contato ou atribuição; entrada impossível recusada explicitamente |
| Experiência | Prévia, seleção de especialidade, ampliação, geração, download e recuperação da candidata nova |
| Integração | Compositor real, contratos reais do backend e harness; serviços externos simulados, sem inferência paga |
| Isolamento | Caso original, outro produto/execução e caminhos v1/v2; artefatos e autorizações históricos preservados |
| Observabilidade | Perfil, versão da composição, fontes, hashes e causa de falha auditáveis; 36 arquivos úteis, sem metadados técnicos no ZIP |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; leitura das peças em aproximadamente 400 pixels de largura |
| Comercial | Testes segregados; zero geração paga, pagamento, campanha ou afirmação de impacto em vendas |

## Aprendizado reutilizável e limites

Lacuna observada: dimensões corretas e fontes licenciadas não asseguram hierarquia
visual nem leitura mobile. O compositor deve preservar áreas e tamanhos mínimos,
e a revisão independente continua avaliando os pixels com o mesmo mínimo 4/5.
Os testes locais comprovam comportamento e ausência dos defeitos determinísticos;
não fabricam um parecer de Psique nem comprovam conversão.

A mudança exige sucessor pelo `CHANGE_PER_CYCLE_V1`. A versão anterior, a revisão
897 e seus custos são preservados. Adoção exige nova homologação; autorizações
financeiras não são transferidas automaticamente. Reversão mantém a família v2
disponível, sem reescrever sessões existentes.

## Resultados

- Compositor: 90 testes aprovados; seis especialidades geraram pacotes reais de 36 arquivos,
  abaixo de 32 MiB, com replay sem geração paga. Nomes longos e entradas impossíveis cobertos.
- Backend: 88 testes do kit e 111 de entrada/planejamento/arquitetura aprovados (92 ArchUnit).
- Psique: 257 testes, 244 executados sem falha e 13 integrações condicionais não habilitadas
  nessa suíte; integração dedicada executou mais quatro testes com navegador real e modelo simulado.
- Frontend: quatro testes e TypeScript aprovados. Navegador: 70 casos aprovados; os 12 casos
  que dependem do diretório do módulo foram executados novamente no diretório correto.
  A rodada paralela excedeu o limite de processos; a validação final foi serial.
- Fluxo local: nove cenários/dispositivos da família v3, outro produto com cabelo e um
  caminho legado de barbearia passaram. Foram preservadas 29 capturas nessas três execuções.
  Três callbacks de revisão, Têmis e gate reais passaram com modelo e cadastros simulados.
  [Resumo das execuções](evidencias/capella-editorial-v3/functional-summary.json).
- Inspeção visual: 30 posts, histórias e leitura a 393 pixels; o primeiro recorte quadrado
  escondia parte do serviço e foi corrigido ainda na sandbox, separando fotografia e texto.
  O story preserva área própria para título, convite, contato e atribuição.
- Ambiente: Docker não respondeu à versão/limpeza, confirmado por timeouts. O percurso
  funcional usou H2 explicitamente no runner existente; não comprova MySQL 5.7. O runner
  MySQL/Liquibase do PR mantém a verificação física, sem mudança nos changelogs.
- Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; Safari nativo não executado.

O relatório funcional conserva identificadores sintéticos e zero chamada a provedor.
A revisão visual real 897 continua rejeitada; os testes não substituem o parecer independente.
A capacidade nova ficará disponível para a candidata `capella-beleza-v4`, em sucessor.
Não houve autorização comercial, aumento de teto ou conclusão de experimento por estes testes.
