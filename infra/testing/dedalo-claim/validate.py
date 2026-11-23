"""Valida migração e exclusão transacional em MySQL sintético, sem dados de produtos."""
import argparse
import concurrent.futures
import pathlib
import subprocess
import textwrap

parser = argparse.ArgumentParser()
parser.add_argument("--project", required=True)
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[3]
compose = ["docker", "compose", "-p", args.project, "-f", str(pathlib.Path(__file__).with_name("compose.yaml"))]


def sql(statement, check=True):
    """Executa somente no container efêmero pertencente ao projeto informado."""
    result = subprocess.run(compose + ["exec", "-T", "mysql", "mysql", "-uroot",
        "-psynthetic-dedalo-fixture", "--batch", "--skip-column-names", "fixture"],
        input=statement, text=True, capture_output=True, check=False)
    if check and result.returncode:
        raise RuntimeError(result.stderr)
    return result


try:
    subprocess.run(compose + ["up", "-d", "--wait", "--wait-timeout", "100"], check=True)
    sql("CREATE TABLE agent_task(id BIGINT PRIMARY KEY,status VARCHAR(30)) ENGINE=InnoDB;")
    migration = (root / "backend/ads-service/src/main/resources/db/changelog/changesets/2026-11-23-dedalo-claim-receipt-v1.yaml").read_text()
    sql(textwrap.dedent(migration.split("sql: |\n", 1)[1]))
    sql("INSERT INTO agent_task(id,status) VALUES(6001,'PENDING'),(7001,'PENDING');")

    def reserve(number):
        """Disputa linhas com lock pessimista antes de atualizar a identidade persistida."""
        return sql(f"START TRANSACTION; SET @candidate=NULL; SELECT id INTO @candidate FROM agent_task WHERE status='PENDING' ORDER BY id LIMIT 1 FOR UPDATE; DO SLEEP(0.1); UPDATE agent_task SET status='IN_PROGRESS',execution_claim_id='00000000-0000-0000-0000-{number:012d}',execution_claim_phase='CLAIMED',execution_claim_heartbeat_at=NOW() WHERE id=@candidate AND status='PENDING'; COMMIT;")

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(reserve, [1, 2]))
    assert sql("SELECT COUNT(DISTINCT execution_claim_id) FROM agent_task WHERE status='IN_PROGRESS';").stdout.strip() == "2"
    assert sql("UPDATE agent_task SET execution_claim_id='00000000-0000-0000-0000-000000000001';", check=False).returncode != 0
    assert sql("SELECT COUNT(*) FROM agent_task WHERE execution_claim_heartbeat_at IS NOT NULL;").stdout.strip() == "2"
    def start_once(_):
        """Compara e altera fase sob transação; somente uma autorização pode vencer."""
        return int(sql("START TRANSACTION; SELECT execution_claim_phase INTO @phase FROM agent_task WHERE id=6001 FOR UPDATE; UPDATE agent_task SET execution_claim_phase='START_AUTHORIZED' WHERE id=6001 AND execution_claim_phase='CLAIMED'; SELECT ROW_COUNT(); COMMIT;").stdout.strip())

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        assert sum(pool.map(start_once, [1, 2])) == 1
    print("PASS: migração MySQL 5.7, duas reservas concorrentes segregadas, unicidade, início único concorrente e datas explícitas.")
finally:
    subprocess.run(compose + ["down", "--volumes", "--remove-orphans"], check=True)
