# Homologacao comercial de Mira v1

Data: 2026-09-27

## Decisao e alternativas

Foram comparadas tres alternativas para publicar Mira:

1. marcar manualmente o slot como valido e criar apenas o checkout: baixo esforco, mas deixa a
   compradora sem entrega comprovada e perpetua a dependencia circular entre entrega e pagamento;
2. converter `mira-private-v3` diretamente em produto pago: esforco medio, mas mistura evidencia de
   pesquisa privada com trafego comercial e viola o contrato sem cobranca ja aprovado;
3. publicar `mira-commercial-v1` em superficie, imagem e dominio proprios, validar primeiro a
   entrega candidata, criar o checkout e concluir depois a validacao comercial completa.

A terceira alternativa foi escolhida por preservar a evidencia privada, tornar pagamento e entrega
auditaveis e permitir que a landing seja publicada sem liberar midia.

## Matriz obrigatoria antes da publicacao

| Area | Cenario | Aceite |
|---|---|---|
| Caminho feliz | pagamento Mercado Pago aprovado, e-mail da compra, link magico e rotina pronta | acesso liberado uma vez, valor de R$ 49 e experimento 93 correlacionados |
| Primeiro uso | informar produtos e orientacoes documentadas | rotina ordenada sem diagnostico, prescricao ou indicacao de nova compra |
| Validacao | e-mail sem compra, token invalido, rotulo insuficiente e objetivo clinico | acesso ou geracao bloqueados com causa clara |
| Falhas | checkout indisponivel, webhook repetido, reembolso e reinicio do servico | sem acesso gratuito, sem duplicidade financeira e reembolso encerra o acesso |
| Integracoes | Marketing Hub, pagamentos, PDE backend, SMTP descartavel e frontend | contratos usam produto 10, experimento 93 e `mira-commercial-v1` |
| Observabilidade | compra, acesso, momento de valor, primeiro uso, conclusao e reembolso | eventos persistidos sem e-mail ou bearer em metadados publicos |
| Metricas | QA e testes automatizados | `mh_internal_test=true`, e-mails `@sandbox.local` e exclusao do funil humano |
| Segregacao | Mira privada, Vega, Rigel e Mira comercial | nenhuma superficie, imagem, slot, evento ou entitlement e compartilhado indevidamente |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | checkout, login, formulario, resultado e politicas utilizaveis sem overflow ou erro de console |
| Video | demonstracao vertical de 18 segundos gerada pelo repositorio | reproduz em 1080x1920, comunica sem audio e nao exibe termos internos de homologacao |
| Publicacao | entrega, checkout e landing prontos; campanha sem verba | experimento permanece `PLANNED`, sem campanha, gasto ou autorizacao de midia |

## Evidências locais concluídas

- a suíte completa do backend administrativo aprovou 3.626 testes; o backend PDE aprovou 190
  testes e o serviço de pagamentos aprovou 51;
- a jornada Docker real aprovou nove cenários em Chromium desktop, iPhone 15 Pro e Pixel 7, com
  MySQL 5.7, SMTP descartável, serviço de pagamentos e reinício do backend;
- pagamento de R$ 49, retry idempotente, e-mail, link sem bearer na URL, duas organizações,
  bloqueio clínico, métricas de QA, privacidade, reembolso e revogação foram comprovados;
- o proxy local aprovou roteamento e reinício isolados de Mira privada, Mira comercial e Vega
  v5–v8, sem trocar containers alheios;
- o vídeo determinístico possui 18 segundos, 1080x1920, H.264/yuv420p e hashes SHA-256
  `2cc1063c4e07e11703ee2bcdbc544f05d27f404c9277a320799f8360b39393ae` para o MP4 e
  `7964614ac7a5c7e42006473be7c0deabff17f45281bd39ec05f30e9ab6d9a081` para o poster;
- o fingerprint imutável da fonte frontend homologada é
  `c32558dfd8437639e965e8744959e332a33075ac9eab1f396874c5620b3addfa`;
- o DNS `A` de `mira.digicomdigital.com.br` foi criado no Route 53 para `163.245.200.7`, sem
  `AAAA`, e a mudança `/change/C02050723IB1BSYFIIBPB` alcançou `INSYNC`.

## Evidências produtivas pendentes do fluxo de entrega

- certificado Let's Encrypt e proxy público publicados pelo workflow oficial;
- checkout real criado pela tela sem realizar compra;
- slot ativo com validação `OK`, landing publicada e experimento ainda sem campanha ou orçamento;
- SHA integrado, workflows, deploys e saúde das superfícies publicadas.
