package com.marketinghub.worker.hotmart;

import lombok.Data;

import java.util.List;

/**
 * Wrapper for responses from Hotmart API.
 */
@Data
public class HotmartResponse {
    private List<HotmartProduct> items;
}
