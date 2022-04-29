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

    public CoreEnv buildCoreEnvForOrderer(Orderer orderer) throws CaException {
        MspEnv mspEnv = buildMspEnvForOrderer();
        TlsEnv tlsEnv = buildOrdererTlsEnv(orderer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }

    public CoreEnv buildCoreEnvForPeer(String networkName, String orgName, Peer peer) {
        MspEnv mspEnv = buildMspEnvForOrg(networkName, orgName);
        TlsEnv tlsEnv = buildPeerTlsEnv(peer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }

    public MspEnv buildMspEnvForOrderer() throws CaException {
        MspEnv mspEnv = new MspEnv();
        mspEnv.setMspId(caClientService.getCaOrganizationName());
        mspEnv.setMspConfig(new File(caClientService.getRootCertfileDir() + "/msp"));
        return mspEnv;
    }

    public MspEnv buildMspEnvForOrg(String networkName, String orgName) {
        String orgCertfileId = IdentifierGenerator.generateCertfileId(networkName, orgName);
        File dir = CertfileUtils.getCertfileDir(orgCertfileId, CertfileType.ADMIN);

        MspEnv mspEnv = new MspEnv();
        mspEnv.setMspId(orgName);
        mspEnv.setMspConfig(new File(dir.getAbsolutePath() + "/msp"));
        return mspEnv;
    }

    public TlsEnv buildOrdererTlsEnv(Orderer orderer) {
        File certfileDir = CertfileUtils.getCertfileDir(orderer.getCaUsername(), CertfileType.ORDERER);
        return buildTlsEnv(orderer, certfileDir);
    }

    public TlsEnv buildPeerTlsEnv(Peer peer) {
        File certfileDir = CertfileUtils.getCertfileDir(peer.getName(), CertfileType.PEER);
        return buildTlsEnv(peer, certfileDir);
    }

    private static TlsEnv buildTlsEnv(Node node, File certfileDir) {
        TlsEnv tlsEnv = new TlsEnv();
        tlsEnv.setAddress(node.addr());
        tlsEnv.setTlsRootCert(new File(certfileDir.getAbsolutePath() + "/tls/ca.crt"));
        return tlsEnv;
    }
}
