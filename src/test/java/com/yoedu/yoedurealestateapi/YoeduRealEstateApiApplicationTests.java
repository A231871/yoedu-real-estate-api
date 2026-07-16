package com.yoedu.yoedurealestateapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.data.redis.listener.auto-startup=false")
class YoeduRealEstateApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
