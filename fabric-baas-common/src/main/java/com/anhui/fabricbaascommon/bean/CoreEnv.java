package com.anhui.fabricbaascommon.bean;

import java.io.File;

public class CoreEnv {
    private String mspId;
    private File mspConfigPath;

    private String address;
    private File tlsRootCertFile;

    public MSPEnv getMSPEnv() {
        return new MSPEnv(mspId, mspConfigPath);
    }

    public TLSEnv getTLSEnv() {
        return new TLSEnv(address, tlsRootCertFile);
    }
}
