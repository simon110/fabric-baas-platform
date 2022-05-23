package com.anhui.fabricbaasorg.remote;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anhui.fabricbaascommon.bean.ChannelStatus;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.response.PaginationQueryResult;
import com.anhui.fabricbaasorg.bean.ChannelPeer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TTPChannelApi {

    private final RemoteHttpClient httpClient;

    public TTPChannelApi(RemoteHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param networkName 网络名称
     * @param channelName 通道名称
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void createChannel(String networkName, String channelName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("networkName", networkName);
        data.set("channelName", channelName);
        httpClient.request("/api/v1/channel/createChannel", data);
    }

    /**
     * @param channelName 通道名称
     * @param peer        Peer节点的地址信息
     * @param peerCertZip Peer节点的所有证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void joinChannel(String channelName, Node peer, File peerCertZip) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        data.set("peer", peer);
        Map<String, Object> map = new HashMap<>();
        map.put("request", data);
        map.put("peerCertZip", peerCertZip);
        httpClient.request("/api/v1/channel/joinChannel", map);
    }

    /**
     * 将通道中所有组织的邀请码发送到TTP以加入通道
     *
     * @param channelName     通道名称
     * @param invitationCodes 通道中所有组织的邀请码
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void submitInvitationCodes(String channelName, List<String> invitationCodes) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        data.set("invitationCodes", invitationCodes);
        httpClient.request("/api/v1/channel/submitInvitationCodes", data);
    }

    /**
     * 向TTP申请生成加入通道的邀请码
     *
     * @param channelName             通道名称
     * @param invitedOrganizationName 被邀请的组织名称
     * @return BASE64形式的邀请码
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public String generateInvitationCode(String channelName, String invitedOrganizationName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        data.set("organizationName", invitedOrganizationName);
        JSONObject response = httpClient.request("/api/v1/channel/generateInvitationCode", data);
        return (String) response.get("result");
    }

    /**
     * 设置通道中当前组织的锚Peer节点
     *
     * @param channelName 通道名称
     * @param peer        Peer地址信息
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void setAnchorPeer(String channelName, Node peer) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        data.set("peer", peer);
        httpClient.request("/api/v1/channel/setAnchorPeer", data);
    }

    /**
     * 获取指定Peer节点的TLS证书
     *
     * @param channelName 通道名称
     * @param peer        Peer的地址信息
     * @param output      TTP端返回的Peer节点TLS证书
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public void queryPeerTlsCert(String channelName, Node peer, File output) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        data.set("peer", peer);
        JSONObject response = httpClient.request("/api/v1/channel/queryPeerTlsCert", data);
        byte[] bytes = httpClient.download((String) response.get("downloadUrl"));
        FileUtils.writeByteArrayToFile(output, bytes);
    }

    /**
     * 获取指定通道内的所有Peer节点
     *
     * @param channelName 通道名称
     * @return TTP端返回的Peer节点列表
     * @throws Exception 返回请求中任何code!=200的情况都应该抛出异常
     */
    public List<ChannelPeer> queryPeers(String channelName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        JSONObject response = httpClient.request("/api/v1/channel/queryPeers", data);
        return JSONUtil.toList(response.getJSONArray("items"), ChannelPeer.class);
    }

    public Object getChannel(String channelName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        JSONObject response = httpClient.request("/api/v1/channel/getChannel", data);
        return response.get("result");
    }

    public PaginationQueryResult<Object> queryOrganizationChannels(String organizationName, int page, int pageSize) throws Exception {
        JSONObject data = new JSONObject();
        data.set("organizationName", organizationName);
        data.set("page", page);
        data.set("pageSize", pageSize);
        JSONObject response = httpClient.request("/api/v1/channel/queryOrganizationChannels", data);
        List<Object> channels = new ArrayList<>(response.getJSONArray("items"));
        return new PaginationQueryResult<>(response.getInt("totalPages"), channels);
    }

    public ChannelStatus getChannelStatus(String channelName) throws Exception {
        JSONObject data = new JSONObject();
        data.set("channelName", channelName);
        JSONObject response = httpClient.request("/api/v1/channel/getChannelStatus", data);
        return JSONUtil.toBean(response.getJSONObject("result"), ChannelStatus.class);
    }
}
