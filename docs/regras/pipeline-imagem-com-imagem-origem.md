# Pipeline para geração das imagens com Imagem Origem

O fluxo abaixo descreve, de ponta a ponta, como ocorre o tratamento das imagens enviadas pelos leads para geração das variações finais utilizando uma imagem de origem. O objetivo é deixar explícitos os responsáveis e o resultado esperado em cada etapa, sem alterar a ordem do pipeline estabelecido.

## 1) Lead envia imagem no Form Lead Portal
- **Entrada:** imagem original anexada pelo lead no formulário do Lead Portal.
- **Saída:** requisição registrada com a imagem armazenada no bucket configurado para processamento.

## 2) Worker IA gera as variações
- **Processo:** o worker de IA consome a imagem enviada e gera as variações necessárias conforme o prompt/regra cadastrada.
- **Saída:** conjunto de imagens derivadas, ainda sem marca d'água.

## 3) Etapa de marca d’água
- **Processo:** aplica-se a marca d'água institucional em todas as imagens derivadas para proteção antes do envio da amostra.
- **Saída:** versões das variações com marca d'água.

## 4) Ocorre a compactação para envio de amostra ao lead
- **Processo:** as imagens com marca d'água são agrupadas e compactadas (ZIP) para facilitar o transporte e controlar o tamanho do anexo.
- **Saída:** arquivo compactado contendo as amostras.

## 5) Link de pagamento gerado para colocar no e-mail
- **Processo:** o sistema gera o link de pagamento correspondente ao pacote solicitado, que será informado no e-mail de amostra.
- **Saída:** URL rastreável pronta para inclusão na comunicação.

## 6) Email de amostra enviado para o lead com link de pagamento
- **Processo:** o lead recebe o e-mail contendo o arquivo de amostras (com marca d'água) e o link de pagamento gerado na etapa anterior.
- **Saída:** lead orientado sobre como efetuar o pagamento para receber o pacote completo.

## 7) Após pagamento envio de pacote com as imagens sem marca d’água
- **Processo:** mediante confirmação do pagamento, o lead recebe um novo pacote contendo as imagens finais sem marca d'água.
- **Saída:** entrega definitiva do material adquirido.
