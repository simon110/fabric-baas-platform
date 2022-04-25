package com.anhui.fabricbaasorg.service;


import com.anhui.fabricbaascommon.exception.DuplicatedOperationException;
import com.anhui.fabricbaasorg.entity.TTPEntity;
import com.anhui.fabricbaasorg.remote.RemoteHttpClient;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.TTPRepo;
import com.anhui.fabricbaasorg.request.TTPInitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TTPService {
    @Autowired
    private TTPRepo ttpRepo;
    @Autowired
    private RemoteHttpClient remoteHttpClient;
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;

    public void init(TTPInitRequest request) throws Exception {
        if (ttpRepo.count() != 0) {
            throw new DuplicatedOperationException("可信第三方的信息已经存在！");
        }

        // 初始化remoteHttpClient
        remoteHttpClient.init(request.getApiServer());
        ttpOrganizationApi.login(request.getOrganizationName(), request.getPassword());

        // 将TTP信息保存到数据库
        TTPEntity ttp = new TTPEntity();
        ttp.setOrganizationName(request.getOrganizationName());
        ttp.setPassword(request.getPassword());
        ttp.setApiServer(request.getApiServer());
        ttpRepo.save(ttp);
    }
}
