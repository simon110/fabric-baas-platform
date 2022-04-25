package com.anhui.fabricbaasorg.remote;

import cn.hutool.json.JSONObject;
import com.anhui.fabricbaascommon.response.LoginResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TTPOrganizationApi {
    @Autowired
    private RemoteHttpClient remoteHttpClient;

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
        Map<String, Object> map = new HashMap<>();
        map.put("request", data);
        JSONObject jsonObject = remoteHttpClient.request("/api/v1/organization/login", map);
        LoginResult result = jsonObject.toBean(LoginResult.class);
        remoteHttpClient.setHeaderProperty("Authorization", result.getToken());
        return result.getToken();
    }
}
