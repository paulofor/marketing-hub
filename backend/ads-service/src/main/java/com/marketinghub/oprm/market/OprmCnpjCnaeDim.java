package com.marketinghub.oprm.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Data;

@Entity
@Data
public class OprmCnpjCnaeDim {
    @Id
    @Column(length = 7)
    private String cnaeCode;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Instant updatedAt;
}
