# Plano financeiro conforme o tipo do produto

## Diagnóstico de 20/11/2026

A tela de Vega e o banco concordam sobre o cadastro do produto 4. O tipo é
Turmalina (7), o contrato é `v2-consultor-continuo`, a economia registrada é
`FREEMIUM_OPTIONAL_SUBSCRIPTION` e o preço está em aberto. A revisão financeira
31 (revisão 7) ainda é do tipo Opala (1), por R$ 67 e 90 dias. O GET de
preparação priorizava essa revisão e bloqueava a nova estrutura. O parecer de
planejamento 908 do ciclo 22/experimento 115 continha ainda outra hipótese por
venda. Nenhum desses valores é uma mensalidade aprovada.

## Decisão e limites

Uma correção só em Vega repetiria o problema na próxima reclassificação. Alterar
as revisões existentes apagaria a unidade econômica usada nos pareceres. Foi
adotada a estrutura pelo código oficial do tipo, com revisão limpa e histórico
imutável. Não exige novo processo nem chamada de IA. A preparação pode ficar
parcial. A eventual contratação da assinatura é outra decisão, fora deste ajuste.

## Matriz de aceite local

| Percurso | Evidência verificável |
| --- | --- |
| Cadastro muda de tipo; envio duplicado | Evento de domínio uma vez por troca; um rascunho LIVE; cadastro sem plano comercial único não seleciona plano arbitrário |
| Produto já reclassificado | Abertura pela tela com versão atual e preço em branco; consulta explícita da revisão antiga por ID |
| Projeção parcial e completa | Três adesões mais sete renovações a R$ 20 produzem R$ 200; 40 pessoas gratuitas a R$ 0,50 custam R$ 20. CAC só nas adesões; teste numérico de contribuição, margem e equilíbrio |
| Entradas e concorrência | Volume fracionário e formulário do tipo anterior recusados; mensalidade ausente fica em aberto |
| Compatibilidade e isolamento | Outros dois produtos; plano anterior por venda; retorno ao tipo anterior sem reativar parecer histórico; TEST separado de LIVE |
| Integração | API administrativa, Hibernate, MySQL 5.7 e formulário real em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Agente e observabilidade | A revisão enviada a Plutus identifica unidade mensal, receita condicional e custo gratuito; ler e salvar preserva fila e número de execuções financeiras |
| Limite comercial | Somente fixtures locais. Nenhum checkout, modelo pago ou mídia. O teste não mede conversão nem venda |

A matriz reutiliza o runner em `infra/testing/product-financial-plan`. A regressão
`subscription-matrix.mjs` usa o mesmo servidor, a mesma persistência e o Plutus
simulado. Nos navegadores, acessos externos à API são recusados.

## Resultado observado na sandbox

A troca e o retorno passaram nos produtos sintéticos 95124, 95125 e 95126,
em Chromium desktop e nos perfis móveis emulados de iPhone 15 Pro e Pixel 7.
A preparação mensal incompleta ficou editável. O reenvio da mesma troca
preservou a quantidade de revisões, a simulação ficou isolada e a fila
financeira manteve as mesmas execuções.

No percurso preexistente, passaram 65 verificações de API, incluindo oito
requisições concorrentes, mais 26 verificações da preparação e 87 no navegador.
O cálculo, os contratos do agente e a tela também foram cobertos por testes
unitários. Passaram 233 testes no backend, incluindo as 92 verificações de
arquitetura existentes. O worker passou nos 90 testes da suíte e os componentes financeiros
do frontend passaram nos 43 testes selecionados. TypeScript e build passaram.
O pacote de evidências comerciais foi validado. As capturas declaradas no
manifesto local usam somente os produtos sintéticos.

A primeira execução remota encontrou indentação divergente no texto do ciclo.
A versão corrigida passou localmente no mesmo perfil Spotless aplicado a 161
arquivos do ciclo, no perfil dos 26 arquivos Java da entrega e em 44 testes
do resumo e da revisão financeira. O registro do
MySQL apontou também HTTP 429 ao baixar o POM do Spring no runner da migração,
antes de executar o SQL.

## Memória reutilizável de Plutus

Quando a unidade do plano mudar, usar o preço e os limites da revisão compatível
e qualificar o parecer antigo como histórico. A hipótese é reduzir a recorrência
de recomendações com a unidade errada e o retrabalho antes do mercado. Adoção
somente quando a origem estiver estruturada e a projeção distinguir base gratuita,
aquisição e renovação. Falha de identidade ou retorno de preço antigo reprova a
regressão. Revisões já aceitas permanecem disponíveis para auditoria.
