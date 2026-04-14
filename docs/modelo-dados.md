# Modelo de Dados (snapshot)

- Fonte: esquema atual do banco configurado para o ambiente de sandbox.
- Critério: apenas tabelas com `table_rows > 0` no `information_schema` (consultado em 2026-03-11T15:29:33+00:00).
- Observação: os valores de `table_rows` no MySQL são estimativas; use-os como referência aproximada.

## Tabelas com registros

```
tabela|qtd_registros_aprox
ad_set|3
agent_theme|2
ai_service|8
ai_worker_generation|114
angle|8
asset|757
chat_dialog|48
creative|15
creative_variants|2
DATABASECHANGELOG|247
deliverable|4
email_log|31
emotional_trigger|8
experiment|5
experiment_adset_job|20
experiment_adset_job_api_log|77
experiment_adset_spec|6
experiment_adset_workflow|3
experiment_campaign_metric|5
experiment_facebook_api_log|39
experiment_sample_email|2
experiment_targeting_selection|4
facebook_ads_ad|123
facebook_ads_ad_creative|123
facebook_ads_ad_set|123
facebook_ads_campaign|124
fb_account|1
fb_instant_form|14
fb_page|1
flows|8
flow_access|1002
flow_submissions|100
flow_submission_image_item|366
flow_submission_image_package|94
flow_submission_image_package_status_history|479
flow_submission_image_watermark|325
funnel_step|8
hypothesis|24
hypothesis_prompt_attribute_description|142
ig_account|1
image_generation_model|4
image_generation_price|27
image_generation_quality|9
interaction_journey_element|11
interaction_journey_step|5
journey|3
journey_assignment|2
journey_metadata|19
journey_step|19
journey_step_metadata|16
journey_template|5
journey_template_metadata|5
journey_template_phase|20
journey_template_tag|4
lead_portal_flow|14
lead_portal_flow_question|220
lead_portal_flow_question_option|259
lead_portal_premium_delivery|7
lead_portal_purchase|28
market_niche|17
mercadopago_webhook_log|78
metric_preset|2
niche_detailed_description|7
openai_model|7
prompt|2
prompt_attribute|8
prompt_attribute_description|11
prompt_domain|4
prompt_domain_object|8
prompt_entity|2
sales_funnel|3
success_product|11
targeting_candidate|82
targeting_candidate_seed_variant|113
targeting_element|11
targeting_option|5
targeting_request|9
targeting_resolution_job|92
visual_proof|8
```

## Dicionário de dados

Formato: `tabela|coluna|tipo|nullable|default|chave|extra`.

