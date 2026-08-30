# Governança de dados do Kit WhatsApp Pronto v1

## Escopo e fonte pública

Este contrato cobre `kit-whatsapp-pronto`, experimento 89 e experiência
`kit-whatsapp-pronto-pde-v2`. A identidade produtiva congelada é Digicom Digital, CNPJ
25.215.414/0001-69, suporte `contato@digicomdigital.com.br` e políticas públicas no domínio
`https://kit-whatsapp-pronto.digicomdigital.com.br`.

## Dados funcionais

- e-mail da compradora, confirmação autoritativa do pagamento e direito de acesso;
- respostas anonimizadas do briefing, progresso, entrega, primeira aplicação e suporte;
- solicitações de acesso, correção, exclusão ou anonimização;
- nenhum nome, telefone, endereço ou conversa identificável de cliente final é necessário.

## Telemetria técnica

As páginas podem gerar identificadores aleatórios first-party de visitante e sessão e registrar:

- página e origem da visita sem credencial, campanha e parâmetros UTM;
- seção, ação, material e tempo visível;
- tipo de dispositivo, navegador/user-agent, tamanho de tela e viewport;
- IP público resolvido pelo backend;
- classificação de tráfego humano, bot, plataforma, QA ou desconhecido.

O navegador não envia user-agent duplicado no JSON: o backend usa o header HTTP recebido. Bearer de
acesso nunca entra em URL, corpo, referrer, telemetria ou log; eventos comerciais guardam somente a
referência SHA-256 não reutilizável quando precisam correlacionar o acesso.

## Finalidade e segregação

Os dados servem para entregar e dar suporte ao produto, medir o funil, diagnosticar falhas e
desempenho, separar tráfego humano de automação/QA e proteger pagamento e acesso contra abuso.
Telemetria não envia WhatsApp, não autoriza contato e não transforma QA, parecer ou clique em venda.

## Retenção e direitos

Identificadores e contexto detalhado da telemetria são anonimizados depois de 180 dias. Permanecem
somente fatos agregáveis do evento, sem e-mail, acesso, IP, navegador, URL, referrer, sessão,
visitante, UTM, dispositivo, dimensões, seção, ação ou metadata bruta. Dados funcionais necessários
ao contrato e ao pagamento permanecem enquanto o acesso estiver ativo e pelo prazo legal ou
contábil aplicável.

A titular autenticada pode consultar os próprios dados, corrigir e-mail e solicitar exclusão ou
anonimização antecipada quando cabível. O suporte e esses direitos continuam disponíveis após
reembolso ou chargeback. Solicitações também podem ser feitas por
`contato@digicomdigital.com.br`.
