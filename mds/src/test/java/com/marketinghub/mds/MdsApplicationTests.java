package com.marketinghub.mds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mds.loop-enabled=false")
class MdsApplicationTests {

    @Test
    void contextLoads() {
    }
}
