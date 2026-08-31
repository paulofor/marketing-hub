#!/usr/bin/env bash
set -euo pipefail

AUDIT_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUDIT_MODULE_DIR="$(cd "${AUDIT_SCRIPT_DIR}/.." && pwd)"
AUDIT_COMPOSE_FILE="${AUDIT_MODULE_DIR}/docker-compose.product-discovery-bpm-audit-mysql57.yml"
AUDIT_COMPOSE_PROJECT="${PRODUCT_DISCOVERY_BPM_COMPOSE_PROJECT:-marketing-hub-product-discovery-bpm-audit}"

audit_compose() {
  docker compose -p "${AUDIT_COMPOSE_PROJECT}" -f "${AUDIT_COMPOSE_FILE}" "$@"
}

audit_cleanup() {
  audit_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

audit_db_scalar() {
  audit_compose exec -T mysql57-product-discovery-bpm-audit \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

audit_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

audit_liquibase_command() {
  local command="$1"
  audit_compose run --rm liquibase-product-discovery-bpm-audit sh -lc \
    'AUDIT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$AUDIT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

audit_argos_market_update() {
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-30-argos-market-discovery-v1.yaml \
    liquibase-product-discovery-bpm-audit
}

audit_argos_market_command() {
  local command="$1"
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-30-argos-market-discovery-v1.yaml \
    liquibase-product-discovery-bpm-audit sh -lc \
    'AUDIT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$AUDIT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

audit_argos_meta_browser_update() {
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-30-argos-meta-public-browser-v1.yaml \
    liquibase-product-discovery-bpm-audit
}

audit_argos_meta_browser_command() {
  local command="$1"
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-30-argos-meta-public-browser-v1.yaml \
    liquibase-product-discovery-bpm-audit sh -lc \
    'AUDIT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$AUDIT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

audit_autonomous_handoff_update() {
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-31-product-discovery-autonomous-handoff-v1.yaml \
    liquibase-product-discovery-bpm-audit
}

audit_autonomous_handoff_command() {
  local command="$1"
  audit_compose run --rm \
    -e ADS_LIQUIBASE_CHANGELOG_FILE=db/changelog/changesets/2026-08-31-product-discovery-autonomous-handoff-v1.yaml \
    liquibase-product-discovery-bpm-audit sh -lc \
    'AUDIT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$AUDIT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

trap audit_cleanup EXIT
audit_cleanup

docker version >/dev/null
docker compose version >/dev/null
audit_compose up -d --build mysql57-product-discovery-bpm-audit
audit_compose run --rm --build liquibase-product-discovery-bpm-audit

audit_assert_equal \
  "status e correlação dos ciclos" \
  "6:6:37=BLOCKED,38=BLOCKED,39=BLOCKED,40=COMPLETED,41=PENDING,42=IN_PROGRESS" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT GROUP_CONCAT(CONCAT(SUBSTRING_INDEX(source_reference, ':', -1), '=', status) ORDER BY id SEPARATOR ',') FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%')
  );")"
audit_assert_equal \
  "plano estruturado e oportunidade real" \
  "OBJECT:1" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT JSON_TYPE(JSON_EXTRACT(evidence_json, '$.researchPlan')) FROM agent_task WHERE source_reference = 'product-discovery-cycle:37'), ':',
    (SELECT JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.opportunityCount')) FROM agent_task WHERE source_reference = 'product-discovery-cycle:40')
  );")"
audit_assert_equal \
  "resultado bloqueado preserva a causa" \
  "OBJECT:POST complete falhou com status 422" \
  "$(audit_db_scalar "SELECT CONCAT(
    JSON_TYPE(JSON_EXTRACT(result_json, '$')), ':',
    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.error'))
  ) FROM agent_task WHERE source_reference = 'product-discovery-cycle:37';")"
audit_assert_equal \
  "falhas preservadas sem datas ou custo inventados" \
  "3:3:0" \
  "$(audit_db_scalar "SELECT CONCAT(
    SUM(status = 'BLOCKED' AND execution_error = 'POST complete falhou com status 422'), ':',
    SUM(status = 'BLOCKED' AND received_at IS NULL AND delivered_at IS NULL), ':',
    SUM(input_tokens IS NOT NULL OR estimated_cost_usd IS NOT NULL)
  ) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%';")"
audit_assert_equal \
  "marcador exclusivo do retroativo" \
  "6" \
  "$(audit_db_scalar "SELECT COUNT(*) FROM agent_task
    WHERE JSON_VALID(evidence_json)
      AND JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.backfillSource'))
        = 'product-discovery-bpm-audit/v1';")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG WHERE ID = '2026-08-28-product-discovery-bpm-audit-1-backfill';" \
  >/dev/null 2>&1
audit_compose run --rm liquibase-product-discovery-bpm-audit
audit_assert_equal \
  "reaplicação idempotente" \
  "6:6" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%')
  );")"

audit_liquibase_command "rollbackCount 1"
audit_assert_equal \
  "rollback preserva os ciclos de origem" \
  "0:0:6" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT COUNT(*) FROM product_discovery_cycle)
  );")"

audit_compose run --rm liquibase-product-discovery-bpm-audit
audit_assert_equal \
  "reaplicação após rollback" \
  "6:6:3" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE status = 'BLOCKED' AND execution_error = 'POST complete falhou com status 422')
  );")"

