package com.anhui.fabricbaascommon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TLSEnv {
    private String address;
    private File tlsRootCertFile;

    public TLSEnv(String address, File tlsRootCertFile) {
        assert tlsRootCertFile.isFile();
        this.address = address;
        this.tlsRootCertFile = tlsRootCertFile;
    }
}

