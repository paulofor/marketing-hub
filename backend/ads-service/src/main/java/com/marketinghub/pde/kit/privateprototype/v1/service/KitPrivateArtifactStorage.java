package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.marketinghub.pde.kit.privateprototype.v1.KitPrivateArtifact;
import com.marketinghub.pde.kit.privateprototype.v1.KitPrivateArtifactChunk;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactChunkRepository;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: armazenar e recompor ZIPs íntegros em transação sem ampliar limites do MySQL.
 */
@Service
public class KitPrivateArtifactStorage {
  public static final int PART_BYTES = 1024 * 1024;
  private final KitPrivateArtifactChunkRepository chunks;

  /** Recebe a persistência das partes; nenhuma cópia fica em armazenamento público. */
  public KitPrivateArtifactStorage(KitPrivateArtifactChunkRepository chunks) {
    this.chunks = chunks;
  }

  /** Grava partes limitadas na transação do callback; qualquer falha desfaz o pacote inteiro. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void store(KitPrivateArtifact artifact, byte[] bytes) {
    if (bytes == null || bytes.length == 0 || bytes.length > KitArtifactContract.MAX_ZIP_BYTES)
      throw new IllegalArgumentException("Tamanho do pacote privado inválido.");
    if (bytes.length <= PART_BYTES) {
      artifact.setZipBytes(bytes);
      return;
    }
    artifact.setZipBytes(null);
    for (int offset = 0, part = 0; offset < bytes.length; offset += PART_BYTES, part++) {
      var chunk = new KitPrivateArtifactChunk();
      chunk.setArtifactId(artifact.getId());
      chunk.setPartNumber(part);
      chunk.setPartBytes(
          Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + PART_BYTES)));
      // O flush por parte impede um lote SQL maior que o limite publicado de 16 MiB.
      chunks.saveAndFlush(chunk);
    }
  }

  /**
   * Recompõe bytes privados, preservando arquivos legados e recusando partes ausentes ou trocadas.
   */
  @Transactional(readOnly = true)
  public byte[] read(KitPrivateArtifact artifact) {
    byte[] result = artifact.getZipBytes();
    if (result == null) {
      var output = new ByteArrayOutputStream();
      int expected = 0;
      for (var chunk : chunks.findByArtifactIdOrderByPartNumberAsc(artifact.getId())) {
        byte[] part = chunk.getPartBytes();
        if (!artifact.getId().equals(chunk.getArtifactId())
            || chunk.getPartNumber() != expected++
            || part == null
            || part.length == 0
            || part.length > PART_BYTES
            || output.size() + part.length > KitArtifactContract.MAX_ZIP_BYTES)
          throw new IllegalStateException("Partes do pacote privado inconsistentes.");
        output.writeBytes(part);
      }
      result = output.toByteArray();
    }
    if (result.length == 0
        || (artifact.getZipSha256() != null
            && !KitArtifactContract.hash(result).equals(artifact.getZipSha256())))
      throw new IllegalStateException("Integridade do pacote privado divergente.");
    return result;
  }
}
