package com.marketinghub.oprm.market;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;

@Entity
@Data
public class OprmCnpjImportFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private OprmCnpjImportRun run;
    @Column(nullable = false, length = 120) private String fileName;
    @Column(nullable = false) private String fileUrl;
    @Column(nullable = false, length = 30) private String datasetType;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private long rowsRead;
    @Column(nullable = false) private long rowsValid;
    @Column(nullable = false) private long rowsRejected;
    @Column(nullable = false) private Instant startedAt;
    private Instant finishedAt;
    @Column(length = 1000) private String errorMessage;
}
