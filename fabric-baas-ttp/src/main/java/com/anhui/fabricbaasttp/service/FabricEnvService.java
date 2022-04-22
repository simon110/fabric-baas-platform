package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.CoreEnv;
import com.anhui.fabricbaascommon.bean.MSPEnv;
import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.bean.TLSEnv;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.CAException;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.bean.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FabricEnvService {
    @Autowired
    private CAService caService;

    public CoreEnv buildCoreEnv(MSPEnv mspEnv, TLSEnv tlsEnv) {
        return new CoreEnv(
                mspEnv.getMspId(),
                mspEnv.getMspConfig(),
                tlsEnv.getAddress(),
                tlsEnv.getTlsRootCert()
        );
    }

    public CoreEnv buildCoreEnvForOrderer(Orderer orderer) throws CAException {
        MSPEnv mspEnv = buildMSPEnvForOrderer();
        TLSEnv tlsEnv = buildTLSEnvForOrderer(orderer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }

    public CoreEnv buildCoreEnvForPeer(String orgName, Peer peer) {
        MSPEnv mspEnv = buildMSPEnvForOrg(orgName);
        TLSEnv tlsEnv = buildTLSEnvForPeer(peer);
        return buildCoreEnv(mspEnv, tlsEnv);
    }

    public MSPEnv buildMSPEnvForOrderer() throws CAException {
        MSPEnv mspEnv = new MSPEnv();
        mspEnv.setMspId(caService.getAdminOrganizationName());
        mspEnv.setMspConfig(new File(caService.getAdminCertfileDir() + "/msp"));
        return mspEnv;
    }

    public MSPEnv buildMSPEnvForOrg(String orgName) {
        File dir = ResourceUtils.getCertfileDir(orgName, CertfileType.ADMIN);

        MSPEnv mspEnv = new MSPEnv();
        mspEnv.setMspId(orgName);
        mspEnv.setMspConfig(new File(dir.getAbsolutePath() + "/msp"));
        return mspEnv;
    }

    public TLSEnv buildTLSEnvForOrderer(Orderer orderer) {
        File homeDir = ResourceUtils.getCertfileDir(orderer.getCaUsername(), CertfileType.ORDERER);
        return buildTLSEnv(orderer, homeDir);
    }

    public TLSEnv buildTLSEnvForPeer(Peer peer) {
        File homeDir = ResourceUtils.getCertfileDir(peer.getName(), CertfileType.PEER);
        return buildTLSEnv(peer, homeDir);
    }

    private static TLSEnv buildTLSEnv(Node node, File certfileDir) {
        TLSEnv tlsEnv = new TLSEnv();
        tlsEnv.setAddress(node.getAddr());
        tlsEnv.setTlsRootCert(new File(certfileDir.getAbsolutePath() + "/tls/ca.crt"));
        return tlsEnv;
    }
}
