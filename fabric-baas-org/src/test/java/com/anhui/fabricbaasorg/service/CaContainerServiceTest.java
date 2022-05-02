package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.service.CaContainerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
class CaContainerServiceTest {
    @Autowired
    private CaContainerService caContainerService;

    @Test
    void test() throws Exception {
        Assertions.assertFalse(caContainerService.checkCaContainer());
    }
}