package com.anhui.fabricbaasorg.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
class TTPOrganizationApiTest {
    @Autowired
    private RemoteHttpClient remoteHttpClient;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

    @BeforeEach
    public void initClient() throws Exception {
        remoteHttpClient.init("192.168.0.208:8080");
        String token = ttpOrganizationApi.login("admin", "12345678");
        System.out.println(token);
    }

    @Test
    public void test() throws Exception {
        String ordererOrganizationName = ttpOrganizationApi.getOrdererOrganizationName();
        System.out.println(ordererOrganizationName);
    }
}