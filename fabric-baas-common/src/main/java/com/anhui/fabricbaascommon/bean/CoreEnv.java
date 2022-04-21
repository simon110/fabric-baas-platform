package com.anhui.fabricbaascommon.bean;

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

    public MSPEnv getMSPEnv() {
        return new MSPEnv(mspId, mspConfig);
    }

    public TLSEnv getTLSEnv() {
        return new TLSEnv(address, tlsRootCert);
    }
}
