package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.util.PasswordUtils;
import com.anhui.fabricbaascommon.util.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
class CertServiceTest {
    @Autowired
    private CertService certService;

    @Test
    void test() throws Exception {
        for (int i = 0; i < 30; i++) {
            CertfileEntity certfile = new CertfileEntity();
            certfile.setCaUsertype(RandomUtils.select(CertfileType.ALL));
            certfile.setCaPassword(PasswordUtils.generate());
            certfile.setCaUsername("TestCertfile" + i);
            certService.generate(certfile);
        }
        for (String usertype : CertfileType.ALL) {
            Page<CertfileEntity> page = certService.query(usertype, 1, 10);
            System.out.println(page.getTotalPages());
            System.out.println(page.getContent());
        }
    }
}