package com.marketinghub.pde.kit.privateprototype.v1;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar uma parte ordenada do ZIP privado sem exceder o pacote SQL. */
@Entity
@Table(name = "kit_private_artifact_chunk_v1")
@Getter
@Setter
public class KitPrivateArtifactChunk {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "artifact_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
  private String artifactId;

  @Column(name = "part_number", nullable = false)
  private Integer partNumber;

  @Lob
  @Column(name = "part_bytes", nullable = false, columnDefinition = "MEDIUMBLOB")
  private byte[] partBytes;
}
