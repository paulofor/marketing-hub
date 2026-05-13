package com.marketinghub.oprm.market;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Data;

@Data
@Embeddable
public class OprmMarketSizeByCnaeId implements Serializable {
    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, length = 7)
    private String cnaeCode;
}
