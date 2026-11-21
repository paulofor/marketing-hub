# PDE Harness SDK Java

Biblioteca Java 21 que transforma o protocolo local do Codex App Server em um contrato de execução
segregado, auditável e orientado a Produtos Digitais Experienciais.

## Fronteira

- inicia `codex app-server --listen stdio://` como processo local;
- usa JSONL/JSON-RPC, sessão ChatGPT gerenciada pelo Codex e versão fixada do protocolo;
- cria ou retoma thread, inicia turno, valida a saída final contra o JSON Schema e entrega eventos e
  resultado tipados com modelo, versões e hashes;
- recebe em todo contato um snapshot de memória canônica do backend, com revisão, procedência,
  validade e limite de contexto; a memória é apresentada ao modelo como dado não confiável por um
  template versionado;
- exige o escopo em cada fato memorizado e rejeita snapshots que misturem itens de outro cliente,
  ainda que o envelope tenha sido rotulado com o cliente atual;
- vincula cada thread a `tenant + produto + versão + cliente + conversa`; um `threadId` isolado não
  é aceito para retomada, e revisão regressiva ou escopo divergente bloqueiam antes de carregar o
  histórico;
- deriva workspaces sem identificadores pessoais, permite apenas uma execução local por conversa e
  descarta o workspace da interação inclusive em falhas;
- recebe até oito imagens JPEG, PNG ou WebP por turno, valida tamanho, assinatura binária e SHA-256
  e entrega ao App Server somente uma cópia no workspace efêmero por `localImage`;
- devolve vínculo de thread atualizado e auditoria da memória por hash, além de oferecer exclusão da
  thread após o backend autorizar o esquecimento;
- remove chaves da OpenAI do ambiente filho e não possui fallback para API;
- valida workspace, schema estruturado, versão do Codex e integridade do bundle oficial;
- não acessa banco, não faz polling, não decide próxima etapa, não publica e não gasta.

O worker de cada PDE continua responsável por consumir o endpoint `pending`, carregar prompts,
schemas e memória autorizada, chamar esta biblioteca e reportar o resultado ao backend. O backend
persiste as interações e promove somente fatos duráveis para a próxima revisão de memória; o SDK não
acessa banco e o histórico da thread nunca é a única memória do cliente.

A consulta do backend deve filtrar o armazenamento pelo escopo exato antes de qualquer busca
semântica. `threadId`, vínculo e fingerprint são internos e nunca podem ser aceitos do frontend ou
do canal do cliente. Uma correção explícita atual do cliente substitui a memória conflitante na
próxima revisão, enquanto inferências permanecem identificadas por origem, confiança e validade.

## Perfil básico de consultores

O pacote `com.marketinghub.pde.harness.v1.consultant` oferece a base comum para Turmalina (PWA) e
Fluorita (WhatsApp). Ele associa o canal ao código canônico, compõe um envelope versionado com parte
do agente, parte da atividade e mensagem atual, carrega o schema de resposta do classpath e devolve
as três visões do prompt junto ao resultado auditável.

O envelope `consultant-envelope-v2` exige microvalor operacional, personalização baseada em dados
declarados ou autorizados, incerteza explícita, uma próxima ação e controle proporcional ao risco.
Ele impede que o perfil comum trate conselho genérico, inferência íntima ou ação externa não
confirmada como entrega válida; regras específicas continuam pertencendo ao worker do produto.

O perfil não contém a identidade de Amora ou de outro produto concreto. Cada worker especializa
agente, atividade, schema e regras do domínio, consome `pending` no próprio backend e reporta o
resultado. O kit React da PWA fica em `pde-platform/frontend/src/consultant-sdk/v1` e nunca chama o
App Server diretamente.

## Validação local

```bash
mvn spotless:check test
mvn -Pcodex-app-server-it verify
```

O segundo comando executa somente o handshake real `initialize`/`initialized`; não abre turno de
modelo e não gera conteúdo. O contrato fixado é o Codex CLI `0.149.0`.

## Atualização do protocolo

Antes de atualizar o Codex, gere o bundle com a nova versão, substitua o recurso versionado, atualize
o SHA-256 do manifesto e execute novamente todos os testes de contrato e o handshake real. Uma
incompatibilidade deve bloquear o SDK; nunca autoriza chamada direta à OpenAI API.

## Concepção e impressão plana — Jaspe v1

`harness.v1.manufacturing` adiciona uma base para o tipo
`AI_DESIGNED_MANUFACTURED_PRODUCT`, inicialmente **PROPOSED**. Não implementa um
executor de fabricação, integração comercial ou produto pronto para venda.

- `ManufacturingConceptionProfile` carrega prompt/schema do classpath; compõe o brief
  persistido e valida a saída contra escopo e SKUs. O worker usa o `PdeHarnessSdk` existente
  para inferência, com sua auditoria e custo; a biblioteca nova não abre chamada paga.
- `ManufacturingPrintPreflight.inspect` recebe bytes e hash do artefato, escopo esperado
  e escopo do artefato e uma `PrintSpecification` proveniente do backend. Mede pixels
  reais, proporção incluindo sangria, DPI efetivo, transparência e cor decodificada sRGB.
- `technicalCheckPassed()` permite seguir à **revisão técnica**. Não comprova impressão,
  direitos, autoria, aprovação visual, fidelidade de cor física, margem ou entrega.
  `productionAuthorized()` sempre retorna `false`.

O worker deve consumir contratos `pending` oficiais e reportar os resultados para persistência
no backend. A biblioteca não aceita URL nem faz download (o transporte autenticado fica no
executor), não cria diretórios de clientes, não grava banco, não envia pedido e não decide a
próxima etapa. PNG/JPEG são o recorte v1; PDF, CAD, faca, bordado e moldes exigem outro validador.
Arquivos acima de 25 MiB ou 40 milhões de pixels são rejeitados antes da decodificação completa.
A ficha precisa vir de uma fonte confiável: passar texto de um modelo como ficha não é homologação.

O perfil não detecta conteúdo ilícito, upscale artificial, qualidade estética, texto errado ou
marcas. Esses critérios continuam em revisão independente. O decoder pode normalizar perfis de
cor; a checagem é do espaço decodificado e não certifica ICC/PDF/X ou reprodução impressa.
Retentativa do mesmo arquivo é determinística e não gera nova inferência nem fabricação.

Execute `mvn -f pde-platform/pde-harness-sdk/pom.xml spotless:check test` na raiz. A suíte usa
imagens sintéticas e transporte falso nos testes do harness; não registra dados comerciais.
Veja a [pesquisa](../../docs/pesquisas/ia-concepcao-fabricacao-2026-11-21.md) e a
[matriz de homologação](../../docs/homologacao/ai-manufactured-product-v1.md).
