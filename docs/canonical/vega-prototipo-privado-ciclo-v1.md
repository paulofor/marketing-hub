# Vega: implementação privada por ciclo — v1

A correção do ciclo #2 segue o contrato aprovado nas tarefas #362, #366, #369 e #376.
A versão executável `musa-pde-entry-v9-primeiro-ajuste-aplicavel` sucede a especificação v8;
a v7 comercial e os resultados do #91 são preservados. O runtime tem superfície e imagem próprias.

## Responsabilidades e contrato

- Backend principal: persiste leitura, entrada, fila, callbacks, cartão imutável, sinais e auditoria.
  O contexto do ciclo publica a URL apenas após registro explícito da prova de implementação da
  mesma versão. O handoff fica no evento imutável REWORK; não sobrescreve o produto comercial.
- Executor `pde-platform/pde-vega-private-worker`: consome o `pending` canônico, reserva, registra
  request, executa a geração e devolve resultado. Polling, timeout e reenvio ficam nele. A caixa
  de saída persiste antes do callback e reenvia sem regeneração após indisponibilidade do backend.
- UI privada: ocasião, roupa disponível, cartão, compreensão, aplicação, autoavaliação, preferência,
  simulação e retomada. Nenhum prompt, custo, modelo ou metadado técnico aparece para a cliente.
- Dédalo: revisa a correção materializada e retorna à homologação técnica. Parecer de planejamento
  não comprova executável. Psique tem cenários próprios do Vega, nunca reutiliza o produto Mira.

## Resultado e limites

Uma microação usando somente itens já disponíveis, como aplicar, ocasião e autoavaliação.
O cartão completo é produzido por IA, com prompt/schema versionados, Flex e validação fechada.
Erro, resposta incompleta ou aquisição necessária não emitem VALUE_MOMENT. Tentativas anteriores
são preservadas; repetição de clique não inicia outra inferência enquanto a execução está ativa.

São cinco sinais: EXPERIENCE_STARTED, VALUE_MOMENT, READY_RESULT_USED, PREFERRED_OVER_FREE e
CHECKOUT_STARTED. Cada sinal exige ação explícita e seus pré-requisitos. A preferência pela
alternativa gratuita não registra preferência pelo cartão. Abertura da simulação não é compra.

Duas leituras humanas possíveis usam convites individuais, revogáveis e limitados a sete dias.
Nova autenticação preserva a leitura e invalida a sessão anterior. O banco armazena hashes dos
segredos; links usam fragmento removido imediatamente. QA_INTERNAL/AGENT_VALIDATION ficam
segregados de HUMAN, dos eventos da v7 e da contabilidade comercial. Os testes locais podem
simular HUMAN apenas no banco sintético. Homologação por agentes não substitui pessoas reais.

Custo monetário fica indisponível quando o provedor não o fornece; tokens e modelo são registrados.
O relatório não inventa custo realizado, preferência humana, receita ou validação econômica.
Teto de Plutus e aprovação para recrutamento/venda permanecem gates posteriores.

## Publicação e evidência

Imagens são construídas pelos Dockerfiles e Compose versionados neste repositório. A nova
superfície `/vega-private` deve ser roteada ao container próprio, sem alterar assets ou imagem da v7.
O registro pela tela do ciclo exige URL HTTPS sem parâmetros, versão nova, imagem, relatório,
data recente e confirmações dos testes. Ele libera a revisão da correção, sem aprovar o próprio
parecer, a homologação, contatos, campanha, cobrança ou gasto de mídia.

A tabela de testes e evidências fica em `docs/homologacao/vega-tarefa-380-prototipo-v1.md`.
