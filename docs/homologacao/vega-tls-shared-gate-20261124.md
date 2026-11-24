# Recuperação do gate TLS compartilhado — 24/11/2026

## Evidência e causa

A entrega de acesso #5881 está integrada na main
`48d6942d89a676d7330067c8f16ea677d40b912c`. Backend e workers foram publicados;
a superfície privada v10 também aplicou o fonte
`3fd96bb05b578118a9f6a7a14d9cef565720cffe86a9fce09fc01b66d213c8c5`.
O [deploy PDE](https://github.com/paulofor/marketing-hub/actions/runs/45411244614)
falhou depois, no gate HTTPS que preserva outros produtos do mesmo proxy.

Kit WhatsApp servia certificado vencido em20/11/2026 às20:55:05UTC. A linhagem em
disco e a servida têm a mesma impressão SHA256
`94FF5EAE5B5A2EEACBF0571EE7838E4712FB41ABD2B904BEA0383B73DA8A3360`.
O único SAN é o domínio já existente do Kit. Os volumes e a configuração webroot
foram conferidos somente por leitura, sem consultar chaves ou credenciais.
Clube MUSA, Mira e Alcyone responderam HTTPS confiável no mesmo diagnóstico.

Há duas causas compartilhadas: a manutenção só selecionava Clube MUSA, deixando o
Kit fora; e o último agendamento conferido parou antes de acessar o host, na geração
local de certificado vencido com duração negativa. O
[histórico da rotina](https://github.com/paulofor/marketing-hub/actions/runs/45319673214)
comprova a falha dessa fixture. O padrão portátil já existe no teste TLS do MCP.

## Alternativas e escolha

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Deploy completo de pagamentos com emissão do Kit | Comando já existe | Recria serviço alheio ao defeito e não corrige manutenção | Não adotada |
| Renovação manual no host sob coordenação | Recuperação imediata | Exige intervenção operacional e mantém recorrência | Não adotada |
| Corrigir a manutenção existente e executá-la pelo pipeline | Resolve certificado e recorrência com isolamento | Requer PR e testes do mesmo shell | Adotada |

O mesmo script aceita dois perfis fechados. O padrão anterior continua Clube MUSA;
Kit usa somente sua linhagem existente. O agendamento e `renew_tls` percorrem ambos
sob a fila já existente, sem novo workflow ou mudança de aplicação. Os testes de
CA permanecem obrigatórios no CI; a manutenção executa validação estática e os
preflights reais, evitando que uma fixture interrompa outra vez a renovação. Renovação,
instalação e reload permanecem restritos ao perfil selecionado. A confiança externa
continua obrigatória, com validade mínima de14dias. O registro anterior #5881,
a930, a925 e os aceites da103 permanecem preservados.

## Homologação local

- Matriz existente estendida ao segundo perfil: renovar vencido, recuperar reload
  perdido, idempotência, desafio/volume/proxy inválidos, falha da CA, validade curta,
  nome removido, outra linhagem, falha de cópia/reload e perfil desconhecido.
- Certificados reais emitidos por CA local; Docker/HTTP simulados. TLS local real
  recusa certificado vencido, nome errado, CA não confiável e validade insuficiente.
-25 testes passaram no OpenSSL local. A fronteira que recusa duração negativa
  reproduziu a falha antiga; os mesmos25 passaram com datas explícitas nessa fronteira.
- `bash -n`, ShellCheck, YAML e contratos existentes de transporte/seleção passaram.
- A suíte completa local de pagamentos passou:90 testes, zero falhas/erros/skips.
- Os testes não consomem a CA pública, provedores de IA ou dados comerciais.

A comprovação operacional posterior deve registrar workflow da renovação, confiança
HTTPS, validade do certificado novo e recuperação do gate da publicação. Nenhuma
saúde isolada substitui a conclusão desses passos. Não há alegação de venda ou
renovação de autorização comercial. A103 só será retomada após essa comprovação.
