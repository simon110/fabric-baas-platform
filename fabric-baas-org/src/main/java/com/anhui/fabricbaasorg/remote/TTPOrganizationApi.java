package com.anhui.fabricbaasorg.remote;

import cn.hutool.json.JSONObject;
import com.anhui.fabricbaascommon.response.LoginResult;
import org.springframework.stereotype.Component;

@Component
public class TTPOrganizationApi {
    private final RemoteHttpClient remoteHttpClient;

    public TTPOrganizationApi(RemoteHttpClient remoteHttpClient) {
        this.remoteHttpClient = remoteHttpClient;
    }


    /**
     * 调用远程服务端的login接口并调用setHeaderProperty设置Header的Authentication字段为返回token
     * 登录TTP端（即获取返回的Token）
     *
     * @param organizationName 当前组织的名称
     * @param password         当前组织的密码
     * @return TTP端所返回的Token
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public String login(String organizationName, String password) throws Exception {
        JSONObject data = new JSONObject();
        data.set("organizationName", organizationName);
        data.set("password", password);
        JSONObject jsonObject = remoteHttpClient.request("/api/v1/organization/login", data);
        LoginResult result = jsonObject.toBean(LoginResult.class);
        remoteHttpClient.setHeaderProperty("Authorization", result.getToken());
        return result.getToken();
    }

    public String getOrdererOrganizationName() throws Exception {
        JSONObject data = new JSONObject();
        JSONObject response = remoteHttpClient.request("/api/v1/organization/getOrdererOrganizationName", data);
        return (String) response.get("result");
    }
}