audit_argos_market_update
audit_assert_equal \
  "schema e versão da descoberta ampla de mercados" \
  "6:4" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name IN (
          'research_mode',
          'market_type',
          'reference_sources',
          'research_analysis_raw_response',
          'research_analysis_model',
          'research_evidence_report_json'
        )), ':',
    (SELECT current_version FROM agent WHERE agent_key = 'market-radar')
  );")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG WHERE ID LIKE '2026-08-30-argos-market-discovery-v1-%';" \
  >/dev/null 2>&1
audit_argos_market_update
audit_assert_equal \
  "retomada idempotente da descoberta ampla" \
  "6:4" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name IN (
          'research_mode',
          'market_type',
          'reference_sources',
          'research_analysis_raw_response',
          'research_analysis_model',
          'research_evidence_report_json'
        )), ':',
    (SELECT current_version FROM agent WHERE agent_key = 'market-radar')
  );")"

audit_argos_market_command "rollbackCount 2"
audit_assert_equal \
  "rollback isolado da descoberta ampla" \
  "0:3" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name IN (
          'research_mode',
          'market_type',
          'reference_sources',
          'research_analysis_raw_response',
          'research_analysis_model',
          'research_evidence_report_json'
        )), ':',
    (SELECT current_version FROM agent WHERE agent_key = 'market-radar')
  );")"

audit_argos_market_update
audit_assert_equal \
  "reaplicação da descoberta ampla após rollback" \
  "6:4" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name IN (
          'research_mode',
          'market_type',
          'reference_sources',
          'research_analysis_raw_response',
          'research_analysis_model',
          'research_evidence_report_json'
        )), ':',
    (SELECT current_version FROM agent WHERE agent_key = 'market-radar')
  );")"

audit_argos_meta_browser_update
audit_argos_meta_browser_update
audit_assert_equal \
  "vínculo e auditoria do navegador público de Argos" \
  "1:16:3" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name = 'meta_ad_investigation_id'), ':',
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_meta_browser_run'), ':',
    (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_meta_browser_run'
        AND index_name IN (
          'PRIMARY',
          'uk_product_discovery_meta_browser_run',
          'idx_product_discovery_meta_browser_latest'
        ))
  );")"

audit_argos_meta_browser_command "rollbackCount 2"
audit_assert_equal \
  "rollback isolado do navegador público" \
  "0:0" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name = 'meta_ad_investigation_id'), ':',
    (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_meta_browser_run')
  );")"

audit_argos_meta_browser_update
audit_assert_equal \
  "reaplicação do navegador público após rollback" \
  "1:1" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_cycle'
        AND column_name = 'meta_ad_investigation_id'), ':',
    (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_meta_browser_run')
  );")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "INSERT INTO product_discovery_opportunity
        (cycle_id, name, evidence_json, score, decision)
      VALUES
        (40, 'Dossie declarado pronto',
          JSON_OBJECT('candidateEvidence', JSON_OBJECT('maturity', 'DOSSIER_READY')),
          81.00, 'RESEARCH_MORE'),
        (40, 'Aprovacao legada', 'payload-invalido', 72.00, 'APPROVE'),
        (40, 'Revisao sensivel',
          JSON_OBJECT('candidateEvidence', JSON_OBJECT('maturity', 'HUMAN_REVIEW')),
          55.00, 'APPROVE'),
        (40, 'Candidata descartada', NULL, 12.00, 'REJECT');" \
  >/dev/null 2>&1

audit_autonomous_handoff_update
audit_assert_equal \
  "maturidade factual retroativa" \
  "RESEARCHABLE,DOSSIER_READY,DOSSIER_READY,HUMAN_REVIEW,REJECTED" \
  "$(audit_db_scalar "SELECT GROUP_CONCAT(maturity_status ORDER BY id SEPARATOR ',')
    FROM product_discovery_opportunity;")"
