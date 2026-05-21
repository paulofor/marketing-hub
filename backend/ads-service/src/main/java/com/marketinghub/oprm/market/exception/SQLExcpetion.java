package com.marketinghub.oprm.market.exception;

/**
 * Responsável por representar falhas SQL do módulo OPRM preservando a instrução SQL tentada no contexto do erro.
 */
public class SQLExcpetion extends RuntimeException {

    private final String attemptedSql;

    /**
     * Cria a exception com a SQL tentada e a causa original para diagnóstico operacional.
     */
    public SQLExcpetion(String attemptedSql, Throwable cause) {
        super("Falha ao executar SQL no módulo OPRM.", cause);
        this.attemptedSql = attemptedSql;
    }

    /**
     * Retorna a SQL que estava sendo tentada quando a falha ocorreu.
     */
    public String getAttemptedSql() {
        return attemptedSql;
    }
}
