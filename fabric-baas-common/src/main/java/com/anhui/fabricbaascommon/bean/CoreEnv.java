package com.anhui.fabricbaascommon.bean;

import com.anhui.fabricbaascommon.exception.CertfileException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CoreEnv {
    private String mspId;
    private File mspConfig;

    private String address;
    private File tlsRootCert;

    public MspEnv getMspEnv() {
        return new MspEnv(mspId, mspConfig);
    }

    public TlsEnv getTlsEnv() {
        return new TlsEnv(address, tlsRootCert);
    }

    public void selfAssert() throws CertfileException {
        getMspEnv().assertMspCert();
        getTlsEnv().assertTlsCert();
    }
}
