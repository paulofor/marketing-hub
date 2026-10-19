package com.marketinghub.repository.jpa.aiworkerconnection;

import com.marketinghub.aiworkerconnection.AiWorkerConnection;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Armazena a auditoria de conexão e serializa comandos pela linha de controle inicial. */
public interface AiWorkerConnectionRepository extends JpaRepository<AiWorkerConnection, Long> {
  /** Bloqueia a linha permanente antes de consultar ou modificar a fila do Worker. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AiWorkerConnection c where c.id = 1")
  AiWorkerConnection lockControl();

  /** Recupera o último comando sem apagar o histórico de tentativas encerradas. */
  Optional<AiWorkerConnection> findFirstByOrderByIdDesc();
}
