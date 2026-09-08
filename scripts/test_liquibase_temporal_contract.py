"""Protege o validador contra falsos bloqueios de SQL válido e defaults aparentes."""
import unittest
from scripts.liquibase_temporal_contract import timestamp_columns_without_default


class TemporalContractTest(unittest.TestCase):
    def test_accepts_explicit_current_timestamp_default(self):
        self.assertEqual([], timestamp_columns_without_default("created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;"))

    def test_accepts_microsecond_precision_and_multiline_default(self):
        self.assertEqual([], timestamp_columns_without_default("created_at TIMESTAMP(6) NOT NULL\n DEFAULT CURRENT_TIMESTAMP(6);"))

    def test_rejects_missing_default(self):
        self.assertEqual([2], timestamp_columns_without_default("CREATE TABLE t (\ncreated_at TIMESTAMP(6) NOT NULL);"))

    def test_other_column_default_cannot_hide_failure(self):
        self.assertEqual([1], timestamp_columns_without_default("a TIMESTAMP NOT NULL, b TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;"))

    def test_comments_and_literal_text_cannot_hide_failure(self):
        self.assertEqual([1], timestamp_columns_without_default("a TIMESTAMP NOT NULL COMMENT 'DEFAULT CURRENT_TIMESTAMP' /* DEFAULT 0 */;"))

    def test_nullable_timestamp_and_application_datetime_are_valid(self):
        self.assertEqual([], timestamp_columns_without_default("a TIMESTAMP NULL, b DATETIME(6) NOT NULL;"))

    def test_default_before_nullability_is_valid(self):
        self.assertEqual([], timestamp_columns_without_default("a TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL;"))

    def test_real_cycle_clock_migration_has_no_violation(self):
        from pathlib import Path
        source = Path("backend/ads-service/src/main/resources/db/changelog/changesets/2026-09-08-learning-sales-cycles-v1.yaml").read_text()
        self.assertEqual([], timestamp_columns_without_default(source))


if __name__ == "__main__":
    unittest.main()