```
ad_set|id|bigint(20)|NO|NULL|PRI|auto_increment
ad_set|budget|decimal(38,2)|YES|NULL||
ad_set|created_at|datetime(6)|YES|NULL||
ad_set|duration_days|int(11)|YES|NULL||
ad_set|interests|tinytext|YES|NULL||
ad_set|location|varchar(255)|YES|NULL||
ad_set|lookalikes|tinytext|YES|NULL||
ad_set|updated_at|datetime(6)|YES|NULL||
ad_set|experiment_id|bigint(20)|NO|NULL|MUL|
ad_set|model|varchar(255)|YES|NULL||
ad_set|targeting_request_id|binary(16)|YES|NULL|MUL|
ad_set|prompt|text|YES|NULL||
ad_set|targeting_json|text|YES|NULL||
ad_set|behaviors|text|YES|NULL||
ad_set|job_titles|text|YES|NULL||
agent_theme|id|bigint(20)|NO|NULL|PRI|auto_increment
agent_theme|name|varchar(255)|NO|NULL|UNI|
agent_theme|description|tinytext|YES|NULL||
agent_theme|created_at|timestamp|NO|CURRENT_TIMESTAMP||
agent_theme|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
ai_service|id|bigint(20)|NO|NULL|PRI|auto_increment
ai_service|cost|decimal(38,2)|YES|NULL||
ai_service|created_at|datetime(6)|YES|NULL||
ai_service|name|varchar(255)|YES|NULL||
ai_service|objective|tinytext|YES|NULL||
ai_service|price|decimal(38,2)|YES|NULL||
ai_service|updated_at|datetime(6)|YES|NULL||
ai_service|url|varchar(255)|YES|NULL||
ai_service|phase|varchar(255)|YES|NULL||
ai_service|observation|tinytext|YES|NULL||
ai_worker_generation|id|bigint(20)|NO|NULL|PRI|auto_increment
ai_worker_generation|domain|varchar(100)|NO|NULL|MUL|
ai_worker_generation|reference_id|varchar(100)|YES|NULL||
ai_worker_generation|model|varchar(191)|YES|NULL||
ai_worker_generation|prompt|longtext|YES|NULL||
ai_worker_generation|raw_response|longtext|YES|NULL||
ai_worker_generation|input_tokens|int(11)|YES|NULL||
ai_worker_generation|output_tokens|int(11)|YES|NULL||
ai_worker_generation|cost_usd|decimal(10,4)|NO|0.0000||
ai_worker_generation|created_at|timestamp|NO|CURRENT_TIMESTAMP||
angle|id|bigint(20)|NO|NULL|PRI|auto_increment
angle|description|tinytext|YES|NULL||
angle|frame_type|varchar(255)|YES|NULL||
angle|name|varchar(255)|YES|NULL||
asset|id|bigint(20)|NO|NULL|PRI|auto_increment
asset|campaign_id|bigint(20)|YES|NULL||
asset|created_at|datetime(6)|YES|NULL||
asset|external_id|varchar(255)|YES|NULL||
asset|payload|longtext|YES|NULL||
asset|provider|enum('SYNTHESIA','HEYGEN','ELEVENLABS','RUNWAY','OPENAI','WATERMARKER')|YES|NULL||
asset|status|enum('PENDING','PROCESSING','READY','FAILED')|YES|NULL||
asset|type|enum('VIDEO','AUDIO','BROLL','IMAGE')|YES|NULL||
asset|updated_at|datetime(6)|YES|NULL||
asset|url|varchar(255)|YES|NULL||
asset|model|varchar(255)|YES|NULL||
asset|prompt|longtext|YES|NULL||
chat_dialog|id|bigint(20)|NO|NULL|PRI|auto_increment
chat_dialog|url|varchar(500)|YES|NULL||
chat_dialog|description|tinytext|YES|NULL||
chat_dialog|theme|varchar(255)|YES|NULL||
chat_dialog|created_at|timestamp|NO|CURRENT_TIMESTAMP||
chat_dialog|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
creative|id|bigint(20)|NO|NULL|PRI|auto_increment
creative|experiment_id|bigint(20)|NO|NULL|MUL|
creative|headline|varchar(255)|YES|NULL||
creative|primary_text|varchar(255)|YES|NULL||
creative|image_url|varchar(500)|YES|NULL||
creative|ad_format|varchar(32)|YES|NULL||
creative|description|varchar(255)|YES|NULL||
creative|call_to_action|varchar(32)|YES|NULL||
creative|destination_url|varchar(512)|YES|NULL||
creative|lead_gen_form_id|varchar(64)|YES|NULL||
creative|instagram_user_id|varchar(64)|YES|NULL||
creative|image_hash|varchar(255)|YES|NULL||
creative|video_id|varchar(255)|YES|NULL||
creative|status|enum('DRAFT','READY')|YES|NULL||
creative_variants|id|bigint(20)|NO|NULL|PRI|auto_increment
creative_variants|headline|varchar(255)|YES|NULL||
creative_variants|image_url|varchar(255)|YES|NULL||
creative_variants|primary_text|varchar(255)|YES|NULL||
creative_variants|status|enum('DRAFT','READY')|YES|NULL||
creative_variants|experiment_id|bigint(20)|NO|NULL|MUL|
creative_variants|image_hash|varchar(255)|YES|NULL||
creative_variants|video_id|varchar(255)|YES|NULL||
DATABASECHANGELOG|ID|varchar(255)|NO|NULL||
DATABASECHANGELOG|AUTHOR|varchar(255)|NO|NULL||
DATABASECHANGELOG|FILENAME|varchar(255)|NO|NULL||
DATABASECHANGELOG|DATEEXECUTED|datetime|NO|NULL||
DATABASECHANGELOG|ORDEREXECUTED|int(11)|NO|NULL||
DATABASECHANGELOG|EXECTYPE|varchar(10)|NO|NULL||
DATABASECHANGELOG|MD5SUM|varchar(35)|YES|NULL||
DATABASECHANGELOG|DESCRIPTION|varchar(255)|YES|NULL||
DATABASECHANGELOG|COMMENTS|varchar(255)|YES|NULL||
DATABASECHANGELOG|TAG|varchar(255)|YES|NULL||
DATABASECHANGELOG|LIQUIBASE|varchar(20)|YES|NULL||
DATABASECHANGELOG|CONTEXTS|varchar(255)|YES|NULL||
DATABASECHANGELOG|LABELS|varchar(255)|YES|NULL||
DATABASECHANGELOG|DEPLOYMENT_ID|varchar(10)|YES|NULL||
deliverable|id|bigint(20)|NO|NULL|PRI|auto_increment
deliverable|market_niche_id|bigint(20)|NO|NULL|MUL|
deliverable|title|varchar(255)|NO|NULL||
deliverable|description|longtext|YES|NULL||
deliverable|content|longtext|YES|NULL||
deliverable|model|varchar(255)|YES|NULL||
deliverable|prompt|longtext|NO|NULL||
deliverable|created_at|timestamp|NO|CURRENT_TIMESTAMP||
deliverable|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
email_log|id|bigint(20)|NO|NULL|PRI|auto_increment
email_log|request_id|varchar(36)|NO|NULL|UNI|
email_log|recipients|text|NO|NULL||
email_log|subject|varchar(255)|NO|NULL||
email_log|template_id|varchar(100)|YES|NULL||
email_log|status|varchar(20)|NO|NULL||
email_log|error_message|text|YES|NULL||
email_log|created_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
email_log|sent_at|timestamp|YES|NULL||
email_log|opened_at|timestamp|YES|NULL||
emotional_trigger|id|bigint(20)|NO|NULL|PRI|auto_increment
emotional_trigger|description|tinytext|YES|NULL||
emotional_trigger|name|varchar(255)|YES|NULL||
emotional_trigger|valence|enum('POS','NEG')|YES|NULL||
experiment|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment|created_at|datetime(6)|YES|NULL||
experiment|end_date|date|YES|NULL||
experiment|hypothesis|varchar(255)|YES|NULL||
experiment|kpi_goal|decimal(38,2)|YES|NULL||
experiment|start_date|date|YES|NULL||
experiment|updated_at|datetime(6)|YES|NULL||
experiment|kpi_target|decimal(38,2)|YES|NULL||
experiment|name|varchar(255)|NO|NULL||
experiment|platform|enum('FACEBOOK')|YES|NULL||
experiment|niche_id|bigint(20)|NO|NULL|MUL|
experiment|hypothesis_id|binary(16)|NO|NULL|MUL|
experiment|mde|decimal(5,2)|YES|NULL||
experiment|sample_size|int(11)|YES|NULL||
experiment|stop_loss_cpl|decimal(10,2)|YES|NULL||
experiment|kpi_target_cpl|decimal(10,2)|YES|45.00||
experiment|baseline_cvr|decimal(5,2)|YES|3.00||
experiment|target_cvr|decimal(5,2)|YES|5.00||
experiment|mde_percent|decimal(5,2)|YES|40.00||
experiment|metric_preset_id|varchar(50)|YES|NULL|MUL|
experiment|journey_template_id|bigint(20)|NO|NULL|MUL|
experiment|creatives_to_generate|int(11)|YES|NULL||
experiment|instant_forms_to_generate|int(11)|YES|NULL||
experiment|emails_to_generate|int(11)|YES|NULL||
experiment|deliverables_to_generate|int(11)|YES|NULL||
experiment|creative_approved|bit(1)|NO|NULL||
experiment|facebook_page_id|bigint(20)|YES|NULL|MUL|
experiment|facebook_instant_form_id|bigint(20)|YES|NULL|MUL|
experiment|facebook_pixel_id|varchar(64)|YES|NULL||
experiment|facebook_pixel_code|longtext|YES|NULL||
experiment|facebook_pixel_created_at|timestamp|YES|NULL||
experiment|facebook_release_requested_at|datetime|YES|NULL||
experiment|instagram_account_id|bigint(20)|YES|NULL|MUL|
experiment|follow_up_action_url|varchar(512)|YES|NULL||
experiment|lead_portal_flow_id|bigint(20)|YES|NULL|MUL|
experiment|lead_portal_flows_to_generate|int(11)|YES|NULL||
experiment|images_per_package|int(11)|NO|20||
experiment|open_images_per_package|int(11)|YES|NULL||
experiment|compressed_images_per_package|int(11)|YES|NULL||
experiment|daily_budget|decimal(10,2)|YES|NULL||
experiment|unit_price_brl|decimal(10,2)|YES|NULL||
experiment|image_model_id|bigint(20)|YES|NULL|MUL|
experiment|image_model_quality_id|bigint(20)|YES|NULL|MUL|
experiment|sample_emails_to_generate|int(11)|YES|NULL||
experiment|selected_sample_email_id|bigint(20)|YES|NULL|MUL|
experiment|cost|decimal(10,2)|YES|NULL||
experiment|expense|decimal(10,2)|YES|NULL||
experiment|total_cost|decimal(12,2)|YES|NULL||
experiment|lead_portal_flow_model|varchar(191)|YES|NULL||
experiment|status|enum('PLANNED','RUNNING','PAUSED','VALIDATED','INVALIDATED','INCONCLUSIVE','FINISHED','FAILED')|YES|NULL||
experiment_adset_job|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_adset_job|workflow_id|bigint(20)|NO|NULL|MUL|
experiment_adset_job|type|enum('AI_PREPARE_SEED','FACEBOOK_SEED_LOOKUP','FACEBOOK_TARGETING_SUGGESTIONS','FACEBOOK_SOCIAL_POSITIONS','AI_BUILD_SPECS','FACEBOOK_VALIDATE_SPEC','FACEBOOK_REACH_ESTIMATE')|NO|NULL||
experiment_adset_job|worker|enum('AI','FACEBOOK')|NO|NULL||
experiment_adset_job|status|enum('PENDING','RUNNING','SUCCEEDED','FAILED')|NO|NULL|MUL|
experiment_adset_job|resource_id|bigint(20)|YES|NULL||
experiment_adset_job|attempt_count|int(11)|NO|0||
experiment_adset_job|payload|longtext|YES|NULL||
experiment_adset_job|result_payload|longtext|YES|NULL||
experiment_adset_job|error_message|longtext|YES|NULL||
experiment_adset_job|locked_by|varchar(191)|YES|NULL||
experiment_adset_job|locked_at|datetime(6)|YES|NULL|MUL|
experiment_adset_job|started_at|datetime(6)|YES|NULL||
experiment_adset_job|finished_at|datetime(6)|YES|NULL||
experiment_adset_job|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
experiment_adset_job|updated_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||on update CURRENT_TIMESTAMP(6)
experiment_adset_job_api_log|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_adset_job_api_log|job_id|bigint(20)|NO|NULL|MUL|
experiment_adset_job_api_log|provider|varchar(32)|NO|NULL||
experiment_adset_job_api_log|endpoint|longtext|YES|NULL||
experiment_adset_job_api_log|http_method|varchar(16)|YES|NULL||
experiment_adset_job_api_log|status_code|int(11)|YES|NULL||
experiment_adset_job_api_log|requested_at|datetime(6)|YES|NULL||
experiment_adset_job_api_log|responded_at|datetime(6)|YES|NULL||
experiment_adset_job_api_log|request_payload|longtext|YES|NULL||
experiment_adset_job_api_log|response_payload|longtext|YES|NULL||
experiment_adset_job_api_log|error_message|longtext|YES|NULL||
experiment_adset_job_api_log|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
experiment_adset_job_api_log|updated_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||on update CURRENT_TIMESTAMP(6)
experiment_adset_spec|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_adset_spec|workflow_id|bigint(20)|NO|NULL|MUL|
experiment_adset_spec|slot|enum('DESIGNERS','MARKETING','SMB')|NO|NULL||
experiment_adset_spec|label|varchar(255)|YES|NULL||
experiment_adset_spec|age_min|int(11)|YES|NULL||
experiment_adset_spec|age_max|int(11)|YES|NULL||
experiment_adset_spec|targeting_spec|longtext|YES|NULL||
experiment_adset_spec|validation_status|varchar(32)|YES|NULL||
experiment_adset_spec|validation_response|longtext|YES|NULL||
experiment_adset_spec|reach_status|varchar(32)|YES|NULL||
experiment_adset_spec|reach_response|longtext|YES|NULL||
experiment_adset_spec|reach_lower_bound|bigint(20)|YES|NULL||
experiment_adset_spec|reach_upper_bound|bigint(20)|YES|NULL||
experiment_adset_spec|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
experiment_adset_spec|updated_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||on update CURRENT_TIMESTAMP(6)
experiment_adset_workflow|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_adset_workflow|experiment_id|bigint(20)|NO|NULL|UNI|
experiment_adset_workflow|status|enum('NOT_STARTED','RUNNING','COMPLETED','FAILED')|NO|NULL||
experiment_adset_workflow|seed_keyword|varchar(255)|YES|NULL||
experiment_adset_workflow|seed_locale|varchar(10)|YES|NULL||
experiment_adset_workflow|seed_interest_id|varchar(64)|YES|NULL||
experiment_adset_workflow|seed_interest_name|varchar(255)|YES|NULL||
experiment_adset_workflow|seed_audience_lower|bigint(20)|YES|NULL||
experiment_adset_workflow|seed_audience_upper|bigint(20)|YES|NULL||
experiment_adset_workflow|ai_notes|longtext|YES|NULL||
experiment_adset_workflow|last_error|longtext|YES|NULL||
experiment_adset_workflow|completed_at|datetime(6)|YES|NULL||
experiment_adset_workflow|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
experiment_adset_workflow|updated_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||on update CURRENT_TIMESTAMP(6)
experiment_campaign_metric|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_campaign_metric|clicks|bigint(20)|YES|NULL||
experiment_campaign_metric|cpc|decimal(12,2)|YES|NULL||
experiment_campaign_metric|cpl|decimal(12,2)|YES|NULL||
experiment_campaign_metric|created_at|datetime(6)|YES|NULL||
experiment_campaign_metric|date_start|date|YES|NULL||
experiment_campaign_metric|date_stop|date|YES|NULL||
experiment_campaign_metric|impressions|bigint(20)|YES|NULL||
experiment_campaign_metric|leads|bigint(20)|YES|NULL||
experiment_campaign_metric|spend|decimal(12,2)|YES|NULL||
experiment_campaign_metric|updated_at|datetime(6)|YES|NULL||
experiment_campaign_metric|campaign_id|char(36)|NO|NULL|UNI|
experiment_campaign_metric|experiment_id|bigint(20)|NO|NULL|UNI|
experiment_facebook_api_log|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_facebook_api_log|experiment_id|bigint(20)|NO|NULL|MUL|
experiment_facebook_api_log|context|enum('PLAYBOOK','CAMPAIGN_CREATION','CAMPAIGN_AD_SET','CAMPAIGN_AD_CREATIVE','CAMPAIGN_AD')|NO|NULL||
experiment_facebook_api_log|provider|varchar(32)|NO|NULL||
experiment_facebook_api_log|endpoint|longtext|YES|NULL||
experiment_facebook_api_log|http_method|varchar(16)|YES|NULL||
experiment_facebook_api_log|status_code|int(11)|YES|NULL||
experiment_facebook_api_log|requested_at|datetime(6)|YES|NULL||
experiment_facebook_api_log|responded_at|datetime(6)|YES|NULL||
experiment_facebook_api_log|request_payload|longtext|YES|NULL||
experiment_facebook_api_log|response_payload|longtext|YES|NULL||
experiment_facebook_api_log|error_message|longtext|YES|NULL||
experiment_facebook_api_log|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
experiment_sample_email|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_sample_email|body|text|YES|NULL||
experiment_sample_email|call_to_action|varchar(500)|YES|NULL||
experiment_sample_email|created_at|datetime(6)|YES|NULL||
experiment_sample_email|model|varchar(128)|YES|NULL||
experiment_sample_email|preview_text|varchar(255)|YES|NULL||
experiment_sample_email|prompt|text|YES|NULL||
experiment_sample_email|subject|varchar(255)|NO|NULL||
experiment_sample_email|updated_at|datetime(6)|YES|NULL||
experiment_sample_email|experiment_id|bigint(20)|NO|NULL|MUL|
experiment_targeting_selection|id|bigint(20)|NO|NULL|PRI|auto_increment
experiment_targeting_selection|experiment_id|bigint(20)|NO|NULL|MUL|
experiment_targeting_selection|candidate_type|enum('INTEREST','BEHAVIOR','WORK_POSITION')|NO|NULL||
experiment_targeting_selection|term|varchar(191)|NO|NULL||
experiment_targeting_selection|created_at|timestamp|YES|CURRENT_TIMESTAMP||
experiment_targeting_selection|updated_at|timestamp|YES|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
experiment_targeting_selection|targeting_element_id|bigint(20)|YES|NULL|MUL|
facebook_ads_ad|id|char(36)|NO|NULL|PRI|
facebook_ads_ad|external_id|varchar(64)|YES|NULL||
facebook_ads_ad|adset_id|char(36)|NO|NULL|MUL|
facebook_ads_ad|name|varchar(255)|NO|NULL||
facebook_ads_ad|creative_id|char(36)|NO|NULL|MUL|
facebook_ads_ad|status|enum('PAUSED','ACTIVE','ARCHIVED','DELETED')|NO|PAUSED||
facebook_ads_ad|created_at|timestamp|NO|CURRENT_TIMESTAMP||
facebook_ads_ad|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
facebook_ads_ad_creative|id|char(36)|NO|NULL|PRI|
facebook_ads_ad_creative|external_id|varchar(64)|YES|NULL||
facebook_ads_ad_creative|page_id|varchar(64)|NO|NULL||
facebook_ads_ad_creative|instagram_user_id|varchar(64)|YES|NULL||
facebook_ads_ad_creative|kind|enum('LINK','VIDEO','CAROUSEL')|NO|NULL||
facebook_ads_ad_creative|link_data_json|longtext|YES|NULL||
facebook_ads_ad_creative|video_data_json|longtext|YES|NULL||
facebook_ads_ad_creative|carousel_data_json|longtext|YES|NULL||
facebook_ads_ad_creative|last_preview_url|varchar(1024)|YES|NULL||
facebook_ads_ad_creative|created_at|timestamp|NO|CURRENT_TIMESTAMP||
facebook_ads_ad_creative|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
facebook_ads_ad_set|id|char(36)|NO|NULL|PRI|
facebook_ads_ad_set|external_id|varchar(64)|YES|NULL||
facebook_ads_ad_set|experiment_ad_set_id|bigint(20)|YES|NULL|MUL|
facebook_ads_ad_set|campaign_id|char(36)|NO|NULL|MUL|
facebook_ads_ad_set|name|varchar(255)|NO|NULL||
facebook_ads_ad_set|status|enum('PAUSED','ACTIVE','ARCHIVED','DELETED')|NO|PAUSED||
facebook_ads_ad_set|daily_budget_minor|bigint(20) unsigned|YES|NULL||
facebook_ads_ad_set|lifetime_budget_minor|bigint(20) unsigned|YES|NULL||
facebook_ads_ad_set|start_time|datetime|YES|NULL||
facebook_ads_ad_set|end_time|datetime|YES|NULL||
facebook_ads_ad_set|billing_event|varchar(32)|NO|NULL||
facebook_ads_ad_set|optimization_goal|varchar(64)|NO|NULL||
facebook_ads_ad_set|bid_strategy|varchar(64)|NO|NULL||
facebook_ads_ad_set|bid_amount_minor|bigint(20) unsigned|YES|NULL||
facebook_ads_ad_set|promoted_object_json|longtext|YES|NULL||
facebook_ads_ad_set|targeting_json|longtext|NO|NULL||
facebook_ads_ad_set|created_at|timestamp|NO|CURRENT_TIMESTAMP||
facebook_ads_ad_set|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
facebook_ads_campaign|id|char(36)|NO|NULL|PRI|
facebook_ads_campaign|external_id|varchar(64)|YES|NULL||
facebook_ads_campaign|ad_account_id|varchar(64)|NO|NULL||
facebook_ads_campaign|name|varchar(255)|NO|NULL||
facebook_ads_campaign|objective|varchar(64)|NO|NULL||
facebook_ads_campaign|status|enum('PAUSED','ACTIVE','ARCHIVED','DELETED')|NO|PAUSED||
facebook_ads_campaign|budget_mode|enum('CAMPAIGN','ADSET')|NO|NULL||
facebook_ads_campaign|daily_budget_minor|bigint(20) unsigned|YES|NULL||
facebook_ads_campaign|lifetime_budget_minor|bigint(20) unsigned|YES|NULL||
facebook_ads_campaign|api_version|varchar(16)|YES|NULL||
facebook_ads_campaign|created_at|timestamp|NO|CURRENT_TIMESTAMP||
facebook_ads_campaign|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
facebook_ads_campaign|experiment_id|bigint(20)|NO|NULL|MUL|
facebook_ads_campaign|facebook_account_id|bigint(20)|NO|NULL|MUL|
facebook_ads_campaign|metrics_last_error|text|YES|NULL||
facebook_ads_campaign|metrics_last_synced_at|datetime(6)|YES|NULL||
fb_account|id|bigint(20)|NO|NULL|PRI|
fb_account|currency|varchar(255)|YES|NULL||
fb_account|name|varchar(255)|YES|NULL||
fb_account|access_token|longtext|YES|NULL||
fb_account|token_expires_at|datetime|YES|NULL||
fb_account|token_last_refreshed_at|datetime|YES|NULL||
fb_account|authorized_user_id|varchar(128)|YES|NULL||
fb_account|authorized_user_name|varchar(255)|YES|NULL||
fb_account|authorized_user_email|varchar(320)|YES|NULL||
fb_account|app_id|varchar(255)|YES|NULL||
fb_account|app_secret|longtext|YES|NULL||
fb_account|system_user_access_token|longtext|YES|NULL||
fb_account|token_renewal_enabled|tinyint(1)|NO|0||
fb_account|token_renewal_status|varchar(40)|YES|NULL||
fb_account|token_renewal_last_attempt_at|datetime|YES|NULL||
fb_account|token_renewed_at|datetime|YES|NULL||
fb_account|token_renewal_last_error|longtext|YES|NULL||
fb_account|ad_account_id|varchar(64)|YES|NULL||
fb_account|default_page_id|varchar(128)|YES|NULL||
fb_account|default_website_url|varchar(512)|YES|NULL||
fb_account|default_lead_gen_form_id|varchar(64)|YES|NULL||
fb_account|default_instagram_actor_id|varchar(64)|YES|NULL||
fb_account|default_creative_message_template|varchar(255)|YES|NULL||
fb_account|default_call_to_action_type|varchar(64)|YES|NULL||
fb_account|ad_set_daily_budget|varchar(32)|YES|NULL||
fb_account|ad_set_billing_event|varchar(64)|YES|NULL||
fb_account|ad_set_optimization_goal|varchar(64)|YES|NULL||
fb_account|ad_set_destination_type|varchar(64)|YES|NULL||
fb_account|ad_set_bid_strategy|varchar(64)|YES|NULL||
fb_account|ad_set_bid_amount|varchar(32)|YES|NULL||
fb_account|ad_set_target_country|varchar(32)|YES|NULL||
fb_account|worker_enabled|tinyint(1)|NO|0||
fb_account|worker_last_validation_at|datetime|YES|NULL||
fb_account|worker_last_validation_error_code|varchar(128)|YES|NULL||
fb_account|worker_last_validation_error_detail|varchar(512)|YES|NULL||
fb_instant_form|id|bigint(20)|NO|NULL|PRI|auto_increment
fb_instant_form|hypothesis_id|binary(16)|NO|NULL|MUL|
fb_instant_form|page_id|bigint(20)|NO|NULL|MUL|
fb_instant_form|form_id|varchar(128)|YES|NULL|UNI|
fb_instant_form|name|varchar(255)|NO|NULL||
fb_instant_form|status|varchar(50)|YES|NULL||
fb_instant_form|locale|varchar(12)|YES|NULL||
fb_instant_form|leads_count|bigint(20)|YES|NULL||
fb_instant_form|created_time|datetime|YES|NULL||
fb_instant_form|updated_time|datetime|YES|NULL||
fb_instant_form|follow_up_action_url|varchar(512)|YES|NULL||
fb_instant_form|privacy_policy_url|varchar(512)|YES|NULL||
fb_instant_form|model|varchar(128)|YES|NULL||
fb_instant_form|prompt|longtext|YES|NULL||
fb_instant_form|questions|longtext|YES|NULL||
fb_instant_form|created_at|datetime|YES|CURRENT_TIMESTAMP||
fb_instant_form|updated_at|datetime|YES|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
fb_instant_form|approved|bit(1)|NO|NULL||
fb_instant_form|approved_at|datetime(6)|YES|NULL||
fb_instant_form|published|bit(1)|NO|NULL||
fb_instant_form|published_at|datetime(6)|YES|NULL||
fb_instant_form|share_link|varchar(512)|YES|NULL||
fb_page|id|bigint(20)|NO|NULL|PRI|auto_increment
fb_page|account_id|bigint(20)|NO|NULL|MUL|
fb_page|page_id|varchar(128)|NO|NULL||
fb_page|name|varchar(255)|NO|NULL||
flows|slug|varchar(190)|NO|NULL|PRI|
flows|description|varchar(500)|YES|NULL||
flows|model|varchar(255)|YES|NULL||
flows|name|varchar(255)|NO|NULL||
flows|prompt|text|YES|NULL||
flows|questions|longtext|YES|NULL||
flows|access_count|bigint(20)|NO|0||
flows|simple_form_style_definition|longtext|YES|NULL||
flows|simple_form_style_name|varchar(150)|YES|NULL||
flows|simple_form_style_slug|varchar(120)|YES|NULL||
flow_access|id|bigint(20)|NO|NULL|PRI|auto_increment
flow_access|accessed_at|datetime(6)|YES|NULL||
flow_access|client_ip|varchar(64)|YES|NULL||
flow_access|flow_slug|varchar(190)|NO|NULL||
flow_access|referer|varchar(1024)|YES|NULL||
flow_access|user_agent|varchar(1024)|YES|NULL||
flow_access|visitor_id|varchar(128)|YES|NULL||
flow_access|campaign_code|varchar(190)|YES|NULL||
flow_submissions|id|varchar(36)|NO|NULL|PRI|
flow_submissions|answers|longtext|YES|NULL||
flow_submissions|content_type|varchar(255)|YES|NULL||
flow_submissions|created_at|datetime(6)|NO|NULL||
flow_submissions|email|varchar(255)|NO|NULL||
flow_submissions|flow_slug|varchar(190)|NO|NULL||
flow_submissions|image_question_key|varchar(255)|YES|NULL||
flow_submissions|name|varchar(255)|NO|NULL||
flow_submissions|original_file_name|varchar(255)|YES|NULL||
flow_submissions|stored_file_name|varchar(255)|YES|NULL||
flow_submissions|campaign_code|varchar(190)|YES|NULL||
flow_submission_image_item|id|bigint(20)|NO|NULL|PRI|auto_increment
flow_submission_image_item|package_id|bigint(20)|NO|NULL|MUL|
flow_submission_image_item|asset_id|bigint(20)|NO|NULL|MUL|
flow_submission_image_item|access_type|varchar(20)|NO|NULL||
flow_submission_image_item|position_index|int(11)|NO|NULL||
flow_submission_image_item|created_at|timestamp|NO|CURRENT_TIMESTAMP||
flow_submission_image_package|id|bigint(20)|NO|NULL|PRI|auto_increment
flow_submission_image_package|submission_id|varchar(36)|NO|NULL|MUL|
flow_submission_image_package|status|varchar(30)|NO|RECEIVED|MUL|
flow_submission_image_package|planned_outputs|int(11)|YES|NULL||
flow_submission_image_package|free_images|int(11)|NO|0||
flow_submission_image_package|model|varchar(255)|YES|NULL||
flow_submission_image_package|prompt|longtext|NO|NULL||
flow_submission_image_package|created_at|timestamp|NO|CURRENT_TIMESTAMP||
flow_submission_image_package|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
flow_submission_image_package|failure_reason|longtext|YES|NULL||
flow_submission_image_package|image_model_id|bigint(20)|YES|NULL|MUL|
flow_submission_image_package|image_model_quality_id|bigint(20)|YES|NULL|MUL|
flow_submission_image_package|image_orientation|varchar(16)|YES|NULL||
flow_submission_image_package|image_width|int(11)|YES|NULL||
flow_submission_image_package|image_height|int(11)|YES|NULL||
flow_submission_image_package|image_unit_price_usd|decimal(38,2)|YES|NULL||
flow_submission_image_package|image_total_price_usd|decimal(38,2)|YES|NULL||
flow_submission_image_package|image_currency|varchar(3)|NO|USD||
flow_submission_image_package|notification_attempts|int(11)|NO|NULL||
flow_submission_image_package|notification_last_attempt|datetime(6)|YES|NULL||
flow_submission_image_package|notification_last_error|text|YES|NULL||
flow_submission_image_package|email_opened_at|timestamp|YES|NULL||
flow_submission_image_package|images_viewed_at|timestamp|YES|NULL||
flow_submission_image_package|notified_at|datetime(6)|YES|NULL||
flow_submission_image_package|zip_object_key|varchar(512)|YES|NULL|MUL|
flow_submission_image_package|payment_purchase_id|bigint(20)|YES|NULL|MUL|
flow_submission_image_package|payment_checkout_url|varchar(1200)|YES|NULL||
flow_submission_image_package|payment_checkout_expires_at|timestamp|YES|NULL||
flow_submission_image_package|payment_amount|decimal(12,2)|YES|NULL||
flow_submission_image_package|payment_currency|varchar(12)|YES|NULL||
flow_submission_image_package|payment_statement_descriptor|varchar(120)|YES|NULL||
flow_submission_image_package|zip_size_bytes|bigint(20)|YES|NULL||
flow_submission_image_package|zip_generated_at|timestamp|YES|NULL||
flow_submission_image_package|zip_last_error|text|YES|NULL||
flow_submission_image_package|zip_attempts|int(11)|NO|0||
flow_submission_image_package|zip_last_attempt|timestamp|YES|NULL||
flow_submission_image_package_status_history|id|bigint(20)|NO|NULL|PRI|auto_increment
flow_submission_image_package_status_history|package_id|bigint(20)|NO|NULL|MUL|
flow_submission_image_package_status_history|status|varchar(30)|NO|NULL||
flow_submission_image_package_status_history|failure_reason|longtext|YES|NULL||
flow_submission_image_package_status_history|created_at|timestamp|NO|CURRENT_TIMESTAMP||
flow_submission_image_watermark|id|bigint(20)|NO|NULL|PRI|auto_increment
flow_submission_image_watermark|item_id|bigint(20)|NO|NULL|UNI|
flow_submission_image_watermark|asset_id|bigint(20)|NO|NULL|MUL|
flow_submission_image_watermark|optimized_asset_id|bigint(20)|YES|NULL|MUL|
flow_submission_image_watermark|created_at|timestamp|NO|CURRENT_TIMESTAMP||
flow_submission_image_watermark|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
funnel_step|id|binary(16)|NO|NULL|PRI|
funnel_step|funnel_id|binary(16)|NO|NULL|MUL|
funnel_step|order_idx|int(11)|YES|NULL||
funnel_step|stimulus_type|enum('DM','EMAIL','IG_POST_BOOST','FB_AD','STORY','WHATSAPP','CALL','SMS','WEBINAR','PUSH')|YES|NULL||
funnel_step|channel|varchar(50)|YES|NULL||
funnel_step|template_id|varchar(50)|YES|NULL||
funnel_step|expected_action|enum('OPEN','CLICK','REPLY','VIEW','PURCHASE','REGISTRATION','OPT_IN','OPT_OUT','BOUNCE','SHARE')|YES|NULL||
funnel_step|score_inc|int(11)|YES|NULL||
funnel_step|revenue_target|decimal(38,2)|YES|NULL||
funnel_step|created_at|timestamp|YES|CURRENT_TIMESTAMP||
funnel_step|is_active|tinyint(1)|YES|1||
funnel_step|note|text|YES|NULL||
hypothesis|id|binary(16)|NO|NULL|PRI|
hypothesis|created_at|datetime(6)|YES|NULL||
hypothesis|kpi_target_cpl|decimal(7,2)|YES|NULL||
hypothesis|offer_type|enum('LEAD','TRIPWIRE')|YES|NULL||
hypothesis|status|enum('BACKLOG','TESTING','VALIDATED','INVALIDATED')|YES|NULL||
hypothesis|title|varchar(255)|NO|NULL||
hypothesis|premise_angle_id|bigint(20)|YES|NULL|MUL|
hypothesis|price|decimal(6,2)|YES|NULL||
hypothesis|updated_at|datetime(6)|YES|NULL||
hypothesis|market_niche_id|bigint(20)|YES|NULL|MUL|
hypothesis|persona|varchar(255)|NO|NULL||
hypothesis|problem|longtext|YES|NULL||
hypothesis|promise|longtext|YES|NULL||
hypothesis|success_rule|tinytext|YES|NULL||
hypothesis|experiment_id|bigint(20)|YES|NULL||
hypothesis|hypothesiscol|varchar(45)|YES|NULL||
hypothesis|unique_mechanism|longtext|YES|NULL||
hypothesis|mechanism|longtext|YES|NULL||
hypothesis|model|varchar(255)|YES|NULL||
hypothesis|prompt|longtext|YES|NULL||
hypothesis|generated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
hypothesis|entrega|longtext|YES|NULL||
hypothesis|cost_usd|decimal(10,4)|YES|NULL||
hypothesis|cost|decimal(10,2)|YES|NULL||
hypothesis|expense|decimal(10,2)|YES|NULL||
hypothesis|total_cost|decimal(12,2)|YES|NULL||
hypothesis_prompt_attribute_description|hypothesis_id|binary(16)|NO|NULL|PRI|
hypothesis_prompt_attribute_description|prompt_attribute_description_id|bigint(20)|NO|NULL|PRI|
ig_account|id|bigint(20)|NO|NULL|PRI|
ig_account|name|varchar(255)|YES|NULL||
ig_account|handle|varchar(255)|NO|NULL||
ig_account|account_code|varchar(255)|NO|NULL||
image_generation_model|id|bigint(20)|NO|NULL|PRI|auto_increment
image_generation_model|code|varchar(64)|NO|NULL|UNI|
image_generation_model|display_name|varchar(128)|NO|NULL||
image_generation_model|provider|enum('OPENAI')|NO|NULL||
image_generation_model|api_model|varchar(128)|NO|NULL||
image_generation_model|description|tinytext|YES|NULL||
image_generation_model|created_at|timestamp|NO|CURRENT_TIMESTAMP||
image_generation_model|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
image_generation_price|id|bigint(20)|NO|NULL|PRI|auto_increment
image_generation_price|quality_id|bigint(20)|NO|NULL|MUL|
image_generation_price|orientation|enum('SQUARE','PORTRAIT','LANDSCAPE')|NO|NULL||
image_generation_price|width|int(11)|NO|NULL||
image_generation_price|height|int(11)|NO|NULL||
image_generation_price|size_label|varchar(32)|NO|NULL||
image_generation_price|unit_price_usd|decimal(10,5)|NO|NULL||
image_generation_price|preferred|tinyint(1)|NO|0||
image_generation_price|created_at|timestamp|NO|CURRENT_TIMESTAMP||
image_generation_price|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
image_generation_quality|id|bigint(20)|NO|NULL|PRI|auto_increment
image_generation_quality|model_id|bigint(20)|NO|NULL|MUL|
image_generation_quality|code|varchar(32)|NO|NULL||
image_generation_quality|display_name|varchar(64)|NO|NULL||
image_generation_quality|api_quality|varchar(32)|YES|NULL||
image_generation_quality|is_default|tinyint(1)|NO|0||
image_generation_quality|position|int(11)|NO|0||
image_generation_quality|created_at|timestamp|NO|CURRENT_TIMESTAMP||
image_generation_quality|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
interaction_journey_element|id|bigint(20)|NO|NULL|PRI|auto_increment
interaction_journey_element|step_id|bigint(20)|NO|NULL|MUL|
interaction_journey_element|parent_id|bigint(20)|YES|NULL|MUL|
interaction_journey_element|order_index|int(11)|NO|NULL||
interaction_journey_element|label|varchar(255)|NO|NULL||
interaction_journey_element|type|varchar(100)|YES|NULL||
interaction_journey_element|notes|longtext|YES|NULL||
interaction_journey_element|max_quantity|int(11)|YES|NULL||
interaction_journey_element|min_quantity|int(11)|YES|NULL||
interaction_journey_step|id|bigint(20)|NO|NULL|PRI|auto_increment
interaction_journey_step|journey_id|bigint(20)|NO|NULL|MUL|
interaction_journey_step|order_index|int(11)|NO|NULL||
interaction_journey_step|title|varchar(255)|NO|NULL||
interaction_journey_step|description|longtext|YES|NULL||
journey|id|bigint(20)|NO|NULL|PRI|auto_increment
journey|template_id|bigint(20)|NO|NULL|MUL|
journey|name|varchar(255)|NO|NULL|UNI|
journey|description|tinytext|YES|NULL||
journey|status|enum('DRAFT','ACTIVE','PAUSED','COMPLETED','ARCHIVED')|NO|NULL|MUL|
journey|niche_id|bigint(20)|YES|NULL|MUL|
journey|experiment_id|bigint(20)|YES|NULL|MUL|
journey|segment_reference|varchar(255)|YES|NULL||
journey|segment_filter|tinytext|YES|NULL||
journey|start_at|datetime|YES|NULL||
journey|end_at|datetime|YES|NULL||
journey|created_at|datetime|YES|CURRENT_TIMESTAMP||
journey|updated_at|datetime|YES|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
journey_assignment|id|bigint(20)|NO|NULL|PRI|auto_increment
journey_assignment|journey_id|bigint(20)|NO|NULL|MUL|
journey_assignment|type|enum('LEAD','SEGMENT')|NO|NULL||
journey_assignment|lead_id|binary(16)|YES|NULL|MUL|
journey_assignment|segment_identifier|varchar(255)|YES|NULL||
journey_assignment|status|enum('PENDING','IN_PROGRESS','COMPLETED','STOPPED')|NO|NULL||
journey_assignment|current_step_id|bigint(20)|YES|NULL|MUL|
journey_assignment|next_step_id|bigint(20)|YES|NULL|MUL|
journey_assignment|last_event_at|datetime|YES|NULL||
journey_assignment|context_payload|tinytext|YES|NULL||
journey_assignment|created_at|datetime|YES|CURRENT_TIMESTAMP||
journey_assignment|updated_at|datetime|YES|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
journey_assignment|next_attempt_at|datetime|YES|NULL|MUL|
journey_assignment|retry_count|int(11)|YES|0||
journey_metadata|journey_id|bigint(20)|NO|NULL|PRI|
journey_metadata|meta_key|varchar(255)|NO|NULL|PRI|
journey_metadata|meta_value|longtext|YES|NULL||
journey_step|id|bigint(20)|NO|NULL|PRI|auto_increment
journey_step|template_id|bigint(20)|NO|NULL|MUL|
journey_step|position|int(11)|NO|NULL||
journey_step|name|varchar(255)|YES|NULL||
journey_step|description|tinytext|YES|NULL||
journey_step|phase|enum('ATTENTION','INTEREST','DESIRE','ACTION')|NO|NULL||
journey_step|stimulus_type|enum('AD','EMAIL','WHATSAPP','LANDING_PAGE','INSTANT_FORM','LEAD_PORTAL_IMAGE_FLOW','SHOWCASE_IMAGE','PAYMENT_PAGE')|NO|NULL||
journey_step|creative_id|bigint(20)|YES|NULL|MUL|
journey_step|angle_id|bigint(20)|YES|NULL|MUL|
journey_step|visual_proof_id|bigint(20)|YES|NULL|MUL|
journey_step|emotional_trigger_id|bigint(20)|YES|NULL|MUL|
journey_step|entry_condition|varchar(255)|YES|NULL||
journey_step|exit_condition|varchar(255)|YES|NULL||
journey_step|delay_minutes|int(11)|YES|NULL||
journey_step_metadata|step_id|bigint(20)|NO|NULL|PRI|
journey_step_metadata|meta_key|varchar(255)|NO|NULL|PRI|
journey_step_metadata|meta_value|longtext|YES|NULL||
journey_template|id|bigint(20)|NO|NULL|PRI|auto_increment
journey_template|name|varchar(255)|NO|NULL|UNI|
journey_template|description|tinytext|YES|NULL||
journey_template|objective|varchar(255)|YES|NULL||
journey_template|preferred_channel|varchar(100)|YES|NULL||
journey_template|created_at|datetime|YES|CURRENT_TIMESTAMP||
journey_template|updated_at|datetime|YES|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
journey_template_metadata|template_id|bigint(20)|NO|NULL|PRI|
journey_template_metadata|meta_key|varchar(255)|NO|NULL|PRI|
journey_template_metadata|meta_value|longtext|YES|NULL||
journey_template_phase|template_id|bigint(20)|NO|NULL|PRI|
journey_template_phase|phase_order|int(11)|NO|NULL|PRI|
journey_template_phase|phase|enum('ATTENTION','INTEREST','DESIRE','ACTION')|NO|NULL||
journey_template_tag|template_id|bigint(20)|NO|NULL|PRI|
journey_template_tag|tag|varchar(255)|NO|NULL|PRI|
lead_portal_flow|id|bigint(20)|NO|NULL|PRI|auto_increment
lead_portal_flow|name|varchar(150)|NO|NULL||
lead_portal_flow|slug|varchar(120)|NO|NULL|UNI|
lead_portal_flow|description|longtext|YES|NULL||
lead_portal_flow|created_at|timestamp|NO|CURRENT_TIMESTAMP||
lead_portal_flow|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
lead_portal_flow|model|varchar(128)|YES|NULL||
lead_portal_flow|prompt|longtext|YES|NULL||
lead_portal_flow|approved|tinyint(1)|NO|0||
lead_portal_flow|approved_at|timestamp|YES|NULL||
lead_portal_flow|experiment_id|bigint(20)|YES|NULL|MUL|
lead_portal_flow|market_niche_id|bigint(20)|YES|NULL|MUL|
lead_portal_flow|cost_usd|decimal(10,4)|YES|NULL||
lead_portal_flow|simple_form_style_id|bigint(20)|YES|NULL|MUL|
lead_portal_flow_question|id|bigint(20)|NO|NULL|PRI|auto_increment
lead_portal_flow_question|flow_id|bigint(20)|NO|NULL|MUL|
lead_portal_flow_question|title|longtext|YES|NULL||
lead_portal_flow_question|data_key|varchar(120)|NO|NULL||
lead_portal_flow_question|type|enum('TEXT','TEXTAREA','NUMBER','EMAIL','PHONE','DATE','SINGLE_CHOICE','MULTIPLE_CHOICE','IMAGE_UPLOAD')|NO|NULL||
lead_portal_flow_question|required|tinyint(1)|NO|NULL||
lead_portal_flow_question|description|longtext|YES|NULL||
lead_portal_flow_question|placeholder|longtext|YES|NULL||
lead_portal_flow_question|position_index|int(11)|NO|NULL||
lead_portal_flow_question_option|id|bigint(20)|NO|NULL|PRI|auto_increment
lead_portal_flow_question_option|question_id|bigint(20)|NO|NULL|MUL|
lead_portal_flow_question_option|option_order|int(11)|NO|NULL||
lead_portal_flow_question_option|option_value|longtext|YES|NULL||
lead_portal_premium_delivery|id|bigint(20)|NO|NULL|PRI|auto_increment
lead_portal_premium_delivery|purchase_id|bigint(20)|NO|NULL|UNI|
lead_portal_premium_delivery|package_id|bigint(20)|NO|NULL||
lead_portal_premium_delivery|submission_id|varchar(36)|YES|NULL||
lead_portal_premium_delivery|submission_name|varchar(255)|YES|NULL||
lead_portal_premium_delivery|submission_email|varchar(320)|YES|NULL||
lead_portal_premium_delivery|buyer_name|varchar(255)|YES|NULL||
lead_portal_premium_delivery|buyer_email|varchar(320)|YES|NULL||
lead_portal_premium_delivery|recipient_name|varchar(255)|YES|NULL||
lead_portal_premium_delivery|recipient_email|varchar(320)|NO|NULL||
lead_portal_premium_delivery|status|varchar(30)|NO|PENDING_ZIP|MUL|
lead_portal_premium_delivery|zip_object_key|varchar(512)|YES|NULL||
lead_portal_premium_delivery|zip_download_url|varchar(1024)|YES|NULL||
lead_portal_premium_delivery|zip_size_bytes|bigint(20)|YES|NULL||
lead_portal_premium_delivery|zip_generated_at|timestamp|YES|NULL||
lead_portal_premium_delivery|zip_attempts|int(11)|NO|0||
lead_portal_premium_delivery|zip_last_attempt|timestamp|YES|NULL||
lead_portal_premium_delivery|zip_last_error|text|YES|NULL||
lead_portal_premium_delivery|email_request_id|varchar(64)|YES|NULL||
lead_portal_premium_delivery|email_sent_at|timestamp|YES|NULL||
lead_portal_premium_delivery|email_attempts|int(11)|NO|0||
lead_portal_premium_delivery|email_last_attempt|timestamp|YES|NULL||
lead_portal_premium_delivery|email_last_error|text|YES|NULL||
lead_portal_premium_delivery|created_at|timestamp|NO|CURRENT_TIMESTAMP||
lead_portal_premium_delivery|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
lead_portal_purchase|id|bigint(20)|NO|NULL|PRI|auto_increment
lead_portal_purchase|package_id|bigint(20)|NO|NULL|MUL|
lead_portal_purchase|submission_id|varchar(64)|YES|NULL||
lead_portal_purchase|buyer_name|varchar(255)|YES|NULL||
lead_portal_purchase|buyer_email|varchar(320)|YES|NULL||
lead_portal_purchase|status|varchar(40)|NO|NULL||
lead_portal_purchase|mp_preference_id|varchar(150)|YES|NULL||
lead_portal_purchase|mp_payment_id|varchar(150)|YES|NULL|MUL|
lead_portal_purchase|mp_status|varchar(80)|YES|NULL||
lead_portal_purchase|checkout_url|varchar(1200)|YES|NULL||
lead_portal_purchase|checkout_expires_at|timestamp|YES|NULL||
lead_portal_purchase|amount|decimal(12,2)|YES|NULL||
lead_portal_purchase|currency|varchar(8)|YES|NULL||
lead_portal_purchase|notification_payload|longtext|YES|NULL||
lead_portal_purchase|mp_payment_payload|longtext|YES|NULL||
lead_portal_purchase|delivery_attempts|int(11)|YES|0||
lead_portal_purchase|delivery_error|longtext|YES|NULL||
lead_portal_purchase|delivered_at|timestamp|YES|NULL||
lead_portal_purchase|payment_approved_at|timestamp|YES|NULL||
lead_portal_purchase|pixel_conversion_recorded_at|timestamp|YES|NULL||
lead_portal_purchase|zip_object_key|varchar(512)|YES|NULL||
lead_portal_purchase|zip_size_bytes|bigint(20)|YES|NULL||
lead_portal_purchase|zip_generated_at|timestamp|YES|NULL||
lead_portal_purchase|created_at|timestamp|NO|CURRENT_TIMESTAMP||
lead_portal_purchase|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
market_niche|id|bigint(20)|NO|NULL|PRI|auto_increment
market_niche|created_at|datetime(6)|YES|NULL||
market_niche|demand_volume|longtext|YES|NULL||
market_niche|description|longtext|YES|NULL||
market_niche|name|varchar(255)|YES|NULL||
market_niche|offers|longtext|YES|NULL||
market_niche|promises|longtext|YES|NULL||
market_niche|updated_at|datetime(6)|YES|NULL||
market_niche|base_segmentation|longtext|YES|NULL||
market_niche|demographic_filters|longtext|YES|NULL||
market_niche|extra_tips|longtext|YES|NULL||
market_niche|interests|longtext|YES|NULL||
market_niche|chat_dialog_id|bigint(20)|YES|NULL|MUL|
market_niche|hypotheses_to_generate|int(11)|YES|NULL||
market_niche|interest_category|varchar(255)|YES|NULL||
market_niche|role_category|varchar(255)|YES|NULL||
market_niche|hypothesis_model|varchar(191)|YES|NULL||
market_niche|differentiated_technology_id|bigint(20)|YES|NULL|MUL|
market_niche|cost|decimal(10,2)|YES|NULL||
market_niche|expense|decimal(10,2)|YES|NULL||
market_niche|total_cost|decimal(12,2)|YES|NULL||
market_niche|total_revenue|decimal(12,2)|YES|NULL||
market_niche|detailed_descriptions_to_generate|int(11)|YES|NULL||
market_niche|detailed_description_model|varchar(191)|YES|NULL||
market_niche|hypothesis_detailed_description_id|bigint(20)|YES|NULL|MUL|
market_niche|interests_to_generate|int(11)|YES|NULL||
market_niche|job_titles_to_generate|int(11)|YES|NULL||
market_niche|behaviors_to_generate|int(11)|YES|NULL||
market_niche|interest_model|varchar(191)|YES|NULL||
market_niche|job_title_model|varchar(191)|YES|NULL||
market_niche|behavior_model|varchar(191)|YES|NULL||
market_niche|interest_list|longtext|YES|NULL||
market_niche|role_list|longtext|YES|NULL||
market_niche|behavior_list|longtext|YES|NULL||
mercadopago_webhook_log|id|bigint(20)|NO|NULL|PRI|auto_increment
mercadopago_webhook_log|resource_id|varchar(150)|YES|NULL|MUL|
mercadopago_webhook_log|topic|varchar(100)|YES|NULL||
mercadopago_webhook_log|query_id|varchar(150)|YES|NULL||
mercadopago_webhook_log|query_topic|varchar(100)|YES|NULL||
mercadopago_webhook_log|payload_type|varchar(100)|YES|NULL||
mercadopago_webhook_log|payload_action|varchar(100)|YES|NULL||
mercadopago_webhook_log|has_payload|tinyint(1)|YES|NULL||
mercadopago_webhook_log|payload|longtext|YES|NULL||
mercadopago_webhook_log|mercadopago_status|varchar(80)|YES|NULL||
mercadopago_webhook_log|mercadopago_response|longtext|YES|NULL||
mercadopago_webhook_log|processing_status|varchar(40)|NO|NULL||
mercadopago_webhook_log|error_message|longtext|YES|NULL||
mercadopago_webhook_log|created_at|timestamp|NO|CURRENT_TIMESTAMP|MUL|
metric_preset|id|varchar(50)|NO|NULL|PRI|
metric_preset|name|varchar(100)|YES|NULL||
metric_preset|sample_size|int(11)|YES|NULL||
metric_preset|stop_loss_factor|decimal(5,2)|YES|NULL||
metric_preset|default_mde_pp|decimal(5,2)|YES|NULL||
niche_detailed_description|id|bigint(20)|NO|NULL|PRI|auto_increment
niche_detailed_description|market_niche_id|bigint(20)|NO|NULL|MUL|
niche_detailed_description|prompt_id|bigint(20)|YES|NULL|MUL|
niche_detailed_description|title|varchar(255)|YES|NULL||
niche_detailed_description|description|longtext|YES|NULL||
niche_detailed_description|pains|longtext|YES|NULL||
niche_detailed_description|desires|longtext|YES|NULL||
niche_detailed_description|needs|longtext|YES|NULL||
niche_detailed_description|prompt|longtext|YES|NULL||
niche_detailed_description|model|varchar(191)|YES|NULL||
niche_detailed_description|cost_usd|decimal(10,4)|YES|NULL||
niche_detailed_description|input_tokens|int(11)|YES|NULL||
niche_detailed_description|output_tokens|int(11)|YES|NULL||
niche_detailed_description|created_at|timestamp|NO|CURRENT_TIMESTAMP||
niche_detailed_description|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
niche_detailed_description|active|tinyint(1)|NO|1||
openai_model|id|bigint(20)|NO|NULL|PRI|auto_increment
openai_model|name|varchar(255)|NO|NULL||
openai_model|code|varchar(128)|NO|NULL|UNI|
openai_model|price_input_standard|decimal(12,5)|NO|NULL||
openai_model|price_input_cached_standard|decimal(12,5)|NO|NULL||
openai_model|price_output_standard|decimal(12,5)|NO|NULL||
openai_model|price_input_batch|decimal(12,5)|NO|NULL||
openai_model|price_input_cached_batch|decimal(12,5)|NO|NULL||
openai_model|price_output_batch|decimal(12,5)|NO|NULL||
openai_model|created_at|timestamp|NO|CURRENT_TIMESTAMP||
openai_model|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
prompt|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt|name|varchar(191)|NO|NULL||
prompt|domain|varchar(100)|NO|NULL|MUL|
prompt|template|longtext|NO|NULL||
prompt|active|tinyint(1)|NO|0||
prompt|created_at|timestamp|NO|CURRENT_TIMESTAMP||
prompt|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
prompt_attribute|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt_attribute|created_at|datetime(6)|YES|NULL||
prompt_attribute|description|longtext|YES|NULL||
prompt_attribute|name|varchar(255)|NO|NULL||
prompt_attribute|updated_at|datetime(6)|YES|NULL||
prompt_attribute|prompt_entity_id|bigint(20)|YES|NULL|MUL|
prompt_attribute_description|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt_attribute_description|created_at|datetime(6)|YES|NULL||
prompt_attribute_description|description|longtext|YES|NULL||
prompt_attribute_description|updated_at|datetime(6)|YES|NULL||
prompt_attribute_description|prompt_attribute_id|bigint(20)|YES|NULL|MUL|
prompt_attribute_description|active|tinyint(1)|YES|1||
prompt_domain|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt_domain|code|varchar(100)|NO|NULL|UNI|
prompt_domain|name|varchar(191)|NO|NULL||
prompt_domain|description|varchar(500)|YES|NULL||
prompt_domain|created_at|timestamp|NO|CURRENT_TIMESTAMP||
prompt_domain|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
prompt_domain_object|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt_domain_object|prompt_domain_id|bigint(20)|NO|NULL|MUL|
prompt_domain_object|object_type|enum('DETAILED_DESCRIPTION','DIFFERENTIATED_TECHNOLOGY','NICHE','JOURNEY','EXPERIMENT','HYPOTHESIS')|NO|NULL||
prompt_entity|id|bigint(20)|NO|NULL|PRI|auto_increment
prompt_entity|created_at|datetime(6)|YES|NULL||
prompt_entity|name|varchar(255)|NO|NULL|UNI|
prompt_entity|updated_at|datetime(6)|YES|NULL||
sales_funnel|id|binary(16)|NO|NULL|PRI|
sales_funnel|name|varchar(100)|YES|NULL||
sales_funnel|objective|varchar(255)|YES|NULL||
sales_funnel|created_at|timestamp|YES|CURRENT_TIMESTAMP||
success_product|id|bigint(20)|NO|NULL|PRI|auto_increment
success_product|description|longtext|YES|NULL||
success_product|novo|tinyint(1)|YES|1||
success_product|niche|varchar(255)|YES|NULL||
success_product|avatar|varchar(255)|YES|NULL||
success_product|instagram_account_id|bigint(20)|YES|NULL|MUL|
success_product|explicit_pain|longtext|YES|NULL||
success_product|promise|longtext|YES|NULL||
success_product|unique_mechanism|longtext|YES|NULL||
success_product|tripwire|longtext|YES|NULL||
success_product|risk_reversal|longtext|YES|NULL||
success_product|social_proof|longtext|YES|NULL||
success_product|checkout_monetization|longtext|YES|NULL||
success_product|funnel|longtext|YES|NULL||
success_product|creative_volume|longtext|YES|NULL||
success_product|storytelling|longtext|YES|NULL||
success_product|created_at|timestamp|NO|CURRENT_TIMESTAMP||
success_product|updated_at|timestamp|NO|CURRENT_TIMESTAMP||on update CURRENT_TIMESTAMP
success_product|name|varchar(255)|YES|NULL||
success_product|facebook_url|varchar(255)|YES|NULL||
success_product|instagram_url|varchar(255)|YES|NULL||
success_product|sales_page_url|varchar(255)|YES|NULL||
success_product|youtube_url|varchar(255)|YES|NULL||
success_product|audience_type|varchar(255)|YES|NULL||
success_product|sales_funnel|longtext|YES|NULL||
success_product|platform|enum('COFRE','HOTMART','CLICKBANK')|NO|NULL||
success_product|generate_niche_hypothesis|bit(1)|NO|NULL||
targeting_candidate|id|bigint(20)|NO|NULL|PRI|auto_increment
targeting_candidate|request_id|binary(16)|NO|NULL|MUL|
targeting_candidate|texto_sugerido|varchar(255)|NO|NULL||
targeting_candidate|type|enum('INTEREST','BEHAVIOR','WORK_POSITION')|NO|NULL||
targeting_candidate|status|enum('PENDING_FACEBOOK_MATCH','VALIDATED','NO_MATCH')|NO|NULL||
targeting_candidate|idioma|varchar(10)|YES|NULL||
targeting_candidate|country|varchar(5)|YES|NULL|MUL|
targeting_candidate|origem|varchar(32)|YES|NULL||
targeting_candidate|intent_tag|varchar(32)|YES|NULL||
targeting_candidate|score|decimal(5,4)|YES|NULL||
targeting_candidate|rationale|longtext|YES|NULL||
targeting_candidate|rejection_reason|longtext|YES|NULL||
targeting_candidate|created_at|datetime(6)|YES|NULL||
targeting_candidate|updated_at|datetime(6)|YES|NULL||
targeting_candidate_seed_variant|candidate_id|bigint(20)|NO|NULL|PRI|
targeting_candidate_seed_variant|variant_order|int(11)|NO|NULL|PRI|
targeting_candidate_seed_variant|variant_value|varchar(255)|NO|NULL||
targeting_element|id|bigint(20)|NO|NULL|PRI|auto_increment
targeting_element|market_niche_id|bigint(20)|NO|NULL|MUL|
targeting_element|hypothesis_id|binary(16)|YES|NULL|MUL|
targeting_element|type|enum('INTEREST','JOB_TITLE','BEHAVIOR')|NO|NULL||
targeting_element|term|varchar(255)|NO|NULL||
targeting_element|description|longtext|YES|NULL||
targeting_element|prompt|longtext|YES|NULL||
targeting_element|model|varchar(191)|YES|NULL||
targeting_element|source|enum('MANUAL','AI')|YES|NULL||
targeting_element|status|enum('DRAFT','NEEDS_REVIEW','APPROVED','REJECTED')|NO|NULL||
targeting_element|notes|longtext|YES|NULL||
targeting_element|last_reviewed_by|varchar(191)|YES|NULL||
targeting_element|meta_id|varchar(100)|YES|NULL||
targeting_element|meta_key|varchar(191)|YES|NULL||
targeting_element|confidence|decimal(10,4)|YES|NULL||
targeting_element|created_at|datetime(6)|YES|NULL||
targeting_element|updated_at|datetime(6)|YES|NULL||
targeting_element|meta_audience_size_lower_bound|bigint(20)|YES|NULL||
targeting_element|meta_audience_size_upper_bound|bigint(20)|YES|NULL||
targeting_option|id|bigint(20)|NO|NULL|PRI|auto_increment
targeting_option|candidate_id|bigint(20)|NO|NULL|MUL|
targeting_option|facebook_id|varchar(100)|NO|NULL||
targeting_option|name|varchar(255)|NO|NULL||
targeting_option|type|enum('INTEREST','BEHAVIOR','WORK_POSITION')|NO|NULL||
targeting_option|audience_size|bigint(20)|YES|NULL||
targeting_option|match_score|decimal(5,4)|YES|NULL||
targeting_option|final_score|decimal(5,4)|YES|NULL||
targeting_option|search_locale|varchar(10)|YES|NULL||
targeting_option|search_country|varchar(5)|YES|NULL||
targeting_option|search_term|varchar(255)|YES|NULL||
targeting_option|source|enum('SEARCH','SUGGESTION','BROWSE')|YES|NULL||
targeting_option|seed_variant|varchar(255)|YES|NULL||
targeting_option|created_at|datetime(6)|YES|NULL||
targeting_option|updated_at|datetime(6)|YES|NULL||
targeting_request|id|binary(16)|NO|NULL|PRI|
targeting_request|descricao|varchar(500)|NO|NULL||
targeting_request|locale|varchar(10)|YES|NULL||
targeting_request|country|varchar(5)|YES|NULL||
targeting_request|audience_type|enum('PROSPECT','REMARKETING')|YES|NULL||
targeting_request|status|enum('PENDING_AI','COMPLETED','FAILED')|YES|NULL|MUL|
targeting_request|origin|enum('CLIENT','INTERNAL')|YES|NULL||
targeting_request|market_niche_id|bigint(20)|YES|NULL|MUL|
targeting_request|hypothesis_id|binary(16)|YES|NULL|MUL|
targeting_request|experiment_id|bigint(20)|YES|NULL|MUL|
targeting_request|created_at|datetime(6)|YES|NULL||
targeting_request|updated_at|datetime(6)|YES|NULL||
targeting_resolution_job|id|bigint(20)|NO|NULL|PRI|auto_increment
targeting_resolution_job|candidate_id|bigint(20)|NO|NULL|UNI|
targeting_resolution_job|request_id|binary(16)|NO|NULL|MUL|
targeting_resolution_job|status|enum('PENDING','PROCESSING','SUCCEEDED','FAILED')|NO|NULL|MUL|
targeting_resolution_job|attempt_count|int(11)|NO|0||
targeting_resolution_job|result_count|int(11)|YES|NULL||
targeting_resolution_job|last_error|text|YES|NULL||
targeting_resolution_job|locked_by|varchar(64)|YES|NULL||
targeting_resolution_job|locked_at|datetime(6)|YES|NULL||
targeting_resolution_job|started_at|datetime(6)|YES|NULL||
targeting_resolution_job|finished_at|datetime(6)|YES|NULL||
targeting_resolution_job|created_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||
targeting_resolution_job|updated_at|datetime(6)|NO|CURRENT_TIMESTAMP(6)||on update CURRENT_TIMESTAMP(6)
visual_proof|id|bigint(20)|NO|NULL|PRI|auto_increment
visual_proof|description|tinytext|YES|NULL||
visual_proof|name|varchar(255)|YES|NULL||
visual_proof|proof_type|varchar(255)|YES|NULL||
```

