package com.marketinghub.oprm.market;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Data
public class OprmCnpjImportRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDate snapshotDate;
    @Column(nullable = false)
    private String sourceUrl;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private Instant startedAt;
    private Instant finishedAt;
    @Column(nullable = false) private int filesTotal;
    @Column(nullable = false) private int filesProcessed;
    @Column(nullable = false) private long rowsRead;
    @Column(nullable = false) private long rowsValid;
    @Column(nullable = false) private long rowsRejected;
    @Column(length = 1000)
    private String errorMessage;
}
