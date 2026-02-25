# Pipeline para geração das imagens sem Imagem Origem

Este fluxo descreve o tratamento completo de pedidos em que o lead não envia uma imagem inicial. Todo o processo parte apenas das informações preenchidas no formulário, transformando o briefing em imagens finais prontas para entrega.

## 1) Lead preenche o formulário com briefing completo
- **Entrada:** dados estruturados e campo aberto de descrição fornecidos pelo lead (estilo desejado, contexto, referências textuais, cores, público etc.).
- **Saída:** requisição registrada com o briefing textual armazenado e vinculado ao lead.

## 2) Curadoria transforma o briefing em instruções para IA
- **Processo:** equipe ou rotina automatizada interpreta os dados do formulário, resolve dúvidas pendentes e estrutura prompts e parâmetros (modelos, estilos, quantidade de variações) para a geração das imagens.
- **Saída:** conjunto de prompts e presets aprovados para execução.

## 3) Worker IA gera todas as variações a partir do briefing
- **Processo:** o worker de IA consome os prompts preparados e gera as imagens solicitadas, sem depender de uma imagem de origem.
- **Saída:** lote inicial de imagens produzidas diretamente a partir do texto, ainda sem marca d'água.

## 4) Etapa de marca d’água
- **Processo:** aplica-se a marca d'água institucional em todas as imagens geradas para proteção antes do envio da amostra.
- **Saída:** versões das variações com marca d'água.

## 5) Compactação para envio da amostra
- **Processo:** as imagens com marca d'água são compactadas (ZIP) para otimizar o envio e controlar o tamanho do anexo.
- **Saída:** arquivo compactado contendo as amostras.

## 6) Geração do link de pagamento
- **Processo:** o sistema produz o link de pagamento referente ao pacote solicitado, pronto para ser inserido no e-mail de amostra.
- **Saída:** URL rastreável associada à proposta enviada ao lead.

## 7) Envio do e-mail de amostra
- **Processo:** o lead recebe o e-mail com o arquivo de amostras (com marca d'água) e o link de pagamento gerado.
- **Saída:** lead instruído a efetuar o pagamento para acessar o pacote completo.

## 8) Entrega final após pagamento
- **Processo:** após a confirmação do pagamento, o lead recebe um novo pacote com todas as imagens finais, agora sem marca d'água.
- **Saída:** entrega definitiva das artes adquiridas, geradas integralmente a partir do briefing textual.
