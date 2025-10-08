--liquibase formatted sql

--changeset repo:2026-08-30-seed-lifecycle-lead-ads-journey-template dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM journey_template WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d';
INSERT INTO journey_template(name, description, objective, preferred_channel)
VALUES (
    'Lifecycle Pós-Clique Lead Ads 14d',
    'Blueprint pós-clique inspirado em Lead Ads do Facebook para transformar cliques em relacionamento multicanal de 14 dias com foco em canais próprios e mensuração completa.',
    'Converter curiosidade em relacionamento contínuo com canais próprios de baixo custo',
    'EMAIL'
);
SET @template_id = LAST_INSERT_ID();

INSERT INTO journey_template_phase(template_id, phase_order, phase) VALUES
(@template_id, 1, 'ATTENTION'),
(@template_id, 2, 'INTEREST'),
(@template_id, 3, 'DESIRE'),
(@template_id, 4, 'ACTION');

INSERT INTO journey_template_tag(template_id, tag) VALUES
(@template_id, 'facebook'),
(@template_id, 'lead ads'),
(@template_id, 'lifecycle'),
(@template_id, 'own-media');

INSERT INTO journey_template_metadata(template_id, meta_key, meta_value) VALUES
(@template_id, 'measurement.meta_conversions_api', 'true'),
(@template_id, 'measurement.google_enhanced_conversions', 'true'),
(@template_id, 'consent.lgpd', 'Registrar opt-in granular para e-mail e WhatsApp com rastreabilidade do consentimento.'),
(@template_id, 'playbook.window_days', '14'),
(@template_id, 'playbook.notes', 'Fluxo alinhado ao plano pós-clique: captura de identidade, nutrição por e-mail/push e uso parcimonioso de WhatsApp.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 1, 'Clique no anúncio Lead Ads (Facebook)', 'Anúncio no Facebook com CTA para Instant Form ou WhatsApp, utilizando UTMs e click_id para atribuição completa.', 'ATTENTION', 'AD', 'Evento de clique Meta Ads recebido', 'Lead encaminhado ao formulário ou WhatsApp', 0);
SET @step_capture = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_capture, 'kpi', 'CTR, CPC, CPL'),
(@step_capture, 'tracking', 'Enviar click_id via Conversions API + Enhanced Conversions para reconciliar atribuição.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 2, 'Captura de lead e consentimento', 'Instant Form ou landing page de 1 CTA consolidando identidade, preferências de canal e consentimento LGPD.', 'INTEREST', 'LANDING_PAGE', 'Clique validado e sessão ativa', 'Lead criado com consentimento registrado', 0);
SET @step_identity = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_identity, 'channel', 'Instant Form (Meta) ou landing própria com CDP'),
(@step_identity, 'automation', 'Sincronizar consentimento, click_id e device_id com o CDP e CRM.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 3, 'Boas-vindas D0', 'Envio imediato de boas-vindas por e-mail confirmando opt-ins, entregando valor inicial e orientando o próximo passo.', 'INTEREST', 'EMAIL', 'Lead com opt-in de e-mail confirmado', 'Mensagem enviada', 0);
SET @step_welcome = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_welcome, 'secondary_channel', 'Web push opcional para leads que autorizaram notificações.'),
(@step_welcome, 'content', 'Boas-vindas + promessa entregue (ex.: checklist ou mini aula).');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 4, 'Conteúdo educativo D1', 'E-mail com conteúdo educativo + prova social reforçando autoridade e estimulando consumo do material.', 'INTEREST', 'EMAIL', '24h após boas-vindas ou primeiro engajamento', 'Mensagem enviada', 1440);
SET @step_education = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_education, 'goal', 'Educar e construir prova social (case, depoimento).'),
(@step_education, 'kpi', 'Taxa de abertura e cliques no conteúdo.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 5, 'Nudge comportamental D3', 'Lembrete via web push para quem não avançou, retomando onde parou e reforçando o próximo passo.', 'DESIRE', 'EMAIL', '72h após captura sem evento de conversão', 'Notificação disparada', 4320);
SET @step_nudge = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_nudge, 'delivery_channel', 'Web push (service worker ativo).'),
(@step_nudge, 'copy_hint', 'Mensagem curta destacando benefício ou urgência leve.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 6, 'Oferta leve D5', 'E-mail com oferta leve ou pedido de micro conversão (ex.: responder pesquisa, agendar demonstração) para leads aquecidos.', 'DESIRE', 'EMAIL', '120h após captura OU após consumo de conteúdo', 'Mensagem enviada', 7200);
SET @step_offer = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_offer, 'offer_type', 'Oferta leve com benefício adicional ou desconto temporário.'),
(@step_offer, 'segmentation', 'Enviar somente para leads com engajamento mínimo (abertura ou clique prévio).');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 7, 'WhatsApp utilitário D5', 'Mensagem template no WhatsApp com utilidade (guia, checklist) para leads que deram opt-in e engajaram.', 'DESIRE', 'WHATSAPP', 'Lead com opt-in WhatsApp e engajamento prévio', 'Template aprovado enviado', 7260);
SET @step_whatsapp = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_whatsapp, 'template_name', 'boa_vindas_valor_utilitario'),
(@step_whatsapp, 'usage', 'Usar somente em gatilhos de alto valor para controlar custos por conversa.');

INSERT INTO journey_step(template_id, position, name, description, phase, stimulus_type, entry_condition, exit_condition, delay_minutes)
VALUES
(@template_id, 8, 'Retargeting Meta Ads D7-D14', 'Sequência de anúncios de retargeting para quem abriu/clicou mas não converteu, priorizando criativos com prova social.', 'ACTION', 'AD', 'Lead com engajamento sem conversão após 7 dias', 'Campanha ativa por até 7 dias adicionais', 10080);
SET @step_ret = LAST_INSERT_ID();
INSERT INTO journey_step_metadata(step_id, meta_key, meta_value) VALUES
(@step_ret, 'audience', 'Custom Audiences via Pixel/CAPI e listas do CRM.'),
(@step_ret, 'budget_priority', 'Otimizar para ROAS; investir apenas em segmentos de alto potencial.');
