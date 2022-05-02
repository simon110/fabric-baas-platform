package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaasorg.entity.OrdererEntity;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
class NetworkServiceTest {
    @Autowired
    private NetworkService networkService;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;

    @Test
    public void test() throws Exception {
        ttpOrganizationApi.login("TestOrgA", "12345678");
        List<OrdererEntity> orderers = Arrays.asList(
                new OrdererEntity("TestOrgAOrderer0", "kubenode1", 30500),
                new OrdererEntity("TestOrgAOrderer1", "kubenode2", 30501)
        );
        networkService.create("HelloNetwork", "HelloConsortium", orderers);
    }
}