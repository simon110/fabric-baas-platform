package com.anhui.fabricbaasorg.remote;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.Network;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import com.anhui.fabricbaasorg.bean.Participation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TTPNetworkApi {
    private final RemoteHttpClient httpClient;

    public TTPNetworkApi(RemoteHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param networkName    要创建的网络名称
     * @param consortiumName 网络对应的联盟名称
     * @param orderers       当前组织预计提供的Orderer节点的地址
     * @param adminCertZip   当前组织在所创建网络中的管理员证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void createNetwork(String networkName, String consortiumName, List<Node> orderers, File adminCertZip, File output) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("consortiumName", consortiumName);
        data.set("orderers", orderers);
        Map<String, Object> map = new HashMap<>();
        map.put("request", data);
        map.put("adminCertZip", adminCertZip);
        JSONObject response = httpClient.request("/api/v1/network/createNetwork", map);
        byte[] bytes = httpClient.download((String) response.get("downloadUrl"));
        FileUtils.writeByteArrayToFile(output, bytes);
    }

    /**
     * 从TTP查询已经创建的网络
     *
     * @param networkNameKeyword      网络名称关键词
     * @param organizationNameKeyword 组织名称关键词
     * @return 所有相关的网络信息
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public List<Network> queryNetworks(String networkNameKeyword, String organizationNameKeyword, int page, int pageSize) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkNameKeyword", networkNameKeyword);
        data.set("organizationNameKeyword", organizationNameKeyword);
        data.set("page", page);
        data.set("pageSize", pageSize);
        JSONObject response = httpClient.request("/api/v1/network/queryNetworks", data);
        return JSONUtil.toList(response.getJSONArray("items"), Network.class);
    }

    /**
     * 当前组织申请加入网络
     *
     * @param networkName  要加入的网络名称
     * @param description  当前组织的名称
     * @param adminCertZip 当前组织在申请加入的网络中的管理员证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void applyParticipation(String networkName, String description, File adminCertZip) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("description", description);
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
        data.set("page", page);
        data.set("pageSize", pageSize);
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
     * @param isAllowed        是否同意
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void handleParticipation(String networkName, String organizationName, boolean isAllowed) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("organizationName", organizationName);
        data.set("allowed", isAllowed);
        httpClient.request("/api/v1/network/handleParticipation", data);
    }

    /**
     * 向指定的网络添加Orderer节点的定义
     *
     * @param networkName 网络名称
     * @param orderer     新添加的Orderer的地址信息
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void addOrderer(String networkName, Node orderer) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/addOrderer", data);
    }

    /**
     * 获取指定Orderer节点的TLS证书
     *
     * @param networkName 网络名称
     * @param orderer     Orderer的地址信息
     * @param output      TTP端返回的Orderer节点TLS证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void queryOrdererTlsCert(String networkName, Node orderer, File output) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/queryOrdererTlsCert", data);
        byte[] bytes = httpClient.download((String) response.get("downloadUrl"));
        FileUtils.writeByteArrayToFile(output, bytes);
    }

    /**
     * 获取指定Orderer节点的证书
     *
     * @param networkName 网络名称
     * @param orderer     Orderer的地址信息
     * @param output      TTP端返回的Orderer节点证书zip
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void queryOrdererCert(String networkName, Node orderer, File output) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("orderer", orderer);
        JSONObject response = httpClient.request("/api/v1/network/queryOrdererCert", data);
        byte[] bytes = httpClient.download((String) response.get("downloadUrl"));
        FileUtils.writeByteArrayToFile(output, bytes);
    }

    /**
     * 获取指定网络的创世区块
     *
     * @param networkName 网络名称
     * @param output      创世区块写出的位置
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void queryGenesisBlock(String networkName, File output) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        JSONObject response = httpClient.request("/api/v1/network/queryGenesisBlock", data);
        byte[] blockData = httpClient.download((String) response.get("downloadUrl"));
        FileUtils.writeByteArrayToFile(output, blockData);
    }

    public List<NetworkOrderer> queryOrderers(String networkName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        JSONObject response = httpClient.request("/api/v1/network/queryOrderers", data);
        return JSONUtil.toList(response.getJSONArray("items"), NetworkOrderer.class);
    }

    public List<String> queryOrganizations(String networkName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        JSONObject response = httpClient.request("/api/v1/network/queryOrganizations", data);
        return JSONUtil.toList(response.getJSONArray("items"), String.class);
    }
}
