#!/usr/bin/env bash
set -euo pipefail

PDE_LIQUIBASE_PROJECT="${PDE_LIQUIBASE_PROJECT:-aihub-8b839b4d-30f5-46ba-8e6f-35ae06a26cc8-e60d0ea702}"
PDE_LIQUIBASE_DATABASE="musa_v7_liquibase_validation"
PDE_LIQUIBASE_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PDE_LIQUIBASE_ROOT="$(cd "${PDE_LIQUIBASE_SCRIPT_DIR}/../.." && pwd)"
PDE_LIQUIBASE_COMPOSE=(
  docker compose
  -p "${PDE_LIQUIBASE_PROJECT}"
  -f "${PDE_LIQUIBASE_ROOT}/pde-platform/docker-compose.yml"
  -f "${PDE_LIQUIBASE_ROOT}/pde-platform/docker-compose.local-validation.yml"
  --profile local-e2e
  --profile liquibase-validation
)
PDE_LIQUIBASE_MYSQL_CONTAINER="$("${PDE_LIQUIBASE_COMPOSE[@]}" ps -q pde-platform-local-mysql)"
test -n "${PDE_LIQUIBASE_MYSQL_CONTAINER}"

docker exec "${PDE_LIQUIBASE_MYSQL_CONTAINER}" mysql -u root -ppde-root --execute "
  DROP DATABASE IF EXISTS ${PDE_LIQUIBASE_DATABASE};
  CREATE DATABASE ${PDE_LIQUIBASE_DATABASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  USE ${PDE_LIQUIBASE_DATABASE};
  CREATE TABLE pde_access_grant (
    token VARCHAR(36) NOT NULL,
    source VARCHAR(80) NOT NULL,
    PRIMARY KEY (token)
  ) ENGINE=InnoDB;
  CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(191) NOT NULL,
    pde_experience_json JSON NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_slug (slug)
  ) ENGINE=InnoDB;
  CREATE TABLE pde_production_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_slug VARCHAR(191) NOT NULL,
    experience_version VARCHAR(80) NOT NULL,
    draft_experience_json JSON NULL,
    published_experience_json JSON NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
  ) ENGINE=InnoDB;
  INSERT INTO product (slug, pde_experience_json, updated_at)
  VALUES ('metodo-musa-7-dias', JSON_OBJECT('legacy', TRUE), UTC_TIMESTAMP());
  INSERT INTO pde_production_slot (
    product_slug, experience_version, draft_experience_json, published_experience_json, updated_at
  ) VALUES (
    'metodo-musa-7-dias', 'musa-pde-entry-v7-espelho-antes-de-sair',
    JSON_OBJECT('legacy', TRUE), JSON_OBJECT('legacy', TRUE), UTC_TIMESTAMP()
  );
"

"${PDE_LIQUIBASE_COMPOSE[@]}" --profile liquibase-validation build pde-liquibase-validation >/dev/null

for PDE_LIQUIBASE_ROUND in 1 2; do
  "${PDE_LIQUIBASE_COMPOSE[@]}" --profile liquibase-validation run --rm --no-deps pde-liquibase-validation \
    --url="jdbc:mysql://pde-platform-local-mysql:3306/${PDE_LIQUIBASE_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    --username=root \
    --password=pde-root \
    --search-path=/liquibase/changelog \
    --changelog-file=changesets/2026-08-23-musa-v7-commercial-access.yaml \
    update >/dev/null
  "${PDE_LIQUIBASE_COMPOSE[@]}" --profile liquibase-validation run --rm --no-deps pde-liquibase-validation \
    --url="jdbc:mysql://pde-platform-local-mysql:3306/${PDE_LIQUIBASE_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    --username=root \
    --password=pde-root \
    --search-path=/liquibase/changelog \
    --changelog-file=changesets/2026-08-24-musa-v7-refund-reconciliation.yaml \
    update >/dev/null
done

"${PDE_LIQUIBASE_COMPOSE[@]}" run --rm --no-deps pde-liquibase-validation \
  --url="jdbc:mysql://pde-platform-local-mysql:3306/${PDE_LIQUIBASE_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  --username=root \
  --password=pde-root \
  --search-path=/liquibase/changelog \
  --changelog-file=changesets/2026-08-24-musa-v7-refund-reconciliation.yaml \
  rollback-count 1 >/dev/null

PDE_LIQUIBASE_ROLLBACK_RESULT="$(docker exec "${PDE_LIQUIBASE_MYSQL_CONTAINER}" mysql -N -u root -ppde-root \
  --database "${PDE_LIQUIBASE_DATABASE}" --execute "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE() AND table_name = 'pde_payment_audit'
         AND column_name IN ('refunded_at', 'experience_version')),
      ':', (SELECT COUNT(*) FROM DATABASECHANGELOG)
    );
  ")"
test "${PDE_LIQUIBASE_ROLLBACK_RESULT}" = "0:3"

"${PDE_LIQUIBASE_COMPOSE[@]}" run --rm --no-deps pde-liquibase-validation \
  --url="jdbc:mysql://pde-platform-local-mysql:3306/${PDE_LIQUIBASE_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  --username=root \
  --password=pde-root \
  --search-path=/liquibase/changelog \
  --changelog-file=changesets/2026-08-24-musa-v7-refund-reconciliation.yaml \
  update >/dev/null

PDE_LIQUIBASE_RESULT="$(docker exec "${PDE_LIQUIBASE_MYSQL_CONTAINER}" mysql -N -u root -ppde-root \
  --database "${PDE_LIQUIBASE_DATABASE}" --execute "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE() AND table_name = 'pde_access_grant'
         AND column_name IN ('experience_version', 'paid_at', 'expires_at')),
      ':', JSON_VALID(p.pde_experience_json),
      ':', JSON_UNQUOTE(JSON_EXTRACT(p.pde_experience_json, '\$.experienceVersion')),
      ':', (p.pde_experience_json = s.draft_experience_json),
      ':', (p.pde_experience_json = s.published_experience_json),
      ':', (SELECT COUNT(*) FROM information_schema.tables
             WHERE table_schema = DATABASE() AND table_name = 'pde_payment_audit'),
      ':', (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'pde_payment_audit'
               AND column_name IN ('refunded_at', 'experience_version')),
      ':', (SELECT COUNT(*) FROM DATABASECHANGELOG)
    )
    FROM product p
    JOIN pde_production_slot s ON s.product_slug = p.slug;
  ")"

test "${PDE_LIQUIBASE_RESULT}" = "3:1:musa-pde-entry-v7-espelho-antes-de-sair:1:1:1:2:4"
echo "Liquibase MUSA v7 aprovado no MySQL 5.7: duas aplicações, rollback e reaplicação idempotentes."