## Explicação das tabelas e campos

> Seção de apoio funcional para facilitar leitura do snapshot técnico acima. As descrições abaixo são orientadas pelo nome dos objetos e pelo contexto do produto.

### `ad_set`
- **Finalidade da tabela:** Armazena dados relacionados a **ad_set** no contexto do Marketing Hub.
- **Campos (15):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `budget`: Campo usado para armazenar informações de **orçamento**. (tipo `decimal(38,2)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `duration_days`: Campo usado para armazenar informações de **duração dias**. (tipo `int(11)`; opcional).
  - `interests`: Campo usado para armazenar informações de **interests**. (tipo `tinytext`; opcional).
  - `location`: Campo usado para armazenar informações de **localização**. (tipo `varchar(255)`; opcional).
  - `lookalikes`: Campo usado para armazenar informações de **audiências semelhantes**. (tipo `tinytext`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `targeting_request_id`: Chave de referência para o registro relacionado de **targeting_request**. (tipo `binary(16)`; opcional; chave `MUL`).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `text`; opcional).
  - `targeting_json`: Campo usado para armazenar informações de **segmentação estrutura JSON**. (tipo `text`; opcional).
  - `behaviors`: Campo usado para armazenar informações de **comportamentos**. (tipo `text`; opcional).
  - `job_titles`: Campo usado para armazenar informações de **job cargos**. (tipo `text`; opcional).

### `agent_theme`
- **Finalidade da tabela:** Armazena dados relacionados a **agent_theme** no contexto do Marketing Hub.
- **Campos (5):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório; chave `UNI`).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `ai_service`
- **Finalidade da tabela:** Armazena dados relacionados a **ai_service** no contexto do Marketing Hub.
- **Campos (10):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `cost`: Campo usado para armazenar informações de **custo**. (tipo `decimal(38,2)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `objective`: Campo usado para armazenar informações de **objetivo**. (tipo `tinytext`; opcional).
  - `price`: Campo usado para armazenar informações de **preço**. (tipo `decimal(38,2)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `url`: URL principal associada ao registro. (tipo `varchar(255)`; opcional).
  - `phase`: Campo usado para armazenar informações de **fase**. (tipo `varchar(255)`; opcional).
  - `observation`: Campo usado para armazenar informações de **observações**. (tipo `tinytext`; opcional).

### `ai_worker_generation`
- **Finalidade da tabela:** Armazena artefatos e saídas geradas manualmente ou por fluxos do Worker IA.
- **Campos (10):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `domain`: Campo usado para armazenar informações de **domínio**. (tipo `varchar(100)`; obrigatório; chave `MUL`).
  - `reference_id`: Chave de referência para o registro relacionado de **reference**. (tipo `varchar(100)`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(191)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `raw_response`: Campo usado para armazenar informações de **resposta bruta resposta**. (tipo `longtext`; opcional).
  - `input_tokens`: Campo usado para armazenar informações de **entrada tokens**. (tipo `int(11)`; opcional).
  - `output_tokens`: Campo usado para armazenar informações de **saída tokens**. (tipo `int(11)`; opcional).
  - `cost_usd`: Campo usado para armazenar informações de **custo usd**. (tipo `decimal(10,4)`; obrigatório; default `0.0000`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).

### `angle`
- **Finalidade da tabela:** Armazena dados relacionados a **angle** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `frame_type`: Campo usado para armazenar informações de **enquadramento tipo**. (tipo `varchar(255)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).

### `asset`
- **Finalidade da tabela:** Armazena artefatos e saídas geradas manualmente ou por fluxos do Worker IA.
- **Campos (12):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `campaign_id`: Chave de referência para o registro relacionado de **campaign**. (tipo `bigint(20)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `external_id`: Identificador do recurso em sistema externo. (tipo `varchar(255)`; opcional).
  - `payload`: Carga útil em formato textual/JSON retornada por integração. (tipo `longtext`; opcional).
  - `provider`: Campo usado para armazenar informações de **provedor**. (tipo `enum('SYNTHESIA','HEYGEN','ELEVENLABS','RUNWAY','OPENAI','WATERMARKER')`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING','PROCESSING','READY','FAILED')`; opcional).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('VIDEO','AUDIO','BROLL','IMAGE')`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `url`: URL principal associada ao registro. (tipo `varchar(255)`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).

### `chat_dialog`
- **Finalidade da tabela:** Armazena dados relacionados a **chat_dialog** no contexto do Marketing Hub.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `url`: URL principal associada ao registro. (tipo `varchar(500)`; opcional).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `theme`: Campo usado para armazenar informações de **tema**. (tipo `varchar(255)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `creative`
- **Finalidade da tabela:** Armazena artefatos e saídas geradas manualmente ou por fluxos do Worker IA.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `headline`: Campo usado para armazenar informações de **headline**. (tipo `varchar(255)`; opcional).
  - `primary_text`: Campo usado para armazenar informações de **texto principal texto**. (tipo `varchar(255)`; opcional).
  - `image_url`: Campo usado para armazenar informações de **imagem url**. (tipo `varchar(500)`; opcional).
  - `ad_format`: Campo usado para armazenar informações de **ad formato**. (tipo `varchar(32)`; opcional).
  - `description`: Descrição textual do registro. (tipo `varchar(255)`; opcional).
  - `call_to_action`: Campo usado para armazenar informações de **chamada to ação**. (tipo `varchar(32)`; opcional).
  - `destination_url`: Campo usado para armazenar informações de **destino url**. (tipo `varchar(512)`; opcional).
  - `lead_gen_form_id`: Chave de referência para o registro relacionado de **lead_gen_form**. (tipo `varchar(64)`; opcional).
  - `instagram_user_id`: Chave de referência para o registro relacionado de **instagram_user**. (tipo `varchar(64)`; opcional).
  - `image_hash`: Campo usado para armazenar informações de **imagem hash**. (tipo `varchar(255)`; opcional).
  - `video_id`: Chave de referência para o registro relacionado de **video**. (tipo `varchar(255)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('DRAFT','READY')`; opcional).

### `creative_variants`
- **Finalidade da tabela:** Armazena artefatos e saídas geradas manualmente ou por fluxos do Worker IA.
- **Campos (8):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `headline`: Campo usado para armazenar informações de **headline**. (tipo `varchar(255)`; opcional).
  - `image_url`: Campo usado para armazenar informações de **imagem url**. (tipo `varchar(255)`; opcional).
  - `primary_text`: Campo usado para armazenar informações de **texto principal texto**. (tipo `varchar(255)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('DRAFT','READY')`; opcional).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `image_hash`: Campo usado para armazenar informações de **imagem hash**. (tipo `varchar(255)`; opcional).
  - `video_id`: Chave de referência para o registro relacionado de **video**. (tipo `varchar(255)`; opcional).

### `DATABASECHANGELOG`
- **Finalidade da tabela:** Tabela de controle do Liquibase com o histórico de execução de migrations no banco.
- **Campos (14):**
  - `ID`: Campo usado para armazenar informações de **ID**. (tipo `varchar(255)`; obrigatório).
  - `AUTHOR`: Campo usado para armazenar informações de **AUTHOR**. (tipo `varchar(255)`; obrigatório).
  - `FILENAME`: Campo usado para armazenar informações de **FILENAME**. (tipo `varchar(255)`; obrigatório).
  - `DATEEXECUTED`: Campo usado para armazenar informações de **DATEEXECUTED**. (tipo `datetime`; obrigatório).
  - `ORDEREXECUTED`: Campo usado para armazenar informações de **ORDEREXECUTED**. (tipo `int(11)`; obrigatório).
  - `EXECTYPE`: Campo usado para armazenar informações de **EXECTYPE**. (tipo `varchar(10)`; obrigatório).
  - `MD5SUM`: Campo usado para armazenar informações de **MD5SUM**. (tipo `varchar(35)`; opcional).
  - `DESCRIPTION`: Campo usado para armazenar informações de **DESCRIPTION**. (tipo `varchar(255)`; opcional).
  - `COMMENTS`: Campo usado para armazenar informações de **COMMENTS**. (tipo `varchar(255)`; opcional).
  - `TAG`: Campo usado para armazenar informações de **TAG**. (tipo `varchar(255)`; opcional).
  - `LIQUIBASE`: Campo usado para armazenar informações de **LIQUIBASE**. (tipo `varchar(20)`; opcional).
  - `CONTEXTS`: Campo usado para armazenar informações de **CONTEXTS**. (tipo `varchar(255)`; opcional).
  - `LABELS`: Campo usado para armazenar informações de **LABELS**. (tipo `varchar(255)`; opcional).
  - `DEPLOYMENT_ID`: Campo usado para armazenar informações de **DEPLOYMENT ID**. (tipo `varchar(10)`; opcional).

### `deliverable`
- **Finalidade da tabela:** Armazena artefatos e saídas geradas manualmente ou por fluxos do Worker IA.
- **Campos (9):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `title`: Campo usado para armazenar informações de **título**. (tipo `varchar(255)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `content`: Campo usado para armazenar informações de **conteúdo**. (tipo `longtext`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `email_log`
- **Finalidade da tabela:** Armazena dados relacionados a **email_log** no contexto do Marketing Hub.
- **Campos (10):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `request_id`: Chave de referência para o registro relacionado de **request**. (tipo `varchar(36)`; obrigatório; chave `UNI`).
  - `recipients`: Campo usado para armazenar informações de **recipients**. (tipo `text`; obrigatório).
  - `subject`: Campo usado para armazenar informações de **assunto**. (tipo `varchar(255)`; obrigatório).
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `varchar(100)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(20)`; obrigatório).
  - `error_message`: Campo usado para armazenar informações de **erro message**. (tipo `text`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `sent_at`: Campo usado para armazenar informações de **envio at**. (tipo `timestamp`; opcional).
  - `opened_at`: Campo usado para armazenar informações de **abertura at**. (tipo `timestamp`; opcional).

### `emotional_trigger`
- **Finalidade da tabela:** Armazena dados relacionados a **emotional_trigger** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `valence`: Campo usado para armazenar informações de **valência**. (tipo `enum('POS','NEG')`; opcional).

### `experiment`
- **Finalidade da tabela:** Armazena dados relacionados a **experiment** no contexto do Marketing Hub.
- **Campos (49):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `end_date`: Campo usado para armazenar informações de **fim date**. (tipo `date`; opcional).
  - `hypothesis`: Campo usado para armazenar informações de **hipótese**. (tipo `varchar(255)`; opcional).
  - `kpi_goal`: Campo usado para armazenar informações de **KPI goal**. (tipo `decimal(38,2)`; opcional).
  - `start_date`: Campo usado para armazenar informações de **início date**. (tipo `date`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `kpi_target`: Campo usado para armazenar informações de **KPI meta**. (tipo `decimal(38,2)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `platform`: Campo usado para armazenar informações de **plataforma**. (tipo `enum('FACEBOOK')`; opcional).
  - `niche_id`: Chave de referência para o registro relacionado de **niche**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `hypothesis_id`: Chave de referência para o registro relacionado de **hypothesis**. (tipo `binary(16)`; obrigatório; chave `MUL`).
  - `mde`: Campo usado para armazenar informações de **mde**. (tipo `decimal(5,2)`; opcional).
  - `sample_size`: Campo usado para armazenar informações de **amostra size**. (tipo `int(11)`; opcional).
  - `stop_loss_cpl`: Campo usado para armazenar informações de **stop loss cpl**. (tipo `decimal(10,2)`; opcional).
  - `kpi_target_cpl`: Campo usado para armazenar informações de **KPI meta cpl**. (tipo `decimal(10,2)`; opcional; default `45.00`).
  - `baseline_cvr`: Campo usado para armazenar informações de **linha de base cvr**. (tipo `decimal(5,2)`; opcional; default `3.00`).
  - `target_cvr`: Campo usado para armazenar informações de **meta cvr**. (tipo `decimal(5,2)`; opcional; default `5.00`).
  - `mde_percent`: Campo usado para armazenar informações de **mde percent**. (tipo `decimal(5,2)`; opcional; default `40.00`).
  - `metric_preset_id`: Chave de referência para o registro relacionado de **metric_preset**. (tipo `varchar(50)`; opcional; chave `MUL`).
  - `journey_template_id`: Chave de referência para o registro relacionado de **journey_template**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `creatives_to_generate`: Campo usado para armazenar informações de **creatives to generate**. (tipo `int(11)`; opcional).
  - `instant_forms_to_generate`: Campo usado para armazenar informações de **instantâneo forms to generate**. (tipo `int(11)`; opcional).
  - `emails_to_generate`: Campo usado para armazenar informações de **emails to generate**. (tipo `int(11)`; opcional).
  - `deliverables_to_generate`: Campo usado para armazenar informações de **deliverables to generate**. (tipo `int(11)`; opcional).
  - `creative_approved`: Campo usado para armazenar informações de **creative approved**. (tipo `bit(1)`; obrigatório).
  - `facebook_page_id`: Chave de referência para o registro relacionado de **facebook_page**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `facebook_instant_form_id`: Chave de referência para o registro relacionado de **facebook_instant_form**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `facebook_pixel_id`: Chave de referência para o registro relacionado de **facebook_pixel**. (tipo `varchar(64)`; opcional).
  - `facebook_pixel_code`: Campo usado para armazenar informações de **facebook pixel code**. (tipo `longtext`; opcional).
  - `facebook_pixel_created_at`: Campo usado para armazenar informações de **facebook pixel created at**. (tipo `timestamp`; opcional).
  - `facebook_release_requested_at`: Instante em que o operador liberou o experimento para o Facebook Ads Worker. (tipo `datetime`; opcional).
  - `instagram_account_id`: Chave de referência para o registro relacionado de **instagram_account**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `follow_up_action_url`: Campo usado para armazenar informações de **follow up ação url**. (tipo `varchar(512)`; opcional).
  - `lead_portal_flow_id`: Chave de referência para o registro relacionado de **lead_portal_flow**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `lead_portal_flows_to_generate`: Campo usado para armazenar informações de **lead portal flows to generate**. (tipo `int(11)`; opcional).
  - `images_per_package`: Campo usado para armazenar informações de **images per package**. (tipo `int(11)`; obrigatório; default `20`).
  - `open_images_per_package`: Campo usado para armazenar informações de **open images per package**. (tipo `int(11)`; opcional).
  - `compressed_images_per_package`: Campo usado para armazenar informações de **compressed images per package**. (tipo `int(11)`; opcional).
  - `daily_budget`: Campo usado para armazenar informações de **daily orçamento**. (tipo `decimal(10,2)`; opcional).
  - `unit_price_brl`: Campo usado para armazenar informações de **unit preço brl**. (tipo `decimal(10,2)`; opcional).
  - `image_model_id`: Chave de referência para o registro relacionado de **image_model**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `image_model_quality_id`: Chave de referência para o registro relacionado de **image_model_quality**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `sample_emails_to_generate`: Campo usado para armazenar informações de **amostra emails to generate**. (tipo `int(11)`; opcional).
  - `selected_sample_email_id`: Chave de referência para o registro relacionado de **selected_sample_email**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `cost`: Campo usado para armazenar informações de **custo**. (tipo `decimal(10,2)`; opcional).
  - `expense`: Campo usado para armazenar informações de **expense**. (tipo `decimal(10,2)`; opcional).
  - `total_cost`: Campo usado para armazenar informações de **total custo**. (tipo `decimal(12,2)`; opcional).
  - `lead_portal_flow_model`: Campo usado para armazenar informações de **lead portal flow model**. (tipo `varchar(191)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PLANNED','RUNNING','PAUSED','VALIDATED','INVALIDATED','INCONCLUSIVE','FINISHED','FAILED')`; opcional).

### `experiment_adset_job`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (16):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `workflow_id`: Chave de referência para o registro relacionado de **workflow**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('AI_PREPARE_SEED','FACEBOOK_SEED_LOOKUP','FACEBOOK_TARGETING_SUGGESTIONS','FACEBOOK_SOCIAL_POSITIONS','AI_BUILD_SPECS','FACEBOOK_VALIDATE_SPEC','FACEBOOK_REACH_ESTIMATE')`; obrigatório).
  - `worker`: Campo usado para armazenar informações de **worker**. (tipo `enum('AI','FACEBOOK')`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING','RUNNING','SUCCEEDED','FAILED')`; obrigatório; chave `MUL`).
  - `resource_id`: Chave de referência para o registro relacionado de **resource**. (tipo `bigint(20)`; opcional).
  - `attempt_count`: Campo usado para armazenar informações de **attempt count**. (tipo `int(11)`; obrigatório; default `0`).
  - `payload`: Carga útil em formato textual/JSON retornada por integração. (tipo `longtext`; opcional).
  - `result_payload`: Campo usado para armazenar informações de **result payload**. (tipo `longtext`; opcional).
  - `error_message`: Campo usado para armazenar informações de **erro message**. (tipo `longtext`; opcional).
  - `locked_by`: Campo usado para armazenar informações de **locked by**. (tipo `varchar(191)`; opcional).
  - `locked_at`: Campo usado para armazenar informações de **locked at**. (tipo `datetime(6)`; opcional; chave `MUL`).
  - `started_at`: Campo usado para armazenar informações de **started at**. (tipo `datetime(6)`; opcional).
  - `finished_at`: Campo usado para armazenar informações de **finished at**. (tipo `datetime(6)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`; on update CURRENT_TIMESTAMP(6)).

### `experiment_adset_job_api_log`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (13):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `job_id`: Chave de referência para o registro relacionado de **job**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `provider`: Campo usado para armazenar informações de **provedor**. (tipo `varchar(32)`; obrigatório).
  - `endpoint`: Campo usado para armazenar informações de **endpoint**. (tipo `longtext`; opcional).
  - `http_method`: Campo usado para armazenar informações de **http method**. (tipo `varchar(16)`; opcional).
  - `status_code`: Campo usado para armazenar informações de **status code**. (tipo `int(11)`; opcional).
  - `requested_at`: Campo usado para armazenar informações de **requested at**. (tipo `datetime(6)`; opcional).
  - `responded_at`: Campo usado para armazenar informações de **responded at**. (tipo `datetime(6)`; opcional).
  - `request_payload`: Campo usado para armazenar informações de **requisição payload**. (tipo `longtext`; opcional).
  - `response_payload`: Campo usado para armazenar informações de **resposta payload**. (tipo `longtext`; opcional).
  - `error_message`: Campo usado para armazenar informações de **erro message**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`; on update CURRENT_TIMESTAMP(6)).

### `experiment_adset_spec`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (15):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `workflow_id`: Chave de referência para o registro relacionado de **workflow**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `slot`: Campo usado para armazenar informações de **slot**. (tipo `enum('DESIGNERS','MARKETING','SMB')`; obrigatório).
  - `label`: Campo usado para armazenar informações de **label**. (tipo `varchar(255)`; opcional).
  - `age_min`: Campo usado para armazenar informações de **age min**. (tipo `int(11)`; opcional).
  - `age_max`: Campo usado para armazenar informações de **age max**. (tipo `int(11)`; opcional).
  - `targeting_spec`: Campo usado para armazenar informações de **segmentação spec**. (tipo `longtext`; opcional).
  - `validation_status`: Campo usado para armazenar informações de **validation status**. (tipo `varchar(32)`; opcional).
  - `validation_response`: Campo usado para armazenar informações de **validation resposta**. (tipo `longtext`; opcional).
  - `reach_status`: Campo usado para armazenar informações de **reach status**. (tipo `varchar(32)`; opcional).
  - `reach_response`: Campo usado para armazenar informações de **reach resposta**. (tipo `longtext`; opcional).
  - `reach_lower_bound`: Campo usado para armazenar informações de **reach lower bound**. (tipo `bigint(20)`; opcional).
  - `reach_upper_bound`: Campo usado para armazenar informações de **reach upper bound**. (tipo `bigint(20)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`; on update CURRENT_TIMESTAMP(6)).

### `experiment_adset_workflow`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `UNI`).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('NOT_STARTED','RUNNING','COMPLETED','FAILED')`; obrigatório).
  - `seed_keyword`: Campo usado para armazenar informações de **semente keyword**. (tipo `varchar(255)`; opcional).
  - `seed_locale`: Campo usado para armazenar informações de **semente locale**. (tipo `varchar(10)`; opcional).
  - `seed_interest_id`: Chave de referência para o registro relacionado de **seed_interest**. (tipo `varchar(64)`; opcional).
  - `seed_interest_name`: Campo usado para armazenar informações de **semente interesses name**. (tipo `varchar(255)`; opcional).
  - `seed_audience_lower`: Campo usado para armazenar informações de **semente audience lower**. (tipo `bigint(20)`; opcional).
  - `seed_audience_upper`: Campo usado para armazenar informações de **semente audience upper**. (tipo `bigint(20)`; opcional).
  - `ai_notes`: Campo usado para armazenar informações de **ai notes**. (tipo `longtext`; opcional).
  - `last_error`: Campo usado para armazenar informações de **last erro**. (tipo `longtext`; opcional).
  - `completed_at`: Campo usado para armazenar informações de **completed at**. (tipo `datetime(6)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`; on update CURRENT_TIMESTAMP(6)).

### `experiment_campaign_metric`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (13):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `clicks`: Campo usado para armazenar informações de **clicks**. (tipo `bigint(20)`; opcional).
  - `cpc`: Campo usado para armazenar informações de **cpc**. (tipo `decimal(12,2)`; opcional).
  - `cpl`: Campo usado para armazenar informações de **cpl**. (tipo `decimal(12,2)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `date_start`: Campo usado para armazenar informações de **date início**. (tipo `date`; opcional).
  - `date_stop`: Campo usado para armazenar informações de **date stop**. (tipo `date`; opcional).
  - `impressions`: Campo usado para armazenar informações de **impressions**. (tipo `bigint(20)`; opcional).
  - `leads`: Campo usado para armazenar informações de **leads**. (tipo `bigint(20)`; opcional).
  - `spend`: Campo usado para armazenar informações de **spend**. (tipo `decimal(12,2)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `campaign_id`: Chave de referência para o registro relacionado de **campaign**. (tipo `char(36)`; obrigatório; chave `UNI`).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `UNI`).

### `experiment_facebook_api_log`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (13):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `context`: Campo usado para armazenar informações de **context**. (tipo `enum('PLAYBOOK','CAMPAIGN_CREATION','CAMPAIGN_AD_SET','CAMPAIGN_AD_CREATIVE','CAMPAIGN_AD')`; obrigatório).
  - `provider`: Campo usado para armazenar informações de **provedor**. (tipo `varchar(32)`; obrigatório).
  - `endpoint`: Campo usado para armazenar informações de **endpoint**. (tipo `longtext`; opcional).
  - `http_method`: Campo usado para armazenar informações de **http method**. (tipo `varchar(16)`; opcional).
  - `status_code`: Campo usado para armazenar informações de **status code**. (tipo `int(11)`; opcional).
  - `requested_at`: Campo usado para armazenar informações de **requested at**. (tipo `datetime(6)`; opcional).
  - `responded_at`: Campo usado para armazenar informações de **responded at**. (tipo `datetime(6)`; opcional).
  - `request_payload`: Campo usado para armazenar informações de **requisição payload**. (tipo `longtext`; opcional).
  - `response_payload`: Campo usado para armazenar informações de **resposta payload**. (tipo `longtext`; opcional).
  - `error_message`: Campo usado para armazenar informações de **erro message**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).

### `experiment_sample_email`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (10):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `body`: Campo usado para armazenar informações de **body**. (tipo `text`; opcional).
  - `call_to_action`: Campo usado para armazenar informações de **chamada to ação**. (tipo `varchar(500)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(128)`; opcional).
  - `preview_text`: Campo usado para armazenar informações de **preview texto**. (tipo `varchar(255)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `text`; opcional).
  - `subject`: Campo usado para armazenar informações de **assunto**. (tipo `varchar(255)`; obrigatório).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).

### `experiment_targeting_selection`
- **Finalidade da tabela:** Armazena informações operacionais e analíticas associadas ao ciclo de experimentos.
- **Campos (7):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `candidate_type`: Campo usado para armazenar informações de **candidate tipo**. (tipo `enum('INTEREST','BEHAVIOR','WORK_POSITION')`; obrigatório).
  - `term`: Campo usado para armazenar informações de **term**. (tipo `varchar(191)`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; opcional; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; opcional; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `targeting_element_id`: Chave de referência para o registro relacionado de **targeting_element**. (tipo `bigint(20)`; opcional; chave `MUL`).

### `facebook_ads_ad`
- **Finalidade da tabela:** Registra entidades sincronizadas da API do Facebook Ads para campanhas, conjuntos, criativos e anúncios.
- **Campos (8):**
  - `id`: Identificador único do registro. (tipo `char(36)`; obrigatório; chave `PRI`).
  - `external_id`: Identificador do recurso em sistema externo. (tipo `varchar(64)`; opcional).
  - `adset_id`: Chave de referência para o registro relacionado de **adset**. (tipo `char(36)`; obrigatório; chave `MUL`).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `creative_id`: Chave de referência para o registro relacionado de **creative**. (tipo `char(36)`; obrigatório; chave `MUL`).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PAUSED','ACTIVE','ARCHIVED','DELETED')`; obrigatório; default `PAUSED`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `facebook_ads_ad_creative`
- **Finalidade da tabela:** Registra entidades sincronizadas da API do Facebook Ads para campanhas, conjuntos, criativos e anúncios.
- **Campos (11):**
  - `id`: Identificador único do registro. (tipo `char(36)`; obrigatório; chave `PRI`).
  - `external_id`: Identificador do recurso em sistema externo. (tipo `varchar(64)`; opcional).
  - `page_id`: Chave de referência para o registro relacionado de **page**. (tipo `varchar(64)`; obrigatório).
  - `instagram_user_id`: Chave de referência para o registro relacionado de **instagram_user**. (tipo `varchar(64)`; opcional).
  - `kind`: Campo usado para armazenar informações de **kind**. (tipo `enum('LINK','VIDEO','CAROUSEL')`; obrigatório).
  - `link_data_json`: Campo usado para armazenar informações de **link data estrutura JSON**. (tipo `longtext`; opcional).
  - `video_data_json`: Campo usado para armazenar informações de **vídeo data estrutura JSON**. (tipo `longtext`; opcional).
  - `carousel_data_json`: Campo usado para armazenar informações de **carousel data estrutura JSON**. (tipo `longtext`; opcional).
  - `last_preview_url`: Campo usado para armazenar informações de **last preview url**. (tipo `varchar(1024)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `facebook_ads_ad_set`
- **Finalidade da tabela:** Registra entidades sincronizadas da API do Facebook Ads para campanhas, conjuntos, criativos e anúncios.
- **Campos (18):**
  - `id`: Identificador único do registro. (tipo `char(36)`; obrigatório; chave `PRI`).
  - `external_id`: Identificador do recurso em sistema externo. (tipo `varchar(64)`; opcional).
  - `experiment_ad_set_id`: Chave de referência para o registro relacionado de **experiment_ad_set**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `campaign_id`: Chave de referência para o registro relacionado de **campaign**. (tipo `char(36)`; obrigatório; chave `MUL`).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PAUSED','ACTIVE','ARCHIVED','DELETED')`; obrigatório; default `PAUSED`).
  - `daily_budget_minor`: Campo usado para armazenar informações de **daily orçamento minor**. (tipo `bigint(20) unsigned`; opcional).
  - `lifetime_budget_minor`: Campo usado para armazenar informações de **lifetime orçamento minor**. (tipo `bigint(20) unsigned`; opcional).
  - `start_time`: Campo usado para armazenar informações de **início time**. (tipo `datetime`; opcional).
  - `end_time`: Campo usado para armazenar informações de **fim time**. (tipo `datetime`; opcional).
  - `billing_event`: Campo usado para armazenar informações de **billing event**. (tipo `varchar(32)`; obrigatório).
  - `optimization_goal`: Campo usado para armazenar informações de **optimization goal**. (tipo `varchar(64)`; obrigatório).
  - `bid_strategy`: Campo usado para armazenar informações de **bid strategy**. (tipo `varchar(64)`; obrigatório).
  - `bid_amount_minor`: Campo usado para armazenar informações de **bid amount minor**. (tipo `bigint(20) unsigned`; opcional).
  - `promoted_object_json`: Campo usado para armazenar informações de **promoted object estrutura JSON**. (tipo `longtext`; opcional).
  - `targeting_json`: Campo usado para armazenar informações de **segmentação estrutura JSON**. (tipo `longtext`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `facebook_ads_campaign`
- **Finalidade da tabela:** Registra entidades sincronizadas da API do Facebook Ads para campanhas, conjuntos, criativos e anúncios.
- **Campos (16):**
  - `id`: Identificador único do registro. (tipo `char(36)`; obrigatório; chave `PRI`).
  - `external_id`: Identificador do recurso em sistema externo. (tipo `varchar(64)`; opcional).
  - `ad_account_id`: Chave de referência para o registro relacionado de **ad_account**. (tipo `varchar(64)`; obrigatório).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `objective`: Campo usado para armazenar informações de **objetivo**. (tipo `varchar(64)`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PAUSED','ACTIVE','ARCHIVED','DELETED')`; obrigatório; default `PAUSED`).
  - `budget_mode`: Campo usado para armazenar informações de **orçamento mode**. (tipo `enum('CAMPAIGN','ADSET')`; obrigatório).
  - `daily_budget_minor`: Campo usado para armazenar informações de **daily orçamento minor**. (tipo `bigint(20) unsigned`; opcional).
  - `lifetime_budget_minor`: Campo usado para armazenar informações de **lifetime orçamento minor**. (tipo `bigint(20) unsigned`; opcional).
  - `api_version`: Campo usado para armazenar informações de **API version**. (tipo `varchar(16)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `facebook_account_id`: Chave de referência para o registro relacionado de **facebook_account**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `metrics_last_error`: Campo usado para armazenar informações de **metrics last erro**. (tipo `text`; opcional).
  - `metrics_last_synced_at`: Campo usado para armazenar informações de **metrics last synced at**. (tipo `datetime(6)`; opcional).

### `fb_account`
- **Finalidade da tabela:** Armazena dados relacionados a **fb_account** no contexto do Marketing Hub.
- **Campos (35):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `currency`: Campo usado para armazenar informações de **currency**. (tipo `varchar(255)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `access_token`: Campo usado para armazenar informações de **access token**. (tipo `longtext`; opcional).
  - `token_expires_at`: Campo usado para armazenar informações de **token expires at**. (tipo `datetime`; opcional).
  - `token_last_refreshed_at`: Campo usado para armazenar informações de **token last refreshed at**. (tipo `datetime`; opcional).
  - `authorized_user_id`: Chave de referência para o registro relacionado de **authorized_user**. (tipo `varchar(128)`; opcional).
  - `authorized_user_name`: Campo usado para armazenar informações de **authorized user name**. (tipo `varchar(255)`; opcional).
  - `authorized_user_email`: Campo usado para armazenar informações de **authorized user email**. (tipo `varchar(320)`; opcional).
  - `app_id`: Chave de referência para o registro relacionado de **app**. (tipo `varchar(255)`; opcional).
  - `app_secret`: Campo usado para armazenar informações de **app secret**. (tipo `longtext`; opcional).
  - `system_user_access_token`: Campo usado para armazenar o **token de usuário do sistema** utilizado pelo worker do Facebook Ads. (tipo `longtext`; opcional).
  - `token_renewal_enabled`: Campo usado para armazenar informações de **token renewal enabled**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `token_renewal_status`: Campo usado para armazenar informações de **token renewal status**. (tipo `varchar(40)`; opcional).
  - `token_renewal_last_attempt_at`: Campo usado para armazenar informações de **token renewal last attempt at**. (tipo `datetime`; opcional).
  - `token_renewed_at`: Campo usado para armazenar informações de **token renewed at**. (tipo `datetime`; opcional).
  - `token_renewal_last_error`: Campo usado para armazenar informações de **token renewal last erro**. (tipo `longtext`; opcional).
  - `ad_account_id`: Chave de referência para o registro relacionado de **ad_account**. (tipo `varchar(64)`; opcional).
  - `default_page_id`: Chave de referência para o registro relacionado de **default_page**. (tipo `varchar(128)`; opcional).
  - `default_website_url`: Campo usado para armazenar informações de **default website url**. (tipo `varchar(512)`; opcional).
  - `default_lead_gen_form_id`: Chave de referência para o registro relacionado de **default_lead_gen_form**. (tipo `varchar(64)`; opcional).
  - `default_instagram_actor_id`: Chave de referência para o registro relacionado de **default_instagram_actor**. (tipo `varchar(64)`; opcional).
  - `default_creative_message_template`: Campo usado para armazenar informações de **default creative message template**. (tipo `varchar(255)`; opcional).
  - `default_call_to_action_type`: Campo usado para armazenar informações de **default chamada to ação tipo**. (tipo `varchar(64)`; opcional).
  - `ad_set_daily_budget`: Campo usado para armazenar informações de **ad set daily orçamento**. (tipo `varchar(32)`; opcional).
  - `ad_set_billing_event`: Campo usado para armazenar informações de **ad set billing event**. (tipo `varchar(64)`; opcional).
  - `ad_set_optimization_goal`: Campo usado para armazenar informações de **ad set optimization goal**. (tipo `varchar(64)`; opcional).
  - `ad_set_destination_type`: Campo usado para armazenar informações de **ad set destino tipo**. (tipo `varchar(64)`; opcional).
  - `ad_set_bid_strategy`: Campo usado para armazenar informações de **ad set bid strategy**. (tipo `varchar(64)`; opcional).
  - `ad_set_bid_amount`: Campo usado para armazenar informações de **ad set bid amount**. (tipo `varchar(32)`; opcional).
  - `ad_set_target_country`: Campo usado para armazenar informações de **ad set meta country**. (tipo `varchar(32)`; opcional).
  - `worker_enabled`: Campo usado para armazenar informações de **worker enabled**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `worker_last_validation_at`: Campo usado para armazenar informações de **worker last validation at**. (tipo `datetime`; opcional).
  - `worker_last_validation_error_code`: Campo usado para armazenar informações de **worker last validation erro code**. (tipo `varchar(128)`; opcional).
  - `worker_last_validation_error_detail`: Campo usado para armazenar informações de **worker last validation erro detail**. (tipo `varchar(512)`; opcional).

### `fb_instant_form`
- **Finalidade da tabela:** Armazena dados relacionados a **fb_instant_form** no contexto do Marketing Hub.
- **Campos (22):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `hypothesis_id`: Chave de referência para o registro relacionado de **hypothesis**. (tipo `binary(16)`; obrigatório; chave `MUL`).
  - `page_id`: Chave de referência para o registro relacionado de **page**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `form_id`: Chave de referência para o registro relacionado de **form**. (tipo `varchar(128)`; opcional; chave `UNI`).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(50)`; opcional).
  - `locale`: Campo usado para armazenar informações de **locale**. (tipo `varchar(12)`; opcional).
  - `leads_count`: Campo usado para armazenar informações de **leads count**. (tipo `bigint(20)`; opcional).
  - `created_time`: Campo usado para armazenar informações de **created time**. (tipo `datetime`; opcional).
  - `updated_time`: Campo usado para armazenar informações de **updated time**. (tipo `datetime`; opcional).
  - `follow_up_action_url`: Campo usado para armazenar informações de **follow up ação url**. (tipo `varchar(512)`; opcional).
  - `privacy_policy_url`: Campo usado para armazenar informações de **privacy policy url**. (tipo `varchar(512)`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(128)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `questions`: Campo usado para armazenar informações de **questions**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `approved`: Campo usado para armazenar informações de **approved**. (tipo `bit(1)`; obrigatório).
  - `approved_at`: Campo usado para armazenar informações de **approved at**. (tipo `datetime(6)`; opcional).
  - `published`: Campo usado para armazenar informações de **published**. (tipo `bit(1)`; obrigatório).
  - `published_at`: Campo usado para armazenar informações de **published at**. (tipo `datetime(6)`; opcional).
  - `share_link`: Campo usado para armazenar informações de **share link**. (tipo `varchar(512)`; opcional).

### `fb_page`
- **Finalidade da tabela:** Armazena dados relacionados a **fb_page** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `account_id`: Chave de referência para o registro relacionado de **account**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `page_id`: Chave de referência para o registro relacionado de **page**. (tipo `varchar(128)`; obrigatório).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).

### `flows`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (10):**
  - `slug`: Campo usado para armazenar informações de **slug**. (tipo `varchar(190)`; obrigatório; chave `PRI`).
  - `description`: Descrição textual do registro. (tipo `varchar(500)`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `text`; opcional).
  - `questions`: Campo usado para armazenar informações de **questions**. (tipo `longtext`; opcional).
  - `access_count`: Campo usado para armazenar informações de **access count**. (tipo `bigint(20)`; obrigatório; default `0`).
  - `simple_form_style_definition`: Campo usado para armazenar informações de **simple formulário style definition**. (tipo `longtext`; opcional).
  - `simple_form_style_name`: Campo usado para armazenar informações de **simple formulário style name**. (tipo `varchar(150)`; opcional).
  - `simple_form_style_slug`: Campo usado para armazenar informações de **simple formulário style slug**. (tipo `varchar(120)`; opcional).

### `flow_access`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (8):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `accessed_at`: Campo usado para armazenar informações de **accessed at**. (tipo `datetime(6)`; opcional).
  - `client_ip`: Campo usado para armazenar informações de **client ip**. (tipo `varchar(64)`; opcional).
  - `flow_slug`: Campo usado para armazenar informações de **flow slug**. (tipo `varchar(190)`; obrigatório).
  - `referer`: Campo usado para armazenar informações de **referer**. (tipo `varchar(1024)`; opcional).
  - `user_agent`: Campo usado para armazenar informações de **user agent**. (tipo `varchar(1024)`; opcional).
  - `visitor_id`: Chave de referência para o registro relacionado de **visitor**. (tipo `varchar(128)`; opcional).
  - `campaign_code`: Campo usado para armazenar informações de **campanha code**. (tipo `varchar(190)`; opcional).

### `flow_submissions`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (11):**
  - `id`: Identificador único do registro. (tipo `varchar(36)`; obrigatório; chave `PRI`).
  - `answers`: Campo usado para armazenar informações de **answers**. (tipo `longtext`; opcional).
  - `content_type`: Campo usado para armazenar informações de **conteúdo tipo**. (tipo `varchar(255)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório).
  - `email`: Campo usado para armazenar informações de **email**. (tipo `varchar(255)`; obrigatório).
  - `flow_slug`: Campo usado para armazenar informações de **flow slug**. (tipo `varchar(190)`; obrigatório).
  - `image_question_key`: Campo usado para armazenar informações de **imagem question key**. (tipo `varchar(255)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `original_file_name`: Campo usado para armazenar informações de **original file name**. (tipo `varchar(255)`; opcional).
  - `stored_file_name`: Campo usado para armazenar informações de **stored file name**. (tipo `varchar(255)`; opcional).
  - `campaign_code`: Campo usado para armazenar informações de **campanha code**. (tipo `varchar(190)`; opcional).

### `flow_submission_image_item`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `package_id`: Chave de referência para o registro relacionado de **package**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `asset_id`: Chave de referência para o registro relacionado de **asset**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `access_type`: Campo usado para armazenar informações de **access tipo**. (tipo `varchar(20)`; obrigatório).
  - `position_index`: Campo usado para armazenar informações de **position index**. (tipo `int(11)`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).

### `flow_submission_image_package`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (36):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `submission_id`: Chave de referência para o registro relacionado de **submission**. (tipo `varchar(36)`; obrigatório; chave `MUL`).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(30)`; obrigatório; chave `MUL`; default `RECEIVED`).
  - `planned_outputs`: Campo usado para armazenar informações de **planned outputs**. (tipo `int(11)`; opcional).
  - `free_images`: Campo usado para armazenar informações de **free images**. (tipo `int(11)`; obrigatório; default `0`).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `failure_reason`: Campo usado para armazenar informações de **failure reason**. (tipo `longtext`; opcional).
  - `image_model_id`: Chave de referência para o registro relacionado de **image_model**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `image_model_quality_id`: Chave de referência para o registro relacionado de **image_model_quality**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `image_orientation`: Campo usado para armazenar informações de **imagem orientation**. (tipo `varchar(16)`; opcional).
  - `image_width`: Campo usado para armazenar informações de **imagem width**. (tipo `int(11)`; opcional).
  - `image_height`: Campo usado para armazenar informações de **imagem height**. (tipo `int(11)`; opcional).
  - `image_unit_price_usd`: Campo usado para armazenar informações de **imagem unit preço usd**. (tipo `decimal(38,2)`; opcional).
  - `image_total_price_usd`: Campo usado para armazenar informações de **imagem total preço usd**. (tipo `decimal(38,2)`; opcional).
  - `image_currency`: Campo usado para armazenar informações de **imagem currency**. (tipo `varchar(3)`; obrigatório; default `USD`).
  - `notification_attempts`: Campo usado para armazenar informações de **notification attempts**. (tipo `int(11)`; obrigatório).
  - `notification_last_attempt`: Campo usado para armazenar informações de **notification last attempt**. (tipo `datetime(6)`; opcional).
  - `notification_last_error`: Campo usado para armazenar informações de **notification last erro**. (tipo `text`; opcional).
  - `email_opened_at`: Campo usado para armazenar informações de **email abertura at**. (tipo `timestamp`; opcional).
  - `images_viewed_at`: Campo usado para armazenar informações de **images viewed at**. (tipo `timestamp`; opcional).
  - `notified_at`: Campo usado para armazenar informações de **notified at**. (tipo `datetime(6)`; opcional).
  - `zip_object_key`: Campo usado para armazenar informações de **zip object key**. (tipo `varchar(512)`; opcional; chave `MUL`).
  - `payment_purchase_id`: Chave de referência para o registro relacionado de **payment_purchase**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `payment_checkout_url`: Campo usado para armazenar informações de **payment checkout url**. (tipo `varchar(1200)`; opcional).
  - `payment_checkout_expires_at`: Campo usado para armazenar informações de **payment checkout expires at**. (tipo `timestamp`; opcional).
  - `payment_amount`: Campo usado para armazenar informações de **payment amount**. (tipo `decimal(12,2)`; opcional).
  - `payment_currency`: Campo usado para armazenar informações de **payment currency**. (tipo `varchar(12)`; opcional).
  - `payment_statement_descriptor`: Campo usado para armazenar informações de **payment statement descriptor**. (tipo `varchar(120)`; opcional).
  - `zip_size_bytes`: Campo usado para armazenar informações de **zip size bytes**. (tipo `bigint(20)`; opcional).
  - `zip_generated_at`: Campo usado para armazenar informações de **zip generated at**. (tipo `timestamp`; opcional).
  - `zip_last_error`: Campo usado para armazenar informações de **zip last erro**. (tipo `text`; opcional).
  - `zip_attempts`: Campo usado para armazenar informações de **zip attempts**. (tipo `int(11)`; obrigatório; default `0`).
  - `zip_last_attempt`: Campo usado para armazenar informações de **zip last attempt**. (tipo `timestamp`; opcional).

### `flow_submission_image_package_status_history`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (5):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `package_id`: Chave de referência para o registro relacionado de **package**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(30)`; obrigatório).
  - `failure_reason`: Campo usado para armazenar informações de **failure reason**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).

### `flow_submission_image_watermark`
- **Finalidade da tabela:** Mantém a estrutura de fluxos e submissões realizadas por usuários no portal de captação.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `item_id`: Chave de referência para o registro relacionado de **item**. (tipo `bigint(20)`; obrigatório; chave `UNI`).
  - `asset_id`: Chave de referência para o registro relacionado de **asset**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `optimized_asset_id`: Chave de referência para o registro relacionado de **optimized_asset**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `funnel_step`
- **Finalidade da tabela:** Armazena dados relacionados a **funnel_step** no contexto do Marketing Hub.
- **Campos (12):**
  - `id`: Identificador único do registro. (tipo `binary(16)`; obrigatório; chave `PRI`).
  - `funnel_id`: Chave de referência para o registro relacionado de **funnel**. (tipo `binary(16)`; obrigatório; chave `MUL`).
  - `order_idx`: Campo usado para armazenar informações de **order idx**. (tipo `int(11)`; opcional).
  - `stimulus_type`: Campo usado para armazenar informações de **stimulus tipo**. (tipo `enum('DM','EMAIL','IG_POST_BOOST','FB_AD','STORY','WHATSAPP','CALL','SMS','WEBINAR','PUSH')`; opcional).
  - `channel`: Campo usado para armazenar informações de **channel**. (tipo `varchar(50)`; opcional).
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `varchar(50)`; opcional).
  - `expected_action`: Campo usado para armazenar informações de **expected ação**. (tipo `enum('OPEN','CLICK','REPLY','VIEW','PURCHASE','REGISTRATION','OPT_IN','OPT_OUT','BOUNCE','SHARE')`; opcional).
  - `score_inc`: Campo usado para armazenar informações de **score inc**. (tipo `int(11)`; opcional).
  - `revenue_target`: Campo usado para armazenar informações de **revenue meta**. (tipo `decimal(38,2)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; opcional; default `CURRENT_TIMESTAMP`).
  - `is_active`: Campo usado para armazenar informações de **is active**. (tipo `tinyint(1)`; opcional; default `1`).
  - `note`: Campo usado para armazenar informações de **note**. (tipo `text`; opcional).

### `hypothesis`
- **Finalidade da tabela:** Armazena dados relacionados a **hypothesis** no contexto do Marketing Hub.
- **Campos (26):**
  - `id`: Identificador único do registro. (tipo `binary(16)`; obrigatório; chave `PRI`).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `kpi_target_cpl`: Campo usado para armazenar informações de **KPI meta cpl**. (tipo `decimal(7,2)`; opcional).
  - `offer_type`: Campo usado para armazenar informações de **offer tipo**. (tipo `enum('LEAD','TRIPWIRE')`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('BACKLOG','TESTING','VALIDATED','INVALIDATED')`; opcional).
  - `title`: Campo usado para armazenar informações de **título**. (tipo `varchar(255)`; obrigatório).
  - `premise_angle_id`: Chave de referência para o registro relacionado de **premise_angle**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `price`: Campo usado para armazenar informações de **preço**. (tipo `decimal(6,2)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `persona`: Campo usado para armazenar informações de **persona**. (tipo `varchar(255)`; obrigatório).
  - `problem`: Campo usado para armazenar informações de **problem**. (tipo `longtext`; opcional).
  - `promise`: Campo usado para armazenar informações de **promise**. (tipo `longtext`; opcional).
  - `success_rule`: Campo usado para armazenar informações de **success rule**. (tipo `tinytext`; opcional).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; opcional).
  - `hypothesiscol`: Campo usado para armazenar informações de **hypothesiscol**. (tipo `varchar(45)`; opcional).
  - `unique_mechanism`: Campo usado para armazenar informações de **unique mechanism**. (tipo `longtext`; opcional).
  - `mechanism`: Campo usado para armazenar informações de **mechanism**. (tipo `longtext`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(255)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `generated_at`: Campo usado para armazenar informações de **generated at**. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `entrega`: Campo usado para armazenar informações de **entrega**. (tipo `longtext`; opcional).
  - `cost_usd`: Campo usado para armazenar informações de **custo usd**. (tipo `decimal(10,4)`; opcional).
  - `cost`: Campo usado para armazenar informações de **custo**. (tipo `decimal(10,2)`; opcional).
  - `expense`: Campo usado para armazenar informações de **expense**. (tipo `decimal(10,2)`; opcional).
  - `total_cost`: Campo usado para armazenar informações de **total custo**. (tipo `decimal(12,2)`; opcional).

### `hypothesis_prompt_attribute_description`
- **Finalidade da tabela:** Armazena dados relacionados a **hypothesis_prompt_attribute_description** no contexto do Marketing Hub.
- **Campos (2):**
  - `hypothesis_id`: Chave de referência para o registro relacionado de **hypothesis**. (tipo `binary(16)`; obrigatório; chave `PRI`).
  - `prompt_attribute_description_id`: Chave de referência para o registro relacionado de **prompt_attribute_description**. (tipo `bigint(20)`; obrigatório; chave `PRI`).

### `ig_account`
- **Finalidade da tabela:** Armazena dados relacionados a **ig_account** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `handle`: Campo usado para armazenar informações de **handle**. (tipo `varchar(255)`; obrigatório).
  - `account_code`: Campo usado para armazenar informações de **conta code**. (tipo `varchar(255)`; obrigatório).

### `image_generation_model`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (8):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `code`: Campo usado para armazenar informações de **code**. (tipo `varchar(64)`; obrigatório; chave `UNI`).
  - `display_name`: Campo usado para armazenar informações de **display name**. (tipo `varchar(128)`; obrigatório).
  - `provider`: Campo usado para armazenar informações de **provedor**. (tipo `enum('OPENAI')`; obrigatório).
  - `api_model`: Campo usado para armazenar informações de **API model**. (tipo `varchar(128)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `image_generation_price`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (10):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `quality_id`: Chave de referência para o registro relacionado de **quality**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `orientation`: Campo usado para armazenar informações de **orientation**. (tipo `enum('SQUARE','PORTRAIT','LANDSCAPE')`; obrigatório).
  - `width`: Campo usado para armazenar informações de **width**. (tipo `int(11)`; obrigatório).
  - `height`: Campo usado para armazenar informações de **height**. (tipo `int(11)`; obrigatório).
  - `size_label`: Campo usado para armazenar informações de **size label**. (tipo `varchar(32)`; obrigatório).
  - `unit_price_usd`: Campo usado para armazenar informações de **unit preço usd**. (tipo `decimal(10,5)`; obrigatório).
  - `preferred`: Campo usado para armazenar informações de **preferred**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `image_generation_quality`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (9):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `model_id`: Chave de referência para o registro relacionado de **model**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `code`: Campo usado para armazenar informações de **code**. (tipo `varchar(32)`; obrigatório).
  - `display_name`: Campo usado para armazenar informações de **display name**. (tipo `varchar(64)`; obrigatório).
  - `api_quality`: Campo usado para armazenar informações de **API qualidade**. (tipo `varchar(32)`; opcional).
  - `is_default`: Campo usado para armazenar informações de **is default**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `position`: Campo usado para armazenar informações de **position**. (tipo `int(11)`; obrigatório; default `0`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `interaction_journey_element`
- **Finalidade da tabela:** Armazena dados relacionados a **interaction_journey_element** no contexto do Marketing Hub.
- **Campos (9):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `step_id`: Chave de referência para o registro relacionado de **step**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `parent_id`: Chave de referência para o registro relacionado de **parent**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `order_index`: Campo usado para armazenar informações de **order index**. (tipo `int(11)`; obrigatório).
  - `label`: Campo usado para armazenar informações de **label**. (tipo `varchar(255)`; obrigatório).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `varchar(100)`; opcional).
  - `notes`: Campo usado para armazenar informações de **notes**. (tipo `longtext`; opcional).
  - `max_quantity`: Campo usado para armazenar informações de **max quantity**. (tipo `int(11)`; opcional).
  - `min_quantity`: Campo usado para armazenar informações de **min quantity**. (tipo `int(11)`; opcional).

### `interaction_journey_step`
- **Finalidade da tabela:** Armazena dados relacionados a **interaction_journey_step** no contexto do Marketing Hub.
- **Campos (5):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `journey_id`: Chave de referência para o registro relacionado de **journey**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `order_index`: Campo usado para armazenar informações de **order index**. (tipo `int(11)`; obrigatório).
  - `title`: Campo usado para armazenar informações de **título**. (tipo `varchar(255)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).

### `journey`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (13):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório; chave `UNI`).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('DRAFT','ACTIVE','PAUSED','COMPLETED','ARCHIVED')`; obrigatório; chave `MUL`).
  - `niche_id`: Chave de referência para o registro relacionado de **niche**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `segment_reference`: Campo usado para armazenar informações de **segment referência**. (tipo `varchar(255)`; opcional).
  - `segment_filter`: Campo usado para armazenar informações de **segment filter**. (tipo `tinytext`; opcional).
  - `start_at`: Campo usado para armazenar informações de **início at**. (tipo `datetime`; opcional).
  - `end_at`: Campo usado para armazenar informações de **fim at**. (tipo `datetime`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `journey_assignment`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `journey_id`: Chave de referência para o registro relacionado de **journey**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('LEAD','SEGMENT')`; obrigatório).
  - `lead_id`: Chave de referência para o registro relacionado de **lead**. (tipo `binary(16)`; opcional; chave `MUL`).
  - `segment_identifier`: Campo usado para armazenar informações de **segment identifier**. (tipo `varchar(255)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING','IN_PROGRESS','COMPLETED','STOPPED')`; obrigatório).
  - `current_step_id`: Chave de referência para o registro relacionado de **current_step**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `next_step_id`: Chave de referência para o registro relacionado de **next_step**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `last_event_at`: Campo usado para armazenar informações de **last event at**. (tipo `datetime`; opcional).
  - `context_payload`: Campo usado para armazenar informações de **context payload**. (tipo `tinytext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `next_attempt_at`: Campo usado para armazenar informações de **next attempt at**. (tipo `datetime`; opcional; chave `MUL`).
  - `retry_count`: Campo usado para armazenar informações de **retry count**. (tipo `int(11)`; opcional; default `0`).

### `journey_metadata`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (3):**
  - `journey_id`: Chave de referência para o registro relacionado de **journey**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `meta_key`: Campo usado para armazenar informações de **meta key**. (tipo `varchar(255)`; obrigatório; chave `PRI`).
  - `meta_value`: Campo usado para armazenar informações de **meta value**. (tipo `longtext`; opcional).

### `journey_step`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `position`: Campo usado para armazenar informações de **position**. (tipo `int(11)`; obrigatório).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `phase`: Campo usado para armazenar informações de **fase**. (tipo `enum('ATTENTION','INTEREST','DESIRE','ACTION')`; obrigatório).
  - `stimulus_type`: Campo usado para armazenar informações de **stimulus tipo**. (tipo `enum('AD','EMAIL','WHATSAPP','LANDING_PAGE','INSTANT_FORM','LEAD_PORTAL_IMAGE_FLOW','SHOWCASE_IMAGE','PAYMENT_PAGE')`; obrigatório).
  - `creative_id`: Chave de referência para o registro relacionado de **creative**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `angle_id`: Chave de referência para o registro relacionado de **angle**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `visual_proof_id`: Chave de referência para o registro relacionado de **visual_proof**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `emotional_trigger_id`: Chave de referência para o registro relacionado de **emotional_trigger**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `entry_condition`: Campo usado para armazenar informações de **entry condition**. (tipo `varchar(255)`; opcional).
  - `exit_condition`: Campo usado para armazenar informações de **exit condition**. (tipo `varchar(255)`; opcional).
  - `delay_minutes`: Campo usado para armazenar informações de **delay minutes**. (tipo `int(11)`; opcional).

### `journey_step_metadata`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (3):**
  - `step_id`: Chave de referência para o registro relacionado de **step**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `meta_key`: Campo usado para armazenar informações de **meta key**. (tipo `varchar(255)`; obrigatório; chave `PRI`).
  - `meta_value`: Campo usado para armazenar informações de **meta value**. (tipo `longtext`; opcional).

### `journey_template`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (7):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório; chave `UNI`).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `objective`: Campo usado para armazenar informações de **objetivo**. (tipo `varchar(255)`; opcional).
  - `preferred_channel`: Campo usado para armazenar informações de **preferred channel**. (tipo `varchar(100)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime`; opcional; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `journey_template_metadata`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (3):**
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `meta_key`: Campo usado para armazenar informações de **meta key**. (tipo `varchar(255)`; obrigatório; chave `PRI`).
  - `meta_value`: Campo usado para armazenar informações de **meta value**. (tipo `longtext`; opcional).

### `journey_template_phase`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (3):**
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `phase_order`: Campo usado para armazenar informações de **fase order**. (tipo `int(11)`; obrigatório; chave `PRI`).
  - `phase`: Campo usado para armazenar informações de **fase**. (tipo `enum('ATTENTION','INTEREST','DESIRE','ACTION')`; obrigatório).

### `journey_template_tag`
- **Finalidade da tabela:** Mantém jornadas, templates e metadados usados na orquestração de experiências e automações.
- **Campos (2):**
  - `template_id`: Chave de referência para o registro relacionado de **template**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `tag`: Campo usado para armazenar informações de **tag**. (tipo `varchar(255)`; obrigatório; chave `PRI`).

### `lead_portal_flow`
- **Finalidade da tabela:** Concentra dados de fluxos, perguntas, compras e entregas do portal de leads.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `name`: Nome de exibição do registro. (tipo `varchar(150)`; obrigatório).
  - `slug`: Campo usado para armazenar informações de **slug**. (tipo `varchar(120)`; obrigatório; chave `UNI`).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(128)`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `approved`: Campo usado para armazenar informações de **approved**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `approved_at`: Campo usado para armazenar informações de **approved at**. (tipo `timestamp`; opcional).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `cost_usd`: Campo usado para armazenar informações de **custo usd**. (tipo `decimal(10,4)`; opcional).
  - `simple_form_style_id`: Chave de referência para o registro relacionado de **simple_form_style**. (tipo `bigint(20)`; opcional; chave `MUL`).

### `lead_portal_flow_question`
- **Finalidade da tabela:** Concentra dados de fluxos, perguntas, compras e entregas do portal de leads.
- **Campos (9):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `flow_id`: Chave de referência para o registro relacionado de **flow**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `title`: Campo usado para armazenar informações de **título**. (tipo `longtext`; opcional).
  - `data_key`: Campo usado para armazenar informações de **data key**. (tipo `varchar(120)`; obrigatório).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('TEXT','TEXTAREA','NUMBER','EMAIL','PHONE','DATE','SINGLE_CHOICE','MULTIPLE_CHOICE','IMAGE_UPLOAD')`; obrigatório).
  - `required`: Campo usado para armazenar informações de **required**. (tipo `tinyint(1)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `placeholder`: Campo usado para armazenar informações de **placeholder**. (tipo `longtext`; opcional).
  - `position_index`: Campo usado para armazenar informações de **position index**. (tipo `int(11)`; obrigatório).

### `lead_portal_flow_question_option`
- **Finalidade da tabela:** Concentra dados de fluxos, perguntas, compras e entregas do portal de leads.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `question_id`: Chave de referência para o registro relacionado de **question**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `option_order`: Campo usado para armazenar informações de **option order**. (tipo `int(11)`; obrigatório).
  - `option_value`: Campo usado para armazenar informações de **option value**. (tipo `longtext`; opcional).

### `lead_portal_premium_delivery`
- **Finalidade da tabela:** Concentra dados de fluxos, perguntas, compras e entregas do portal de leads.
- **Campos (25):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `purchase_id`: Chave de referência para o registro relacionado de **purchase**. (tipo `bigint(20)`; obrigatório; chave `UNI`).
  - `package_id`: Chave de referência para o registro relacionado de **package**. (tipo `bigint(20)`; obrigatório).
  - `submission_id`: Chave de referência para o registro relacionado de **submission**. (tipo `varchar(36)`; opcional).
  - `submission_name`: Campo usado para armazenar informações de **submission name**. (tipo `varchar(255)`; opcional).
  - `submission_email`: Campo usado para armazenar informações de **submission email**. (tipo `varchar(320)`; opcional).
  - `buyer_name`: Campo usado para armazenar informações de **buyer name**. (tipo `varchar(255)`; opcional).
  - `buyer_email`: Campo usado para armazenar informações de **buyer email**. (tipo `varchar(320)`; opcional).
  - `recipient_name`: Campo usado para armazenar informações de **recipient name**. (tipo `varchar(255)`; opcional).
  - `recipient_email`: Campo usado para armazenar informações de **recipient email**. (tipo `varchar(320)`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(30)`; obrigatório; chave `MUL`; default `PENDING_ZIP`).
  - `zip_object_key`: Campo usado para armazenar informações de **zip object key**. (tipo `varchar(512)`; opcional).
  - `zip_download_url`: Campo usado para armazenar informações de **zip download url**. (tipo `varchar(1024)`; opcional).
  - `zip_size_bytes`: Campo usado para armazenar informações de **zip size bytes**. (tipo `bigint(20)`; opcional).
  - `zip_generated_at`: Campo usado para armazenar informações de **zip generated at**. (tipo `timestamp`; opcional).
  - `zip_attempts`: Campo usado para armazenar informações de **zip attempts**. (tipo `int(11)`; obrigatório; default `0`).
  - `zip_last_attempt`: Campo usado para armazenar informações de **zip last attempt**. (tipo `timestamp`; opcional).
  - `zip_last_error`: Campo usado para armazenar informações de **zip last erro**. (tipo `text`; opcional).
  - `email_request_id`: Chave de referência para o registro relacionado de **email_request**. (tipo `varchar(64)`; opcional).
  - `email_sent_at`: Campo usado para armazenar informações de **email envio at**. (tipo `timestamp`; opcional).
  - `email_attempts`: Campo usado para armazenar informações de **email attempts**. (tipo `int(11)`; obrigatório; default `0`).
  - `email_last_attempt`: Campo usado para armazenar informações de **email last attempt**. (tipo `timestamp`; opcional).
  - `email_last_error`: Campo usado para armazenar informações de **email last erro**. (tipo `text`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `lead_portal_purchase`
- **Finalidade da tabela:** Concentra dados de fluxos, perguntas, compras e entregas do portal de leads.
- **Campos (25):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `package_id`: Chave de referência para o registro relacionado de **package**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `submission_id`: Chave de referência para o registro relacionado de **submission**. (tipo `varchar(64)`; opcional).
  - `buyer_name`: Campo usado para armazenar informações de **buyer name**. (tipo `varchar(255)`; opcional).
  - `buyer_email`: Campo usado para armazenar informações de **buyer email**. (tipo `varchar(320)`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `varchar(40)`; obrigatório).
  - `mp_preference_id`: Chave de referência para o registro relacionado de **mp_preference**. (tipo `varchar(150)`; opcional).
  - `mp_payment_id`: Chave de referência para o registro relacionado de **mp_payment**. (tipo `varchar(150)`; opcional; chave `MUL`).
  - `mp_status`: Campo usado para armazenar informações de **mp status**. (tipo `varchar(80)`; opcional).
  - `checkout_url`: Campo usado para armazenar informações de **checkout url**. (tipo `varchar(1200)`; opcional).
  - `checkout_expires_at`: Campo usado para armazenar informações de **checkout expires at**. (tipo `timestamp`; opcional).
  - `amount`: Campo usado para armazenar informações de **amount**. (tipo `decimal(12,2)`; opcional).
  - `currency`: Campo usado para armazenar informações de **currency**. (tipo `varchar(8)`; opcional).
  - `notification_payload`: Campo usado para armazenar informações de **notification payload**. (tipo `longtext`; opcional).
  - `mp_payment_payload`: Campo usado para armazenar informações de **mp payment payload**. (tipo `longtext`; opcional).
  - `delivery_attempts`: Campo usado para armazenar informações de **delivery attempts**. (tipo `int(11)`; opcional; default `0`).
  - `delivery_error`: Campo usado para armazenar informações de **delivery erro**. (tipo `longtext`; opcional).
  - `delivered_at`: Campo usado para armazenar informações de **delivered at**. (tipo `timestamp`; opcional).
  - `payment_approved_at`: Campo usado para armazenar informações de **payment approved at**. (tipo `timestamp`; opcional).
  - `pixel_conversion_recorded_at`: Campo usado para armazenar informações de **pixel conversion recorded at**. (tipo `timestamp`; opcional).
  - `zip_object_key`: Campo usado para armazenar informações de **zip object key**. (tipo `varchar(512)`; opcional).
  - `zip_size_bytes`: Campo usado para armazenar informações de **zip size bytes**. (tipo `bigint(20)`; opcional).
  - `zip_generated_at`: Campo usado para armazenar informações de **zip generated at**. (tipo `timestamp`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `market_niche`
- **Finalidade da tabela:** Armazena dados relacionados a **market_niche** no contexto do Marketing Hub.
- **Campos (34):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `demand_volume`: Campo usado para armazenar informações de **demand volume**. (tipo `longtext`; opcional).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `offers`: Campo usado para armazenar informações de **offers**. (tipo `longtext`; opcional).
  - `promises`: Campo usado para armazenar informações de **promises**. (tipo `longtext`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `base_segmentation`: Campo usado para armazenar informações de **base segmentation**. (tipo `longtext`; opcional).
  - `demographic_filters`: Campo usado para armazenar informações de **demographic filters**. (tipo `longtext`; opcional).
  - `extra_tips`: Campo usado para armazenar informações de **extra tips**. (tipo `longtext`; opcional).
  - `interests`: Campo usado para armazenar informações de **interests**. (tipo `longtext`; opcional).
  - `chat_dialog_id`: Chave de referência para o registro relacionado de **chat_dialog**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `hypotheses_to_generate`: Campo usado para armazenar informações de **hypotheses to generate**. (tipo `int(11)`; opcional).
  - `interest_category`: Campo usado para armazenar informações de **interesses category**. (tipo `varchar(255)`; opcional).
  - `role_category`: Campo usado para armazenar informações de **role category**. (tipo `varchar(255)`; opcional).
  - `hypothesis_model`: Campo usado para armazenar informações de **hipótese model**. (tipo `varchar(191)`; opcional).
  - `differentiated_technology_id`: Chave de referência para o registro relacionado de **differentiated_technology**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `cost`: Campo usado para armazenar informações de **custo**. (tipo `decimal(10,2)`; opcional).
  - `expense`: Campo usado para armazenar informações de **expense**. (tipo `decimal(10,2)`; opcional).
  - `total_cost`: Campo usado para armazenar informações de **total custo**. (tipo `decimal(12,2)`; opcional).
  - `total_revenue`: Campo usado para armazenar informações de **total revenue**. (tipo `decimal(12,2)`; opcional).
  - `detailed_descriptions_to_generate`: Campo usado para armazenar informações de **detailed descriptions to generate**. (tipo `int(11)`; opcional).
  - `detailed_description_model`: Campo usado para armazenar informações de **detailed description model**. (tipo `varchar(191)`; opcional).
  - `hypothesis_detailed_description_id`: Chave de referência para o registro relacionado de **hypothesis_detailed_description**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `interests_to_generate`: Campo usado para armazenar informações de **interests to generate**. (tipo `int(11)`; opcional).
  - `job_titles_to_generate`: Campo usado para armazenar informações de **job cargos to generate**. (tipo `int(11)`; opcional).
  - `behaviors_to_generate`: Campo usado para armazenar informações de **comportamentos to generate**. (tipo `int(11)`; opcional).
  - `interest_model`: Campo usado para armazenar informações de **interesses model**. (tipo `varchar(191)`; opcional).
  - `job_title_model`: Campo usado para armazenar informações de **job título model**. (tipo `varchar(191)`; opcional).
  - `behavior_model`: Campo usado para armazenar informações de **behavior model**. (tipo `varchar(191)`; opcional).
  - `interest_list`: Campo usado para armazenar informações de **interesses list**. (tipo `longtext`; opcional).
  - `role_list`: Campo usado para armazenar informações de **role list**. (tipo `longtext`; opcional).
  - `behavior_list`: Campo usado para armazenar informações de **behavior list**. (tipo `longtext`; opcional).

### `mercadopago_webhook_log`
- **Finalidade da tabela:** Armazena dados relacionados a **mercadopago_webhook_log** no contexto do Marketing Hub.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `resource_id`: Chave de referência para o registro relacionado de **resource**. (tipo `varchar(150)`; opcional; chave `MUL`).
  - `topic`: Campo usado para armazenar informações de **topic**. (tipo `varchar(100)`; opcional).
  - `query_id`: Chave de referência para o registro relacionado de **query**. (tipo `varchar(150)`; opcional).
  - `query_topic`: Campo usado para armazenar informações de **query topic**. (tipo `varchar(100)`; opcional).
  - `payload_type`: Campo usado para armazenar informações de **payload tipo**. (tipo `varchar(100)`; opcional).
  - `payload_action`: Campo usado para armazenar informações de **payload ação**. (tipo `varchar(100)`; opcional).
  - `has_payload`: Campo usado para armazenar informações de **has payload**. (tipo `tinyint(1)`; opcional).
  - `payload`: Carga útil em formato textual/JSON retornada por integração. (tipo `longtext`; opcional).
  - `mercadopago_status`: Campo usado para armazenar informações de **mercadopago status**. (tipo `varchar(80)`; opcional).
  - `mercadopago_response`: Campo usado para armazenar informações de **mercadopago resposta**. (tipo `longtext`; opcional).
  - `processing_status`: Campo usado para armazenar informações de **processing status**. (tipo `varchar(40)`; obrigatório).
  - `error_message`: Campo usado para armazenar informações de **erro message**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; chave `MUL`; default `CURRENT_TIMESTAMP`).

### `metric_preset`
- **Finalidade da tabela:** Armazena dados relacionados a **metric_preset** no contexto do Marketing Hub.
- **Campos (5):**
  - `id`: Identificador único do registro. (tipo `varchar(50)`; obrigatório; chave `PRI`).
  - `name`: Nome de exibição do registro. (tipo `varchar(100)`; opcional).
  - `sample_size`: Campo usado para armazenar informações de **amostra size**. (tipo `int(11)`; opcional).
  - `stop_loss_factor`: Campo usado para armazenar informações de **stop loss factor**. (tipo `decimal(5,2)`; opcional).
  - `default_mde_pp`: Campo usado para armazenar informações de **default mde pp**. (tipo `decimal(5,2)`; opcional).

### `niche_detailed_description`
- **Finalidade da tabela:** Armazena dados relacionados a **niche_detailed_description** no contexto do Marketing Hub.
- **Campos (16):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `prompt_id`: Chave de referência para o registro relacionado de **prompt**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `title`: Campo usado para armazenar informações de **título**. (tipo `varchar(255)`; opcional).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `pains`: Campo usado para armazenar informações de **pains**. (tipo `longtext`; opcional).
  - `desires`: Campo usado para armazenar informações de **desires**. (tipo `longtext`; opcional).
  - `needs`: Campo usado para armazenar informações de **needs**. (tipo `longtext`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(191)`; opcional).
  - `cost_usd`: Campo usado para armazenar informações de **custo usd**. (tipo `decimal(10,4)`; opcional).
  - `input_tokens`: Campo usado para armazenar informações de **entrada tokens**. (tipo `int(11)`; opcional).
  - `output_tokens`: Campo usado para armazenar informações de **saída tokens**. (tipo `int(11)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `active`: Campo usado para armazenar informações de **active**. (tipo `tinyint(1)`; obrigatório; default `1`).

### `openai_model`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (11):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `code`: Campo usado para armazenar informações de **code**. (tipo `varchar(128)`; obrigatório; chave `UNI`).
  - `price_input_standard`: Campo usado para armazenar informações de **preço entrada standard**. (tipo `decimal(12,5)`; obrigatório).
  - `price_input_cached_standard`: Campo usado para armazenar informações de **preço entrada cached standard**. (tipo `decimal(12,5)`; obrigatório).
  - `price_output_standard`: Campo usado para armazenar informações de **preço saída standard**. (tipo `decimal(12,5)`; obrigatório).
  - `price_input_batch`: Campo usado para armazenar informações de **preço entrada batch**. (tipo `decimal(12,5)`; obrigatório).
  - `price_input_cached_batch`: Campo usado para armazenar informações de **preço entrada cached batch**. (tipo `decimal(12,5)`; obrigatório).
  - `price_output_batch`: Campo usado para armazenar informações de **preço saída batch**. (tipo `decimal(12,5)`; obrigatório).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `prompt`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (7):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `name`: Nome de exibição do registro. (tipo `varchar(191)`; obrigatório).
  - `domain`: Campo usado para armazenar informações de **domínio**. (tipo `varchar(100)`; obrigatório; chave `MUL`).
  - `template`: Campo usado para armazenar informações de **template**. (tipo `longtext`; obrigatório).
  - `active`: Campo usado para armazenar informações de **active**. (tipo `tinyint(1)`; obrigatório; default `0`).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `prompt_attribute`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `prompt_entity_id`: Chave de referência para o registro relacionado de **prompt_entity**. (tipo `bigint(20)`; opcional; chave `MUL`).

### `prompt_attribute_description`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `prompt_attribute_id`: Chave de referência para o registro relacionado de **prompt_attribute**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `active`: Campo usado para armazenar informações de **active**. (tipo `tinyint(1)`; opcional; default `1`).

### `prompt_domain`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (6):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `code`: Campo usado para armazenar informações de **code**. (tipo `varchar(100)`; obrigatório; chave `UNI`).
  - `name`: Nome de exibição do registro. (tipo `varchar(191)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `varchar(500)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).

### `prompt_domain_object`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (3):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `prompt_domain_id`: Chave de referência para o registro relacionado de **prompt_domain**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `object_type`: Campo usado para armazenar informações de **object tipo**. (tipo `enum('DETAILED_DESCRIPTION','DIFFERENTIATED_TECHNOLOGY','NICHE','JOURNEY','EXPERIMENT','HYPOTHESIS')`; obrigatório).

### `prompt_entity`
- **Finalidade da tabela:** Configura domínios, atributos e parâmetros usados na construção de prompts e geração com IA.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório; chave `UNI`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).

### `sales_funnel`
- **Finalidade da tabela:** Armazena dados relacionados a **sales_funnel** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `binary(16)`; obrigatório; chave `PRI`).
  - `name`: Nome de exibição do registro. (tipo `varchar(100)`; opcional).
  - `objective`: Campo usado para armazenar informações de **objetivo**. (tipo `varchar(255)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; opcional; default `CURRENT_TIMESTAMP`).

### `success_product`
- **Finalidade da tabela:** Armazena dados relacionados a **success_product** no contexto do Marketing Hub.
- **Campos (27):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `novo`: Campo usado para armazenar informações de **novo**. (tipo `tinyint(1)`; opcional; default `1`).
  - `niche`: Campo usado para armazenar informações de **nicho**. (tipo `varchar(255)`; opcional).
  - `avatar`: Campo usado para armazenar informações de **avatar**. (tipo `varchar(255)`; opcional).
  - `instagram_account_id`: Chave de referência para o registro relacionado de **instagram_account**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `explicit_pain`: Campo usado para armazenar informações de **explicit pain**. (tipo `longtext`; opcional).
  - `promise`: Campo usado para armazenar informações de **promise**. (tipo `longtext`; opcional).
  - `unique_mechanism`: Campo usado para armazenar informações de **unique mechanism**. (tipo `longtext`; opcional).
  - `tripwire`: Campo usado para armazenar informações de **tripwire**. (tipo `longtext`; opcional).
  - `risk_reversal`: Campo usado para armazenar informações de **risk reversal**. (tipo `longtext`; opcional).
  - `social_proof`: Campo usado para armazenar informações de **social proof**. (tipo `longtext`; opcional).
  - `checkout_monetization`: Campo usado para armazenar informações de **checkout monetization**. (tipo `longtext`; opcional).
  - `funnel`: Campo usado para armazenar informações de **funil**. (tipo `longtext`; opcional).
  - `creative_volume`: Campo usado para armazenar informações de **creative volume**. (tipo `longtext`; opcional).
  - `storytelling`: Campo usado para armazenar informações de **storytelling**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `timestamp`; obrigatório; default `CURRENT_TIMESTAMP`; on update CURRENT_TIMESTAMP).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `facebook_url`: Campo usado para armazenar informações de **facebook url**. (tipo `varchar(255)`; opcional).
  - `instagram_url`: Campo usado para armazenar informações de **Instagram url**. (tipo `varchar(255)`; opcional).
  - `sales_page_url`: Campo usado para armazenar informações de **sales página url**. (tipo `varchar(255)`; opcional).
  - `youtube_url`: Campo usado para armazenar informações de **youtube url**. (tipo `varchar(255)`; opcional).
  - `audience_type`: Campo usado para armazenar informações de **audience tipo**. (tipo `varchar(255)`; opcional).
  - `sales_funnel`: Campo usado para armazenar informações de **sales funil**. (tipo `longtext`; opcional).
  - `platform`: Campo usado para armazenar informações de **plataforma**. (tipo `enum('COFRE','HOTMART','CLICKBANK')`; obrigatório).
  - `generate_niche_hypothesis`: Campo usado para armazenar informações de **generate nicho hipótese**. (tipo `bit(1)`; obrigatório).

### `targeting_candidate`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (14):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `request_id`: Chave de referência para o registro relacionado de **request**. (tipo `binary(16)`; obrigatório; chave `MUL`).
  - `texto_sugerido`: Campo usado para armazenar informações de **texto sugerido**. (tipo `varchar(255)`; obrigatório).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('INTEREST','BEHAVIOR','WORK_POSITION')`; obrigatório).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING_FACEBOOK_MATCH','VALIDATED','NO_MATCH')`; obrigatório).
  - `idioma`: Campo usado para armazenar informações de **idioma**. (tipo `varchar(10)`; opcional).
  - `country`: Campo usado para armazenar informações de **country**. (tipo `varchar(5)`; opcional; chave `MUL`).
  - `origem`: Campo usado para armazenar informações de **origem**. (tipo `varchar(32)`; opcional).
  - `intent_tag`: Campo usado para armazenar informações de **intent tag**. (tipo `varchar(32)`; opcional).
  - `score`: Campo usado para armazenar informações de **score**. (tipo `decimal(5,4)`; opcional).
  - `rationale`: Campo usado para armazenar informações de **rationale**. (tipo `longtext`; opcional).
  - `rejection_reason`: Campo usado para armazenar informações de **rejection reason**. (tipo `longtext`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).

### `targeting_candidate_seed_variant`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (3):**
  - `candidate_id`: Chave de referência para o registro relacionado de **candidate**. (tipo `bigint(20)`; obrigatório; chave `PRI`).
  - `variant_order`: Campo usado para armazenar informações de **variante order**. (tipo `int(11)`; obrigatório; chave `PRI`).
  - `variant_value`: Campo usado para armazenar informações de **variante value**. (tipo `varchar(255)`; obrigatório).

### `targeting_element`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (19):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `hypothesis_id`: Chave de referência para o registro relacionado de **hypothesis**. (tipo `binary(16)`; opcional; chave `MUL`).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('INTEREST','JOB_TITLE','BEHAVIOR')`; obrigatório).
  - `term`: Campo usado para armazenar informações de **term**. (tipo `varchar(255)`; obrigatório).
  - `description`: Descrição textual do registro. (tipo `longtext`; opcional).
  - `prompt`: Prompt enviado ao modelo de IA para gerar o conteúdo. (tipo `longtext`; opcional).
  - `model`: Modelo de IA utilizado para gerar ou processar o conteúdo. (tipo `varchar(191)`; opcional).
  - `source`: Campo usado para armazenar informações de **source**. (tipo `enum('MANUAL','AI')`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('DRAFT','NEEDS_REVIEW','APPROVED','REJECTED')`; obrigatório).
  - `notes`: Campo usado para armazenar informações de **notes**. (tipo `longtext`; opcional).
  - `last_reviewed_by`: Campo usado para armazenar informações de **last reviewed by**. (tipo `varchar(191)`; opcional).
  - `meta_id`: Chave de referência para o registro relacionado de **meta**. (tipo `varchar(100)`; opcional).
  - `meta_key`: Campo usado para armazenar informações de **meta key**. (tipo `varchar(191)`; opcional).
  - `confidence`: Campo usado para armazenar informações de **confidence**. (tipo `decimal(10,4)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).
  - `meta_audience_size_lower_bound`: Campo usado para armazenar informações de **meta audience size lower bound**. (tipo `bigint(20)`; opcional).
  - `meta_audience_size_upper_bound`: Campo usado para armazenar informações de **meta audience size upper bound**. (tipo `bigint(20)`; opcional).

### `targeting_option`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (15):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `candidate_id`: Chave de referência para o registro relacionado de **candidate**. (tipo `bigint(20)`; obrigatório; chave `MUL`).
  - `facebook_id`: Chave de referência para o registro relacionado de **facebook**. (tipo `varchar(100)`; obrigatório).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; obrigatório).
  - `type`: Campo usado para armazenar informações de **tipo**. (tipo `enum('INTEREST','BEHAVIOR','WORK_POSITION')`; obrigatório).
  - `audience_size`: Campo usado para armazenar informações de **audience size**. (tipo `bigint(20)`; opcional).
  - `match_score`: Campo usado para armazenar informações de **match score**. (tipo `decimal(5,4)`; opcional).
  - `final_score`: Campo usado para armazenar informações de **final score**. (tipo `decimal(5,4)`; opcional).
  - `search_locale`: Campo usado para armazenar informações de **search locale**. (tipo `varchar(10)`; opcional).
  - `search_country`: Campo usado para armazenar informações de **search country**. (tipo `varchar(5)`; opcional).
  - `search_term`: Campo usado para armazenar informações de **search term**. (tipo `varchar(255)`; opcional).
  - `source`: Campo usado para armazenar informações de **source**. (tipo `enum('SEARCH','SUGGESTION','BROWSE')`; opcional).
  - `seed_variant`: Campo usado para armazenar informações de **semente variante**. (tipo `varchar(255)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).

### `targeting_request`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (12):**
  - `id`: Identificador único do registro. (tipo `binary(16)`; obrigatório; chave `PRI`).
  - `descricao`: Campo usado para armazenar informações de **descricao**. (tipo `varchar(500)`; obrigatório).
  - `locale`: Campo usado para armazenar informações de **locale**. (tipo `varchar(10)`; opcional).
  - `country`: Campo usado para armazenar informações de **country**. (tipo `varchar(5)`; opcional).
  - `audience_type`: Campo usado para armazenar informações de **audience tipo**. (tipo `enum('PROSPECT','REMARKETING')`; opcional).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING_AI','COMPLETED','FAILED')`; opcional; chave `MUL`).
  - `origin`: Campo usado para armazenar informações de **origin**. (tipo `enum('CLIENT','INTERNAL')`; opcional).
  - `market_niche_id`: Chave de referência para o registro relacionado de **market_niche**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `hypothesis_id`: Chave de referência para o registro relacionado de **hypothesis**. (tipo `binary(16)`; opcional; chave `MUL`).
  - `experiment_id`: Chave de referência para o registro relacionado de **experiment**. (tipo `bigint(20)`; opcional; chave `MUL`).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; opcional).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; opcional).

