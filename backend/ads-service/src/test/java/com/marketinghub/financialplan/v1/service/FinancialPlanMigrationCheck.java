package com.marketinghub.financialplan.v1.service;

import java.sql.DriverManager;
import liquibase.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Responsabilidade: verificar reaplicação, rollback e recriação no MySQL descartável. */
public final class FinancialPlanMigrationCheck {
  /** Impede instanciar o verificador. */
  private FinancialPlanMigrationCheck() {}

  /** Confere histórico, nomes/precisão de colunas, reversão e reaplicação do changelog real. */
  public static void main(String[] args) throws Exception {
    try (var c =
        DriverManager.getConnection(
            FinancialPlanLocalApplication.jdbcUrl(), "root", "financial-plans-local-only")) {
      var lb =
          new Liquibase(
              FinancialPlanLocalApplication.CHANGELOG,
              new ClassLoaderResourceAccessor(),
              new JdbcConnection(c));
      if (!lb.listUnrunChangeSets(new Contexts(), new LabelExpression()).isEmpty())
        throw new IllegalStateException("Migração não aplicada");
      long count;
      try (var s = c.createStatement();
          var r = s.executeQuery("SELECT COUNT(*) FROM product_financial_plan_v1")) {
        r.next();
        count = r.getLong(1);
      }
      lb.update(new Contexts(), new LabelExpression());
      try (var s = c.createStatement();
          var r = s.executeQuery("SELECT COUNT(*) FROM product_financial_plan_v1")) {
        r.next();
        if (r.getLong(1) != count) throw new IllegalStateException("Histórico alterado");
      }
      try (var s = c.createStatement();
          var r =
              s.executeQuery(
                  "SELECT DATA_TYPE,DATETIME_PRECISION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_financial_plan_v1' AND COLUMN_NAME='created_at'")) {
        r.next();
        if (!"datetime".equals(r.getString(1)) || r.getInt(2) != 6)
          throw new IllegalStateException("Campo temporal divergente");
      }
      lb.rollback(1, new Contexts(), new LabelExpression());
      try (var s = c.createStatement();
          var r =
              s.executeQuery(
                  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_financial_plan_v1'")) {
        r.next();
        if (r.getInt(1) != 0) throw new IllegalStateException("Rollback incompleto");
      }
      lb.update(new Contexts(), new LabelExpression());
      lb.update(new Contexts(), new LabelExpression());
      System.out.println("MYSQL57_APPLY_REAPPLY_HISTORY_ROLLBACK_RESTORE_PASS");
    }
  }
}
