package com.anhui.fabricbaasorg.service;

import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.bean.TlsEnv;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CaEntity;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.exception.NodeException;
import com.anhui.fabricbaascommon.fabric.CertfileUtils;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.MyFileUtils;
import com.anhui.fabricbaasorg.bean.NetworkOrderer;
import com.anhui.fabricbaasorg.entity.ChannelEntity;
import com.anhui.fabricbaasorg.entity.PeerEntity;
import com.anhui.fabricbaasorg.remote.TTPChannelApi;
import com.anhui.fabricbaasorg.remote.TTPNetworkApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class FabricService {
    @Autowired
    private TTPChannelApi ttpChannelApi;
    @Autowired
    private TTPNetworkApi ttpNetworkApi;
    @Autowired
    private CaClientService caClientService;
    @Autowired
    private PeerService peerService;
    @Autowired
    private ChannelService channelService;

    public CoreEnv buildPeerCoreEnv(String peerName) throws NodeException, CertfileException, CaException {
        // 获取Peer证书
        PeerEntity peer = peerService.findPeerOrThrowEx(peerName);
        File certfileDir = CertfileUtils.getCertfileDir(peer.getCaUsername(), CertfileType.PEER);
        CertfileUtils.assertCertfile(certfileDir);

        CoreEnv coreEnv = new CoreEnv();
        CaEntity caEntity = caClientService.findCaEntityOrThrowEx();
        coreEnv.setAddress(caEntity.getDomain() + ":" + peer.getKubeNodePort());
        coreEnv.setMspConfig(CertfileUtils.getMspDir(caClientService.getRootCertfileDir()));
        coreEnv.setMspId(caEntity.getOrganizationName());
        coreEnv.setTlsRootCert(CertfileUtils.getTlsCaCert(certfileDir));
        return coreEnv;
    }

    public TlsEnv buildOrdererTlsEnv(String channelName) throws Exception {
        ChannelEntity channel = channelService.findChannelOrThrowEx(channelName);
        String networkName = channel.getNetworkName();
        List<NetworkOrderer> orderers = ttpNetworkApi.queryOrderers(networkName);
        // Node selectedOrderer = RandomUtils.select(orderers);
        Node selectedOrderer = orderers.get(0);

        File ordererTlsCert = MyFileUtils.createTempFile("crt");
        ttpNetworkApi.queryOrdererTlsCert(networkName, selectedOrderer, ordererTlsCert);
        return new TlsEnv(selectedOrderer.getAddr(), ordererTlsCert);
    }

    public TlsEnv buildEndorserTlsEnv(String channelName, Node endorser) throws Exception {
        File endorserTlsCert = MyFileUtils.createTempFile("crt");
        ttpChannelApi.queryPeerTlsCert(channelName, endorser, endorserTlsCert);
        return new TlsEnv(endorser.getAddr(), endorserTlsCert);
    }
}
