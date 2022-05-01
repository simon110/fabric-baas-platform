package com.anhui.fabricbaasorg.remote;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaasweb.bean.CommonResponse;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RemoteHttpClient {
    private String baseUrl = null;
    private Map<String, List<String>> headers = new HashMap<>();

    public RemoteHttpClient() {
    }

    public void init(String baseUrl) {
        assert this.baseUrl == null;
        this.baseUrl = baseUrl;
    }

    public void setHeaderProperty(String key, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        this.headers.put(key, values);
    }

    /**
     * 普通的请求
     *
     * @param api  请求地址
     * @param data 请求数据
     * @return http响应响应的jSON对象
     */
    @SuppressWarnings("rawtypes")
    public JSONObject request(String api, Object data) throws Exception {
        String postUrl = "http://" + this.baseUrl + api;
        HttpRequest header = HttpRequest.post(postUrl).header(this.headers);
        if (data instanceof JSONObject) {
            header.header("Content-Type", "application/json");
            header.body(JSONUtil.toJsonStr(data));
        } else if (data instanceof Map) {
            header.header("Content-Type", "multipart/form-data");
            Map map = (Map) data;
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof File) {
                    header.form((String) key, value);
                } else {
                    header.form((String) key, JSONUtil.toJsonStr(value));
                }
            }
        } else {
            throw new Exception("非法数据类型：" + data.getClass().getName());
        }
        HttpResponse httpResponse = header.execute();
        System.out.println(httpResponse.body());
        CommonResponse commonResponse = JSONUtil.toBean(httpResponse.body(), CommonResponse.class);
        if (httpResponse.getStatus() != 200 || commonResponse.getCode() != 200) {
            throw new Exception(commonResponse.getMessage());
        }
        Object responseData = commonResponse.getData();
        return responseData instanceof cn.hutool.json.JSONNull ? null : (JSONObject) responseData;
    }

    /**
     * 根据链接下载
     *
     * @param url 文件地址
     * @return 文件byte数组
     */
    public byte[] download(String url) {
        String portUrl = this.baseUrl + url;
        return HttpRequest.get(portUrl).header(this.headers).execute().bodyBytes();
    }
}

