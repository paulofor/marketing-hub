# Governança de dados — MUSA v7

## Escopo e minimização

A MUSA v7 usa somente os dados necessários para entregar e retomar a experiência. Foto, medidas,
saúde, emoção, personalidade, raça, religião e texto livre não pertencem ao contrato das sete missões.
As escolhas categoriais são processadas por `MUSA_LOCAL_RULES_V1` e não são enviadas à OpenAI nem a
geradores de vídeo.

| Dado | Finalidade | Retenção operacional | Destino |
| --- | --- | --- | --- |
| E-mail | autenticação, retomada, suporte e entrega | durante o acesso e até 180 dias após expiração | backend PDE e transporte de e-mail |
| Escolhas categoriais | organizar os ajustes e preservar progresso | durante o acesso e até 180 dias após expiração | backend PDE |
| Missões e progresso | retomar a jornada e comprovar entrega | durante o acesso e até 180 dias após expiração | backend PDE |
| Estado do acesso | liberar produto e retomar a jornada | durante o acesso e até 180 dias após expiração | backend PDE |
| Auditoria financeira minimizada | impedir duplicidade e atender obrigações, com hash de compradora e acesso, sem e-mail ou token em claro | prazo legal e contábil aplicável | backend PDE e provedor de pagamento |
| Eventos técnicos first-party | segurança, falhas, funil e segregação de QA | até 180 dias | backend PDE |
| Pedido de suporte ou de direitos | atender a solicitação e manter trilha de resposta | pelo período necessário ao atendimento e à defesa de direitos | backend PDE e e-mail de suporte |

Encerrados os prazos, os dados que não precisarem ser preservados por obrigação legal ou defesa de
direitos devem ser excluídos ou anonimizados. A política não autoriza reutilização para treinamento de
modelo, inferência de perfil sensível ou venda de dados.

## Transparência e direitos

A experiência deve mostrar o aviso antes da captura de e-mail e dentro da área da cliente. A titular
pode exportar os próprios dados, corrigir o e-mail e solicitar exclusão aplicável dentro da área
autenticada; o endereço `contato@digicomdigital.com.br` permanece como canal alternativo. A
solicitação e a resposta ficam registradas. Limitação legal à exclusão deve ser explicada, sem
apresentar silêncio ou erro como atendimento concluído.

O suporte aceita mensagem voluntária de até 2.000 caracteres exclusivamente para atender o pedido.
A interface deve orientar a titular a não incluir saúde, intimidade, documentos ou outros dados
sensíveis e desnecessários. Essa exceção operacional não autoriza texto livre nas sete missões.

A rotina de retenção é acionada por executor autorizado em endpoint interno. Ao vencer o prazo, o
backend remove respostas, progresso, orientações e PII do funil, preservando apenas trilha anônima
necessária à auditoria. Não existe agendamento operacional dentro do backend PDE.

Na exclusão ou retenção, o token original também é invalidado e removido; a trilha técnica recebe
outro identificador aleatório e e-mail `privacy.invalid`, sem permitir correlação com a titular. URL,
IP, navegador, referência, sessão, visitante e metadados dos eventos são apagados. O executor externo
espera a saúde do backend e repete falhas transitórias com a mesma operação auditável antes de seguir
para o intervalo diário.

A trilha financeira legalmente necessária permanece separada: provedor, transação, produto, oferta,
valor, moeda, status e hashes não reutilizáveis. Ela não contém o e-mail nem o token em claro e não
autoriza reconstruir respostas, progresso ou perfil da cliente.

## Evidência e bloqueios

- tráfego `INTERNAL_QA` não compõe visitantes, vendas, receita ou prova comercial;
- uma degustação iniciada ou concluída nunca conta como compra;
- o produto deve bloquear chave ou valor fora das categorias versionadas;
- todos os sete dias devem concluir localmente com zero token, zero custo de modelo e nenhuma fila;
- acesso integral de QA exige segredo e trava local, fica desabilitado em produção e não registra
  compra ou receita;
- webhook só libera acesso depois de reconciliar status, oferta, valor exato e moeda diretamente na Pepper;
- produto e e-mail isolados nunca devolvem bearer token; retomada exige link mágico entregue ao
  endereço ou identidade Google verificada;
- catálogo público não contém missões, materiais pagos ou pacote científico, e downloads exigem
  acesso `ACTIVE`;
- qualquer reintrodução de IA, foto, texto livre ou vídeo gerado exige novo gate de privacidade, ledger
  por produto e revalidação da margem antes de publicação.