### `targeting_resolution_job`
- **Finalidade da tabela:** Armazena solicitações, candidatos e elementos usados para resolver segmentação de audiência.
- **Campos (13):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `candidate_id`: Chave de referência para o registro relacionado de **candidate**. (tipo `bigint(20)`; obrigatório; chave `UNI`).
  - `request_id`: Chave de referência para o registro relacionado de **request**. (tipo `binary(16)`; obrigatório; chave `MUL`).
  - `status`: Status atual do registro no fluxo de negócio. (tipo `enum('PENDING','PROCESSING','SUCCEEDED','FAILED')`; obrigatório; chave `MUL`).
  - `attempt_count`: Campo usado para armazenar informações de **attempt count**. (tipo `int(11)`; obrigatório; default `0`).
  - `result_count`: Campo usado para armazenar informações de **result count**. (tipo `int(11)`; opcional).
  - `last_error`: Campo usado para armazenar informações de **last erro**. (tipo `text`; opcional).
  - `locked_by`: Campo usado para armazenar informações de **locked by**. (tipo `varchar(64)`; opcional).
  - `locked_at`: Campo usado para armazenar informações de **locked at**. (tipo `datetime(6)`; opcional).
  - `started_at`: Campo usado para armazenar informações de **started at**. (tipo `datetime(6)`; opcional).
  - `finished_at`: Campo usado para armazenar informações de **finished at**. (tipo `datetime(6)`; opcional).
  - `created_at`: Data/hora de criação do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`).
  - `updated_at`: Data/hora da última atualização do registro. (tipo `datetime(6)`; obrigatório; default `CURRENT_TIMESTAMP(6)`; on update CURRENT_TIMESTAMP(6)).

### `visual_proof`
- **Finalidade da tabela:** Armazena dados relacionados a **visual_proof** no contexto do Marketing Hub.
- **Campos (4):**
  - `id`: Identificador único do registro. (tipo `bigint(20)`; obrigatório; chave `PRI`; auto_increment).
  - `description`: Descrição textual do registro. (tipo `tinytext`; opcional).
  - `name`: Nome de exibição do registro. (tipo `varchar(255)`; opcional).
  - `proof_type`: Campo usado para armazenar informações de **proof tipo**. (tipo `varchar(255)`; opcional).
