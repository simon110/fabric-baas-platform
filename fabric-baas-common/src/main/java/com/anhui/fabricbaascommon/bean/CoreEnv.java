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
