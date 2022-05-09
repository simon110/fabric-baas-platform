package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.service.CaContainerService;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest
class SystemServiceTest {
    @Autowired
    private SystemService systemService;
    @Autowired
    private CaContainerService caContainerService;

    @Test
    public void test() throws Exception {
        CaEntity org = new CaEntity();
        org.setOrganizationName("AnhuiTech");
        org.setCountryCode("CN");
        org.setDomain("anhui.example.com");
        org.setLocality("Guilin");
        org.setStateOrProvince("Guangxi");

        RemoteUserEntity remoteUser = new RemoteUserEntity();
        remoteUser.setOrganizationName("TestOrgA");
        remoteUser.setApiServer("192.168.0.208:8080");
        remoteUser.setPassword("12345678");

        String adminPassword = "12345678";
        MultipartFile kubernetesConfig = MyFileUtils.toMultipartFile(new File("example/kubernetes/admin.conf"));

        try {
            Assertions.assertFalse(systemService.isAvailable());
            systemService.init(org, remoteUser, adminPassword, kubernetesConfig);
            Assertions.assertTrue(systemService.isAvailable());

        } catch (Exception e) {
            caContainerService.cleanCaContainer();
            throw e;
        }
    }
}