audit_assert_equal \
  "schema retomável do handoff autônomo" \
  "NO:SIGNAL:1:2:2:2" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT is_nullable FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_opportunity'
        AND column_name = 'maturity_status'), ':',
    (SELECT column_default FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_opportunity'
        AND column_name = 'maturity_status'), ':',
    (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_opportunity'
        AND index_name = 'idx_product_discovery_opportunity_maturity'), ':',
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND column_name IN ('product_discovery_opportunity_id', 'created_product_id')), ':',
    (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND index_name IN (
          'uk_opportunity_dossier_discovery_opportunity',
          'uk_opportunity_dossier_created_product'
        )), ':',
    (SELECT COUNT(*) FROM information_schema.referential_constraints
      WHERE constraint_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND constraint_name IN (
          'fk_opportunity_dossier_discovery_opportunity',
          'fk_opportunity_dossier_created_product'
        ))
  );")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "UPDATE opportunity_dossier
         SET product_discovery_opportunity_id = 2,
             created_product_id = 900
       WHERE id = 700;
      INSERT INTO product (id, name) VALUES (901, 'Produto temporario para FK');
      INSERT INTO product_discovery_opportunity
        (id, cycle_id, name, evidence_json, score, decision)
      VALUES (600, 40, 'Candidata temporaria para FK', NULL, 10.00, 'REJECT');
      INSERT INTO opportunity_dossier
        (id, product_discovery_cycle_id, converted_plan_id, title,
         product_discovery_opportunity_id, created_product_id)
      VALUES (701, 40, NULL, 'Dossie temporario para FK', 600, 901);
      DELETE FROM product_discovery_opportunity WHERE id = 600;
      INSERT IGNORE INTO opportunity_dossier
        (id, product_discovery_cycle_id, converted_plan_id, title,
         product_discovery_opportunity_id, created_product_id)
      VALUES (702, 40, NULL, 'Duplicata que deve ser ignorada', 3, 900);" \
  >/dev/null 2>&1
audit_assert_equal \
  "linhagem, unicidade e exclusão segura" \
  "2:900:NULL:901:1" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT product_discovery_opportunity_id FROM opportunity_dossier WHERE id = 700), ':',
    (SELECT created_product_id FROM opportunity_dossier WHERE id = 700), ':',
    COALESCE((SELECT CAST(product_discovery_opportunity_id AS CHAR)
      FROM opportunity_dossier WHERE id = 701), 'NULL'), ':',
    (SELECT created_product_id FROM opportunity_dossier WHERE id = 701), ':',
    (SELECT COUNT(*) FROM opportunity_dossier WHERE created_product_id = 900)
  );")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG
      WHERE ID LIKE '2026-08-31-product-discovery-autonomous-handoff-v1-%';" \
  >/dev/null 2>&1
audit_autonomous_handoff_update
audit_assert_equal \
  "retomada após DDL aplicado sem registro" \
  "7:1:2" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM DATABASECHANGELOG
      WHERE ID LIKE '2026-08-31-product-discovery-autonomous-handoff-v1-%'), ':',
    (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_opportunity'
        AND index_name = 'idx_product_discovery_opportunity_maturity'), ':',
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND column_name IN ('product_discovery_opportunity_id', 'created_product_id'))
  );")"

audit_autonomous_handoff_command "rollbackCount 7"
audit_assert_equal \
  "rollback integral do handoff autônomo" \
  "0:0" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'product_discovery_opportunity'
        AND column_name = 'maturity_status'), ':',
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND column_name IN ('product_discovery_opportunity_id', 'created_product_id'))
  );")"

audit_autonomous_handoff_update
audit_assert_equal \
  "reaplicação do handoff autônomo após rollback" \
  "5:2:2" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM product_discovery_opportunity
      WHERE maturity_status IS NOT NULL), ':',
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND column_name IN ('product_discovery_opportunity_id', 'created_product_id')), ':',
    (SELECT COUNT(*) FROM information_schema.referential_constraints
      WHERE constraint_schema = DATABASE()
        AND table_name = 'opportunity_dossier'
        AND constraint_name IN (
          'fk_opportunity_dossier_discovery_opportunity',
          'fk_opportunity_dossier_created_product'
        ))
  );")"

echo "Auditoria BPM da descoberta PDE aprovada no MySQL 5.7."
