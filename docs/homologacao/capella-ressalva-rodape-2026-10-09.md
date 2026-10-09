# Capella — ressalva factual no cartão privado

Solicitação #3290, produto 7, ciclo 12, experimento 105, capella-private-v3.
Este registro complementa as entregas dos PRs #5553 a #5556; não reabre seu deploy.

## Causa confirmada

Íris #691 incluiu `footer=Sem garantia de clientes ou agendamentos.`. O PNG 595,
SHA-256 `1ef18f52650f0ebad9853ce4d0dc57fdc1dcec3b17988b29d84e64c14a4a4021`,
omitiu essa frase e imprimiu somente o aviso sintético. Psique #692 pediu o
restabelecimento da mesma ressalva. O renderizador substituía `footer` por uma
constante quando `privateValidation=true`. Os testes anteriores cobriam pixels,
recorte e rótulo, mas não a preservação do limite factual. A regressão nova falhou
antes da correção em dois domínios e na recusa de rodapé vazio.

O histórico de Mira já tinha registrado a sobrescrita de `eyebrow` pelo mesmo
template privado. Não é lacuna de estratégia nem falta de instrução para Íris.
Outra inferência sobre a mesma especificação não corrigiria o renderizador.

## Alternativas e escolha

| Alternativa | Benefício | Risco e esforço |
| --- | --- | --- |
| Linha factual separada em fonte menor | Preserva o quadro e o aviso existente. | Reduz a leitura da ressalva em celular; exige mais regras de tipografia. |
| Reorganizar prova, CTA e rodapé | Oferece mais linhas para os limites. | Altera a composição já homologada e reduz a área da prova; esforço maior. |
| Preservar os dois textos no rodapé com a mesma fonte | Conserva prova, CTA, hierarquia e aviso; alteração pequena. | Ressalva longa deve ser bloqueada, sem truncamento. |

Escolha: imprimir aviso privado e `footer` nos 98 pixels disponíveis após o CTA,
em fonte 36, com validação real de largura e altura. O caminho comercial conserva
seu rodapé e sua composição. Nenhuma exceção por produto ou ID é introduzida.

## Matriz local definida para a correção

| Critério | Fonte e validação | Aceite |
| --- | --- | --- |
| Caso original | Fonte aprovada 584 e renderSpec de #691; PNG e prévia a 393 px. | Ressalva e aviso privado íntegros; prova e CTA preservados. |
| Outra identidade/domínio | Fixture 910219 com limite educativo. | A ressalva altera os pixels; nenhuma seleção por nome/ID. |
| Caminho antes válido | Composição comercial, prova, rótulos, recorte e replay existentes. | Rodapé comercial preservado; unitários do worker sem falhas. |
| Entradas inválidas | Rodapé vazio ou que exige mais de duas linhas. | Bloqueio antes de persistir imagem; nenhum corte ou fonte reduzida. |
| Integração e recuperação | Materializador com HTTP local, hash, upload, callback e saída bruta preservada. | Peça concluída após armazenamento; replay sem nova inferência. |
| Observabilidade e finanças | IDs, fonte, hash e custos originais mantidos. | Sem aprovação fabricada, custo duplicado ou autorização adicional. |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7, com redução real da imagem. | Imagem íntegra e sem transbordamento lateral. |
| Segregação | Dados e servidores de teste locais; nenhuma composição de QA enviada à produção. | Sem campanha, compra, envio, mídia ou vídeo pago. |

## Resultados

- Antes da correção, a regressão teve três falhas: duas identidades com rodapé
  ignorado e ausência de erro para ressalva vazia. O caminho comercial continuou válido.
- Depois da correção: 78 testes do worker, zero falhas/erros e duas condições
  opcionais não aplicáveis à rodada padrão. O replay com fonte externa fornecida
  foi executado separadamente e passou; o outro cenário opcional depende de uma
  entrada específica de Vega, não alterada neste PR.
- Renderização real da especificação original #691 sobre a fonte 584 aprovada:
  PNG 1080 × 1350 com aviso privado e ressalva factual completos. Chromium em
  desktop, iPhone 15 Pro e Pixel 7 preservou a imagem; largura mobile 393/412 px,
  sem transbordamento lateral. Nenhuma imagem local foi enviada à produção.
- O JAR empacotado contém exatamente os 34 arquivos compilados: 24 classes e
  dez recursos, com igualdade dos bytes. SHA-256 do JAR local:
  `fe1d05555a03e3ed2ff05237b07678faa394da112bead197bbbe065ffcecaa1d`.
- Os testes existentes de HTTP exercitaram armazenamento, callback, saída bruta,
  replay sem inferência e falha de upload. O rodapé longo é recusado por largura
  ou altura efetiva; não há truncamento ou redução de fonte.
- A correção de Íris #693, PNG 596, incorporou a ressalva no texto principal e
  recebeu `APPROVED` de Psique #694. Preservar essa entrega e seu custo, sem
  regenerá-la para comprovar a alteração do template. Têmis ainda precisa revisar
  os mesmos pixels antes do aceite privado do operador.
- Pela tela, os runs 65/64 foram pausados preservando a conclusão de #694; não
  houve cancelamento da inferência em curso. Consumo conhecido desta autorização:
  US$ 7,541778 estimados, com US$ 2,458222 de saldo estimado. A homologação
  determinística registra zero chamadas de provedor e custo geral não informado;
  esses valores não são fatura conciliada nem prova de venda ou contribuição.

Publicação seguirá PR, revisão e merge; acompanhar a imagem testada e a saúde de
Íris antes da retomada pela interface. Não alterar o experimento histórico nem
reaproveitar parecer sobre pixels diferentes como aprovação da peça corrente.
