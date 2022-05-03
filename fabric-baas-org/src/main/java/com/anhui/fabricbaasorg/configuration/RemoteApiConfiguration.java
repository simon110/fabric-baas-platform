package com.anhui.fabricbaasorg.configuration;

import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import com.anhui.fabricbaasorg.remote.RemoteHttpClient;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.RemoteUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RemoteApiConfiguration {
    @Autowired
    private RemoteHttpClient remoteHttpClient;
    @Autowired
    private RemoteUserRepo remoteUserRepo;

    @Bean
    public TTPChannelApi ttpChannelApi() {
        return new TTPChannelApi(remoteHttpClient);
    }

    @Bean
    public TTPNetworkApi ttpNetworkApi() {
        return new TTPNetworkApi(remoteHttpClient);
    }

    @Bean
    public TTPOrganizationApi ttpOrganizationApi() throws Exception {
        TTPOrganizationApi ttpOrganizationApi = new TTPOrganizationApi(remoteHttpClient);
        List<RemoteUserEntity> ttpEntities = remoteUserRepo.findAll();
        assert ttpEntities.size() <= 1;
        if (!ttpEntities.isEmpty()) {
            RemoteUserEntity ttpAccount = ttpEntities.get(0);
            ttpOrganizationApi.login(ttpAccount.getOrganizationName(), ttpAccount.getPassword());
        }
        return ttpOrganizationApi;
    }
}
