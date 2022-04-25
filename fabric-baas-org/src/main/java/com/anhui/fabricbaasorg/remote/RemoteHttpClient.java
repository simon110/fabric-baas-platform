package com.anhui.fabricbaasorg.remote;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaasweb.bean.CommonResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Data
public class RemoteHttpClient {
    private String addr;
    private int port;
    private String url;
    private Map<String, List<String>> headers = new HashMap<>();

    public RemoteHttpClient() {
    }

    public void init(String addr, int port) {
        this.addr = addr;
        this.port = port;
        this.url = String.format("http://%s:%d", this.addr, this.port);
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
        String postUrl = this.url + api;
        HttpRequest header = HttpRequest.post(postUrl).header(this.headers);
        if (data instanceof JSONObject) {
            header.header("Content-Type", "application/json");
            header.body(JSONUtil.toJsonStr(data));
        } else if (data instanceof Map) {
            header.header("Content-Type", "multipart/form-data");
            Map<String, Object> map = (Map) data;
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof File) {
                    header.form(key, value);
                } else {
                    header.form(key, JSONUtil.toJsonStr(value));
                }
            }
        } else {
            throw new Exception("非法数据类型：" + data.getClass().getName());
        }
        HttpResponse httpResponse = header.execute();
        CommonResponse commonResponse = JSONUtil.toBean(httpResponse.body(), CommonResponse.class);
        if (httpResponse.getStatus() != 200 || commonResponse.getCode() != 200) {
            throw new Exception(commonResponse.getMessage());
        }
        return (JSONObject) commonResponse.getData();
    }

    /**
     * 根据链接下载
     *
     * @param url 文件地址
     * @return 文件byte数组
     */
    public byte[] download(String url) {
        String portUrl = this.url + url;
        return HttpRequest.get(portUrl).header(this.headers).execute().bodyBytes();
    }
}

