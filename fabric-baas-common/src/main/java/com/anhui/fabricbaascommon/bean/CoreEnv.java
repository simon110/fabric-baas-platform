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

    public void check() throws CertfileException {
        MspEnv.from(this).check();
        TlsEnv.from(this).check();
    }
}
