# Vega ciclo 7: rota privada e validação de preparação

## Estado e causa observados

Solicitação #3275, Vega #4, ciclo #7, experimento #100. O teto autorizado permanece
US$ 10 no total da preparação; não é uma nova autorização. A versão executável é
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`, fonte
`16540344be571bf6e548c2f07714a6e5f4da71041914f91bfaee0576db060e7f`.

Após os PRs #5538 e #5540, o domínio v8 entregava a página privada e a identidade corretas,
mas o contrato privado respondia 502. A mesma consulta direta ao backend PDE na porta 8096
respondia 200. A configuração efetiva do proxy, consultada sem alteração operacional,
encaminhava `/api/pde/vega/private/v1/` ao container histórico do frontend, aposentado.
O gateway tipado já existente no backend PDE não era alcançado. A correção troca somente
o destino dessa API; mantém autorização interna, cabeçalhos privados e demais produtos.

Foram comparados três caminhos: reativar o frontend histórico manteria a dependência
aposentada e duplicaria o runtime; encaminhar ao backend principal pularia a fronteira
canônica PDE; encaminhar ao backend PDE reutiliza seu gateway tipado, com menor alteração
e sem nova infraestrutura. Foi escolhido o terceiro.

O workflow PDE `37727907678` também falhou ao exigir a oferta comercial de Vega. O backend
respondeu 412, `Experimento não está disponível para venda`, coerente com preparação privada,
sem janela ou autorização de venda. Não se alterou esse gate. Foram comparados três caminhos:
liberar a venda contradiria o escopo; remover todos os testes ocultaria defeitos; selecionar
um teste privado pelo contrato imutável verifica a capacidade realmente publicada e mantém
a validação comercial nos contratos comerciais. Foi escolhido o terceiro.

## Mudança reutilizável e limites

O manifesto sucessor de preparação v3 declara `PRIVATE_PREPARATION_READ_ONLY`. O precheck
exige produto, slot, fonte, rotas exatas, fixture determinística e ausência de autorização
adicional, mídia, pagamento e vídeo pago. Modo desconhecido e declarações inseguras falham.
Manifestos sem esse campo conservam o teste comercial anterior. Não há bypass por nome,
ID, erro HTTP ou estado do executor.

O novo teste de publicação consulta identidade, contrato e acesso interno sem credencial,
abertura da tela, cabeçalhos, layout e ausência de escritas. Executa em desktop, iPhone e
Pixel. Não cria sessão, tarefa, inferência ou callback, e não comprova demanda nem parecer
independente. As revisões reais de Psique e Têmis permanecem pendentes do fluxo oficial.

A fixture de API é construída pelo Dockerfile versionado, no Compose de isolamento existente.
Ela não usa bind mount dependente do host do daemon e não é imagem de produção. O teste
local usa o Nginx de produção e não contém o frontend histórico: a antiga rota falharia.

## Validação local proporcional

- Topologia real de proxy: contrato privado por v8 e v7 responde 200; fila interna sem
  credencial responde 403. Alcyone, Mira privada/comercial, demais versões Vega e trocas de
  containers foram conferidos pelo teste de isolamento existente.
- Playwright: três dispositivos passaram na imagem v8 construída pelo Dockerfile do repositório,
  com o proxy Nginx e fixture sem custo. Um relay HTTP apenas da sandbox adapta o TLS do daemon
  remoto ao Chromium; não altera produto, especificação ou configuração de produção.
- Quatorze testes do contrato de publicação passaram, incluindo casos negativos de rotas,
  autorizações e modo desconhecido. O teste shell verifica seleção privada e preserva o padrão
  comercial. Bash e ShellCheck validam os scripts alterados.
- A matriz funcional anterior de cinco cenários pelo backend/gateway reais, os nove testes
  conjuntos MySQL de Mira/Vega e as jornadas comerciais já aprovadas continuam válidos:
  esta alteração não muda cartões, geração, pixels, persistência, preço ou pagamento.

Atestações anteriores permanecem imutáveis. Sucessores de compatibilidade atualizam somente
hashes de fontes compartilhadas comprovadamente alteradas e mantêm deploy automático de
outros produtos desativado. Não são nova aprovação comercial ou técnica independente.
A publicação deve passar por novo PR e pipeline; depois, conferir a rota no domínio real,
registrar a prova pelo frontend e retomar a execução existente #57. Esta evidência local
não afirma que esses passos já ocorreram nem que houve venda ou lucro.
