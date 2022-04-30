package com.anhui.fabricbaasorg.configuration;

import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import com.anhui.fabricbaasorg.remote.RemoteHttpClient;
import com.anhui.fabricbaasorg.repository.RemoteUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RemoteHttpClientConfiguration {
    @Autowired
    private RemoteUserRepo remoteUserRepo;

    /**
     * 如果数据库中存在TTP信息，则根据TTP信息对客户端进行初始化
     *
     * @return 返回一个与远程TTP端通信的HTTP客户端
     */
    @Bean
    public RemoteHttpClient remoteHttpClient() {
        RemoteHttpClient client = new RemoteHttpClient();
        List<RemoteUserEntity> ttpEntities = remoteUserRepo.findAll();
        assert ttpEntities.size() <= 1;
        if (!ttpEntities.isEmpty()) {
            RemoteUserEntity ttpAccount = ttpEntities.get(0);
            client.setBaseUrl(ttpAccount.getApiServer());
        }
        return client;
    }
}
