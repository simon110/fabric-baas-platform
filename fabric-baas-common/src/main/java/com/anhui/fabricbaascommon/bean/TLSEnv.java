package com.anhui.fabricbaascommon.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor
@Data
public class TLSEnv {
    private String address;
    private File tlsRootCert;

    public TLSEnv(String address, File tlsRootCert) {
        assert tlsRootCert.isFile();
        this.address = address;
        this.tlsRootCert = tlsRootCert;
    }
}