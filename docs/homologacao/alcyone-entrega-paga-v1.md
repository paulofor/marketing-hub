# Alcyone — entrega visual após compra, v1

## Escopo e alternativas

A raiz atual é a homologação privada v3, aprovada; ela não é checkout nem área da compradora. O experimento 97 está PLANNED. Perfil público e orçamento de mídia continuam sem decisão. Essa ausência não impede implementar a entrega localmente.

Foram comparados: adaptar a amostra gratuita (inverte a oferta aprovada), duplicar a geração inteira (maior esforço e risco de divergência) e reutilizar geração/auditoria com uma fronteira paga no backend principal. Escolhida a terceira opção: pedido e compra próprios, preservação do executor e da recuperação existentes. Nenhuma compra ou cobrança real é simulada como evidência comercial.

## Matriz de aceite definida antes dos testes

| Área | Critério |
|---|---|
| Compra | Apenas callback interno com pagamento aprovado, preço, produto, experimento e versão exatos libera acesso; pendente, valor errado e outra identidade são recusados. |
| Acesso | Link enviado ao e-mail da compra, bearer fora de URLs HTTP e logs; acesso desconhecido, vencido ou reembolsado não entrega dados. |
| Entrega | Ocasião, peças e restrições produzem três combinações; recuperação usa a mesma imagem; uma regeneração explícita e limitada. |
| Falhas | Callback duplicado/conflitante, resposta inválida, custo desconhecido, concorrência e falha de comunicação preservam resultado/auditoria e não repetem inferência. |
| Integrações | Pagamento e provedor substituídos localmente; e-mail sandbox; backend controla fila e aceites. |
| Métricas | Compra, versão, job, entrada, saída, custo estimado e reembolso vinculados; QA separado de receita real. |
| Dispositivos | Chromium desktop, iPhone e Pixel emulados; carregamento, envio, erro, recuperação, layout e imagem. |
| Regressão | Caso Alcyone, outro produto/experimento e caminho privado antes válido. |

## Limites

Não transfere orçamento entre produtos, não remove a proteção privada, não ativa anúncios e não fabrica homologação comercial. Custos estimados de entrega devem entrar no parecer econômico antes da venda. Testes comprovam capacidade, não demanda, margem ou vendas.

## Evidência executada em 21/10/2026

- Backend principal: 75 testes relevantes aprovados, incluindo o percurso privado antes válido, versão de outra identidade, callback duplicado, reembolso, expiração, limite de regeneração e conferência da superfície publicada. Pagamento confirmado após pausa/encerramento conserva preço e contrato da versão vendida.
- Navegação real contra controllers e executor locais: desktop, iPhone e Pixel; compra simulada → acesso → entrada → worker → imagem → recuperação → regeneração. Seis gerações substituídas localmente, zero chamadas externas. O pagamento simulado existe somente no fixture local; o módulo produtivo exige `live_mode=true` no recibo consultado no provedor e não confia em metadata para isso. A imagem preta usada pelo double é uma fixture estrutural, não peça comercial.
- SMTP: envio efetivo somente para `teste+6a097098-paid-visual@sandbox.local`, confirmado na API da caixa descartável. O SMTP real não foi utilizado para teste.
- MySQL 5.7: DDL incremental, inserção e índices executados na topologia isolada; volumes removidos ao encerrar.
- Pagamentos, gateway e worker: confirmação HTTP de duas identidades, isolamento de credenciais, contratos, respostas e recuperação. O gateway público não expõe a confirmação financeira interna.
- Versão produtiva: campo de endereço completo usa o endpoint existente; a verificação técnica da entrega paga deixa de exigir rotas de amostra e missões que não pertencem ao contrato comprado. A validação comercial exige checkout, suporte e políticas disponíveis, além dos pareceres externos desta etapa.

## Aprendizado reutilizável e adoção

Causa observada: a homologação privada provava geração e revisão, mas não implementava o direito adquirido pela compradora nem a passagem da entrega paga pelo cadastro de versões. A hipótese é reduzir falhas e tempo entre compra confirmada e entrega utilizável. A mudança v1 vincula pagamento, versão, tentativa, imagem e custo; o custo da cliente não consome o teto da preparação.

Um segundo achado local foi a disputa entre executores: uma recusa de auditoria não pode sobrescrever a tentativa já iniciada. O request auditado e o recibo preservado são reutilizados, com regressão privada e paga.

Adotar somente após o PR, a publicação técnica e os pareceres financeiros/comerciais da versão. Reverter a liberação de novas compras diante de falha de acesso, divergência de identidade ou custo sem evidência, preservando compras e artefatos anteriores. Acompanhamento real deve medir tempo compra→entrega, recuperação, custos e contribuição; nenhum aumento de venda foi demonstrado nesta homologação.
