package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.MspEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.bean.TlsEnv;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.CaException;
import com.anhui.fabricbaascommon.service.CaClientService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.bean.Peer;
import com.anhui.fabricbaasttp.util.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FabricEnvService {
    @Autowired
    private CaClientService caClientService;

    public CoreEnv buildCoreEnv(MspEnv mspEnv, TlsEnv tlsEnv) {
        return new CoreEnv(
                mspEnv.getMspId(),
                mspEnv.getMspConfig(),
                tlsEnv.getAddress(),
                tlsEnv.getTlsRootCert()
        );
    }

    public CoreEnv buildOrdererCoreEnv(Orderer orderer) throws CaException {
        MspEnv mspEnv = buildOrdererMspEnv();
        TlsEnv tlsEnv = buildOrdererTlsEnv(orderer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }

    public CoreEnv buildPeerCoreEnv(String networkName, String organizationName, Peer peer) {
        MspEnv mspEnv = buildPeerMspEnv(networkName, organizationName);
        TlsEnv tlsEnv = buildPeerTlsEnv(peer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }


    public MspEnv buildOrdererMspEnv() throws CaException {
        MspEnv mspEnv = new MspEnv();
        mspEnv.setMspId(caClientService.getCaOrganizationName());
        mspEnv.setMspConfig(CertfileUtils.getMspDir(caClientService.getRootCertfileDir()));
        return mspEnv;
    }

    public MspEnv buildPeerMspEnv(String networkName, String orgName) {
        String orgCertfileId = IdentifierGenerator.generateCertfileId(networkName, orgName);
        File dir = CertfileUtils.getCertfileDir(orgCertfileId, CertfileType.ADMIN);

        MspEnv mspEnv = new MspEnv();
        mspEnv.setMspId(orgName);
        mspEnv.setMspConfig(CertfileUtils.getMspDir(dir));
        return mspEnv;
    }

    private static TlsEnv buildTlsEnv(Node node, File certfileDir) {
        TlsEnv tlsEnv = new TlsEnv();
        tlsEnv.setAddress(node.getAddr());
        tlsEnv.setTlsRootCert(CertfileUtils.getTlsCaCert(certfileDir));
        return tlsEnv;
    }

    public TlsEnv buildOrdererTlsEnv(Orderer orderer) {
        File ordererCertfileDir = CertfileUtils.getCertfileDir(orderer.getCaUsername(), CertfileType.ORDERER);
        return buildTlsEnv(orderer, ordererCertfileDir);
    }

    public TlsEnv buildPeerTlsEnv(Peer peer) {
        File peerCertfileDir = CertfileUtils.getCertfileDir(peer.getName(), CertfileType.PEER);
        return buildTlsEnv(peer, peerCertfileDir);
    }

}
