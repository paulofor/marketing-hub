# Actions — evidências do catálogo compartilhado — 2026-09-10

## Diagnóstico confirmado

- Base local: `35584befc3ffac267a02082edaff2bf17f1afb7d`.
- [Run aprovado 34460402227](https://github.com/paulofor/marketing-hub/actions/runs/34460402227): Têmis passou na revisão `1c491da7`.
- [Run de PR 34464419688](https://github.com/paulofor/marketing-hub/actions/runs/34464419688) e [run de main 34466059243](https://github.com/paulofor/marketing-hub/actions/runs/34466059243): `PdeReviewArtifactLoaderTest.validatesCurrentRepositoryHomologationManifest` falhou por SHA-256 divergente de `ProductCatalogService.java`.
- A comparação `1c491da7...51da6964` mostra uma única mudança nesse arquivo: a identidade privada de Mira passou de `mira-private-v2` a `mira-private-v3`. A atestação v6 do Rigel continuou declarando o hash anterior do catálogo compartilhado.
- O gate de Têmis detectou corretamente a divergência. A matriz local de recuperação de Mira verificava PDE e Psique, mas não verificava as provas comerciais consumidas por Têmis. O empacotador também não comparava os hashes das evidências vigentes antes de produzir o pacote.

## Alternativas consideradas

| Alternativa | Benefício | Risco | Esforço | Decisão |
|---|---|---|---|---|
| Nova atestação de compatibilidade e validação no empacotamento e na matriz local | Preserva a história e detecta impactos entre produtos antes da publicação | Exige revalidar dependências compartilhadas a cada alteração | Baixo | Escolhida para fechar esta falha |
| Extrair todos os catálogos por produto e reatestar suas dependências | Reduz alterações em arquivos compartilhados | Refatoração de runtime amplia o escopo e ainda requer validar dependências comuns | Alto | Não necessária para corrigir este Action |
| Implantar inventário completo de dependências e matrizes seletivas por produto | Automatiza a seleção de todos os testes afetados | Exige governar um novo inventário e evitar omissões | Alto | Evolução futura, além deste ajuste |

Não modificar atestações históricas, remover validação de hash, fabricar aprovação comercial ou publicar para descobrir o próximo erro.

## Matriz definida antes dos testes

| Controle | Critério de aceite |
|---|---|
| Reprodução | O teste original falha pelo mesmo arquivo observado no GitHub; empacotamento anterior não detecta a divergência |
| Contratos do empacotador | Aceita a revisão vigente íntegra e preserva versões históricas; recusa hash incorreto, arquivo ausente, caminho indevido e revisão vigente ambígua |
| Catálogo PDE | Suíte local do backend PDE aprovada, incluindo identidade de Mira, oferta e contrato do Rigel |
| Têmis | `mvn verify` completo aprovado, incluindo carregamento e segregação das provas do repositório e do pacote |
| Psique | Suíte Java e leitura das evidências aprovadas após empacotamento pelo contrato compartilhado |
| Integração e observabilidade | Pacotes de ambos os agentes preservam índice, caminhos, hashes e histórico; erro identifica produto, manifesto e arquivo |
| Workflows | Teste preventivo executado pelos workflows afetados; Actionlint e contratos de coordenação aprovados |
| Navegadores e métricas | Jornadas locais existentes de Rigel em Chromium desktop, iPhone 15 Pro e Pixel 7; integrações simuladas/locais, SMTP sandbox e nenhum dado comercial produtivo |
| Artefato final | Imagem do revisor construída pelo Dockerfile versionado; pacote conferido dentro da imagem e runtime visual validado |
| Proxy após deploy | Troca real de IP no Docker: proxies novos recuperam automaticamente, legado reproduz 502 e recupera pelo script de deploy; URI, query, POST, autorização de materiais, limites entre Mira/Vega, imagem e container preservados |
| Entrega | Duas rodadas locais completas consecutivas sem falhas após a última correção; diff íntegro, recursos temporários removidos |

## Segundo erro identificado durante a investigação

O [run PDE 34466059383](https://github.com/paulofor/marketing-hub/actions/runs/34466059383)
passou em testes/build/deploy e falhou no smoke público do MUSA com HTTP 502. Pelo MCP, o backend
iniciou em `10:30:51Z` e estava saudável em `10:31:05Z`. A inspeção SSH somente leitura confirmou
backend atual `172.18.0.4`, enquanto o log do Nginx do frontend v7 registrou conexão recusada para
`172.18.0.2:8096`. GET direto na API respondeu 200; GETs públicos em v5 e v7 responderam 502.

O deploy `push` atualiza o backend e preserva as imagens dos frontends. Recarregava o proxy HTTPS
externo, mas não os proxies internos dos frontends que ainda mantinham a resolução antiga.

Alternativas: (1) recarregar proxies existentes após backend saudável — resolve também imagens
legadas, baixo esforço, exige governança do deploy; (2) DNS dinâmico no Nginx — recuperação automática
para imagens futuras, baixo esforço, exige validar URI e auth; (3) IP estático para o backend — evita
troca, mas aumenta acoplamento à rede e risco em migrações, esforço médio. Escolhidas (1) e (2) em
conjunto, com testes de reconexão real e sem substituir versões de produtos não selecionados.

Referências primárias: [Nginx proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)
e [DNS interno do Docker](https://docs.docker.com/engine/network/#dns-services).

## Resultado

**Duas rodadas locais completas e consecutivas aprovadas**, `validated-1` e `validated-2`, com
**15/15 controles em cada rodada**, depois do último ajuste:

- 169 testes do backend PDE, 89 de Têmis e 89 de Psique por rodada;
- 11 testes do empacotador e repetição dos 10 testes do carregador Java sobre cada um dos dois
  pacotes de evidência, com 120 arquivos e 12 manifestos por pacote;
- nove cenários de acesso, políticas e validação em Chromium desktop, iPhone 15 Pro e Pixel 7,
  mais seis cenários de analytics, primeiro resultado útil e bloqueio de contrato comercial;
- MySQL 5.7 local, API PDE e fonte de contrato simulada; tráfego interno segregado e SMTP sandbox;
- imagem de Têmis construída pelo Dockerfile versionado, integridade do pacote dentro da imagem,
  captura real de landing/checkout e extração de vídeo no runtime confinado;
- troca real de IP entre backends locais, recuperação automática dos proxies novos e recuperação
  do controle legado 502 pelo script do deploy; URI/query, POST e autorização de materiais
  preservados, fronteiras Mira/Vega respeitadas e proxy de outro contexto intocado;
- Actionlint fixado pelo repositório, ShellCheck, coordenação dos agentes, seleção das superfícies
  de deploy, isolamento por produto e revisão do diff aprovados.

As atestações v5 e v6 foram comparadas byte a byte ao `HEAD` e permanecem intactas. A v7 registra
somente a revalidação de compatibilidade; nenhuma venda ou aprovação comercial foi criada.

Houve ajustes na própria topologia de teste antes dessas duas rodadas: MySQL foi colocado em
`tmpfs` para reduzir uso do disco local; a simulação de IP passou a usar uma sub-rede declarada e
a manter o endereço antigo ocupado sem a API, reproduzindo conexão recusada (502) em vez de timeout
de endereço vazio (504). As sondas do teste usam conexões novas após reload para não reutilizar
sockets encerrados do Nginx. Essas tentativas anteriores não contam como rodadas aprovadas.

Logs e resumo executável: `artifacts/actions-evidence-2026-09-10/summary.json`,
`validated-1/results.tsv`, `validated-2/results.tsv` e seus logs por controle (arquivos locais ignorados
pelo Git). O relatório versionado preserva o diagnóstico, os critérios e os resultados.

Comando de reprodução, após instalar as dependências dos módulos e as ferramentas de validação:

```bash
EVIDENCE_COMPOSE_PROJECT=<projeto-exclusivo-da-sandbox> \
  bash scripts/run-docker-homologation.sh \
  bash infra/testing/commercial-evidence/run-local.sh rodada-1 rodada-2
```

Containers, redes e volumes temporários foram removidos com `down --volumes --remove-orphans` no
projeto `aihub-32e4b595-a935-404e-a736-b3752a7b1179-194f4aeb55`; o wrapper removeu as referências
temporárias das imagens da sessão. A sandbox começou com menos de 1 GiB livre; a adaptação local
permitiu concluir os controles sem apagar imagens alheias ou caches globais.

O deploy central `34466059254` terminou verde; a continuação de Têmis `34467401298` apenas propagou
corretamente a falha do run de origem `34466059243`, sem uma terceira causa. Nenhum workflow foi
reexecutado para testar a correção. As mudanças permanecem locais, **sem commit, PR ou publicação**;
os Actions históricos e a correção do acesso público dependem da publicação pelo fluxo do usuário.
