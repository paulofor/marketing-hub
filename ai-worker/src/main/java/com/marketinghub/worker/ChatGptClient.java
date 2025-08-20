package com.marketinghub.worker;

import com.marketinghub.successproduct.SuccessProduct;

public interface ChatGptClient {
    SuccessProduct enrich(SuccessProduct product);
}
