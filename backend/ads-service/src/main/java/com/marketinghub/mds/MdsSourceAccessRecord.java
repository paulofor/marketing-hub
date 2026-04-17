package com.marketinghub.mds;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "source_access_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdsSourceAccessRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_document_id", nullable = false, length = 191)
    private String sourceDocumentId;

    @Column(name = "access_class", nullable = false, length = 64)
    private String accessClass;

    @Column(name = "permission_state", nullable = false, length = 64)
    private String permissionState;

    @Column(name = "license_text", columnDefinition = "LONGTEXT")
    private String licenseText;

    @Column(name = "access_url", length = 512)
    private String accessUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
