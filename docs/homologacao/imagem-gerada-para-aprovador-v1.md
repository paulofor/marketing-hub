# Homologação — imagem gerada para o Aprovador v1

## Objetivo

Validar que uma imagem escolhida no Gerador vira um criativo auditável e entra na fila do Aprovador sem publicar anúncio, retomar campanha ou gerar gasto.

## Matriz ponta a ponta

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Selecionar imagem, preencher a mensagem e enviar | Asset persistido, criativo `DRAFT` criado e revisão `PENDING` solicitada |
| Validação | Não selecionar imagem ou experimento | Envio bloqueado na interface |
| Falha | Upload, criação ou solicitação falhar | Erro visível e ausência de confirmação falsa de conclusão |
| Integração e observabilidade | Conferir as três chamadas oficiais | Upload precede criação, que precede solicitação ao Aprovador |
| Métricas e segurança | Conferir efeitos comerciais | Nenhuma publicação, alteração de campanha ou gasto |
| Segregação | Usar o experimento selecionado | Asset, criativo e invalidação de cache vinculados ao mesmo `experimentId` |
| Navegadores e dispositivos | Desktop, iPhone 15 Pro e Pixel 7 | Seleção, formulário e confirmação legíveis e acionáveis |

## Regra de aprovação local

Executar teste funcional, TypeScript e build de produção em cinco rodadas completas e consecutivas. Qualquer defeito reinicia a contagem. A validação visual em navegadores e dispositivos deve ocorrer contra o ambiente local quando o backend e o storage estiverem disponíveis; caso contrário, fica como critério obrigatório após o deploy, antes de liberar mídia.
