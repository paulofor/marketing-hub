package com.marketinghub.worker.hotmart;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of a product returned by the Hotmart API.
 */
@Data
@NoArgsConstructor
public class HotmartProduct {
    private String id;
    private String name;
    private double temperature;
}
