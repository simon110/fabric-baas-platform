package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.CsrConfig;
import com.anhui.fabricbaascommon.configuration.FabricConfiguration;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.fabric.CaUtils;
import com.anhui.fabricbaascommon.service.CaServerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
class CaServerServiceTest {
    @Autowired
    private CaServerService caServerService;
    @Autowired
    private FabricConfiguration fabricConfiguration;

    @Test
    void test() throws Exception {
        Assertions.assertFalse(caServerService.checkCaServer());

        CaEntity ca = new CaEntity();
        ca.setOrganizationName("AnhuiTech");
        ca.setCountryCode("CN");
        ca.setDomain("anhui.example.com");
        ca.setLocality("Guilin");
        ca.setStateOrProvince("Guangxi");

        CsrConfig csr = CaUtils.buildCsrConfig(ca);
        try {
            caServerService.initCaServer(csr);
            Assertions.assertTrue(caServerService.checkCaServer());
        } finally {
            caServerService.cleanCaServer();
            Assertions.assertFalse(caServerService.checkCaServer());
        }
    }
}