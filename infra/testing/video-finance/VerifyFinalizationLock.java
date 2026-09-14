import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import jakarta.persistence.LockModeType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: reproduzir a leitura concorrente de finalização em MySQL 5.7 segregado. */
public class VerifyFinalizationLock {
    private static final String TABLE = "fixture_video_finalization_lock_v1";

    /** Comprova snapshot antigo, espera pelo lock da fonte e leitura atual do filho persistido. */
    public static void main(String[] args) throws Exception {
        Lock lock = SalesVideoJobRepository.class
                .getMethod("findFirstByRetryOfJob_IdOrderByRequestedAtDescIdDesc", Long.class)
                .getAnnotation(Lock.class);
        require(lock != null && lock.value() == LockModeType.PESSIMISTIC_WRITE,
                "O repositório deixou de exigir leitura atual sob lock.");
        var executor = Executors.newSingleThreadExecutor();
        try (Connection setup = connection(); Connection first = connection(); Connection second = connection()) {
            require(setup.getMetaData().getDatabaseProductVersion().startsWith("5.7."), "MySQL 5.7 obrigatório.");
            sql(setup, "DROP TABLE IF EXISTS " + TABLE);
            sql(setup, "CREATE TABLE " + TABLE + " (id BIGINT PRIMARY KEY, source_id BIGINT NULL, status VARCHAR(32) NOT NULL, requested_at DATETIME NOT NULL, INDEX(source_id)) ENGINE=InnoDB");
            sql(setup, "INSERT INTO " + TABLE + " VALUES (91001,NULL,'VIDEO_READY','2026-09-14 00:00:00')");
            first.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            second.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            first.setAutoCommit(false); second.setAutoCommit(false);
            firstId(first, "SELECT id FROM " + TABLE + " WHERE id=91001 FOR UPDATE");
            CountDownLatch snapshot = new CountDownLatch(1);
            var next = executor.submit(() -> {
                require(firstId(second, "SELECT COUNT(*) FROM " + TABLE + " WHERE source_id=91001") == 0,
                        "A segunda solicitação precisa começar antes da criação do filho.");
                snapshot.countDown();
                firstId(second, "SELECT id FROM " + TABLE + " WHERE id=91001 FOR UPDATE");
                require(firstId(second, "SELECT id FROM " + TABLE + " WHERE source_id=91001") == 0,
                        "O teste precisa reproduzir o snapshot anterior do REPEATABLE READ.");
                long child = firstId(second, "SELECT id FROM " + TABLE + " WHERE source_id=91001 ORDER BY requested_at DESC,id DESC LIMIT 1 FOR UPDATE");
                second.commit();
                return child;
            });
            require(snapshot.await(10, TimeUnit.SECONDS), "A segunda transação não iniciou.");
            sql(first, "INSERT INTO " + TABLE + " VALUES (91002,91001,'VIDEO_REQUESTED','2026-09-14 00:00:01')");
            first.commit();
            require(next.get(15, TimeUnit.SECONDS) == 91002, "O filho confirmado não foi visto pela segunda solicitação.");
            require(firstId(setup, "SELECT COUNT(*) FROM " + TABLE + " WHERE source_id=91001") == 1,
                    "A concorrência criou mais de uma finalização.");
            System.out.println("PASS MySQL 5.7: snapshot anterior reproduzido, filho atual visível sob lock e uma única finalização.");
        } finally {
            executor.shutdownNow();
            try (Connection cleanup = connection()) { sql(cleanup, "DROP TABLE IF EXISTS " + TABLE); }
        }
    }

    /** Abre exclusivamente o MySQL efêmero já usado pela matriz, com credencial sintética. */
    private static Connection connection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:mysql://sandbox-docker:18307/learning_cycles_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "cycles_local", "cycles-local-only");
    }

    /** Executa comandos somente na tabela sintética constante da fixture. */
    private static void sql(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) { statement.setQueryTimeout(10); statement.execute(sql); }
    }

    /** Lê o primeiro identificador ou zero quando o snapshot ainda não enxerga nenhum filho. */
    private static long firstId(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (var result = statement.executeQuery(sql)) { return result.next() ? result.getLong(1) : 0; }
        }
    }

    /** Interrompe a homologação quando o contrato concorrente não é comprovado. */
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
