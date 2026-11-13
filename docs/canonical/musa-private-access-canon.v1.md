# MUSA — acesso privado de homologação v1

Decisão do usuário #3956, reafirmada em #3957 e em 13/11/2026.
Produto Vega/MUSA, cadeia 27, ciclo 22, experimento 115, `musa-consultor-pwa-v1`.

Cada emissão privada persiste `issuedAt` e `expiresAt = issuedAt + 90 dias` em UTC.
Autenticar, reabrir e consultar não renovam o prazo. Renovação explícita pelo operador
é permitida enquanto a homologação estiver aberta, preservando proprietário, dados,
limites, custos e histórico. Encerramento privado revoga todos os acessos da versão
no ciclo e impede reemissão. Ciclo encerrado ou identidade incompatível bloqueia toda
leitura e gravação, inclusive renovação. A sessão HttpOnly de 12 horas não define a
validade da emissão; reautenticação recupera os mesmos dados durante os 90 dias.

A regra não define assinatura, preço, franquia ou duração comercial. A oferta vigente
continua aplicativo de uso contínuo, base gratuita e premium opcional, com preço a
avaliar por Plutus. Preservar resultados #907–#914 e execuções concluídas.

Preparação utiliza o teto cumulativo aprovado de US$ 200, descontando consumo e
reservas anteriores; custos desconhecidos devem ser conciliados antes de gasto novo.
Não se autorizam cobrança, campanha, mídia ou produção audiovisual. Inferência
externa não integra esta candidata. Custo incremental de inferência zero não significa
custo integral zero. Provas sintéticas ficam em AGENT_VALIDATION/mh_internal_test e
não são evidência humana, comercial, venda ou aprovação independente.
