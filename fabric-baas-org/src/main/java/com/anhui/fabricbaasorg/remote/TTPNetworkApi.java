package com.anhui.fabricbaasorg.remote;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.Network;
import com.anhui.fabricbaasorg.bean.Participation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TTPNetworkApi {
    @Autowired
    private RemoteHttpClient httpClient;

    /**
     * @param networkName    要创建的网络名称
     * @param consortiumName 网络对应的联盟名称
     * @param orderers       当前组织预计提供的Orderer节点的地址
     * @param adminCertZip   当前组织在所创建网络中的管理员证书
     * @return TTP返回的网络的创世区块的二进制数据
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public byte[] createNetwork(String networkName, String consortiumName, List<Node> orderers, File adminCertZip) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("consortiumName", consortiumName);
        data.set("orderers", orderers);
        Map<String, Object> map = new HashMap<>();
        map.put("request", data);
        map.put("adminCertZip", adminCertZip);
        JSONObject response = httpClient.request("/api/v1/network/createNetwork", map);
        return httpClient.download((String) response.get("downloadUrl"));
    }

    /**
     * 从TTP查询已经创建的网络
     *
     * @param networkNameKeyword      网络名称关键词
     * @param organizationNameKeyword 组织名称关键词
     * @return 所有相关的网络信息
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public List<Network> queryNetworks(String networkNameKeyword, String organizationNameKeyword) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkNameKeyword", networkNameKeyword);
        data.set("organizationNameKeyword", organizationNameKeyword);
        JSONObject response = httpClient.request("/api/v1/network/queryNetworks", data);
        return JSONUtil.toList(response.getJSONArray("items"), Network.class);
    }

    /**
     * 当前组织申请加入网络
     *
     * @param networkName  要加入的网络名称
     * @param orderers     当前组织预计提供的Orderer节点的地址
     * @param description  当前组织的名称
     * @param adminCertZip 当前组织在申请加入的网络中的管理员证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void applyParticipation(String networkName, List<Node> orderers, String description, File adminCertZip) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("description", description);
        data.set("orderers", orderers);
        Map<String, Object> map = new HashMap<>();
        map.put("request", data);
        map.put("adminCertZip", adminCertZip);
        httpClient.request("/api/v1/network/applyParticipation", map);
    }

    /**
     * @param networkName 指定网络名称
     * @param status      处理状态（-1表示已拒绝、0表示未处理、1表示已通过）
     * @return 指定网络中的加入申请及处理状态
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public PaginationQueryResult<Participation> queryParticipations(String networkName, int status, int page, int pageSize) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("status", status);
        JSONObject response = httpClient.request("/api/v1/network/queryParticipations", data);
        PaginationQueryResult<Participation> result = new PaginationQueryResult<>();
        result.setItems(JSONUtil.toList(response.getJSONArray("items"), Participation.class));
        result.setTotalPages((Integer) response.get("totalPages"));
        return result;
    }

    /**
     * 处理其他组织加入网络的申请
     *
     * @param networkName      指定网络名称
     * @param organizationName 允许或拒绝加入网络的组织名称
     * @param isAccepted       是否同意
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void handleParticipation(String networkName, String organizationName, boolean isAccepted) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("organizationName", organizationName);
        data.set("isAllowed", isAccepted);
        httpClient.request("/api/v1/network/handleParticipation", data);
    }

    /**
     * 向指定的网络添加Orderer节点的定义
     *
     * @param networkName 网络名称
     * @param orderer     新添加的Orderer的地址信息
     * @return TTP端返回的Orderer节点证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public byte[] addOrderer(String networkName, Node orderer) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/addOrderer", data);
        return httpClient.download((String) response.get("downloadUrl"));
    }

    /**
     * 获取指定Orderer节点的TLS证书
     *
     * @param networkName 网络名称
     * @param orderer     Orderer的地址信息
     * @return TTP端返回的Orderer节点TLS证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public byte[] queryOrdererTlsCert(String networkName, Node orderer) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/queryOrdererTlsCert", data);
        return httpClient.download((String) response.get("downloadUrl"));
    }

    /**
     * 获取指定Orderer节点的证书
     *
     * @param networkName 网络名称
     * @param orderer     Orderer的地址信息
     * @return TTP端返回的Orderer节点证书zip
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public byte[] queryOrdererCert(String networkName, Node orderer) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/queryOrdererCert", data);
        return httpClient.download((String) response.get("downloadUrl"));
    }

    /**
     * 获取指定网络的创世区块
     *
     * @param networkName 网络名称
     * @return 创世区块二进制数据
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public byte[] queryGenesisBlock(String networkName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        JSONObject response = httpClient.request("/api/v1/network/queryGenesisBlock", data);
        return httpClient.download((String) response.get("downloadUrl"));
    }
}
